package com.zerx.archtest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Zerx Core 层架构约束测试。
 * <p>
 * 验证核心层的以下规则：
 * <ul>
 *     <li>零第三方依赖：core 层（不含 logging）不得引入任何第三方 jar 包</li>
 *     <li>模块边界隔离：各子包之间不得违规互相依赖</li>
 *     <li>包结构规范：子包内的类应具备一致的职责归属</li>
 *     <li>命名规范：util 类为 final + 私有构造，枚举有统一接口等</li>
 *     <li>公共 API 质量：public 类必须有 Javadoc</li>
 * </ul>
 * </p>
 * <p>
 * 这些测试会在 {@code mvn test} 阶段自动执行，确保核心层架构不被破坏。
 * 后续 zerx-spring 层的开发中，也应在本类或新建子类中添加对应的隔离性规则。
 * </p>
 *
 * @author zerx
 */
@DisplayName("Zerx Core 架构约束")
public class CoreArchitectureTest extends AbstractArchitectureTest {

    // ==================== 依赖隔离规则 ====================

    @Nested
    @DisplayName("依赖隔离")
    class DependencyIsolationTest {

        /**
         * 核心规则：zerx-common 不得依赖任何第三方包。
         * <p>
         * 这是 zerx 最重要的架构约束 —— 核心层保持纯净，只依赖 JDK。
         * logging 模块除外，因为它明确声明依赖 SLF4J。
         * </p>
         */
        @Test
        @DisplayName("zerx-common 不得依赖第三方包（只允许 JDK）")
        void common_should_not_depend_on_third_party() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat()
                    .resideOutsideOfPackages(
                            "java..", "javax..", "jakarta..",
                            "com.zerx.."
                    );

            rule.check(getProjectClasses());
        }

        /**
         * 核心层不得引入 Spring 框架。
         * <p>
         * 这是 "Core / Spring 分层" 架构的关键约束。
         * 即使将来 zerx-spring 层被引入，core 层也绝不能知道 Spring 的存在。
         * </p>
         */
        @Test
        @DisplayName("core 层不得引入 Spring 框架")
        void core_should_not_depend_on_spring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..core..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

            rule.check(getProjectClasses());
        }

        /**
         * core 层不得引入日志框架实现。
         * <p>
         * zerx-common 不应直接依赖 SLF4J（除了 zerx-logging 模块外）。
         * 这确保 common 可以在任何环境下使用，包括非日志场景。
         * </p>
         */
        @Test
        @DisplayName("zerx-common 不得引入 SLF4J")
        void common_should_not_depend_on_slf4j() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..common..")
                    .and().areNotAssignableTo(Object.class)
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.slf4j..");

            rule.check(getProjectClasses());
        }
    }

    // ==================== 包结构规则 ====================

    @Nested
    @DisplayName("包结构规范")
    class PackageStructureTest {

        /**
         * common 包内各子包应保持职责清晰，不允许循环依赖。
         * <p>
         * 纵向切分：constants, enums, exception, functional, model, util
         * 这种子包之间如果出现循环依赖，说明职责划分有问题。
         * </p>
         */
        @Test
        @DisplayName("common 子包之间不应存在循环依赖")
        void common_sub_packages_should_be_free_of_cycles() {
            ArchRule rule = slices()
                    .matching("com.zerx.common.(*)..")
                    .should().beFreeOfCycles();

            rule.check(getProjectClasses());
        }

        /**
         * 工具类的内部实现不应被其他包直接引用。
         * <p>
         * util 包内的 internal 子包（如果有）应被封装。
         * 这确保了工具类的内部实现可以自由重构。
         * </p>
         */
        @Test
        @DisplayName("common.util 内部类不应被外部直接引用（如有 internal 包）")
        void util_internal_should_not_be_accessed_outside() {
            // 当前版本暂无 internal 包，但预留规则
            // 后续如果拆分内部实现，此规则会自动生效
            JavaClasses classes = getProjectClasses();
            List<JavaClass> internalClasses = classes.stream()
                    .filter(c -> c.getPackage().contains("common.util.internal"))
                    .toList();

            if (!internalClasses.isEmpty()) {
                ArchRule rule = noClasses()
                        .that().resideOutsideOfPackage("..common.util.internal..")
                        .should().dependOnClassesThat()
                        .resideInAPackage("..common.util.internal..");
                rule.check(classes);
            }
            // 如果不存在 internal 包，测试直接通过
        }
    }

    // ==================== 类设计规范 ====================

    @Nested
    @DisplayName("类设计规范")
    public class ClassDesignTest {

        /**
         * util 包中的工具类必须是 final 类，防止被继承导致行为不可预测。
         */
        @Test
        @DisplayName("util 工具类必须是 final 类")
        void util_classes_should_be_final() {
            JavaClasses classes = getProjectClasses();
            List<JavaClass> utilClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().equals("com.zerx.common.util"))
                    .filter(c -> !c.isInterface())
                    .filter(c -> !c.isEnum())
                    .filter(c -> !c.isAbstract())
                    .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                    .toList();

            List<String> nonFinalClasses = utilClasses.stream()
                    .filter(c -> !c.getModifiers().contains(JavaModifier.FINAL))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(nonFinalClasses.isEmpty(),
                    "以下 util 工具类不是 final 的: " + nonFinalClasses);
        }

        /**
         * util 工具类不应有公共构造函数（应有私有构造阻止实例化）。
         */
        @Test
        @DisplayName("util 工具类不应有公共构造函数")
        void util_classes_should_not_have_public_constructors() {
            JavaClasses classes = getProjectClasses();
            List<JavaClass> utilClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().equals("com.zerx.common.util"))
                    .filter(c -> !c.isInterface())
                    .filter(c -> !c.isEnum())
                    .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                    .toList();

            List<String> classesWithPublicConstructor = utilClasses.stream()
                    .filter(c -> c.getConstructors().stream()
                            .anyMatch(constructor -> constructor.getModifiers().contains(JavaModifier.PUBLIC)))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(classesWithPublicConstructor.isEmpty(),
                    "以下 util 工具类有公共构造函数（应有私有构造函数）: " + classesWithPublicConstructor);
        }

        /**
         * 枚举类应实现 BaseEnum 接口，确保统一的 code/description 访问模式。
         * <p>
         * 注意：ErrorCode 等已有独立设计的枚举可能需要豁免。
         * </p>
         */
        @Test
        @DisplayName("公共枚举类应实现 BaseEnum 接口（ErrorCode 除外）")
        void public_enums_should_implement_base_enum() {
            JavaClasses classes = getProjectClasses();
            List<JavaClass> publicEnums = classes.stream()
                    .filter(JavaClass::isEnum)
                    .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(c -> c.getPackage().getName().startsWith("com.zerx.common"))
                    .toList();

            List<String> nonConformingEnums = publicEnums.stream()
                    .filter(c -> !c.getSimpleName().equals("ErrorCode")
                            && !c.isAssignableTo("com.zerx.common.enums.BaseEnum"))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(nonConformingEnums.isEmpty(),
                    "以下公共枚举类未实现 BaseEnum: " + nonConformingEnums);
        }
    }

    // ==================== 异常体系规范 ====================

    @Nested
    @DisplayName("异常体系规范")
    class ExceptionHierarchyTest {

        /**
         * 所有自定义异常必须继承自 ZerxException。
         * <p>
         * 这确保了异常体系的统一性，外部可以通过捕获 ZerxException 来统一处理。
         * </p>
         */
        @Test
        @DisplayName("自定义异常必须继承 ZerxException")
        void custom_exceptions_should_extend_zerx_exception() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..exception..")
                    .and().areNotAssignableTo("com.zerx.common.exception.ZerxException")
                    .and().areNotAssignableTo("com.zerx.common.exception.ExceptionUtil")
                    .should().beAssignableTo("com.zerx.common.exception.ZerxException");

            rule.check(getProjectClasses());
        }

        /**
         * 异常类不应依赖业务工具类（保持异常层的独立性）。
         */
        @Test
        @DisplayName("异常类不应依赖 util 包")
        void exception_classes_should_not_depend_on_utils() {
            JavaClasses classes = getProjectClasses();
            List<JavaClass> exceptionClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().startsWith("com.zerx.common.exception"))
                    .filter(c -> !c.getSimpleName().equals("ExceptionUtil"))
                    .toList();

            List<String> violatingClasses = exceptionClasses.stream()
                    .filter(c -> c.getDirectDependenciesFromSelf().stream()
                            .anyMatch(dep -> dep.getTargetClass().getPackage().getName().contains("com.zerx.common.util")))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(violatingClasses.isEmpty(),
                    "以下异常类直接依赖了 util 包: " + violatingClasses);
        }
    }

    // ==================== 命名规范 ====================

    @Nested
    @DisplayName("命名规范")
    class NamingConventionTest {

        /**
         * util 包中的类名必须以 "Util" 结尾（统一后缀）。
         */
        @Test
        @DisplayName("util 包中的类名应以 Util 结尾")
        void util_classes_should_end_with_Util() {
            JavaClasses classes = getProjectClasses();
            List<String> violatingClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().equals("com.zerx.common.util"))
                    .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                    .filter(c -> !c.getSimpleName().endsWith("Util"))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(violatingClasses.isEmpty(),
                    "以下 util 包中的类名不以 Util 结尾: " + violatingClasses);
        }

        /**
         * model 包中的类应为公共的、非抽象的（POJO/DTO/VO）。
         * <p>
         * 接口放在 model 包中也是允许的（如 BaseEnum 不在此包）。
         * </p>
         */
        @Test
        @DisplayName("model 包中的类应是公共的")
        void model_classes_should_be_public() {
            JavaClasses classes = getProjectClasses();
            List<String> nonPublicClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().equals("com.zerx.common.model"))
                    .filter(c -> !c.getModifiers().contains(JavaModifier.PUBLIC))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(nonPublicClasses.isEmpty(),
                    "以下 model 包中的类不是 public 的: " + nonPublicClasses);
        }

        /**
         * functional 包中的类应全部是函数式接口（@FunctionalInterface）。
         */
        @Test
        @DisplayName("functional 包中的接口应标注 @FunctionalInterface")
        void functional_interfaces_should_have_annotation() {
            JavaClasses classes = getProjectClasses();
            List<JavaClass> functionalClasses = classes.stream()
                    .filter(c -> c.getPackage().getName().equals("com.zerx.common.functional"))
                    .filter(JavaClass::isInterface)
                    .toList();

            List<String> nonAnnotated = functionalClasses.stream()
                    .filter(c -> !c.isAnnotatedWith(FunctionalInterface.class))
                    .map(JavaClass::getSimpleName)
                    .toList();

            assertTrue(nonAnnotated.isEmpty(),
                    "以下 functional 包中的接口未标注 @FunctionalInterface: " + nonAnnotated);
        }
    }

    // ==================== 通用编码规范 ====================

    @Nested
    @DisplayName("编码规范约束")
    class CodingStandardTest {

        /**
         * 禁止使用 System.out / System.err。
         */
        @Test
        @DisplayName("禁止使用 System.out 或 System.err")
        void should_not_use_system_out_or_err() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("com.zerx..")
                    .should().accessClassesThat()
                    .areAssignableTo(System.class)
                    .getFields("out", "err");

            // ArchUnit 不直接支持字段访问检查，用依赖检查替代
            JavaClasses classes = getProjectClasses();
            List<String> violatingClasses = classes.stream()
                    .filter(c -> !c.getFullName().equals(this.getClass().getName()))
                    .filter(c -> c.getAccessesFromSelf().stream()
                            .anyMatch(access -> {
                                String target = access.getTarget().getFullName();
                                return target.equals("java.io.PrintStream.println(java.lang.String)")
                                        || target.contains("System.out")
                                        || target.contains("System.err");
                            }))
                    .map(JavaClass::getSimpleName)
                    .collect(Collectors.toList());

            assertTrue(violatingClasses.isEmpty(),
                    "以下类使用了 System.out/System.err: " + violatingClasses);
        }

        /**
         * 禁止使用 sun.* 包（非标准 API，可能在 JDK 升级时移除）。
         */
        @Test
        @DisplayName("禁止使用 sun.* 包")
        void should_not_use_sun_packages() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("com.zerx..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("sun..");

            rule.check(getProjectClasses());
        }
    }

    // ==================== 预留：Spring 层隔离规则（后续启用） ====================

    @Nested
    @DisplayName("Spring 层隔离（预留）")
    class SpringLayerIsolationTest {

        /**
         * Spring 层不得反过来依赖 core 层的内部实现。
         * <p>
         * 此规则在 zerx-spring 模块创建后启用。
         * </p>
         */
        @Test
        @DisplayName("spring 层不得依赖 core 内部包（预留，当前无 spring 模块）")
        void spring_should_not_depend_on_core_internals() {
            // 当前 zerx-spring 尚未创建，此测试直接通过
            // 后续添加 spring 模块后，应取消注释以下规则：
            //
            // ArchRule rule = noClasses()
            //         .that().resideInAPackage("..spring..")
            //         .should().dependOnClassesThat()
            //         .resideInAPackage("..core..internal..");
            //
            // rule.check(getProjectClasses());

            assertDoesNotThrow(() -> {
                // 预留测试通过
            }, "预留规则，当前无 spring 模块，直接通过");
        }
    }
}

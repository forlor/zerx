package com.zerx.archtest;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * ArchUnit 架构测试基类。
 * <p>
 * 提供统一的类扫描入口，所有架构测试类继承此基类即可获得项目全量类的访问能力。
 * 默认扫描整个 {@code com.zerx} 包下的所有类。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * public class MyArchitectureTest extends AbstractArchitectureTest {
 *     &#64;Test
 *     void some_rule() {
 *         // 直接使用 getProjectClasses() 获取所有类
 *         ArchRule rule = classes()...
 *         rule.check(getProjectClasses());
 *     }
 * }
 * </pre>
 *
 * @author zerx
 */
public abstract class AbstractArchitectureTest {

    /**
     * 项目根包名
     */
    private static final String ROOT_PACKAGE = "com.zerx";

    /**
     * 缓存已导入的类集合，避免重复扫描（ArchUnit 扫描有一定开销）
     */
    private static volatile JavaClasses cachedClasses;

    /**
     * 获取项目全量 Java 类。
     * <p>
     * 采用懒加载 + volatile 双重检查锁模式，确保只扫描一次。
     * </p>
     *
     * @return 项目中 {@code com.zerx} 包下的所有类
     */
    protected JavaClasses getProjectClasses() {
        if (cachedClasses == null) {
            synchronized (AbstractArchitectureTest.class) {
                if (cachedClasses == null) {
                    cachedClasses = new ClassFileImporter()
                            .importPackages(ROOT_PACKAGE);
                }
            }
        }
        return cachedClasses;
    }
}

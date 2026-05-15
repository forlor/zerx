package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ReflectUtil} 单元测试
 */
@DisplayName("ReflectUtil - 反射工具类测试")
class ReflectUtilTest {

    // ======================== 测试用内部类 ========================

    /** 用于反射测试的父类 */
    static class TestParent {
        public String pubA = "parentA";
        protected String protB = "parentB";
        private String privC = "parentC";
        public static final String STATIC_FIELD = "STATIC";

        public String getPubA() { return pubA; }
        public void setPubA(String pubA) { this.pubA = pubA; }

        protected String getProtB() { return protB; }

        private String getPrivC() { return privC; }

        public String publicMethod() { return "public"; }

        @Override
        public String toString() { return "TestParent"; }
    }

    /** 用于反射测试的子类 */
    static class TestChild extends TestParent {
        public String pubD = "childD";

        public String getPubD() { return pubD; }
        public void setPubD(String pubD) { this.pubD = pubD; }

        @Override
        public String toString() { return "TestChild"; }

        public String childOnlyMethod() { return "child"; }
    }

    /** 测试用抽象类 */
    static abstract class AbstractTest {
        public abstract void abstractMethod();
        public void concreteMethod() {}
    }

    /** 测试用接口 */
    interface TestInterface {
        void interfaceMethod();
    }

    /** 测试用枚举 */
    enum TestEnum { A, B, C }

    /** 测试用注解 */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    @interface TestAnno {
        String value() default "";
    }

    /** 测试用注解类 */
    @TestAnno("class-level")
    static class AnnotatedClass {
        @TestAnno("field-a")
        public String fieldA;

        @TestAnno("field-b")
        private int fieldB;

        @TestAnno("method-annotated")
        public void annotatedMethod() {}

        public void normalMethod() {}
    }

    /** 测试用 Record (JDK 16+) */
    record TestRecord(String name, int age) {}

    // ======================== 字段操作 ========================

    @Test
    @DisplayName("getDeclaredFields - 获取当前类声明字段")
    void getDeclaredFields_normal() {
        Field[] fields = ReflectUtil.getDeclaredFields(TestChild.class);
        // TestChild has pubD + inherited from Object
        assertTrue(fields.length >= 1);
        boolean hasPubD = false;
        for (Field f : fields) {
            if ("pubD".equals(f.getName())) hasPubD = true;
        }
        assertTrue(hasPubD, "Should contain pubD field");
    }

    @Test
    @DisplayName("getDeclaredFields - null 返回空数组")
    void getDeclaredFields_null() {
        Field[] fields = ReflectUtil.getDeclaredFields(null);
        assertEquals(0, fields.length);
    }

    @Test
    @DisplayName("getDeclaredFields - 不包含继承字段")
    void getDeclaredFields_noInherited() {
        Field[] childFields = ReflectUtil.getDeclaredFields(TestChild.class);
        boolean hasPubA = false;
        for (Field f : childFields) {
            if ("pubA".equals(f.getName())) hasPubA = true;
        }
        assertFalse(hasPubA, "Should not contain inherited field pubA");
    }

    @Test
    @DisplayName("getAllFields - 获取类及父类所有字段")
    void getAllFields_normal() {
        List<Field> fields = ReflectUtil.getAllFields(TestChild.class);
        assertTrue(fields.size() >= 4, "Should have pubA, protB, privC, pubD");

        boolean hasPubA = false, hasProtB = false, hasPrivC = false, hasPubD = false;
        for (Field f : fields) {
            switch (f.getName()) {
                case "pubA" -> hasPubA = true;
                case "protB" -> hasProtB = true;
                case "privC" -> hasPrivC = true;
                case "pubD" -> hasPubD = true;
            }
        }
        assertTrue(hasPubA);
        assertTrue(hasProtB);
        assertTrue(hasPrivC);
        assertTrue(hasPubD);
    }

    @Test
    @DisplayName("getAllFields - null 返回空列表")
    void getAllFields_null() {
        assertTrue(ReflectUtil.getAllFields(null).isEmpty());
    }

    @Test
    @DisplayName("getAllFields(Object.class) - 返回空列表")
    void getAllFields_objectClass() {
        assertTrue(ReflectUtil.getAllFields(Object.class).isEmpty());
    }

    @Test
    @DisplayName("getField - 按名称获取当前类字段")
    void getField_currentClass() {
        Field field = ReflectUtil.getField(TestChild.class, "pubD");
        assertNotNull(field);
        assertEquals("pubD", field.getName());
    }

    @Test
    @DisplayName("getField - 按名称获取父类字段")
    void getField_parentClass() {
        Field field = ReflectUtil.getField(TestChild.class, "pubA");
        assertNotNull(field);
        assertEquals("pubA", field.getName());
    }

    @Test
    @DisplayName("getField - 按名称获取私有父类字段")
    void getField_privateParentField() {
        Field field = ReflectUtil.getField(TestChild.class, "privC");
        assertNotNull(field);
        assertEquals("privC", field.getName());
    }

    @Test
    @DisplayName("getField - 不存在的字段返回 null")
    void getField_notFound() {
        assertNull(ReflectUtil.getField(TestChild.class, "nonExistentField"));
    }

    @Test
    @DisplayName("getField - null 类返回 null")
    void getField_nullClass() {
        assertNull(ReflectUtil.getField(null, "pubA"));
    }

    @Test
    @DisplayName("getField - null 字段名返回 null")
    void getField_nullFieldName() {
        assertNull(ReflectUtil.getField(TestChild.class, null));
    }

    @Test
    @DisplayName("getField - 空白字段名返回 null")
    void getField_blankFieldName() {
        assertNull(ReflectUtil.getField(TestChild.class, ""));
        assertNull(ReflectUtil.getField(TestChild.class, "  "));
    }

    @Test
    @DisplayName("getFieldValue - 获取公共字段值")
    void getFieldValue_publicField() {
        TestChild obj = new TestChild();
        String value = ReflectUtil.getFieldValue(obj, "pubA");
        assertEquals("parentA", value);
    }

    @Test
    @DisplayName("getFieldValue - 获取私有字段值")
    void getFieldValue_privateField() {
        TestParent obj = new TestParent();
        String value = ReflectUtil.getFieldValue(obj, "privC");
        assertEquals("parentC", value);
    }

    @Test
    @DisplayName("getFieldValue - 获取子类字段值")
    void getFieldValue_childField() {
        TestChild obj = new TestChild();
        String value = ReflectUtil.getFieldValue(obj, "pubD");
        assertEquals("childD", value);
    }

    @Test
    @DisplayName("getFieldValue - null 对象返回 null")
    void getFieldValue_nullObj() {
        assertNull(ReflectUtil.getFieldValue(null, "pubA"));
    }

    @Test
    @DisplayName("getFieldValue - 不存在的字段返回 null")
    void getFieldValue_notFound() {
        assertNull(ReflectUtil.getFieldValue(new TestChild(), "nonExistent"));
    }

    @Test
    @DisplayName("setFieldValue - 设置公共字段值")
    void setFieldValue_publicField() {
        TestChild obj = new TestChild();
        boolean result = ReflectUtil.setFieldValue(obj, "pubD", "newValue");
        assertTrue(result);
        assertEquals("newValue", obj.pubD);
    }

    @Test
    @DisplayName("setFieldValue - 设置私有字段值")
    void setFieldValue_privateField() {
        TestParent obj = new TestParent();
        boolean result = ReflectUtil.setFieldValue(obj, "privC", "changed");
        assertTrue(result);
        assertEquals("changed", ReflectUtil.getFieldValue(obj, "privC"));
    }

    @Test
    @DisplayName("setFieldValue - 设置父类字段值")
    void setFieldValue_parentField() {
        TestChild obj = new TestChild();
        boolean result = ReflectUtil.setFieldValue(obj, "pubA", "updated");
        assertTrue(result);
        assertEquals("updated", obj.pubA);
    }

    @Test
    @DisplayName("setFieldValue - null 对象返回 false")
    void setFieldValue_nullObj() {
        assertFalse(ReflectUtil.setFieldValue(null, "pubA", "value"));
    }

    @Test
    @DisplayName("setFieldValue - 不存在的字段返回 false")
    void setFieldValue_notFound() {
        assertFalse(ReflectUtil.setFieldValue(new TestChild(), "nonExistent", "value"));
    }

    @Test
    @DisplayName("filterFields - 按条件过滤字段")
    void filterFields_normal() {
        List<Field> publicFields = ReflectUtil.filterFields(TestParent.class,
                f -> Modifier.isPublic(f.getModifiers()));
        for (Field f : publicFields) {
            assertTrue(Modifier.isPublic(f.getModifiers()));
        }
        assertTrue(publicFields.stream().anyMatch(f -> "pubA".equals(f.getName())));
    }

    @Test
    @DisplayName("getNonStaticFields - 获取非静态字段")
    void getNonStaticFields_normal() {
        List<Field> fields = ReflectUtil.getNonStaticFields(TestParent.class);
        for (Field f : fields) {
            assertFalse(Modifier.isStatic(f.getModifiers()));
        }
        assertTrue(fields.stream().anyMatch(f -> "pubA".equals(f.getName())));
        assertFalse(fields.stream().anyMatch(f -> "STATIC_FIELD".equals(f.getName())));
    }

    // ======================== 方法操作 ========================

    @Test
    @DisplayName("getDeclaredMethods - 获取当前类声明方法")
    void getDeclaredMethods_normal() {
        Method[] methods = ReflectUtil.getDeclaredMethods(TestChild.class);
        assertTrue(methods.length > 0);
        boolean hasToString = false;
        for (Method m : methods) {
            if ("toString".equals(m.getName())) hasToString = true;
        }
        assertTrue(hasToString);
    }

    @Test
    @DisplayName("getDeclaredMethods - null 返回空数组")
    void getDeclaredMethods_null() {
        assertEquals(0, ReflectUtil.getDeclaredMethods(null).length);
    }

    @Test
    @DisplayName("getMethod - 按名称和参数类型获取方法")
    void getMethod_withParams() {
        Method method = ReflectUtil.getMethod(TestParent.class, "setPubA", String.class);
        assertNotNull(method);
        assertEquals("setPubA", method.getName());
        assertEquals(1, method.getParameterCount());
    }

    @Test
    @DisplayName("getMethod - 获取父类方法")
    void getMethod_parentMethod() {
        Method method = ReflectUtil.getMethod(TestChild.class, "setPubA", String.class);
        assertNotNull(method);
    }

    @Test
    @DisplayName("getMethod - 不存在的方法返回 null")
    void getMethod_notFound() {
        assertNull(ReflectUtil.getMethod(TestChild.class, "nonExistentMethod"));
    }

    @Test
    @DisplayName("getMethod - null 类返回 null")
    void getMethod_nullClass() {
        assertNull(ReflectUtil.getMethod(null, "setPubA"));
    }

    @Test
    @DisplayName("getMethod - null 方法名返回 null")
    void getMethod_nullMethodName() {
        assertNull(ReflectUtil.getMethod(TestChild.class, null));
    }

    @Test
    @DisplayName("getMethodsByName - 获取所有同名方法（含重载）")
    void getMethodsByName_normal() {
        List<Method> methods = ReflectUtil.getMethodsByName(TestChild.class, "toString");
        assertTrue(methods.size() >= 1);
        for (Method m : methods) {
            assertEquals("toString", m.getName());
        }
    }

    @Test
    @DisplayName("getMethodsByName - null 类返回空列表")
    void getMethodsByName_nullClass() {
        assertTrue(ReflectUtil.getMethodsByName(null, "toString").isEmpty());
    }

    @Test
    @DisplayName("getMethodsByName - null 方法名返回空列表")
    void getMethodsByName_nullName() {
        assertTrue(ReflectUtil.getMethodsByName(TestChild.class, null).isEmpty());
    }

    @Test
    @DisplayName("invokeMethod - 调用无参方法")
    void invokeMethod_noArgs() {
        TestChild obj = new TestChild();
        Object result = ReflectUtil.invokeMethod(obj, "childOnlyMethod");
        assertEquals("child", result);
    }

    @Test
    @DisplayName("invokeMethod - 调用有参方法")
    void invokeMethod_withArgs() {
        TestParent obj = new TestParent();
        Object result = ReflectUtil.invokeMethod(obj, "setPubA",
                new Class[]{String.class}, new Object[]{"newA"});
        assertNull(result); // void method
        assertEquals("newA", obj.pubA);
    }

    @Test
    @DisplayName("invokeMethod - 调用私有方法")
    void invokeMethod_private() {
        TestParent obj = new TestParent();
        Object result = ReflectUtil.invokeMethod(obj, "getPrivC");
        assertEquals("parentC", result);
    }

    @Test
    @DisplayName("invokeMethod - null 对象返回 null")
    void invokeMethod_nullObj() {
        assertNull(ReflectUtil.invokeMethod(null, "toString"));
    }

    @Test
    @DisplayName("invokeMethod - 不存在的方法返回 null")
    void invokeMethod_notFound() {
        assertNull(ReflectUtil.invokeMethod(new TestChild(), "nonExistent"));
    }

    @Test
    @DisplayName("getAllDeclaredMethods - 获取类及父类所有声明方法")
    void getAllDeclaredMethods_normal() {
        List<Method> methods = ReflectUtil.getAllDeclaredMethods(TestChild.class);
        assertTrue(methods.size() > 5);
        boolean hasChildOnly = false, hasParentMethod = false;
        for (Method m : methods) {
            if ("childOnlyMethod".equals(m.getName())) hasChildOnly = true;
            if ("publicMethod".equals(m.getName())) hasParentMethod = true;
        }
        assertTrue(hasChildOnly);
        assertTrue(hasParentMethod);
    }

    @Test
    @DisplayName("getAllDeclaredMethods - null 返回空列表")
    void getAllDeclaredMethods_null() {
        assertTrue(ReflectUtil.getAllDeclaredMethods(null).isEmpty());
    }

    @Test
    @DisplayName("getAllDeclaredMethods(Object.class) - 返回空列表")
    void getAllDeclaredMethods_objectClass() {
        assertTrue(ReflectUtil.getAllDeclaredMethods(Object.class).isEmpty());
    }

    // ======================== 注解操作 ========================

    @Test
    @DisplayName("getClassAnnotation - 获取类上注解")
    void getClassAnnotation_found() {
        TestAnno anno = ReflectUtil.getClassAnnotation(AnnotatedClass.class, TestAnno.class);
        assertNotNull(anno);
        assertEquals("class-level", anno.value());
    }

    @Test
    @DisplayName("getClassAnnotation - 类上无指定注解返回 null")
    void getClassAnnotation_notFound() {
        assertNull(ReflectUtil.getClassAnnotation(TestChild.class, TestAnno.class));
    }

    @Test
    @DisplayName("getClassAnnotation - null 类返回 null")
    void getClassAnnotation_nullClass() {
        assertNull(ReflectUtil.getClassAnnotation(null, TestAnno.class));
    }

    @Test
    @DisplayName("getClassAnnotation - null 注解类返回 null")
    void getClassAnnotation_nullAnno() {
        assertNull(ReflectUtil.getClassAnnotation(AnnotatedClass.class, null));
    }

    @Test
    @DisplayName("getFieldAnnotation - 获取字段上注解")
    void getFieldAnnotation_found() throws NoSuchFieldException {
        Field field = AnnotatedClass.class.getDeclaredField("fieldA");
        TestAnno anno = ReflectUtil.getFieldAnnotation(field, TestAnno.class);
        assertNotNull(anno);
        assertEquals("field-a", anno.value());
    }

    @Test
    @DisplayName("getFieldAnnotation - 字段上无注解返回 null")
    void getFieldAnnotation_notFound() throws NoSuchFieldException {
        // STATIC_FIELD has no @TestAnno
        Field field = TestParent.class.getDeclaredField("STATIC_FIELD");
        assertNull(ReflectUtil.getFieldAnnotation(field, TestAnno.class));
    }

    @Test
    @DisplayName("getFieldAnnotation - null 字段返回 null")
    void getFieldAnnotation_nullField() {
        assertNull(ReflectUtil.getFieldAnnotation(null, TestAnno.class));
    }

    @Test
    @DisplayName("getFieldAnnotation - null 注解类返回 null")
    void getFieldAnnotation_nullAnno() throws NoSuchFieldException {
        Field field = AnnotatedClass.class.getDeclaredField("fieldA");
        assertNull(ReflectUtil.getFieldAnnotation(field, null));
    }

    @Test
    @DisplayName("getMethodAnnotation - 获取方法上注解")
    void getMethodAnnotation_found() throws NoSuchMethodException {
        Method method = AnnotatedClass.class.getDeclaredMethod("annotatedMethod");
        TestAnno anno = ReflectUtil.getMethodAnnotation(method, TestAnno.class);
        assertNotNull(anno);
        assertEquals("method-annotated", anno.value());
    }

    @Test
    @DisplayName("getMethodAnnotation - 方法上无注解返回 null")
    void getMethodAnnotation_notFound() throws NoSuchMethodException {
        Method method = AnnotatedClass.class.getDeclaredMethod("normalMethod");
        assertNull(ReflectUtil.getMethodAnnotation(method, TestAnno.class));
    }

    @Test
    @DisplayName("getMethodAnnotation - null 方法返回 null")
    void getMethodAnnotation_null() {
        assertNull(ReflectUtil.getMethodAnnotation(null, TestAnno.class));
    }

    @Test
    @DisplayName("getFieldsWithAnnotation - 获取带注解的字段列表")
    void getFieldsWithAnnotation_normal() {
        List<Field> fields = ReflectUtil.getFieldsWithAnnotation(AnnotatedClass.class, TestAnno.class);
        assertTrue(fields.size() >= 2);
        boolean hasFieldA = false, hasFieldB = false;
        for (Field f : fields) {
            if ("fieldA".equals(f.getName())) hasFieldA = true;
            if ("fieldB".equals(f.getName())) hasFieldB = true;
        }
        assertTrue(hasFieldA);
        assertTrue(hasFieldB);
    }

    @Test
    @DisplayName("getFieldsWithAnnotation - 无匹配注解返回空列表")
    void getFieldsWithAnnotation_noMatch() {
        List<Field> fields = ReflectUtil.getFieldsWithAnnotation(TestParent.class, TestAnno.class);
        assertTrue(fields.isEmpty());
    }

    @Test
    @DisplayName("getFieldsWithAnnotation - null 类返回空列表")
    void getFieldsWithAnnotation_nullClass() {
        assertTrue(ReflectUtil.getFieldsWithAnnotation(null, TestAnno.class).isEmpty());
    }

    @Test
    @DisplayName("getMethodsWithAnnotation - 获取带注解的方法列表")
    void getMethodsWithAnnotation_normal() {
        List<Method> methods = ReflectUtil.getMethodsWithAnnotation(AnnotatedClass.class, TestAnno.class);
        assertTrue(methods.size() >= 1);
        boolean hasAnnotated = false;
        for (Method m : methods) {
            if ("annotatedMethod".equals(m.getName())) hasAnnotated = true;
        }
        assertTrue(hasAnnotated);
    }

    @Test
    @DisplayName("getMethodsWithAnnotation - 无匹配注解返回空列表")
    void getMethodsWithAnnotation_noMatch() {
        List<Method> methods = ReflectUtil.getMethodsWithAnnotation(TestParent.class, TestAnno.class);
        assertTrue(methods.isEmpty());
    }

    @Test
    @DisplayName("getMethodsWithAnnotation - null 类返回空列表")
    void getMethodsWithAnnotation_nullClass() {
        assertTrue(ReflectUtil.getMethodsWithAnnotation(null, TestAnno.class).isEmpty());
    }

    // ======================== Record 支持 ========================

    @Test
    @DisplayName("isRecord - Record 类返回 true")
    void isRecord_true() {
        assertTrue(ReflectUtil.isRecord(TestRecord.class));
    }

    @Test
    @DisplayName("isRecord - 普通类返回 false")
    void isRecord_false() {
        assertFalse(ReflectUtil.isRecord(TestParent.class));
        assertFalse(ReflectUtil.isRecord(String.class));
    }

    @Test
    @DisplayName("isRecord - null 返回 false")
    void isRecord_null() {
        assertFalse(ReflectUtil.isRecord(null));
    }

    @Test
    @DisplayName("getRecordComponents - Record 类返回组件数组")
    void getRecordComponents_normal() {
        var components = ReflectUtil.getRecordComponents(TestRecord.class);
        assertEquals(2, components.length);
        assertEquals("name", components[0].getName());
        assertEquals("age", components[1].getName());
    }

    @Test
    @DisplayName("getRecordComponents - 非 Record 返回空数组")
    void getRecordComponents_notRecord() {
        assertEquals(0, ReflectUtil.getRecordComponents(TestParent.class).length);
    }

    @Test
    @DisplayName("recordToMap - Record 对象转换为 Map")
    void recordToMap_normal() {
        TestRecord record = new TestRecord("Alice", 30);
        Map<String, Object> map = ReflectUtil.recordToMap(record);
        assertEquals(2, map.size());
        assertEquals("Alice", map.get("name"));
        assertEquals(30, map.get("age"));
    }

    @Test
    @DisplayName("recordToMap - null 返回空 Map")
    void recordToMap_null() {
        assertTrue(ReflectUtil.recordToMap(null).isEmpty());
    }

    @Test
    @DisplayName("recordToMap - 非 Record 返回空 Map")
    void recordToMap_notRecord() {
        assertTrue(ReflectUtil.recordToMap(new TestParent()).isEmpty());
    }

    // ======================== 类信息 ========================

    @Test
    @DisplayName("getSimpleClassName(Class) - 普通类简单名称")
    void getSimpleClassName_class() {
        assertEquals("TestParent", ReflectUtil.getSimpleClassName(TestParent.class));
        assertEquals("String", ReflectUtil.getSimpleClassName(String.class));
        assertEquals("Integer", ReflectUtil.getSimpleClassName(Integer.class));
    }

    @Test
    @DisplayName("getSimpleClassName(Class) - 数组类型")
    void getSimpleClassName_array() {
        assertEquals("String[]", ReflectUtil.getSimpleClassName(String[].class));
        assertEquals("int[]", ReflectUtil.getSimpleClassName(int[].class));
        assertEquals("int[][]", ReflectUtil.getSimpleClassName(int[][].class));
    }

    @Test
    @DisplayName("getSimpleClassName(Class) - null 返回 'null'")
    void getSimpleClassName_nullClass() {
        assertEquals("null", ReflectUtil.getSimpleClassName((Class<?>) null));
    }

    @Test
    @DisplayName("getSimpleClassName(Object) - 对象简单名称")
    void getSimpleClassName_object() {
        assertEquals("TestParent", ReflectUtil.getSimpleClassName(new TestParent()));
        assertEquals("String", ReflectUtil.getSimpleClassName("hello"));
    }

    @Test
    @DisplayName("getSimpleClassName(Object) - null 对象返回 'null'")
    void getSimpleClassName_nullObject() {
        assertEquals("null", ReflectUtil.getSimpleClassName((Object) null));
    }

    @Test
    @DisplayName("isConcreteClass - 普通具体类返回 true")
    void isConcreteClass_true() {
        assertTrue(ReflectUtil.isConcreteClass(TestParent.class));
        assertTrue(ReflectUtil.isConcreteClass(TestChild.class));
        assertTrue(ReflectUtil.isConcreteClass(String.class));
    }

    @Test
    @DisplayName("isConcreteClass - 接口返回 false")
    void isConcreteClass_interface() {
        assertFalse(ReflectUtil.isConcreteClass(TestInterface.class));
    }

    @Test
    @DisplayName("isConcreteClass - 枚举返回 false")
    void isConcreteClass_enum() {
        assertFalse(ReflectUtil.isConcreteClass(TestEnum.class));
    }

    @Test
    @DisplayName("isConcreteClass - Record 返回 false")
    void isConcreteClass_record() {
        assertFalse(ReflectUtil.isConcreteClass(TestRecord.class));
    }

    @Test
    @DisplayName("isConcreteClass - 注解返回 false")
    void isConcreteClass_annotation() {
        assertFalse(ReflectUtil.isConcreteClass(TestAnno.class));
    }

    @Test
    @DisplayName("isConcreteClass - 数组返回 false")
    void isConcreteClass_array() {
        assertFalse(ReflectUtil.isConcreteClass(String[].class));
    }

    @Test
    @DisplayName("isConcreteClass - 抽象类返回 false")
    void isConcreteClass_abstract() {
        assertFalse(ReflectUtil.isConcreteClass(AbstractTest.class));
    }

    @Test
    @DisplayName("isConcreteClass - null 返回 false")
    void isConcreteClass_null() {
        assertFalse(ReflectUtil.isConcreteClass(null));
    }

    @Test
    @DisplayName("newInstance - 创建实例")
    void newInstance_normal() {
        TestParent obj = ReflectUtil.newInstance(TestParent.class);
        assertNotNull(obj);
        assertInstanceOf(TestParent.class, obj);
    }

    @Test
    @DisplayName("newInstance - null 类返回 null")
    void newInstance_null() {
        assertNull(ReflectUtil.newInstance(null));
    }

    @Test
    @DisplayName("newInstance - 无无参构造器返回 null")
    void newInstance_noDefaultConstructor() {
        // Abstract class can't be instantiated
        assertNull(ReflectUtil.newInstance(AbstractTest.class));
    }
}

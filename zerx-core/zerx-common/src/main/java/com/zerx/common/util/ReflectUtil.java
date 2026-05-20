package com.zerx.common.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 反射工具类
 * <p>
 * 提供反射操作的工具方法，包括字段访问、方法调用、注解解析等功能。
 * 利用 JDK 21 的反射增强特性，提供类型安全的访问方式，减少反射调用的样板代码。
 * </p>
 *
 * @author zerx
 */
public final class ReflectUtil {

    /** 私有构造器，防止实例化 */
    private ReflectUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 字段操作 ========================

    /**
     * 获取指定类的所有声明字段（包含所有访问级别）
     *
     * @param clazz 目标类
     * @return 字段数组（不包含继承的字段）
     */
    public static Field[] getDeclaredFields(Class<?> clazz) {
        if (clazz == null) {
            return new Field[0];
        }
        return clazz.getDeclaredFields();
    }

    /**
     * 获取指定类及其所有父类的声明字段
     *
     * @param clazz 目标类
     * @return 字段列表（包含继承的字段）
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return List.of();
        }
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 根据字段名获取字段（支持在父类中查找）
     *
     * @param clazz     目标类
     * @param fieldName 字段名
     * @return 匹配的字段，未找到返回 null
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz == null || StringUtil.isBlank(fieldName)) {
            return null;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取字段的值（自动设置可访问性）
     *
     * @param obj       目标对象
     * @param fieldName 字段名
     * @param <T>       字段值类型
     * @return 字段值，获取失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object obj, String fieldName) {
        if (obj == null || StringUtil.isBlank(fieldName)) {
            return null;
        }
        Field field = getField(obj.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置字段的值（自动设置可访问性）
     *
     * @param obj        目标对象
     * @param fieldName  字段名
     * @param value      要设置的值
     * @return 设置成功返回 true
     */
    public static boolean setFieldValue(Object obj, String fieldName, Object value) {
        if (obj == null || StringUtil.isBlank(fieldName)) {
            return false;
        }
        Field field = getField(obj.getClass(), fieldName);
        if (field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 按条件过滤字段
     *
     * @param clazz     目标类
     * @param predicate 过滤条件
     * @return 符合条件的字段列表
     */
    public static List<Field> filterFields(Class<?> clazz, Predicate<Field> predicate) {
        List<Field> allFields = getAllFields(clazz);
        return allFields.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * 获取所有非静态字段
     *
     * @param clazz 目标类
     * @return 非静态字段列表
     */
    public static List<Field> getNonStaticFields(Class<?> clazz) {
        return filterFields(clazz, f -> !Modifier.isStatic(f.getModifiers()));
    }

    // ======================== 方法操作 ========================

    /**
     * 获取指定类的所有声明方法
     *
     * @param clazz 目标类
     * @return 方法数组
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        if (clazz == null) {
            return new Method[0];
        }
        return clazz.getDeclaredMethods();
    }

    /**
     * 根据方法名和参数类型获取方法（支持在父类中查找）
     *
     * @param clazz          目标类
     * @param methodName     方法名
     * @param parameterTypes 参数类型数组
     * @return 匹配的方法，未找到返回 null
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null || StringUtil.isBlank(methodName)) {
            return null;
        }
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取类中所有指定名称的方法（包括重载方法）
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @return 匹配的方法列表
     */
    public static List<Method> getMethodsByName(Class<?> clazz, String methodName) {
        if (clazz == null || StringUtil.isBlank(methodName)) {
            return List.of();
        }
        return getAllDeclaredMethods(clazz).stream()
                .filter(m -> m.getName().equals(methodName))
                .toList();
    }

    /**
     * 调用对象的无参方法
     *
     * @param obj        目标对象
     * @param methodName 方法名
     * @return 方法返回值
     */
    public static Object invokeMethod(Object obj, String methodName) {
        return invokeMethod(obj, methodName, new Class[0], new Object[0]);
    }

    /**
     * 调用对象的指定方法
     *
     * @param obj             目标对象
     * @param methodName      方法名
     * @param parameterTypes  参数类型数组
     * @param args            参数值数组
     * @return 方法返回值，调用失败返回 null
     */
    public static Object invokeMethod(Object obj, String methodName,
                                       Class<?>[] parameterTypes, Object[] args) {
        if (obj == null || StringUtil.isBlank(methodName)) {
            return null;
        }
        try {
            Method method = getMethod(obj.getClass(), methodName, parameterTypes);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取指定类及其所有父类的声明方法
     *
     * @param clazz 目标类
     * @return 方法列表（包含继承的方法）
     */
    public static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) {
            return List.of();
        }
        List<Method> methods = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            methods.addAll(Arrays.asList(current.getDeclaredMethods()));
            current = current.getSuperclass();
        }
        return methods;
    }

    // ======================== 注解操作 ========================

    /**
     * 获取类上指定类型的注解（支持在父类中查找）
     *
     * @param clazz          目标类
     * @param annotationClass 注解类型
     * @param <A>            注解类型
     * @return 注解实例，未找到返回 null
     */
    public static <A extends Annotation> A getClassAnnotation(Class<?> clazz, Class<A> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return null;
        }
        A annotation = clazz.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        // 递归查找父类
        return getClassAnnotation(clazz.getSuperclass(), annotationClass);
    }

    /**
     * 获取字段上指定类型的注解
     *
     * @param field           目标字段
     * @param annotationClass 注解类型
     * @param <A>            注解类型
     * @return 注解实例，未找到返回 null
     */
    public static <A extends Annotation> A getFieldAnnotation(Field field, Class<A> annotationClass) {
        if (field == null || annotationClass == null) {
            return null;
        }
        return field.getAnnotation(annotationClass);
    }

    /**
     * 获取方法上指定类型的注解
     *
     * @param method          目标方法
     * @param annotationClass 注解类型
     * @param <A>            注解类型
     * @return 注解实例，未找到返回 null
     */
    public static <A extends Annotation> A getMethodAnnotation(Method method, Class<A> annotationClass) {
        if (method == null || annotationClass == null) {
            return null;
        }
        return method.getAnnotation(annotationClass);
    }

    /**
     * 获取类中所有带有指定注解的字段
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @param <A>            注解类型
     * @return 带有指定注解的字段列表
     */
    public static <A extends Annotation> List<Field> getFieldsWithAnnotation(Class<?> clazz,
                                                                              Class<A> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return List.of();
        }
        return getAllFields(clazz).stream()
                .filter(f -> f.getAnnotation(annotationClass) != null)
                .toList();
    }

    /**
     * 获取类中所有带有指定注解的方法
     *
     * @param clazz           目标类
     * @param annotationClass 注解类型
     * @param <A>            注解类型
     * @return 带有指定注解的方法列表
     */
    public static <A extends Annotation> List<Method> getMethodsWithAnnotation(Class<?> clazz,
                                                                               Class<A> annotationClass) {
        if (clazz == null || annotationClass == null) {
            return List.of();
        }
        return getAllDeclaredMethods(clazz).stream()
                .filter(m -> m.getAnnotation(annotationClass) != null)
                .toList();
    }

    // ======================== Record 支持 ========================

    /**
     * 判断一个类是否为 JDK Record 类型
     *
     * @param clazz 目标类
     * @return 是 Record 返回 true
     */
    public static boolean isRecord(Class<?> clazz) {
        return clazz != null && clazz.isRecord();
    }

    /**
     * 获取 Record 类的所有组件
     *
     * @param clazz 目标 Record 类
     * @return Record 组件数组，非 Record 类返回空数组
     */
    public static RecordComponent[] getRecordComponents(Class<?> clazz) {
        if (!isRecord(clazz)) {
            return new RecordComponent[0];
        }
        return clazz.getRecordComponents();
    }

    /**
     * 将 Record 对象转换为 Map（键为组件名，值为组件值）
     *
     * @param record Record 对象
     * @return 组件名到值的映射，非 Record 返回空 Map
     */
    public static Map<String, Object> recordToMap(Object record) {
        if (record == null || !record.getClass().isRecord()) {
            return Map.of();
        }
        RecordComponent[] components = record.getClass().getRecordComponents();
        Map<String, Object> map = new LinkedHashMap<>();
        for (RecordComponent component : components) {
            try {
                Method accessor = component.getAccessor();
                map.put(component.getName(), accessor.invoke(record));
            } catch (Exception e) {
                map.put(component.getName(), null);
            }
        }
        return map;
    }

    // ======================== 类信息 ========================

    /**
     * 获取类的简单名称（处理数组、匿名类等特殊情况）
     *
     * @param clazz 目标类
     * @return 简单类名
     */
    public static String getSimpleClassName(Class<?> clazz) {
        if (clazz == null) {
            return "null";
        }
        if (clazz.isArray()) {
            return getSimpleClassName(clazz.getComponentType()) + "[]";
        }
        return clazz.getSimpleName();
    }

    /**
     * 获取对象的类简单名称
     *
     * @param obj 目标对象
     * @return 简单类名，null 返回 "null"
     */
    public static String getSimpleClassName(Object obj) {
        if (obj == null) {
            return "null";
        }
        return getSimpleClassName(obj.getClass());
    }

    /**
     * 判断一个类是否为普通类（非接口、非枚举、非 Record、非注解）
     *
     * @param clazz 目标类
     * @return 是普通类返回 true
     */
    public static boolean isConcreteClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return !clazz.isInterface()
                && !clazz.isEnum()
                && !clazz.isRecord()
                && !clazz.isAnnotation()
                && !clazz.isArray()
                && !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * 创建指定类的实例（调用无参构造器）
     *
     * @param clazz 目标类
     * @param <T>   实例类型
     * @return 新实例，创建失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}

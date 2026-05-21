package com.zerx.component.oss.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OssUploadLimit} 注解单元测试
 *
 * @author zerx
 */
class OssUploadLimitTest {

    @Test
    @DisplayName("注解属性默认值")
    void shouldHaveCorrectDefaults() throws NoSuchMethodException {
        var method = SampleController.class.getMethod("upload");
        var annotation = method.getAnnotation(OssUploadLimit.class);

        assertArrayEquals(new String[0], annotation.allowedExtensions());
        assertEquals("10MB", annotation.maxSize());
        assertEquals("", annotation.requiredPrefix());
    }

    @Test
    @DisplayName("注解目标和方法级保留策略")
    void shouldHaveCorrectTargetAndRetention() {
        Target target = OssUploadLimit.class.getAnnotation(Target.class);
        Retention retention = OssUploadLimit.class.getAnnotation(Retention.class);

        assertEquals(ElementType.METHOD, target.value()[0]);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @SuppressWarnings("unused")
    static class SampleController {
        @OssUploadLimit
        public void upload() {
        }
    }
}

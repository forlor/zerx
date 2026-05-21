package com.zerx.component.oss.aspect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zerx.component.oss.OssException;
import com.zerx.component.oss.OssUploadRequest;
import com.zerx.component.oss.annotation.OssUploadLimit;

/**
 * {@link OssUploadLimitAspect} 单元测试
 *
 * @author zerx
 */
class OssUploadLimitAspectTest {

    private OssUploadLimitAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new OssUploadLimitAspect();
    }

    @Test
    @DisplayName("无参数时不抛异常")
    void shouldPassWithNoArgs() {
        @OssUploadLimit
        class Stub {}
        OssUploadLimit annotation = Stub.class.getAnnotation(OssUploadLimit.class);
        assertDoesNotThrow(() -> aspect.checkUploadLimit(null, annotation));
    }

    @Test
    @DisplayName("扩展名校验通过")
    void shouldPassExtensionCheck() {
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OssUploadRequest request = new OssUploadRequest(is, "photo.jpg", "avatars");
        Object[] args = {request};

        @OssUploadLimit(allowedExtensions = {"jpg", "png"})
        class Stub {}
        OssUploadLimit annotation = Stub.class.getAnnotation(OssUploadLimit.class);

        assertDoesNotThrow(() -> aspect.checkUploadLimit(
                new TestJoinPoint(args), annotation));
    }

    @Test
    @DisplayName("扩展名校验失败抛异常")
    void shouldFailExtensionCheck() {
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OssUploadRequest request = new OssUploadRequest(is, "malware.exe", "uploads");
        Object[] args = {request};

        @OssUploadLimit(allowedExtensions = {"jpg", "png"})
        class Stub {}
        OssUploadLimit annotation = Stub.class.getAnnotation(OssUploadLimit.class);

        assertThrows(OssException.class, () -> aspect.checkUploadLimit(
                new TestJoinPoint(args), annotation));
    }

    @Test
    @DisplayName("路径前缀校验通过")
    void shouldPassPrefixCheck() {
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OssUploadRequest request = new OssUploadRequest(is, "file.pdf", "documents/");
        Object[] args = {request};

        @OssUploadLimit(requiredPrefix = "documents/")
        class Stub {}
        OssUploadLimit annotation = Stub.class.getAnnotation(OssUploadLimit.class);

        assertDoesNotThrow(() -> aspect.checkUploadLimit(
                new TestJoinPoint(args), annotation));
    }

    @Test
    @DisplayName("路径前缀校验失败抛异常")
    void shouldFailPrefixCheck() {
        InputStream is = new ByteArrayInputStream(new byte[10]);
        OssUploadRequest request = new OssUploadRequest(is, "file.pdf", "avatars/");
        Object[] args = {request};

        @OssUploadLimit(requiredPrefix = "documents/")
        class Stub {}
        OssUploadLimit annotation = Stub.class.getAnnotation(OssUploadLimit.class);

        assertThrows(OssException.class, () -> aspect.checkUploadLimit(
                new TestJoinPoint(args), annotation));
    }

    /**
     * 简易 JoinPoint 实现，仅提供 getArgs()
     */
    private static class TestJoinPoint implements org.aspectj.lang.JoinPoint {
        private final Object[] args;

        TestJoinPoint(Object[] args) {
            this.args = args;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public String getSignature() { return null; }

        @Override
        public Object getTarget() { return null; }

        @Override
        public Object getThis() { return null; }

        @Override
        public org.aspectj.lang.JoinPoint.StaticPart getStaticPart() { return null; }

        @Override
        public String getKind() { return null; }

        @Override
        public Object proceed() { return null; }

        @Override
        public Object proceed(Object[] args) { return null; }

        @Override
        public void setStackTrace(StackTraceElement[] stackTrace) {}

        @Override
        public StackTraceElement[] getStackTrace() { return new StackTraceElement[0]; }

        @Override
        public String toString() { return "TestJoinPoint"; }

        @Override
        public void addSuppressed(Throwable exception) {}

        @Override
        public Throwable[] getSuppressed() { return new Throwable[0]; }

        @Override
        public synchronized Throwable initCause(Throwable cause) { return null; }

        @Override
        public synchronized Throwable getCause() { return null; }

        @Override
        public String getMessage() { return null; }

        @Override
        public synchronized Throwable fillInStackTrace() { return this; }
    }
}

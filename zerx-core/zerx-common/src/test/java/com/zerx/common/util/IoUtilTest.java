package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IoUtil} 单元测试
 */
@DisplayName("IoUtil - IO 工具类测试")
class IoUtilTest {

    // ======================== 流复制 ========================

    @Test
    @DisplayName("copy(InputStream, OutputStream) - 复制流内容")
    void copy_inputToOutput() throws IOException {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long copied = IoUtil.copy(input, output);
        assertEquals(data.length, copied);
        assertArrayEquals(data, output.toByteArray());
    }

    @Test
    @DisplayName("copy(InputStream, OutputStream) - 大数据复制")
    void copy_largeData() throws IOException {
        byte[] data = new byte[8192 * 3]; // 3 buffers
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long copied = IoUtil.copy(input, output);
        assertEquals(data.length, copied);
        assertArrayEquals(data, output.toByteArray());
    }

    @Test
    @DisplayName("copy(InputStream, OutputStream) - 空流")
    void copy_empty() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long copied = IoUtil.copy(input, output);
        assertEquals(0, copied);
        assertEquals(0, output.size());
    }

    @Test
    @DisplayName("copy(Reader, Writer) - 复制字符流")
    void copy_readerToWriter() throws IOException {
        String text = "Reader/Writer copy test with Unicode: 你好世界";
        StringReader reader = new StringReader(text);
        StringWriter writer = new StringWriter();

        long copied = IoUtil.copy(reader, writer);
        assertEquals(text.length(), copied);
        assertEquals(text, writer.toString());
    }

    @Test
    @DisplayName("copy(Reader, Writer) - 空字符流")
    void copy_emptyReader() throws IOException {
        StringReader reader = new StringReader("");
        StringWriter writer = new StringWriter();

        long copied = IoUtil.copy(reader, writer);
        assertEquals(0, copied);
        assertEquals("", writer.toString());
    }

    @Test
    @DisplayName("copy(Reader, Writer) - 大文本复制")
    void copy_largeText() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        String text = sb.toString();
        StringReader reader = new StringReader(text);
        StringWriter writer = new StringWriter();

        long copied = IoUtil.copy(reader, writer);
        assertEquals(text.length(), copied);
        assertEquals(text, writer.toString());
    }

    // ======================== 流转字节/字符串 ========================

    @Test
    @DisplayName("toBytes(InputStream) - 读取流为字节数组")
    void toBytes_normal() throws IOException {
        byte[] data = "To bytes test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(data);

        byte[] result = IoUtil.toBytes(input);
        assertArrayEquals(data, result);
    }

    @Test
    @DisplayName("toBytes(InputStream) - 空流返回空数组")
    void toBytes_empty() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        byte[] result = IoUtil.toBytes(input);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("toBytes(InputStream) - 二进制数据")
    void toBytes_binary() throws IOException {
        byte[] data = {(byte) 0x00, (byte) 0x01, (byte) 0x7F, (byte) 0x80, (byte) 0xFF};
        ByteArrayInputStream input = new ByteArrayInputStream(data);

        byte[] result = IoUtil.toBytes(input);
        assertArrayEquals(data, result);
    }

    @Test
    @DisplayName("toString(InputStream) - 读取流为 UTF-8 字符串")
    void toString_fromInputStream_utf8() throws IOException {
        String text = "Input stream toString test 你好";
        ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        String result = IoUtil.toString(input);
        assertEquals(text, result);
    }

    @Test
    @DisplayName("toString(InputStream) - 空流返回空字符串")
    void toString_fromInputStream_empty() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        String result = IoUtil.toString(input);
        assertEquals("", result);
    }

    @Test
    @DisplayName("toString(InputStream, Charset) - 指定字符集")
    void toString_fromInputStream_withCharset() throws IOException {
        String text = "ISO test";
        ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1));

        String result = IoUtil.toString(input, StandardCharsets.ISO_8859_1);
        assertEquals(text, result);
    }

    @Test
    @DisplayName("toString(Reader) - 读取字符流为字符串")
    void toString_fromReader() throws IOException {
        String text = "Reader toString test";
        StringReader reader = new StringReader(text);

        String result = IoUtil.toString(reader);
        assertEquals(text, result);
    }

    @Test
    @DisplayName("toString(Reader) - 空字符流返回空字符串")
    void toString_fromReader_empty() throws IOException {
        StringReader reader = new StringReader("");

        String result = IoUtil.toString(reader);
        assertEquals("", result);
    }

    @Test
    @DisplayName("toString(Reader) - 包含特殊字符")
    void toString_fromReader_specialChars() throws IOException {
        String text = "Special chars: \t\n\r\\\"'\u00A9\u00AE";
        StringReader reader = new StringReader(text);

        assertEquals(text, IoUtil.toString(reader));
    }

    // ======================== readLines ========================

    @Test
    @DisplayName("readLines(Reader) - 读取多行")
    void readLines_normal() throws IOException {
        String text = "line1\nline2\nline3";
        StringReader reader = new StringReader(text);

        List<String> lines = IoUtil.readLines(reader);
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }

    @Test
    @DisplayName("readLines(Reader) - 空内容返回空列表")
    void readLines_empty() throws IOException {
        StringReader reader = new StringReader("");

        List<String> lines = IoUtil.readLines(reader);
        assertTrue(lines.isEmpty());
    }

    @Test
    @DisplayName("readLines(Reader) - 单行无换行符")
    void readLines_singleLine() throws IOException {
        StringReader reader = new StringReader("only line");

        List<String> lines = IoUtil.readLines(reader);
        assertEquals(1, lines.size());
        assertEquals("only line", lines.get(0));
    }

    @Test
    @DisplayName("readLines(Reader) - 以换行符结尾")
    void readLines_trailingNewline() throws IOException {
        StringReader reader = new StringReader("line1\nline2\n");

        List<String> lines = IoUtil.readLines(reader);
        assertEquals(2, lines.size());
    }

    @Test
    @DisplayName("readLines(BufferedReader) - 直接传入 BufferedReader")
    void readLines_bufferedReader() throws IOException {
        String text = "a\nb\nc";
        BufferedReader reader = new BufferedReader(new StringReader(text));

        List<String> lines = IoUtil.readLines(reader);
        assertEquals(3, lines.size());
    }

    @Test
    @DisplayName("readLines(Reader) - 仅一行空行")
    void readLines_onlyNewline() throws IOException {
        StringReader reader = new StringReader("\n");

        List<String> lines = IoUtil.readLines(reader);
        assertEquals(1, lines.size());
        assertEquals("", lines.get(0));
    }

    // ======================== closeQuietly ========================

    @Test
    @DisplayName("closeQuietly(Closeable) - 正常关闭不抛异常")
    void closeQuietly_normal() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        assertDoesNotThrow(() -> IoUtil.closeQuietly(input));
    }

    @Test
    @DisplayName("closeQuietly(null) - null 不抛异常")
    void closeQuietly_null() {
        assertDoesNotThrow(() -> IoUtil.closeQuietly((Closeable) null));
    }

    @Test
    @DisplayName("closeQuietly(Closeable) - 关闭抛异常被静默处理")
    void closeQuietly_exception() {
        Closeable throwingCloseable = () -> {
            throw new IOException("Expected close exception");
        };
        assertDoesNotThrow(() -> IoUtil.closeQuietly(throwingCloseable));
    }

    @Test
    @DisplayName("closeQuietly(Closeable...) - 多个 Closeable 关闭")
    void closeQuietly_varargs() throws IOException {
        ByteArrayInputStream in1 = new ByteArrayInputStream("a".getBytes());
        ByteArrayInputStream in2 = new ByteArrayInputStream("b".getBytes());
        ByteArrayInputStream in3 = new ByteArrayInputStream("c".getBytes());

        assertDoesNotThrow(() -> IoUtil.closeQuietly(in1, in2, in3));
    }

    @Test
    @DisplayName("closeQuietly(Closeable...) - null 数组不抛异常")
    void closeQuietly_varargsNull() {
        assertDoesNotThrow(() -> IoUtil.closeQuietly((Closeable[]) null));
    }

    @Test
    @DisplayName("closeQuietly(Closeable...) - 数组中含 null 元素不抛异常")
    void closeQuietly_varargsWithNull() {
        ByteArrayInputStream in = new ByteArrayInputStream("data".getBytes());
        assertDoesNotThrow(() -> IoUtil.closeQuietly(in, null, null));
    }

    // ======================== 常量验证 ========================

    @Test
    @DisplayName("DEFAULT_BUFFER_SIZE - 默认缓冲区大小为 8192")
    void defaultBufferSize() {
        assertEquals(8192, IoUtil.DEFAULT_BUFFER_SIZE);
    }

    // ======================== 综合测试 ========================

    @Test
    @DisplayName("copy 后流已关闭 - 验证资源释放")
    void copy_streamsClosed() throws IOException {
        byte[] data = "close test".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        IoUtil.copy(input, output);

        // After copy, stream is consumed; ByteArrayInputStream.read() returns -1 (no IOException)
        assertEquals(-1, input.read());
    }

    @Test
    @DisplayName("toBytes 后流已关闭")
    void toBytes_streamClosed() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        IoUtil.toBytes(input);
        assertEquals(-1, input.read());
    }

    @Test
    @DisplayName("toString(InputStream) 后流已关闭")
    void toString_inputStreamClosed() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("data".getBytes());
        IoUtil.toString(input);
        assertEquals(-1, input.read());
    }
}

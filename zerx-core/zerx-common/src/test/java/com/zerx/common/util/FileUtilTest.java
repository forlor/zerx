package com.zerx.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FileUtil} 单元测试
 */
@DisplayName("FileUtil - 文件工具类测试")
class FileUtilTest {

    @TempDir
    Path tempDir;

    private Path testFile;
    private Path testDir;

    @BeforeEach
    void setUp() throws IOException {
        testFile = tempDir.resolve("test-file.txt");
        testDir = tempDir.resolve("subdir");
    }

    @AfterEach
    void tearDown() {
        // JUnit @TempDir handles cleanup automatically
    }

    // ======================== 文件读取 ========================

    @Test
    @DisplayName("readFileString(String) - 读取文本文件内容")
    void readFileString_fromString() throws IOException {
        Files.writeString(testFile, "Hello World");
        String content = FileUtil.readFileString(testFile.toString());
        assertEquals("Hello World", content);
    }

    @Test
    @DisplayName("readFileString(String, Charset) - 指定字符集读取")
    void readFileString_withCharset() throws IOException {
        Files.writeString(testFile, "测试内容", StandardCharsets.UTF_8);
        String content = FileUtil.readFileString(testFile.toString(), StandardCharsets.UTF_8);
        assertEquals("测试内容", content);
    }

    @Test
    @DisplayName("readFileString(Path) - 从 Path 读取")
    void readFileString_fromPath() throws IOException {
        Files.writeString(testFile, "Path content");
        String content = FileUtil.readFileString(testFile);
        assertEquals("Path content", content);
    }

    @Test
    @DisplayName("readFileString - 读取空文件")
    void readFileString_empty() throws IOException {
        Files.writeString(testFile, "");
        String content = FileUtil.readFileString(testFile);
        assertEquals("", content);
    }

    @Test
    @DisplayName("readFileString - 读取中文内容")
    void readFileString_chinese() throws IOException {
        String content = "这是中文测试内容，包含标点符号！@#￥%……&*（）";
        Files.writeString(testFile, content);
        assertEquals(content, FileUtil.readFileString(testFile));
    }

    @Test
    @DisplayName("readFileString - 读取多行内容")
    void readFileString_multiline() throws IOException {
        String content = "line1\nline2\nline3";
        Files.writeString(testFile, content);
        assertEquals(content, FileUtil.readFileString(testFile));
    }

    @Test
    @DisplayName("readFileBytes(Path) - 读取文件为字节数组")
    void readFileBytes_normal() throws IOException {
        byte[] data = {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xFE};
        Files.write(testFile, data);
        byte[] result = FileUtil.readFileBytes(testFile);
        assertArrayEquals(data, result);
    }

    @Test
    @DisplayName("readFileBytes - 空文件返回空数组")
    void readFileBytes_empty() throws IOException {
        Files.writeString(testFile, "");
        byte[] result = FileUtil.readFileBytes(testFile);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("readFileLines(Path) - 读取文件行列表")
    void readFileLines_normal() throws IOException {
        Files.writeString(testFile, "line1\nline2\nline3");
        List<String> lines = FileUtil.readFileLines(testFile);
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }

    @Test
    @DisplayName("readFileLines(Path, Charset) - 指定字符集读取行")
    void readFileLines_withCharset() throws IOException {
        Files.writeString(testFile, "第一行\n第二行", StandardCharsets.UTF_8);
        List<String> lines = FileUtil.readFileLines(testFile, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
    }

    @Test
    @DisplayName("readFileLines - 空文件返回空列表")
    void readFileLines_empty() throws IOException {
        Files.writeString(testFile, "");
        List<String> lines = FileUtil.readFileLines(testFile);
        assertTrue(lines.isEmpty());
    }

    @Test
    @DisplayName("readFileLines - 单行无换行符")
    void readFileLines_singleLine() throws IOException {
        Files.writeString(testFile, "only one line");
        List<String> lines = FileUtil.readFileLines(testFile);
        assertEquals(1, lines.size());
        assertEquals("only one line", lines.get(0));
    }

    // ======================== 文件写入 ========================

    @Test
    @DisplayName("writeFile(String, String) - 写入字符串到文件")
    void writeFile_stringPath() throws IOException {
        FileUtil.writeFile(testFile.toString(), "Hello Write");
        assertEquals("Hello Write", Files.readString(testFile));
    }

    @Test
    @DisplayName("writeFile(Path, String, Charset, false) - 覆盖写入")
    void writeFile_overwrite() throws IOException {
        Files.writeString(testFile, "Old content");
        FileUtil.writeFile(testFile, "New content", StandardCharsets.UTF_8, false);
        assertEquals("New content", Files.readString(testFile));
    }

    @Test
    @DisplayName("writeFile(Path, String, Charset, true) - 追加写入")
    void writeFile_append() throws IOException {
        Files.writeString(testFile, "First");
        FileUtil.writeFile(testFile, " Second", StandardCharsets.UTF_8, true);
        assertEquals("First Second", Files.readString(testFile));
    }

    @Test
    @DisplayName("writeFile(Path, String, Charset, true) - 多次追加")
    void writeFile_appendMultiple() throws IOException {
        FileUtil.writeFile(testFile, "A", StandardCharsets.UTF_8, true);
        FileUtil.writeFile(testFile, "B", StandardCharsets.UTF_8, true);
        FileUtil.writeFile(testFile, "C", StandardCharsets.UTF_8, true);
        assertEquals("ABC", Files.readString(testFile));
    }

    @Test
    @DisplayName("writeFile(Path, byte[]) - 写入字节数组")
    void writeFile_bytes() throws IOException {
        byte[] data = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
        FileUtil.writeFile(testFile, data);
        assertArrayEquals(data, Files.readAllBytes(testFile));
    }

    @Test
    @DisplayName("writeFile(Path, byte[]) - 覆盖已有文件")
    void writeFile_bytesOverwrite() throws IOException {
        Files.write(testFile, new byte[]{0x01, 0x02, 0x03});
        byte[] newData = {(byte) 0xAA, (byte) 0xBB};
        FileUtil.writeFile(testFile, newData);
        assertArrayEquals(newData, Files.readAllBytes(testFile));
    }

    @Test
    @DisplayName("writeFileLines(Path, Iterable) - 按行写入")
    void writeFileLines_normal() throws IOException {
        List<String> lines = List.of("line1", "line2", "line3");
        FileUtil.writeFileLines(testFile, lines);
        List<String> readLines = Files.readAllLines(testFile);
        assertEquals(lines, readLines);
    }

    @Test
    @DisplayName("writeFileLines - 覆盖已有内容")
    void writeFileLines_overwrite() throws IOException {
        Files.writeString(testFile, "old content");
        List<String> lines = List.of("new1", "new2");
        FileUtil.writeFileLines(testFile, lines);
        assertEquals(lines, Files.readAllLines(testFile));
    }

    @Test
    @DisplayName("writeFile - 自动创建不存在的父目录")
    void writeFile_createParentDirs() throws IOException {
        Path nestedFile = testDir.resolve("nested").resolve("file.txt");
        FileUtil.writeFile(nestedFile, "nested content", StandardCharsets.UTF_8, false);
        assertEquals("nested content", Files.readString(nestedFile));
    }

    @Test
    @DisplayName("writeFileLines - 自动创建父目录")
    void writeFileLines_createParentDirs() throws IOException {
        Path nestedFile = testDir.resolve("nested").resolve("lines.txt");
        FileUtil.writeFileLines(nestedFile, List.of("a", "b"));
        assertEquals(List.of("a", "b"), Files.readAllLines(nestedFile));
    }

    // ======================== 文件系统操作 ========================

    @Test
    @DisplayName("exists(Path) - 文件存在返回 true")
    void exists_true() throws IOException {
        Files.writeString(testFile, "content");
        assertTrue(FileUtil.exists(testFile));
    }

    @Test
    @DisplayName("exists(Path) - 文件不存在返回 false")
    void exists_false() {
        assertFalse(FileUtil.exists(tempDir.resolve("nonexistent.txt")));
    }

    @Test
    @DisplayName("exists(null) - null 返回 false")
    void exists_null() {
        assertFalse(FileUtil.exists(null));
    }

    @Test
    @DisplayName("isFile(Path) - 普通文件返回 true")
    void isFile_true() throws IOException {
        Files.writeString(testFile, "content");
        assertTrue(FileUtil.isFile(testFile));
    }

    @Test
    @DisplayName("isFile(Path) - 目录返回 false")
    void isFile_directory() {
        assertFalse(FileUtil.isFile(tempDir));
    }

    @Test
    @DisplayName("isFile(null) - null 返回 false")
    void isFile_null() {
        assertFalse(FileUtil.isFile(null));
    }

    @Test
    @DisplayName("isDirectory(Path) - 目录返回 true")
    void isDirectory_true() {
        assertTrue(FileUtil.isDirectory(tempDir));
    }

    @Test
    @DisplayName("isDirectory(Path) - 文件返回 false")
    void isDirectory_file() throws IOException {
        Files.writeString(testFile, "content");
        assertFalse(FileUtil.isDirectory(testFile));
    }

    @Test
    @DisplayName("isDirectory(null) - null 返回 false")
    void isDirectory_null() {
        assertFalse(FileUtil.isDirectory(null));
    }

    @Test
    @DisplayName("delete(Path) - 删除存在的文件返回 true")
    void delete_file() throws IOException {
        Files.writeString(testFile, "content");
        assertTrue(FileUtil.delete(testFile));
        assertFalse(Files.exists(testFile));
    }

    @Test
    @DisplayName("delete(Path) - 删除空目录返回 true")
    void delete_emptyDir() throws IOException {
        Files.createDirectories(testDir);
        assertTrue(FileUtil.delete(testDir));
        assertFalse(Files.exists(testDir));
    }

    @Test
    @DisplayName("delete(Path) - 删除不存在的文件返回 false")
    void delete_nonexistent() {
        assertFalse(FileUtil.delete(tempDir.resolve("nonexistent.txt")));
    }

    @Test
    @DisplayName("delete(null) - null 返回 false")
    void delete_null() {
        assertFalse(FileUtil.delete(null));
    }

    @Test
    @DisplayName("mkdirs(Path) - 创建目录")
    void mkdirs_normal() throws IOException {
        Path nested = tempDir.resolve("a").resolve("b").resolve("c");
        FileUtil.mkdirs(nested);
        assertTrue(Files.isDirectory(nested));
    }

    @Test
    @DisplayName("mkdirs(Path) - 已存在的目录不报错")
    void mkdirs_existing() throws IOException {
        assertDoesNotThrow(() -> FileUtil.mkdirs(tempDir));
    }

    @Test
    @DisplayName("mkdirs(null) - null 不报错")
    void mkdirs_null() {
        assertDoesNotThrow(() -> FileUtil.mkdirs(null));
    }

    @Test
    @DisplayName("fileSize(Path) - 返回文件大小")
    void fileSize_normal() throws IOException {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        Files.write(testFile, data);
        assertEquals(data.length, FileUtil.fileSize(testFile));
    }

    @Test
    @DisplayName("fileSize(Path) - 空文件返回 0")
    void fileSize_empty() throws IOException {
        Files.writeString(testFile, "");
        assertEquals(0, FileUtil.fileSize(testFile));
    }

    @Test
    @DisplayName("fileSize(Path) - 不存在的文件返回 -1")
    void fileSize_nonexistent() {
        assertEquals(-1L, FileUtil.fileSize(tempDir.resolve("nonexistent.txt")));
    }

    @Test
    @DisplayName("fileSize(Path) - 目录返回 -1")
    void fileSize_directory() {
        assertEquals(-1L, FileUtil.fileSize(tempDir));
    }

    @Test
    @DisplayName("fileSize(null) - null 返回 -1")
    void fileSize_null() {
        assertEquals(-1L, FileUtil.fileSize(null));
    }

    // ======================== 文件名操作 ========================

    @Test
    @DisplayName("getFileExtension - 普通文件扩展名")
    void getFileExtension_normal() {
        assertEquals("txt", FileUtil.getFileExtension("test.txt"));
        assertEquals("jpg", FileUtil.getFileExtension("photo.jpg"));
        assertEquals("gz", FileUtil.getFileExtension("archive.tar.gz"));
    }

    @Test
    @DisplayName("getFileExtension - 无扩展名")
    void getFileExtension_noExtension() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension("README"));
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension("Makefile"));
    }

    @Test
    @DisplayName("getFileExtension - 以点开头的文件名")
    void getFileExtension_dotStart() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension(".gitignore"));
    }

    @Test
    @DisplayName("getFileExtension - 以点结尾")
    void getFileExtension_dotEnd() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension("file."));
    }

    @Test
    @DisplayName("getFileExtension - null 输入")
    void getFileExtension_null() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension(null));
    }

    @Test
    @DisplayName("getFileExtension - 空字符串")
    void getFileExtension_empty() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension(""));
    }

    @Test
    @DisplayName("getFileExtension - 空白字符串")
    void getFileExtension_blank() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileExtension("   "));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - 普通文件名")
    void getFileNameWithoutExtension_normal() {
        assertEquals("document", FileUtil.getFileNameWithoutExtension("document.pdf"));
        assertEquals("photo", FileUtil.getFileNameWithoutExtension("photo.jpg"));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - 无扩展名")
    void getFileNameWithoutExtension_noExtension() {
        assertEquals("README", FileUtil.getFileNameWithoutExtension("README"));
        assertEquals("Makefile", FileUtil.getFileNameWithoutExtension("Makefile"));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - 多个点")
    void getFileNameWithoutExtension_multipleDots() {
        assertEquals("archive.tar", FileUtil.getFileNameWithoutExtension("archive.tar.gz"));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - null 输入")
    void getFileNameWithoutExtension_null() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileNameWithoutExtension(null));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - 空字符串")
    void getFileNameWithoutExtension_empty() {
        assertEquals(StringUtil.EMPTY, FileUtil.getFileNameWithoutExtension(""));
    }

    @Test
    @DisplayName("getFileNameWithoutExtension - 以点开头的文件")
    void getFileNameWithoutExtension_dotStart() {
        assertEquals(".gitignore", FileUtil.getFileNameWithoutExtension(".gitignore"));
    }

    // ======================== 综合测试 ========================

    @Test
    @DisplayName("写入后读取一致性 - 字符串往返")
    void roundtrip_stringWriteRead() throws IOException {
        String content = "File round-trip test content! 中文测试 12345";
        FileUtil.writeFile(testFile.toString(), content);
        assertEquals(content, FileUtil.readFileString(testFile));
    }

    @Test
    @DisplayName("写入后读取一致性 - 字节往返")
    void roundtrip_bytesWriteRead() throws IOException {
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        FileUtil.writeFile(testFile, data);
        assertArrayEquals(data, FileUtil.readFileBytes(testFile));
    }

    @Test
    @DisplayName("写入后读取一致性 - 行往返")
    void roundtrip_linesWriteRead() throws IOException {
        List<String> lines = List.of("First", "Second", "Third", "Fourth");
        FileUtil.writeFileLines(testFile, lines);
        assertEquals(lines, FileUtil.readFileLines(testFile));
    }
}

package com.zerx.common.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommonConstant}
 */
class CommonConstantTest {

    // ======================== Private constructor ========================

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException")
    void privateConstructor() throws Exception {
        var constructor = CommonConstant.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }

    // ======================== Boolean identifiers ========================

    @Nested
    @DisplayName("Boolean identifiers")
    class BooleanIdentifiers {

        @Test
        @DisplayName("TRUE equals 'true'")
        void trueConstant() {
            assertEquals("true", CommonConstant.TRUE);
        }

        @Test
        @DisplayName("FALSE equals 'false'")
        void falseConstant() {
            assertEquals("false", CommonConstant.FALSE);
        }
    }

    // ======================== Separators ========================

    @Nested
    @DisplayName("Separators")
    class Separators {

        @Test
        @DisplayName("COMMA equals ','")
        void comma() {
            assertEquals(",", CommonConstant.COMMA);
        }

        @Test
        @DisplayName("DOT equals '.'")
        void dot() {
            assertEquals(".", CommonConstant.DOT);
        }

        @Test
        @DisplayName("COLON equals ':'")
        void colon() {
            assertEquals(":", CommonConstant.COLON);
        }

        @Test
        @DisplayName("SEMICOLON equals ';'")
        void semicolon() {
            assertEquals(";", CommonConstant.SEMICOLON);
        }

        @Test
        @DisplayName("PIPE equals '|'")
        void pipe() {
            assertEquals("|", CommonConstant.PIPE);
        }

        @Test
        @DisplayName("HYPHEN equals '-'")
        void hyphen() {
            assertEquals("-", CommonConstant.HYPHEN);
        }

        @Test
        @DisplayName("UNDERSCORE equals '_'")
        void underscore() {
            assertEquals("_", CommonConstant.UNDERSCORE);
        }

        @Test
        @DisplayName("SLASH equals '/'")
        void slash() {
            assertEquals("/", CommonConstant.SLASH);
        }

        @Test
        @DisplayName("BACKSLASH equals '\\'")
        void backslash() {
            assertEquals("\\", CommonConstant.BACKSLASH);
        }

        @Test
        @DisplayName("SPACE equals ' '")
        void space() {
            assertEquals(" ", CommonConstant.SPACE);
        }

        @Test
        @DisplayName("NEWLINE equals '\\n'")
        void newline() {
            assertEquals("\n", CommonConstant.NEWLINE);
        }

        @Test
        @DisplayName("CRLF equals '\\r\\n'")
        void crlf() {
            assertEquals("\r\n", CommonConstant.CRLF);
        }

        @Test
        @DisplayName("CRLF is two characters long")
        void crlfLength() {
            assertEquals(2, CommonConstant.CRLF.length());
        }
    }

    // ======================== Default values ========================

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("DEFAULT_CHARSET equals 'UTF-8'")
        void defaultCharset() {
            assertEquals("UTF-8", CommonConstant.DEFAULT_CHARSET);
        }

        @Test
        @DisplayName("DEFAULT_SORT_FIELD equals 'createTime'")
        void defaultSortField() {
            assertEquals("createTime", CommonConstant.DEFAULT_SORT_FIELD);
        }

        @Test
        @DisplayName("DEFAULT_SORT_DIRECTION equals 'desc'")
        void defaultSortDirection() {
            assertEquals("desc", CommonConstant.DEFAULT_SORT_DIRECTION);
        }
    }

    // ======================== Common identifiers ========================

    @Nested
    @DisplayName("Common identifiers")
    class CommonIdentifiers {

        @Test
        @DisplayName("ROOT_ID equals 0L")
        void rootId() {
            assertEquals(0L, CommonConstant.ROOT_ID);
        }

        @Test
        @DisplayName("ROOT_PARENT_ID equals 0L")
        void rootParentId() {
            assertEquals(0L, CommonConstant.ROOT_PARENT_ID);
        }

        @Test
        @DisplayName("DEFAULT_SORT_ORDER equals 0")
        void defaultSortOrder() {
            assertEquals(0, CommonConstant.DEFAULT_SORT_ORDER);
        }

        @Test
        @DisplayName("STATUS_SUCCESS equals 1")
        void statusSuccess() {
            assertEquals(1, CommonConstant.STATUS_SUCCESS);
        }

        @Test
        @DisplayName("STATUS_FAIL equals 0")
        void statusFail() {
            assertEquals(0, CommonConstant.STATUS_FAIL);
        }

        @Test
        @DisplayName("ALL equals -1")
        void all() {
            assertEquals(-1, CommonConstant.ALL);
        }
    }

    // ======================== HTTP related ========================

    @Nested
    @DisplayName("HTTP related")
    class HttpRelated {

        @Test
        @DisplayName("HEADER_CONTENT_TYPE equals 'Content-Type'")
        void headerContentType() {
            assertEquals("Content-Type", CommonConstant.HEADER_CONTENT_TYPE);
        }

        @Test
        @DisplayName("HEADER_AUTHORIZATION equals 'Authorization'")
        void headerAuthorization() {
            assertEquals("Authorization", CommonConstant.HEADER_AUTHORIZATION);
        }

        @Test
        @DisplayName("HEADER_ACCEPT equals 'Accept'")
        void headerAccept() {
            assertEquals("Accept", CommonConstant.HEADER_ACCEPT);
        }

        @Test
        @DisplayName("HEADER_USER_AGENT equals 'User-Agent'")
        void headerUserAgent() {
            assertEquals("User-Agent", CommonConstant.HEADER_USER_AGENT);
        }

        @Test
        @DisplayName("CONTENT_TYPE_JSON equals 'application/json'")
        void contentTypeJson() {
            assertEquals("application/json", CommonConstant.CONTENT_TYPE_JSON);
        }

        @Test
        @DisplayName("CONTENT_TYPE_FORM equals 'application/x-www-form-urlencoded'")
        void contentTypeForm() {
            assertEquals("application/x-www-form-urlencoded", CommonConstant.CONTENT_TYPE_FORM);
        }

        @Test
        @DisplayName("CONTENT_TYPE_MULTIPART equals 'multipart/form-data'")
        void contentTypeMultipart() {
            assertEquals("multipart/form-data", CommonConstant.CONTENT_TYPE_MULTIPART);
        }

        @Test
        @DisplayName("BEARER_PREFIX equals 'Bearer '")
        void bearerPrefix() {
            assertEquals("Bearer ", CommonConstant.BEARER_PREFIX);
        }
    }

    // ======================== Encoding related ========================

    @Nested
    @DisplayName("Encoding related")
    class EncodingRelated {

        @Test
        @DisplayName("UTF8_BOM equals '\\uFEFF'")
        void utf8Bom() {
            assertEquals("\uFEFF", CommonConstant.UTF8_BOM);
        }

        @Test
        @DisplayName("ASTERISK equals '*'")
        void asterisk() {
            assertEquals("*", CommonConstant.ASTERISK);
        }

        @Test
        @DisplayName("PERCENT equals '%'")
        void percent() {
            assertEquals("%", CommonConstant.PERCENT);
        }

        @Test
        @DisplayName("HASH equals '#'")
        void hash() {
            assertEquals("#", CommonConstant.HASH);
        }

        @Test
        @DisplayName("AT equals '@'")
        void at() {
            assertEquals("@", CommonConstant.AT);
        }
    }

    // ======================== Non-null verification ========================

    @Nested
    @DisplayName("All constants are non-null")
    class NonNullVerification {

        @Test
        @DisplayName("all String constants are non-null")
        void allStringConstantsNonNull() {
            assertNotNull(CommonConstant.TRUE);
            assertNotNull(CommonConstant.FALSE);
            assertNotNull(CommonConstant.COMMA);
            assertNotNull(CommonConstant.DOT);
            assertNotNull(CommonConstant.COLON);
            assertNotNull(CommonConstant.SEMICOLON);
            assertNotNull(CommonConstant.PIPE);
            assertNotNull(CommonConstant.HYPHEN);
            assertNotNull(CommonConstant.UNDERSCORE);
            assertNotNull(CommonConstant.SLASH);
            assertNotNull(CommonConstant.BACKSLASH);
            assertNotNull(CommonConstant.SPACE);
            assertNotNull(CommonConstant.NEWLINE);
            assertNotNull(CommonConstant.CRLF);
            assertNotNull(CommonConstant.DEFAULT_CHARSET);
            assertNotNull(CommonConstant.DEFAULT_SORT_FIELD);
            assertNotNull(CommonConstant.DEFAULT_SORT_DIRECTION);
            assertNotNull(CommonConstant.HEADER_CONTENT_TYPE);
            assertNotNull(CommonConstant.HEADER_AUTHORIZATION);
            assertNotNull(CommonConstant.HEADER_ACCEPT);
            assertNotNull(CommonConstant.HEADER_USER_AGENT);
            assertNotNull(CommonConstant.CONTENT_TYPE_JSON);
            assertNotNull(CommonConstant.CONTENT_TYPE_FORM);
            assertNotNull(CommonConstant.CONTENT_TYPE_MULTIPART);
            assertNotNull(CommonConstant.BEARER_PREFIX);
            assertNotNull(CommonConstant.UTF8_BOM);
            assertNotNull(CommonConstant.ASTERISK);
            assertNotNull(CommonConstant.PERCENT);
            assertNotNull(CommonConstant.HASH);
            assertNotNull(CommonConstant.AT);
        }

        @Test
        @DisplayName("all Integer/Long constants are non-null")
        void allNumericConstantsNonNull() {
            assertNotNull(CommonConstant.ROOT_ID);
            assertNotNull(CommonConstant.ROOT_PARENT_ID);
            assertNotNull(CommonConstant.DEFAULT_SORT_ORDER);
            assertNotNull(CommonConstant.STATUS_SUCCESS);
            assertNotNull(CommonConstant.STATUS_FAIL);
            assertNotNull(CommonConstant.ALL);
        }
    }
}

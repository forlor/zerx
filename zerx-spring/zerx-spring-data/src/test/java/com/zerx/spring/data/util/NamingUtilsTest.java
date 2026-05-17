package com.zerx.spring.data.util;

import com.zerx.spring.data.domain.BaseEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

class NamingUtilsTest {

    @Test
    void camelToSnake_normalCamelCase() {
        Assertions.assertThat(NamingUtils.camelToSnake("sysUser")).isEqualTo("sys_user");
    }

    @Test
    void camelToSnake_singleWord() {
        Assertions.assertThat(NamingUtils.camelToSnake("user")).isEqualTo("user");
    }

    @Test
    void camelToSnake_multipleWords() {
        Assertions.assertThat(NamingUtils.camelToSnake("UserOrder")).isEqualTo("user_order");
    }

    @Test
    void camelToSnake_consecutiveUppercaseWithLowercase() {
        Assertions.assertThat(NamingUtils.camelToSnake("XMLParser")).isEqualTo("xml_parser");
    }

    @Test
    void camelToSnake_allUppercase() {
        Assertions.assertThat(NamingUtils.camelToSnake("ID")).isEqualTo("id");
    }

    @Test
    void camelToSnake_alreadySnake() {
        Assertions.assertThat(NamingUtils.camelToSnake("already_snake")).isEqualTo("already_snake");
    }

    @Test
    void camelToSnake_nullInput() {
        Assertions.assertThat(NamingUtils.camelToSnake(null)).isNull();
    }

    @Test
    void camelToSnake_emptyInput() {
        Assertions.assertThat(NamingUtils.camelToSnake("")).isEmpty();
    }

    @Test
    void camelToSnake_leadingUppercase() {
        Assertions.assertThat(NamingUtils.camelToSnake("HelloWorld")).isEqualTo("hello_world");
    }

    // ---------- inner test entities ----------

    @Table("custom_table")
    static class CustomTableEntity extends BaseEntity {
        public CustomTableEntity() { super(); }
    }

    @Table("")
    static class BlankTableEntity extends BaseEntity {
        public BlankTableEntity() { super(); }
    }

    static class NoTableEntity extends BaseEntity {
        public NoTableEntity() { super(); }
    }

    // ---------- resolveTableName tests ----------

    @Test
    void resolveTableName_withTableAnnotation() {
        Assertions.assertThat(NamingUtils.resolveTableName(CustomTableEntity.class))
                .isEqualTo("custom_table");
    }

    @Test
    void resolveTableName_withoutTableAnnotation() {
        Assertions.assertThat(NamingUtils.resolveTableName(NoTableEntity.class))
                .isEqualTo("no_table_entity");
    }

    @Test
    void resolveTableName_withBlankTableAnnotation() {
        Assertions.assertThat(NamingUtils.resolveTableName(BlankTableEntity.class))
                .isEqualTo("blank_table_entity");
    }
}

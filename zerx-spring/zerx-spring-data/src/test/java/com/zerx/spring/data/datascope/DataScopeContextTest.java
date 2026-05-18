package com.zerx.spring.data.datascope;

import com.zerx.spring.data.datascope.DataScopeHandler.DataScopeSql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeContextTest {

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    private DataScopeSql createSampleSql(String condition) {
        return new DataScopeSql(condition, List.of("param1", "param2"));
    }

    @Nested
    @DisplayName("set and current")
    class SetAndCurrent {

        @Test
        @DisplayName("set and current returns the same value")
        void set_and_current_returnsValue() {
            DataScopeSql sql = createSampleSql("department_id = 1");

            DataScopeContext.set(sql);
            DataScopeSql result = DataScopeContext.current();

            assertThat(result).isNotNull();
            assertThat(result.condition()).isEqualTo("department_id = 1");
            assertThat(result.params()).containsExactly("param1", "param2");
        }
    }

    @Nested
    @DisplayName("current when not set")
    class CurrentWhenNotSet {

        @Test
        @DisplayName("current returns null when not set")
        void current_returnsNull_whenNotSet() {
            DataScopeSql result = DataScopeContext.current();

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("clear removes the stored value")
        void clear_removesValue() {
            DataScopeSql sql = createSampleSql("status = 'ACTIVE'");

            DataScopeContext.set(sql);
            assertThat(DataScopeContext.current()).isNotNull();

            DataScopeContext.clear();

            assertThat(DataScopeContext.current()).isNull();
        }
    }

    @Nested
    @DisplayName("multiple sets")
    class MultipleSets {

        @Test
        @DisplayName("multiple sets — last one wins")
        void multiple_sets_lastWins() {
            DataScopeSql first = createSampleSql("region = 'NA'");
            DataScopeSql second = new DataScopeSql("role = 'ADMIN'", List.of("admin_param"));

            DataScopeContext.set(first);
            DataScopeContext.set(second);

            DataScopeSql result = DataScopeContext.current();

            assertThat(result).isNotNull();
            assertThat(result.condition()).isEqualTo("role = 'ADMIN'");
            assertThat(result.params()).containsExactly("admin_param");
        }
    }
}

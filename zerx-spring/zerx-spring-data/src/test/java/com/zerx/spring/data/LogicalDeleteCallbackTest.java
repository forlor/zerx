package com.zerx.spring.data;

import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.logicaldelete.LogicalDelete;
import com.zerx.spring.data.logicaldelete.LogicalDeleteCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogicalDeleteCallback 单元测试。
 * <p>
 * 使用 Mockito + Logback ListAppender 验证回调的日志输出行为。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class LogicalDeleteCallbackTest {

    private final LogicalDeleteCallback callback = new LogicalDeleteCallback();

    @Mock
    private org.springframework.data.relational.core.conversion.MutableAggregateChange<BaseEntity> aggregateChange;

    // ======================== 逻辑删除实体物理删除 → WARN 日志 ========================

    @Test
    void physicalDelete_onLogicalDeleteEntity_logsWarn() {
        // 设置 Logback ListAppender 捕获日志
        Logger logger = (Logger) LoggerFactory.getLogger(LogicalDeleteCallback.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // 模拟对逻辑删除实体的物理删除
            LogicalDeleteEntity entity = new LogicalDeleteEntity();
            entity.setId(42L);

            BaseEntity result = callback.onBeforeDelete(entity, aggregateChange);

            // 验证返回值不变
            assertSame(entity, result);

            // 验证日志输出
            List<ILoggingEvent> events = listAppender.list;
            boolean hasWarnLog = events.stream()
                    .anyMatch(e -> e.getLevel() == ch.qos.logback.classic.Level.WARN
                            && e.getFormattedMessage().contains("Logical delete entity")
                            && e.getFormattedMessage().contains("LogicalDeleteEntity[id=42]")
                            && e.getFormattedMessage().contains("physically deleted")
                            && e.getFormattedMessage().contains("LogicalDeleteService.deleteById()"));
            assertTrue(hasWarnLog, "Expected WARN log for physical delete on @LogicalDelete entity");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    // ======================== 非逻辑删除实体物理删除 → 无特殊日志 ========================

    @Test
    void physicalDelete_onNonLogicalDeleteEntity_noWarnLog() {
        // 设置 Logback ListAppender 捕获日志
        Logger logger = (Logger) LoggerFactory.getLogger(LogicalDeleteCallback.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // 模拟对非逻辑删除实体的物理删除
            NonLogicalDeleteEntity entity = new NonLogicalDeleteEntity();
            entity.setId(1L);

            BaseEntity result = callback.onBeforeDelete(entity, aggregateChange);

            // 验证返回值不变
            assertSame(entity, result);

            // 验证没有 WARN 日志
            List<ILoggingEvent> events = listAppender.list;
            boolean hasWarnLog = events.stream()
                    .anyMatch(e -> e.getLevel() == ch.qos.logback.classic.Level.WARN
                            && e.getFormattedMessage().contains("Logical delete entity"));
            assertFalse(hasWarnLog, "Should NOT log WARN for non-@LogicalDelete entity");
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    // ======================== null 实体 → 安全返回 ========================

    @Test
    void nullEntity_returnsNull() {
        BaseEntity result = callback.onBeforeDelete(null, aggregateChange);
        assertNull(result);
    }

    // ======================== 测试实体 ========================

    /**
     * 标注了 @LogicalDelete 的测试实体
     */
    @LogicalDelete
    static class LogicalDeleteEntity extends BaseEntity {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    /**
     * 未标注 @LogicalDelete 的测试实体
     */
    static class NonLogicalDeleteEntity extends BaseEntity {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}

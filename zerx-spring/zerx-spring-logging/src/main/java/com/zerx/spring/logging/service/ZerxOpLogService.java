package com.zerx.spring.logging.service;

import com.zerx.spring.logging.event.ZerxOpLogEvent;

/**
 * 操作日志持久化 SPI 接口
 * <p>
 * 业务层实现此接口并注册为 Spring Bean，框架自动注入到切面中。
 * 如果未提供实现，操作日志仅输出到日志（不持久化）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Service
 * public class DatabaseOpLogService implements ZerxOpLogService {
 *     @Autowired private OpLogRepository repository;
 *
 *     @Override
 *     @Async
 *     public void save(ZerxOpLogEvent event) {
 *         repository.save(toEntity(event));
 *     }
 * }</pre>
 *
 * @author zerx
 */
public interface ZerxOpLogService {

    /**
     * 保存操作日志
     * <p>建议使用 {@code @Async} 异步执行，避免影响主线程性能。</p>
     *
     * @param event 操作日志事件
     */
    void save(ZerxOpLogEvent event);
}

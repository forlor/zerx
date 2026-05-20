package com.zerx.spring.cache.warmer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;

/**
 * 缓存预热触发器 — 应用启动完成后自动执行所有 {@link CacheWarmer}。
 * <p>
 * 按 {@link CacheWarmer#order()} 升序执行。预热失败不影响应用启动。
 * </p>
 *
 * @author zerx
 */
public class CacheWarmUpRunner implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(CacheWarmUpRunner.class);

    private final List<CacheWarmer> warmers;

    public CacheWarmUpRunner(List<CacheWarmer> warmers) {
        this.warmers = warmers;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (warmers == null || warmers.isEmpty()) {
            return;
        }

        warmers.sort(Comparator.comparingInt(CacheWarmer::order));
        LOG.info("Starting cache warm-up: {} warmer(s) registered", warmers.size());

        for (CacheWarmer warmer : warmers) {
            try {
                long start = System.currentTimeMillis();
                warmer.warmUp();
                long elapsed = System.currentTimeMillis() - start;
                LOG.info("Cache warmer [{}] completed in {}ms", warmer.getClass().getSimpleName(), elapsed);
            } catch (Exception e) {
                LOG.warn("Cache warmer [{}] failed: {}", warmer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

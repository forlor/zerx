package com.zerx.spring.cache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Zerx 缓存配置属性。
 * <p>
 * 统一管理缓存类型、键前缀、多级缓存配置等。
 * 通过 {@code zerx.cache.*} 前缀在 application.yml 中配置。
 * </p>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.cache")
public class ZerxCacheProperties {

    /**
     * 缓存类型：caffeine（本地）/ redis（分布式）/ multilevel（多级）
     */
    private CacheType type = CacheType.CAFFEINE;

    /**
     * 缓存键统一前缀
     */
    private String keyPrefix = "zerx:";

    /**
     * 是否启用缓存
     */
    private boolean enabled = true;

    /**
     * 空值缓存时间（防穿透），0 表示不缓存空值
     */
    private Duration nullValueTtl = Duration.ofMinutes(5);

    /**
     * 防击穿：互斥锁等待超时时间
     */
    private Duration lockTimeout = Duration.ofSeconds(5);

    /**
     * 多级缓存配置
     */
    private Multilevel multilevel = new Multilevel();

    /**
     * Caffeine 本地缓存配置
     */
    private CaffeineSpec caffeine = new CaffeineSpec();

    // --- getter/setter ---

    public CacheType getType() { return type; }
    public void setType(CacheType type) { this.type = type; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Duration getNullValueTtl() { return nullValueTtl; }
    public void setNullValueTtl(Duration nullValueTtl) { this.nullValueTtl = nullValueTtl; }

    public Duration getLockTimeout() { return lockTimeout; }
    public void setLockTimeout(Duration lockTimeout) { this.lockTimeout = lockTimeout; }

    public Multilevel getMultilevel() { return multilevel; }
    public void setMultilevel(Multilevel multilevel) { this.multilevel = multilevel; }

    public CaffeineSpec getCaffeine() { return caffeine; }
    public void setCaffeine(CaffeineSpec caffeine) { this.caffeine = caffeine; }

    // ======================== 缓存类型枚举 ========================

    public enum CacheType {
        /** 仅本地缓存（Caffeine） */
        CAFFEINE,
        /** 仅分布式缓存（Redis） */
        REDIS,
        /** 多级缓存（L1 Caffeine + L2 Redis） */
        MULTILEVEL
    }

    // ======================== 多级缓存配置 ========================

    public static class Multilevel {
        private L1Config l1 = new L1Config();
        private L2Config l2 = new L2Config();

        public L1Config getL1() { return l1; }
        public void setL1(L1Config l1) { this.l1 = l1; }
        public L2Config getL2() { return l2; }
        public void setL2(L2Config l2) { this.l2 = l2; }
    }

    public static class L1Config {
        private int maxSize = 1000;
        private Duration expireAfterWrite = Duration.ofMinutes(5);

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        public Duration getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
    }

    public static class L2Config {
        private Duration expireAfterWrite = Duration.ofMinutes(30);

        public Duration getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
    }

    // ======================== Caffeine 配置 ========================

    public static class CaffeineSpec {
        private int maxSize = 10000;
        private Duration expireAfterWrite = Duration.ofMinutes(10);
        private Duration expireAfterAccess = Duration.ofMinutes(30);
        private boolean recordStats = false;

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
        public Duration getExpireAfterWrite() { return expireAfterWrite; }
        public void setExpireAfterWrite(Duration expireAfterWrite) { this.expireAfterWrite = expireAfterWrite; }
        public Duration getExpireAfterAccess() { return expireAfterAccess; }
        public void setExpireAfterAccess(Duration expireAfterAccess) { this.expireAfterAccess = expireAfterAccess; }
        public boolean isRecordStats() { return recordStats; }
        public void setRecordStats(boolean recordStats) { this.recordStats = recordStats; }
    }
}

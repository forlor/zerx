package com.zerx.spring.cache.autoconfigure;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.aspect.ZerxCacheAspect;
import com.zerx.spring.cache.config.CacheInvalidationListener;
import com.zerx.spring.cache.manager.ZerxCacheManager;
import com.zerx.spring.cache.ops.CacheOpsImpl;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import com.zerx.spring.cache.store.CaffeineCacheStore;
import com.zerx.spring.cache.store.MultilevelCacheStore;
import com.zerx.spring.cache.store.RedisCacheStore;

/**
 * Zerx 缓存自动配置。
 * <p>
 * 根据 {@code zerx.cache.type} 配置自动注册对应实现：
 * <ul>
 *     <li>{@code CAFFEINE} — 本地缓存（默认）</li>
 *     <li>{@code REDIS} — Redis 分布式缓存</li>
 *     <li>{@code MULTILEVEL} — 多级缓存（L1 Caffeine + L2 Redis）</li>
 * </ul>
 * </p>
 * <p>
 * 自动注册：
 * <ul>
 *     <li>{@link CacheStore} — 底层 KV 存储实现</li>
 *     <li>{@link CacheOps} — Cache-Aside 高级封装（基于 CacheStore）</li>
 *     <li>{@link ZerxCacheManager} — Spring Cache 抽象适配</li>
 *     <li>{@link ZerxCacheAspect} — 声明式注解 AOP 切面</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
@AutoConfiguration
@EnableConfigurationProperties(ZerxCacheProperties.class)
@ConditionalOnProperty(prefix = "zerx.cache", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ZerxCacheAutoConfiguration {

    // ======================== Caffeine 配置 ========================

    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type",
            havingValue = "CAFFEINE", matchIfMissing = true)
    static class CaffeineCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(CacheStore.class)
        public CacheStore caffeineCacheStore(ZerxCacheProperties properties) {
            return new CaffeineCacheStore(properties);
        }
    }

    // ======================== Redis 配置 ========================

    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type", havingValue = "REDIS")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zerxCacheRedisTemplate")
        public RedisTemplate<String, Object> zerxCacheRedisTemplate(
                RedisConnectionFactory connectionFactory,
                ZerxCacheProperties properties) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(RedisSerializer.string());
            template.setHashKeySerializer(RedisSerializer.string());

            RedisSerializer<Object> valueSerializer = resolveValueSerializer(properties);
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(valueSerializer);
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        @ConditionalOnMissingBean(CacheStore.class)
        public CacheStore redisCacheStore(RedisTemplate<String, Object> redisTemplate,
                                          ZerxCacheProperties properties) {
            return new RedisCacheStore(redisTemplate, properties);
        }
    }

    // ======================== 多级缓存配置 ========================

    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type", havingValue = "MULTILEVEL")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class MultilevelCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zerxCacheRedisTemplate")
        public RedisTemplate<String, Object> zerxCacheRedisTemplate(
                RedisConnectionFactory connectionFactory,
                ZerxCacheProperties properties) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(RedisSerializer.string());
            template.setHashKeySerializer(RedisSerializer.string());

            RedisSerializer<Object> valueSerializer = resolveValueSerializer(properties);
            template.setValueSerializer(valueSerializer);
            template.setHashValueSerializer(valueSerializer);
            template.afterPropertiesSet();
            return template;
        }

        @Bean("l1CacheStore")
        @ConditionalOnMissingBean(name = "l1CacheStore")
        public CacheStore l1CacheStore(ZerxCacheProperties properties) {
            return new CaffeineCacheStore(properties);
        }

        @Bean("l2CacheStore")
        @ConditionalOnMissingBean(name = "l2CacheStore")
        public CacheStore l2CacheStore(RedisTemplate<String, Object> redisTemplate,
                                       ZerxCacheProperties properties) {
            return new RedisCacheStore(redisTemplate, properties);
        }

        @Bean
        @ConditionalOnMissingBean(CacheStore.class)
        public CacheStore multilevelCacheStore(
                @Qualifier("l1CacheStore") CacheStore l1,
                @Qualifier("l2CacheStore") CacheStore l2,
                StringRedisTemplate stringRedisTemplate,
                ZerxCacheProperties properties) {
            return new MultilevelCacheStore(l1, l2, stringRedisTemplate, properties);
        }

        /**
         * 注册 Redis Pub/Sub 消息监听容器。
         * <p>
         * 监听 {@code zerx:cache:invalidate:*} 频道，收到消息后清除本地 L1 缓存。
         * </p>
         */
        @Bean
        @ConditionalOnMissingBean(name = "cacheInvalidationContainer")
        public RedisMessageListenerContainer cacheInvalidationContainer(
                RedisConnectionFactory connectionFactory,
                @Qualifier("l1CacheStore") CacheStore l1Cache) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            CacheInvalidationListener listener = new CacheInvalidationListener(l1Cache);
            container.addMessageListener(listener,
                    new ChannelTopic(com.zerx.spring.cache.CacheConstants.INVALIDATION_CHANNEL_PREFIX + "*"));
            return container;
        }
    }

    // ======================== 通用 Bean ========================

    /**
     * CacheOps — Cache-Aside 高级封装。
     * <p>
     * 在所有缓存类型下都可用，包装 CacheStore 提供防穿透/防击穿能力。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(CacheOps.class)
    public CacheOps cacheOps(CacheStore cacheStore, ZerxCacheProperties properties) {
        return new CacheOpsImpl(cacheStore, properties.getNullValueTtl(), properties.getLockTimeout());
    }

    /**
     * ZerxCacheManager — Spring Cache 抽象适配。
     * <p>
     * 使 Spring 原生 {@code @Cacheable} 注解也能使用 Zerx 缓存。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ZerxCacheManager.class)
    public ZerxCacheManager zerxCacheManager(CacheStore cacheStore,
                                              ZerxCacheProperties properties) {
        return new ZerxCacheManager(cacheStore, properties);
    }

    /**
     * ZerxCacheAspect — 声明式缓存注解 AOP 切面。
     */
    @Bean
    @ConditionalOnMissingBean(ZerxCacheAspect.class)
    public ZerxCacheAspect zerxCacheAspect(CacheOps cacheOps, CacheStore cacheStore,
                                           ZerxCacheProperties properties) {
        return new ZerxCacheAspect(cacheOps, cacheStore, properties);
    }

    // ======================== Micrometer 指标绑定 ========================

    /**
     * 当 Caffeine 开启 recordStats 且 Micrometer MeterRegistry 存在时，
     * 自动将 Caffeine 统计信息绑定到 MeterRegistry。
     * 仅在 CacheStore 实例为 CaffeineCacheStore 时激活。
     */
    @Configuration
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnBean({io.micrometer.core.instrument.MeterRegistry.class, CaffeineCacheStore.class})
    @ConditionalOnProperty(prefix = "zerx.cache.caffeine", name = "record-stats", havingValue = "true")
    static class CaffeineMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zerxCaffeineCacheMetrics")
        @SuppressWarnings({"rawtypes", "unchecked"})
        public io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics zerxCaffeineCacheMetrics(
                CaffeineCacheStore caffeineStore) {
            return new io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics(
                    caffeineStore.getNativeCache(), "zerx-caffeine-cache", java.util.Collections.emptyList());
        }
    }

    // ======================== 序列化策略 ========================

    /**
     * 根据配置选择 Redis 值序列化器。
     * <ul>
     *     <li>{@code JACKSON}：GenericJackson2JsonRedisSerializer（带 {@code @class} 类型头）</li>
     *     <li>{@code JSON}：GenericJackson2JsonRedisSerializer（关闭类型头写入，体积更小）</li>
     * </ul>
     */
    static RedisSerializer<Object> resolveValueSerializer(ZerxCacheProperties properties) {
        return switch (properties.getSerializer()) {
            case JACKSON -> new GenericJackson2JsonRedisSerializer();
            case JSON -> {
                // 无类型头序列化：使用纯 ObjectMapper，不激活 DefaultTyping
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.findAndRegisterModules();
                yield new GenericJackson2JsonRedisSerializer(mapper);
            }
        };
    }
}

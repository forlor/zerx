package com.zerx.spring.cache.autoconfigure;

import com.zerx.spring.cache.config.CacheInvalidationListener;
import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.cache.ops.CaffeineCacheOps;
import com.zerx.spring.cache.ops.MultilevelCacheOps;
import com.zerx.spring.cache.ops.RedisCacheOps;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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

/**
 * Zerx 缓存自动配置。
 * <p>
 * 根据 {@code zerx.cache.type} 配置自动注册对应的 {@link CacheOps} 实现：
 * <ul>
 *     <li>{@code CAFFEINE} — 本地缓存（默认）</li>
 *     <li>{@code REDIS} — Redis 分布式缓存</li>
 *     <li>{@code MULTILEVEL} — 多级缓存（L1 Caffeine + L2 Redis）</li>
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

    /**
     * 注册 Caffeine 本地缓存的 CacheOps 实现。
     * <p>
     * 当 {@code zerx.cache.type=CAFFEINE} 或未指定时激活（matchIfMissing）。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type",
            havingValue = "CAFFEINE", matchIfMissing = true)
    static class CaffeineCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(CacheOps.class)
        public CacheOps caffeineCacheOps(ZerxCacheProperties properties) {
            return new CaffeineCacheOps(properties);
        }
    }

    /**
     * Redis 分布式缓存的 CacheOps 实现。
     * <p>
     * 当 {@code zerx.cache.type=REDIS} 且 Redis 在 classpath 时激活。
     * 自动创建带 JSON 序列化的 {@link RedisTemplate}。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type", havingValue = "REDIS")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zerxCacheRedisTemplate")
        public RedisTemplate<String, Object> zerxCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(RedisSerializer.string());
            template.setHashKeySerializer(RedisSerializer.string());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        @Bean
        @ConditionalOnMissingBean(CacheOps.class)
        public CacheOps redisCacheOps(RedisTemplate<String, Object> redisTemplate,
                                       ZerxCacheProperties properties) {
            return new RedisCacheOps(redisTemplate, properties);
        }
    }

    /**
     * 多级缓存配置（L1 Caffeine + L2 Redis）。
     * <p>
     * 当 {@code zerx.cache.type=MULTILEVEL} 且 Redis 在 classpath 时激活。
     * 包含 Redis Pub/Sub 失效监听，保证多节点 L1 缓存一致性。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.cache", name = "type", havingValue = "MULTILEVEL")
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class MultilevelCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "zerxCacheRedisTemplate")
        public RedisTemplate<String, Object> zerxCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(RedisSerializer.string());
            template.setHashKeySerializer(RedisSerializer.string());
            template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
            template.afterPropertiesSet();
            return template;
        }

        @Bean("l1Cache")
        @ConditionalOnMissingBean(name = "l1Cache")
        public CacheOps l1Cache(ZerxCacheProperties properties) {
            return new CaffeineCacheOps(properties);
        }

        @Bean("l2Cache")
        @ConditionalOnMissingBean(name = "l2Cache")
        public CacheOps l2Cache(RedisTemplate<String, Object> redisTemplate,
                                ZerxCacheProperties properties) {
            return new RedisCacheOps(redisTemplate, properties);
        }

        @Bean
        @ConditionalOnMissingBean(CacheOps.class)
        public CacheOps multilevelCacheOps(@Qualifier("l1Cache") CacheOps l1,
                                           @Qualifier("l2Cache") CacheOps l2,
                                           StringRedisTemplate stringRedisTemplate,
                                           ZerxCacheProperties properties) {
            return new MultilevelCacheOps(l1, l2, stringRedisTemplate, properties);
        }

        /**
         * 注册 Redis Pub/Sub 消息监听容器，用于接收其他节点发布的缓存失效消息。
         */
        @Bean
        @ConditionalOnMissingBean(name = "cacheInvalidationContainer")
        public RedisMessageListenerContainer cacheInvalidationContainer(
                RedisConnectionFactory connectionFactory,
                @Qualifier("l1Cache") CacheOps l1Cache) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            CacheInvalidationListener listener = new CacheInvalidationListener(l1Cache);
            container.addMessageListener(listener, new ChannelTopic("zerx:cache:invalidate:*"));
            return container;
        }
    }
}

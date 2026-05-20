package com.zerx.spring.cache.config;

import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.CacheConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class CacheInvalidationListenerTest {

    private CacheStore l1Cache;
    private CacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        l1Cache = mock(CacheStore.class);
        listener = new CacheInvalidationListener(l1Cache);
    }

    @Test
    void onMessage_evictsL1CacheWithCorrectKey() {
        String key = "zerx:user:123";
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache).evict(key);
    }

    @Test
    void onMessage_evictsL1CacheWithPrefix() {
        String key = "zerx:user:";
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict_prefix".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache).evictByPrefix(key);
        verify(l1Cache, never()).evict(anyString());
    }

    @Test
    void onMessage_handlesChannelWithFullPrefix() {
        String key = "zerx:user:123";
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache).evict(key);
    }

    @Test
    void onMessage_handlesPrefixButEmptyKey() {
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        // Empty key should not call evict
        verify(l1Cache, never()).evict(anyString());
    }

    @Test
    void onMessage_doesNotEvictForUnexpectedChannelFormat() {
        String channel = "some:other:channel:user:123";
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache, never()).evict(anyString());
    }

    @Test
    void onMessage_doesNotThrowForUnexpectedChannel() {
        String channel = "unknown:channel";
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "data".getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void onMessage_catchesExceptionFromL1CacheEvict() {
        String key = "zerx:fail:key";
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        doThrow(new RuntimeException("cache error")).when(l1Cache).evict(key);

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void onMessage_handlesEmptyBody() {
        String key = "zerx:user:456";
        String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                new byte[0]);

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();

        verify(l1Cache).evict(key);
    }

    @Test
    void extractKeyFromChannel_returnsKeyAfterStrippingPrefix() throws Exception {
        Method extractMethod = CacheInvalidationListener.class.getDeclaredMethod("extractKeyFromChannel", String.class);
        extractMethod.setAccessible(true);

        String result = (String) extractMethod.invoke(listener, "zerx:cache:invalidate:zerx:user:123");

        assertThat(result).isEqualTo("zerx:user:123");
    }
}

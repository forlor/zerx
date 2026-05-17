package com.zerx.spring.cache;

import com.zerx.spring.cache.config.CacheInvalidationListener;
import com.zerx.spring.cache.ops.CacheOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link CacheInvalidationListener} 单元测试
 *
 * @author zerx
 */
class CacheInvalidationListenerTest {

    private static final String CHANNEL_PREFIX = "zerx:cache:invalidate:";

    private CacheOps l1Cache;
    private CacheInvalidationListener listener;

    @BeforeEach
    void setUp() {
        l1Cache = mock(CacheOps.class);
        listener = new CacheInvalidationListener(l1Cache);
    }

    @Test
    void onMessage_evictsL1CacheWithCorrectKey() {
        String key = "user:123";
        String channel = CHANNEL_PREFIX + key;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache).evict(key);
    }

    @Test
    void onMessage_handlesChannelWithFullPrefix() {
        String channel = "zerx:cache:invalidate:user:123";
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache).evict("user:123");
    }

    @Test
    void onMessage_handlesChannelWithPrefixButEmptyKey() {
        // Channel is exactly the prefix with no key after it
        String channel = CHANNEL_PREFIX;
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        // extractKeyFromChannel returns "" which is not null, so evict should be called with ""
        verify(l1Cache).evict("");
    }

    @Test
    void onMessage_doesNotEvictForUnexpectedChannelFormat() {
        String channel = "some:other:channel:user:123";
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(l1Cache, never()).evict("user:123");
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
        String channel = CHANNEL_PREFIX + "fail:key";
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                "evict".getBytes(StandardCharsets.UTF_8));

        doThrow(new RuntimeException("cache error")).when(l1Cache).evict("fail:key");

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();
    }

    @Test
    void onMessage_handlesNullMessageBody() {
        String channel = CHANNEL_PREFIX + "user:456";
        // Body is empty byte array (DefaultMessage doesn't accept null)
        var message = new DefaultMessage(
                channel.getBytes(StandardCharsets.UTF_8),
                new byte[0]);

        assertThatCode(() -> listener.onMessage(message, null))
                .doesNotThrowAnyException();

        verify(l1Cache).evict("user:456");
    }

    @Test
    void extractKeyFromChannel_returnsKeyAfterStrippingPrefix() throws Exception {
        Method extractMethod = CacheInvalidationListener.class.getDeclaredMethod("extractKeyFromChannel", String.class);
        extractMethod.setAccessible(true);

        String result = (String) extractMethod.invoke(listener, "zerx:cache:invalidate:user:123");

        assertThat(result).isEqualTo("user:123");
    }
}

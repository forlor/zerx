package com.zerx.spring.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BloomFilter} 接口契约测试。
 * <p>
 * 使用 {@link com.zerx.spring.cache.bloom.CaffeineBloomFilter} 作为实现来验证接口契约。
 * </p>
 *
 * @author zerx
 */
@DisplayName("BloomFilter Interface Tests")
class BloomFilterTest {

    @Test
    @DisplayName("mightContain returns false for non-existent elements")
    void mightContain_nonExistent_returnsFalse() {
        BloomFilter<String> bloom = BloomFilters.create(1000, 0.01);

        assertFalse(bloom.mightContain("non-existent-key"));
    }

    @Test
    @DisplayName("mightContain returns true after put")
    void mightContain_afterPut_returnsTrue() {
        BloomFilter<String> bloom = BloomFilters.create(1000);

        bloom.put("user:1");

        assertTrue(bloom.mightContain("user:1"));
    }

    @Test
    @DisplayName("mightContain accepts null safely (returns false)")
    void mightContain_null_returnsFalse() {
        BloomFilter<String> bloom = BloomFilters.create(100);

        assertFalse(bloom.mightContain(null));
    }

    @Test
    @DisplayName("put null is a no-op")
    void put_null_isNoOp() {
        BloomFilter<String> bloom = BloomFilters.create(100);

        assertDoesNotThrow(() -> bloom.put(null));
    }

    @Test
    @DisplayName("putAll adds multiple elements")
    void putAll_addsMultiple() {
        BloomFilter<String> bloom = BloomFilters.create(1000);

        bloom.putAll(java.util.List.of("key1", "key2", "key3"));

        assertTrue(bloom.mightContain("key1"));
        assertTrue(bloom.mightContain("key2"));
        assertTrue(bloom.mightContain("key3"));
    }

    @Test
    @DisplayName("putAll with null is a no-op")
    void putAll_null_isNoOp() {
        BloomFilter<String> bloom = BloomFilters.create(100);

        assertDoesNotThrow(() -> bloom.putAll(null));
    }

    @Test
    @DisplayName("expectedInsertions and fpp return configured values")
    void metadata_returnsConfiguredValues() {
        BloomFilter<String> bloom = BloomFilters.create(5000, 0.03);

        assertEquals(5000, bloom.expectedInsertions());
        assertEquals(0.03, bloom.fpp());
    }

    @Test
    @DisplayName("create with default fpp uses 0.01")
    void create_defaultFpp() {
        BloomFilter<String> bloom = BloomFilters.create(1000);

        assertEquals(0.01, bloom.fpp());
    }

    @Test
    @DisplayName("false positive rate is within acceptable bounds")
    void falsePositiveRate_withinBounds() {
        int expectedInsertions = 10_000;
        double targetFpp = 0.01;
        BloomFilter<Integer> bloom = BloomFilters.create(expectedInsertions, targetFpp);

        // Insert elements 0 ~ 9999
        for (int i = 0; i < expectedInsertions; i++) {
            bloom.put(i);
        }

        // Test with elements that were NOT inserted
        int falsePositives = 0;
        int testCount = 10_000;
        for (int i = expectedInsertions; i < expectedInsertions + testCount; i++) {
            if (bloom.mightContain(i)) {
                falsePositives++;
            }
        }

        double actualFpp = (double) falsePositives / testCount;
        // Allow 3x the target FPP for statistical variance
        assertTrue(actualFpp < targetFpp * 3,
                "False positive rate too high: " + actualFpp + " (target: " + targetFpp + ")");
    }

    @Test
    @DisplayName("no false negatives")
    void noFalseNegatives() {
        BloomFilter<String> bloom = BloomFilters.create(1000);

        for (int i = 0; i < 1000; i++) {
            bloom.put("key-" + i);
        }

        for (int i = 0; i < 1000; i++) {
            assertTrue(bloom.mightContain("key-" + i),
                    "False negative detected for key-" + i);
        }
    }
}

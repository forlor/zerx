package com.zerx.spring.cache.bloom;

import java.util.concurrent.locks.StampedLock;

import com.zerx.spring.cache.BloomFilter;

/**
 * 基于位图（bit array）的本地布隆过滤器实现。
 * <p>
 * 使用双重哈希（Murmur128 派生两个哈希值，再线性组合生成 k 个哈希索引）
 * 实现概率性判空，提供 O(k) 的 {@link #mightContain} 和 {@link #put} 操作。
 * 通过 {@link StampedLock} 保证线程安全。
 * </p>
 *
 * <h3>性能特征：</h3>
 * <ul>
 *     <li>内存占用：约 {@code expectedInsertions * (-ln(fpp) / (ln2)^2 / 8)} bytes</li>
 *     <li>查询复杂度：O(k)，k 为哈希函数数量（通常 5~7 个）</li>
 *     <li>线程安全：StampedLock 读写分离，高并发读性能优秀</li>
 * </ul>
 *
 * <h3>参数选择参考：</h3>
 * <table>
 *     <tr><th>expectedInsertions</th><th>fpp</th><th>位图大小</th><th>哈希次数</th></tr>
 *     <tr><td>10,000</td><td>0.01</td><td>~12 KB</td><td>7</td></tr>
 *     <tr><td>100,000</td><td>0.01</td><td>~120 KB</td><td>7</td></tr>
 *     <tr><td>1,000,000</td><td>0.01</td><td>~1.2 MB</td><td>7</td></tr>
 *     <tr><td>1,000,000</td><td>0.03</td><td>~0.85 MB</td><td>5</td></tr>
 * </table>
 *
 * @param <T> 元素类型
 * @author zerx
 */
public class DefaultBloomFilter<T> implements BloomFilter<T> {

    private final long[] bits;
    private final int numHashFunctions;
    private final long expectedInsertions;
    private final double fpp;
    private final StampedLock lock = new StampedLock();

    /**
     * 创建布隆过滤器
     *
     * @param expectedInsertions 预计插入数量（必须为正数）
     * @param fpp                预期误判率（必须为 0~1 之间的正数，推荐 0.01~0.03）
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public DefaultBloomFilter(long expectedInsertions, double fpp) {
        if (expectedInsertions <= 0) {
            throw new IllegalArgumentException(
                    "expectedInsertions must be positive, got: " + expectedInsertions);
        }
        if (fpp <= 0 || fpp >= 1) {
            throw new IllegalArgumentException(
                    "fpp must be in (0, 1), got: " + fpp);
        }
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        this.numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, fpp);
        long numBits = optimalNumOfBits(expectedInsertions, fpp);
        this.bits = new long[(int) ((numBits + Long.SIZE - 1) / Long.SIZE)];
    }

    @Override
    public boolean mightContain(T value) {
        if (value == null) {
            return false;
        }
        long stamp = lock.tryOptimisticRead();
        long hash = hash128(value);
        int hash1 = (int) (hash >>> 32);
        int hash2 = (int) hash;

        boolean result = true;
        long bitArrayLength = (long) bits.length * Long.SIZE;
        for (int i = 1; i <= numHashFunctions; i++) {
            int combinedHash = hash1 + (i * hash2);
            int bitIndex = (int) (Math.abs((long) combinedHash) % bitArrayLength);
            if (!getBit(bitIndex)) {
                result = false;
                break;
            }
        }

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = true;
                for (int i = 1; i <= numHashFunctions; i++) {
                    int combinedHash = hash1 + (i * hash2);
                    int bitIndex = (int) (Math.abs((long) combinedHash) % bitArrayLength);
                    if (!getBit(bitIndex)) {
                        result = false;
                        break;
                    }
                }
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public void put(T value) {
        if (value == null) {
            return;
        }
        long stamp = lock.writeLock();
        try {
            long hash = hash128(value);
            int hash1 = (int) (hash >>> 32);
            int hash2 = (int) hash;
            long bitArrayLength = (long) bits.length * Long.SIZE;
            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = hash1 + (i * hash2);
                int bitIndex = (int) (Math.abs((long) combinedHash) % bitArrayLength);
                setBit(bitIndex);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public long expectedInsertions() {
        return expectedInsertions;
    }

    @Override
    public double fpp() {
        return fpp;
    }

    // ======================== 位操作 ========================

    private boolean getBit(int index) {
        return (bits[index >>> 6] & (1L << index)) != 0;
    }

    private void setBit(int index) {
        bits[index >>> 6] |= (1L << index);
    }

    // ======================== 哈希函数 ========================

    /**
     * 基于 String.hashCode() 风格的 128-bit 哈希，通过双重扰动产生高位和低位。
     * 对于 String 类型使用标准 hashCode，其他类型使用 Object.hashCode()。
     */
    private long hash128(T value) {
        int h = value.hashCode();
        // 扩散：高位和低位混合产生 64-bit 结果
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        // 用相同的扰动种子产生第二组 32-bit
        int h2 = value.hashCode();
        h2 ^= (h2 >>> 16);
        h2 *= 0x45d9f3b;
        h2 ^= (h2 >>> 13);
        h2 *= 0x45d9f3b;
        h2 ^= (h2 >>> 16);
        return ((long) h << 32) | (h2 & 0xFFFFFFFFL);
    }

    // ======================== 最优参数计算 ========================

    /**
     * 计算最优位图大小（bits）
     */
    private static long optimalNumOfBits(long n, double p) {
        return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 计算最优哈希函数数量
     */
    private static int optimalNumOfHashFunctions(long n, double p) {
        return Math.max(1, (int) Math.round(Math.log(2) * optimalNumOfBits(n, p) / n));
    }
}

package com.zerx.spring.cache.warmer;

/**
 * 缓存预热器接口。
 * <p>
 * 实现此接口并在 Spring 容器中注册为 Bean，
 * 框架会在 {@link org.springframework.boot.context.event.ApplicationReadyEvent} 后自动调用
 * 所有预热器的 {@link #warmUp()} 方法。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Component
 * public class UserCacheWarmer implements CacheWarmer {
 *     @Override
 *     public void warmUp() {
 *         List<User> hotUsers = userRepository.findHotUsers();
 *         for (User user : hotUsers) {
 *             cacheOps.set("user:" + user.getId(), user, 30, TimeUnit.MINUTES);
 *         }
 *     }
 *
 *     @Override
 *     public int order() {
 *         return 100; // 数字越小越先执行
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
@FunctionalInterface
public interface CacheWarmer {

    /**
     * 执行缓存预热逻辑。
     */
    void warmUp();

    /**
     * 执行顺序，数字越小越先执行。默认 {@code Integer.MAX_VALUE}（最后执行）。
     */
    default int order() {
        return Integer.MAX_VALUE;
    }
}

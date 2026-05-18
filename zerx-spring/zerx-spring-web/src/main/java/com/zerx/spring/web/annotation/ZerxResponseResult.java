package com.zerx.spring.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 统一响应封装标记注解
 * <p>
 * 标注在类上，表示该 Controller 的返回值需要被
 * {@link com.zerx.spring.web.advise.ZerxResponseBodyAdvice} 自动包装为 {@code Result<T>}。
 * </p>
 *
 * <p>
 * 当 {@code @RestController} 未标注此注解时，响应体增强也会默认生效（针对 {@code @RestController}）。
 * 此注解主要用于标注普通 {@code @Controller}（非 REST）类，使其也享受统一响应封装能力。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @RestController
 * @ZerxResponseResult
 * public class OrderController {
 *
 *     @GetMapping("/orders/{id}")
 *     public Order getOrder(@PathVariable Long id) {
 *         // 返回值将被自动包装为 Result<Order>
 *         return orderService.findById(id);
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZerxResponseResult {
}

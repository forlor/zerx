package com.zerx.spring.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zerx.spring.web.properties.ZerxWebProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Jackson 全局自动配置
 * <p>
 * 提供高性能 JSON 序列化/反序列化配置，解决以下常见问题：
 * <ul>
 *   <li>Long 类型转 String，防止 JavaScript 精度丢失（JS Number 最大安全整数 2^53 - 1）</li>
 *   <li>LocalDateTime 统一格式化输出，避免时间戳与格式字符串混用</li>
 *   <li>null 值序列化控制，减少无效传输</li>
 * </ul>
 * </p>
 *
 * <h3>性能考量：</h3>
 * <ul>
 *   <li>Long→String 使用 {@link ToStringSerializer} 内联，零正则开销</li>
 *   <li>日期格式化使用 {@link DateTimeFormatter} 缓存实例（线程安全），避免每次创建</li>
 *   <li>关闭不必要的特性（FAIL_ON_UNKNOWN_PROPERTIES）减少解析开销</li>
 * </ul>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@EnableConfigurationProperties({ZerxWebProperties.class, JacksonProperties.class})
public class JacksonAutoConfiguration {

    /**
     * 配置 ObjectMapper
     * <p>
     * 注册 Long→String 模块、日期时间格式化模块，并设置全局序列化特性。
     * </p>
     *
     * @param webProps   Web 模块配置属性
     * @param jacksonProps Spring Boot Jackson 配置属性
     * @return 配置好的 ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper zerxObjectMapper(ZerxWebProperties webProps, JacksonProperties jacksonProps) {
        ZerxWebProperties.Jackson jackson = webProps.getJackson();
        ObjectMapper mapper = new ObjectMapper();

        // 1. Spring Boot 默认模块（时间、Optional 等）
        mapper.registerModule(new JavaTimeModule());
        // 禁用 WriteDatesAsTimestamps（用字符串格式而非时间戳）
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 2. Long → String 模块（防止 JS 精度丢失）
        SimpleModule longModule = new SimpleModule("zerxLongModule");
        longModule.addSerializer(Long.class, ToStringSerializer.instance);
        longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        mapper.registerModule(longModule);

        // 3. null 值序列化控制
        if (jackson.isIncludeNull()) {
            mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        } else {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        // 4. 日期时间格式
        if (jackson.getDateFormat() != null && !jackson.getDateFormat().isBlank()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(jackson.getDateFormat(), Locale.CHINA);
            SimpleModule dateModule = new SimpleModule("zerxDateModule");
            dateModule.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    gen.writeString(formatter.format(value));
                }
            });
            dateModule.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    return LocalDateTime.parse(p.getText(), formatter);
                }
            });
            mapper.registerModule(dateModule);
        }

        // 5. 通用特性优化（性能 + 兼容性）
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        return mapper;
    }
}

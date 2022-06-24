/**
 * E5Projects @ org.reed.feign/FeignConfiguration.java
 */
package org.reed.feign;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenxiwen
 * @createTime 2019年12月10日 下午3:48
 * @description
 */
@Configuration
public class FeignConfiguration {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}

/**
 * E5Projects @ org.reed.bootup/ReedErrorPageFilterConfiguration.java
 */
package org.reed.bootup;

import org.springframework.boot.web.servlet.support.ErrorPageFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenxiwen
 * @createTime 2019年08月02日 下午3:59
 * @description
 */
@Configuration
class ReedErrorPageFilterConfiguration {
    ReedErrorPageFilterConfiguration() {
    }

    @Bean
    public ErrorPageFilter errorPageFilter() {
        return new ErrorPageFilter();
    }
}

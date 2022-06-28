/**
 * springbootup/org.reed.bootup/ReedWebAppConfigure.java
 */
package org.reed.bootup;

import org.reed.log.ReedLogger;
import org.reed.utils.EnderUtil;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author chenxiwen
 * @date 2018年1月16日下午3:55:17
 */
//@Configuration
@Deprecated
public class ReedWebAppConfigure extends WebMvcConfigurerAdapter{
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").
        allowedOrigins("*").
        allowedMethods("GET", "HEAD", "POST","PUT", "DELETE", "OPTIONS")
        .allowCredentials(true);
        ReedLogger.debug(EnderUtil.devInfo()+"/**  --> All  --> allowCredentials=true");
    }
}

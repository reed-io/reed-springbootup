/**
 * E5Projects @ org.reed.router/MvcRouter.java
 */
package org.reed.router;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author chenxiwen
 * @createTime 2020年02月29日 下午9:58
 * @description
 */
@Configuration
public class MvcRouter implements WebMvcConfigurer {

    public void addViewControllers(ViewControllerRegistry registry){
        registry.addViewController("/").setViewName("redirect:/health/info");
        registry.addViewController("/info").setViewName("redirect:/health/info");
        registry.addViewController("/help").setViewName("redirect:/health/info");

    }
}

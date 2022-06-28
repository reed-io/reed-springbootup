/**
 * E5Projects @ org.reed.interceptor/ReedInterceptorManager.java
 */
package org.reed.interceptor;

import org.reed.interceptor.EnableInterceptor;
import org.reed.interceptor.ReedInterceptor;
import org.reed.log.ReedLogger;
import org.reed.struct.ReedSortedList;
import org.reed.system.SysEngine;
import org.reed.utils.EnderUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.LinkedList;
import java.util.List;

/**
 * @author chenxiwen
 * @createTime 2019年12月08日 下午6:42
 * @description
 */
@Configuration
public class ReedInterceptorManager implements WebMvcConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * Add Spring MVC lifecycle interceptors for pre- and post-processing of
     * controller method invocations. Interceptors can be registered to apply
     * to all requests or be limited to a subset of URL patterns.
     * <p><strong>Note</strong> that interceptors registered here only apply to
     * controllers and not to resource handler requests. To intercept requests for
     * static resources either declare a
     * {@link MappedInterceptor MappedInterceptor}
     * bean or switch to advanced configuration mode by extending
     * {@link WebMvcConfigurationSupport
     * WebMvcConfigurationSupport} and then override {@code resourceHandlerMapping}.
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LinkedList<org.reed.interceptor.ReedInterceptor> reedInterceptors = analysisReedInterceptors();
        for(org.reed.interceptor.ReedInterceptor reedInterceptor : reedInterceptors){
            registry.addInterceptor(reedInterceptor).addPathPatterns(reedInterceptor.pathPatterns())
                    .excludePathPatterns(reedInterceptor.excludePathPatterns());
            ReedLogger.info(EnderUtil.devInfo()+" - ReedInterceptor: "+reedInterceptor.getClass().getName()+" added!");
        }
    }

    private LinkedList<org.reed.interceptor.ReedInterceptor> analysisReedInterceptors(){
        ReedLogger.info(EnderUtil.devInfo()+" - Analysis ReedInterceptor And Load Into Spring Framework Web Servlet");
        List<Class<?>> interceptors = SysEngine.realizeClass(org.reed.interceptor.ReedInterceptor.class, null);
        ReedSortedList<org.reed.interceptor.ReedInterceptor> reedSortedList = new ReedSortedList<>();
        for(Class<?> clz : interceptors){
            org.reed.interceptor.EnableInterceptor interceptorAnnotation = clz.getAnnotation(EnableInterceptor.class);
            if(interceptorAnnotation == null){
                ReedLogger.warn(EnderUtil.devInfo()+" - ReedInterceptor @ ["+clz.getName()+"] " +
                        "has not mark @EnableInterceptor on top of the class, ReedFramework will ignore this!");
                continue;
            }else{
                Object obj = applicationContext.getBean(clz);
                org.reed.interceptor.ReedInterceptor reedInterceptor = (ReedInterceptor)obj;
                if(clz.getPackage().getName().startsWith("org.reed")){
                    ReedLogger.debug("\t"+clz.getName()+"@"+interceptorAnnotation.order());
                    reedSortedList.insert(interceptorAnnotation.order(), reedInterceptor);
                }else{
                    if(interceptorAnnotation.order()<101){
                        ReedLogger.warn(EnderUtil.devInfo()+" - ReedInterceptor @ ["+clz.getName()+"] is using " +
                                "first 100 order, ReedFramework will move it to "+(interceptorAnnotation.order()+100));
                        reedSortedList.insert(interceptorAnnotation.order()+100, reedInterceptor);
                    }else{
                        ReedLogger.debug("\t"+clz.getName()+"@"+interceptorAnnotation.order());
                        reedSortedList.insert(interceptorAnnotation.order(), reedInterceptor);
                    }
                }
            }
        }

        return reedSortedList.linkedList();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

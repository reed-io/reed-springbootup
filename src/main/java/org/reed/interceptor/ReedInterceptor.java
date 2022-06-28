/**
 * E5Projects @ org.reed.interceptor/ReedInterceptor.java
 */
package org.reed.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author chenxiwen
 * @createTime 2019年12月08日 下午6:35
 * @description
 */
public interface ReedInterceptor extends HandlerInterceptor {
    /**
     * the path pattern array of this interceptor deal with
     * @return
     */
    String[] pathPatterns();


    /**
     * Add URL patterns to which the registered interceptor should not apply to.
     * @author chenxiwen
     * @since 0.0.6-SNAPSHOT
     * @date 2021-1-13
     */
    default String[] excludePathPatterns(){
        return new String[0];
    }


}

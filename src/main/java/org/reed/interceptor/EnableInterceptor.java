/**
 * E5Projects @ org.reed.interceptor/EnableInterceptor.java
 */
package org.reed.interceptor;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author chenxiwen
 * @createTime 2019年12月23日 下午4:57
 * @description
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface EnableInterceptor {
    int order() default Integer.MAX_VALUE;
}

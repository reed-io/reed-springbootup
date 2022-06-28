/**
 * E5Projects @ org.reed.auth/ReedAuthorization.java
 */
package org.reed.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author chenxiwen
 * @createTime 2019年12月23日 上午9:27
 * @description
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReedAuthorization {

}

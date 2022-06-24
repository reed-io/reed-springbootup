/**
 * E5Projects#org.reed.interceptor/CorsInterceptor.java
 */
package org.reed.interceptor;

import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author chenxiwen
 * @date 2021-01-12 16:13:46
 */
@EnableInterceptor(order = 1)
public final class CorsInterceptor implements ReedInterceptor {

    public static final String CORS_PATH_PATTERN = "/**";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "*";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "true";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "POST,GET,OPTIONS,DELETE,PUT,HEAD";
    private static final String ACCESS_CONTROL_MAX_AGE = "3600";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Origin,X-Requested-With,Content-Type,Accept,Authorization";
    private static final String REED_ACCESS_CONTROL_ALLOW_HEADERS = ",user_token,app_token,device_id,app_code,client_type,token,TOKEN";

    @Override
    public String[] pathPatterns() {
        return new String[]{CORS_PATH_PATTERN};
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setHeader("Access-Control-Allow-Origin", ACCESS_CONTROL_ALLOW_ORIGIN);
        response.setHeader("Access-Control-Allow-Credentials", ACCESS_CONTROL_ALLOW_CREDENTIALS);
        response.setHeader("Access-Control-Allow-Methods", ACCESS_CONTROL_ALLOW_METHODS);
        response.setHeader("Access-Control-Max-Age", ACCESS_CONTROL_MAX_AGE);
        response.setHeader("Access-Control-Allow-Headers", ACCESS_CONTROL_ALLOW_HEADERS+REED_ACCESS_CONTROL_ALLOW_HEADERS);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}

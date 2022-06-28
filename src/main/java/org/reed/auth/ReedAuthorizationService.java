/**
 * E5Projects @ org.reed.auth/ReedAuthorizationService.java
 */
package org.reed.auth;

import org.reed.feign.FeignConfiguration;
import org.reed.log.ReedLogger;
import org.reed.utils.EnderUtil;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author chenxiwen
 * @createTime 2019年12月23日 上午9:34
 * @description
 */
@Component
@FeignClient(value="AUTH", name = "AUTH",
        fallback = ReedAuthorizationService.ReedAuthorizationServiceFallBack.class, configuration = FeignConfiguration.class)
public interface ReedAuthorizationService {
    @GetMapping("/MulitilingualCode/code/des/get/{code}/{languageCode}")
    boolean check(final String userToken);

    @Component
    class ReedAuthorizationServiceFallBack implements ReedAuthorizationService{
        @Override
        public boolean check(final String userToken) {
            ReedLogger.warn(EnderUtil.devInfo() + " - ReedUserToken:" +userToken);
            return true;
        }
    }
}

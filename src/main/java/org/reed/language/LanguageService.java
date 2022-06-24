/**
 * E5Projects @ org.reed.language/LanguageService.java
 */
package org.reed.language;

import org.reed.bootup.SpringBootBootup;
import org.reed.define.BaseErrorCode;
import org.reed.entity.ReedResult;
import org.reed.feign.FeignConfiguration;
import org.reed.log.ReedLogger;
import org.reed.utils.EnderUtil;
import feign.hystrix.FallbackFactory;
//import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author chenxiwen
 * @createTime 2019年12月10日 上午10:13
 * @description
 */
@Component
@FeignClient(value=SpringBootBootup.LANGUAGE_SERVICE_NAME, name = SpringBootBootup.LANGUAGE_SERVICE_NAME,
        fallback = LanguageService.LanguageServiceFallbackService.class, configuration = FeignConfiguration.class)
public interface LanguageService {

    @GetMapping("/MulitilingualCode/code/des/get/{code}/{languageCode}")
    ReedResult<String> getMessageByLanguageCode(@PathVariable("code") int code,
                                    @PathVariable("languageCode") String languageCode);

    @Component
    final class LanguageServiceFallbackService implements LanguageService{
        @Override
        public ReedResult<String> getMessageByLanguageCode(int code, String languageCode) {
            ReedLogger.warn(EnderUtil.devInfo()+" - Language service Hystrix, code="+code+", languageCode="+languageCode);
            ReedResult<String> result = new ReedResult<>();
            result.setCode(BaseErrorCode.LANGUAGE_SERVICE_HYSTRIX);
            result.setData(code+", "+languageCode);
            return result;
        }
    }

//    @Component
    final class HystrixClientFallbackFactory implements FallbackFactory<LanguageService>{
        /**
         * Returns an instance of the fallback appropriate for the given cause
         *
         */
        @Override
        public LanguageService create(Throwable cause) {
            ReedLogger.debug("!!!!!>>>>>>>>"+cause.getMessage());
            return new LanguageService(){
                @Override
                public ReedResult getMessageByLanguageCode(int code, String languageCode) {
                    ReedLogger.warn(EnderUtil.devInfo()+" - "+cause.getMessage());
                    return null;
                }
            };
        }
    }
}

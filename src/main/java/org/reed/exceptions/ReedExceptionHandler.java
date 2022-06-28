/**
 * E5Projects @ org.reed.exceptions/ReedExceptionHandler.java
 */
package org.reed.exceptions;

import org.reed.define.CodeDescTranslator;
import org.reed.entity.ReedResult;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author chenxiwen
 * @createTime 2020年09月03日 上午10:42
 * @description
 */
//@ControllerAdvice
public class ReedExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ReedResult handleBaseException(BaseException e){
        return new ReedResult.Builder<>().code(e.errorCode())
                .message(CodeDescTranslator.explain(e.errorCode()))
                .data(e)
                .build();
    }
}

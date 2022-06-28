package org.reed.controller;

import org.reed.define.BaseErrorCode;
import org.reed.define.CodeDescTranslator;
import org.reed.log.ReedLogger;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public final class ReedBasicErrorController extends BasicErrorController {
//		public static final String MESSAGE = "Reed Service System Error!";

	    @Autowired
	    public ReedBasicErrorController(ServerProperties serverProperties) {
	        super(new DefaultErrorAttributes(), serverProperties.getError());
	    }

	    /**
	     * 覆盖默认的Json响应
	     */
	    @Override
	    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
//	        Map<String, Object> body = getErrorAttributes(request, this.getErrorAttributeOptions(request, MediaType.ALL));
	        Map<String, Object> body = getErrorAttributes(request, true);
//	        Object traceStack = body.get("trace");
	        HttpStatus status = getStatus(request);

	        ReedLogger.error(ReedBasicErrorController.class.getName()+", error:"+JSONObject.toJSONString(body));

	        //输出自定义的Json格式
//	        String resultStr = genResp(false, -1, "System Error!", JSONObject.toJSONString(body));
	        Map<String, Object> resultMap = new HashMap<String, Object>();
	        resultMap.put("code", BaseErrorCode.UNKNOWN_ERROR);
	        resultMap.put("message", CodeDescTranslator.explain(BaseErrorCode.UNKNOWN_ERROR));
	        resultMap.put("data", body);

	        return new ResponseEntity<Map<String, Object>>(resultMap, status);
	    }

	    /**
	     * 覆盖默认的HTML响应
	     */
	    @Override
	    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
	    	response.setStatus(getStatus(request).value());
	    	//for higher version of springboot like 2.6.1
//			Map<String, Object> model = getErrorAttributes(request, this.getErrorAttributeOptions(request, MediaType.ALL));
//			Map<String, Object> data = getErrorAttributes(request, this.getErrorAttributeOptions(request, MediaType.ALL));
			Map<String, Object> model = getErrorAttributes(request, true);
			Map<String, Object> data = getErrorAttributes(request, isIncludeStackTrace(request, MediaType.ALL));
			Map<String, Object> reedResult = new HashMap<>();
			reedResult.put("code", BaseErrorCode.UNKNOWN_ERROR);
			reedResult.put("data", data);
			reedResult.put("message", CodeDescTranslator.explain(BaseErrorCode.UNKNOWN_ERROR));
			model.put("reedResult", JSON.toJSON(reedResult));
			model.put("data", data);
			return new ModelAndView("error", model);
	    }

}

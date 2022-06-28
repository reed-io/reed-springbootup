/**
 * IdeaProject @ org.reed.controller/HealthController.java
 */
package org.reed.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.reed.system.ReedContext;

/**
 * @author chenxiwen
 * @createTime 2018年11月07日 下午3:52
 * @description
 */
@RestController
@RequestMapping(path="/health", method={RequestMethod.POST, RequestMethod.GET})
public final class HealthController {
    @RequestMapping(value={"/", "/?", "/help","/check"})
    @ResponseBody
    public String help(){
        return "Hi, I'm working...";
    }

    @RequestMapping(value={"/info"})
    @ResponseBody
    public String printInfo(){
        return ReedContext.sysInfo();
    }

//    @RequestMapping(value={"/xml"})
//    @ResponseBody()
//    public String xml(){
//        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response><Say loop=\"3\">xxxxxxx</Say></Response>";
//    }

}

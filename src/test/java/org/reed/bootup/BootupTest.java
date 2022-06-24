/**
 * E5Projects @ org.reed.bootup/BootupTest.java
 */
package org.reed.bootup;


import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author chenxiwen
 * @createTime 2019年12月13日 上午11:16
 * @description
 */
@EnableServiceRegister
@ReedAutoTranslate(enable = false)
public class BootupTest extends SpringBootBootup {
    @Override
    protected void beforeStart() {
//        this.putArgs("spring.boot.admin.client.username", "ender");
//        this.putArgs("spring.boot.admin.client.password", "ender");
    }

    @Override
    protected void afterStart(SpringApplication application, ApplicationContext context) {

    }

    /**
     * @return Project/Module Name
     */
    @Override
    public String getModuleName() {
        return "BootupTest";
    }

    public static void main(String[] args){
        new BootupTest().start(args);
    }


//    @Configuration
//    public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            http.authorizeRequests().anyRequest().permitAll()
//                    .and().csrf().disable();
//        }
//    }
}

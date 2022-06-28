/**
 * E5Projects @ org.reed.bootup/SpringBootup.java
 */
package org.reed.bootup;

import ch.qos.logback.classic.Level;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.reed.define.BaseErrorCode;
import org.reed.define.CodeDescTranslator;
import org.reed.define.HostColonPort;
import org.reed.entity.ReedResult;
import org.reed.exceptions.EnderRuntimeException;
import org.reed.language.LanguageService;
import org.reed.log.ReedLogger;
import org.reed.system.ReedContext;
import org.reed.system.SysEngine;
import org.reed.utils.CollectionUtil;
import org.reed.utils.EnderUtil;
import org.reed.utils.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author chenxiwen
 * @createTime 2019年08月02日 下午3:55
 * @description
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"org.reed", "com.reed", "center.reed"})
@ControllerAdvice
@ComponentScan(basePackages={"org.reed","center.reed","com.reed","com.ender","org.ender","org.springframework.kafka"})
public abstract class SpringBootBootup extends ReedStarter implements WebApplicationInitializer, ResponseBodyAdvice,
        ApplicationListener<WebServerInitializedEvent>, Discoverable, Traceable, Clientable{

    public static final String[] IGNORE_PREFIX = {"eureka", "ribbon", "feign", "hystrix", "logging", "spring.cloud.nacos.config.ext-config[0]"};
    public static final String[] IGNORE_ITEMS = {"spring.application.name", "spring.profiles.active", "spring.cloud.nacos.config.server-addr", "spring.cloud.nacos.config.file-extension", "spring.cloud.nacos.config.namespace", "spring.cloud.nacos.config.group", "spring.cloud.nacos.config.enabled"};
    public static final String DOT = ".";

    private boolean isAutoTranslate = false;
    public static final String LANGUAGE_SERVICE_NAME = "MULTILINGUALCODE";

    private final Map<String,Object> toBeInjected = new HashMap<>();
    private SpringApplication application;
//    private ConfigurableApplicationContext context;
    private ApplicationContext context;

    private boolean registerErrorPageFilter = true;

    protected abstract void beforeStart();
    protected abstract void afterStart(SpringApplication application, ApplicationContext context);

    protected final void setRegisterErrorPageFilter(boolean registerErrorPageFilter) {
        this.registerErrorPageFilter = registerErrorPageFilter;
    }

    /**
     * implement WebApplicationInitializer
     * @param servletContext
     * @throws ServletException
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        setDefault();
        SysEngine.addGenericPath(servletContext.getRealPath("/WEB-INF/classes/"));
        SysEngine.addGenericPath(servletContext.getRealPath("/WEB-INF/conf/"));
        SysEngine.addGenericPath(servletContext.getRealPath("/WEB-INF/lib/"));
        SysEngine.addGenericPath(servletContext.getRealPath("/BOOT-INF/classes/"));
        SysEngine.addGenericPath(servletContext.getRealPath("/BOOT-INF/conf/"));
        SysEngine.addGenericPath(servletContext.getRealPath("/BOOT-INF/lib/"));
        autoTranslate();
        this.bootup();
        //provide beforeStart() invoke
        beforeStart();
        //Spring Business
        WebApplicationContext rootAppContext = this.createRootApplicationContext(servletContext);
        if (rootAppContext != null) {
            servletContext.addListener(new ContextLoaderListener(rootAppContext) {
                public void contextInitialized(ServletContextEvent event) {
                }
            });
        } else {
            ReedLogger.info(EnderUtil.devInfo() + " - No ContextLoaderListener registered, as createRootApplicationContext() did not return an application context");
        }
    }

    protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
        SpringApplicationBuilder builder = this.createSpringApplicationBuilder();
        builder.main(this.getClass());
        ApplicationContext parent = this.getExistingRootWebApplicationContext(servletContext);
        if (parent != null) {
            ReedLogger.info(EnderUtil.devInfo()+" - Root context already created (using as parent).");
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, null);
            builder.initializers(new ParentContextApplicationContextInitializer(parent));
        }

        builder.initializers(new ServletContextApplicationContextInitializer(servletContext));
        //modified since 0.0.8, Spring framework updated, contextClass has been removed from SpringApplicationBuild since an unknown version between 2.1.6 and 2.6.1
        builder.contextClass(AnnotationConfigServletWebServerApplicationContext.class);
//        builder.contextFactory((webApplicationType -> {
//            return new AnnotationConfigServletWebServerApplicationContext();
//        }));
        builder = this.configure(builder);
        builder.listeners(new WebEnvironmentPropertySourceInitializer(servletContext));
        SpringApplication application = builder.build();
        if (application.getAllSources().isEmpty() && AnnotationUtils.findAnnotation(this.getClass(), Configuration.class) != null) {
            application.addPrimarySources(Collections.singleton(this.getClass()));
        }

        Assert.state(!application.getAllSources().isEmpty(), "No SpringApplication sources have been defined. Either override the configure method or add an @Configuration annotation");
        if (this.registerErrorPageFilter) {
            application.addPrimarySources(Collections.singleton(ReedErrorPageFilterConfiguration.class));
        }

        return this.run(application);
    }

    protected SpringApplicationBuilder createSpringApplicationBuilder() {
        return new SpringApplicationBuilder();
    }

    protected WebApplicationContext run(SpringApplication application) {
        WebApplicationContext context =  (WebApplicationContext)application.run(new String[0]);
        //provide afterStart() invoke
        ReedLogger.debug(EnderUtil.devInfo()+" - ExistingRootWebApplicationContext:"+(this.context==null?"null":this.context));
        ReedLogger.debug(EnderUtil.devInfo()+" - WebApplicationContext_From_InternalFramework:"+(context==null?"null":context));
        if(this.context == null){
            if(context == null){
                throw new RuntimeException("Web Container With SpringBoot Loaded Failed! "+getModuleName()+" - Quit with RuntimeException! Please send log file to chenxiwenender@163.com");
            }
            this.context = context;
        }
        afterStart(this.application, this.context);
        return context;
    }

    private ApplicationContext getExistingRootWebApplicationContext(ServletContext servletContext) {
        Object context = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        return context instanceof ApplicationContext ? (ApplicationContext)context : null;
    }

    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        this.application = builder.application();
        this.context = builder.context();
        return builder;
    }

    private static final class WebEnvironmentPropertySourceInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
        private final ServletContext servletContext;

        private WebEnvironmentPropertySourceInitializer(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
            ConfigurableEnvironment environment = event.getEnvironment();
            if (environment instanceof ConfigurableWebEnvironment) {
                ((ConfigurableWebEnvironment)environment).initPropertySources(this.servletContext, null);
            }

        }

        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    /**
     * IDE启动或java -jar或maven插件启动时走此方法
     * 非Servlet容器启动时通常都走此方法
     * @param args
     */
    protected void start(String[] args){
        String path = this.getClass().getResource("/").getPath();
//        System.err.println(path);
//        System.err.println(".jar!/BOOT-INF/classes!/");
//        System.err.println(path.endsWith(".jar!/BOOT-INF/classes!/"));
        if(path.endsWith(".jar!/BOOT-INF/classes!/")){
            System.err.println(path.substring(0, path.indexOf("!")));
            String osname = System.getProperty("os.name");
            boolean isWindows = osname.toLowerCase().indexOf("windows") >= 0;
            if(path.startsWith("file:/") && isWindows){
                path = path.substring("file:/".length(), path.indexOf("!"));
            }else if(path.startsWith("file:/") && !isWindows){
                path = path.substring("file:".length(), path.indexOf("!"));
            }else{
                throw new EnderRuntimeException("System Error!");
            }
            System.err.println(path);
            SysEngine.addJarPath(path);
        }
        this.bootup();

        setDefault();
        autoTranslate();

        beforeStart();
        SpringApplication app = new SpringApplication(this.getClass());
        ConfigurableApplicationContext cac = app.run(args);
        afterStart(app, cac);
    }


    private void setDefault(){
        disableUnifiedConfiguration();
        setDefaultInfo();
        putArgs("ReedCipher{56ba85cc45726bc225c0810cfdf8a028b7552be1c9ff29e0}", StringUtil.isEmpty(getModuleName())?ReedStarter.DEFAULT:getModuleName());
        //让框架知道我要用自定义配置的LoggingSystem来替换Springboot的默认LoggingSystem
        putArgs(LoggingSystem.class.getName(), "ReedCipher{a9cfd414898b23ab063e51ed918652ca2868f43f6a0dd14b9efc4778691adf027e09966100be7875}");

        //make sure Spring Data JPA Repositories Using Lazy Mode
        putArgs("ReedCipher{9bee010be12ec665d4ba4b62defd3c59934066cf03995c39fe87a76164c1ce6bedf46431317076bd75597176b015576e}", "ReedCipher{36c7ffbe36c79bcd}");

        //for mybatis, free developer from setting mapper location
//        mybatis.mapper-locations  classpath:/mapper/*.xml
        putArgs("ReedCipher{9fae3c4910a59ee9d6c35a49c5bfd6991f1c779c43895365feb959b7d4642fcb}", "ReedCipher{a780cf465d3f6b5bbe4c964728fde08cc83d53ec1f0858b4}");

        //enable the whitelabel page
//        this.putArgs("ReedCipher{73c580f2ca6ae09b369e77e61a6ccebf8b1f5631f18eeb95d97bfef3e68b3099}", "ReedCipher{676b7279f7e33a7c}");
        //enable feign hystrix this.putArgs("feign.hystrix.enabled", "true");
        putArgs("ReedCipher{1b683ad9b48edddc5d00d19de4fb7098c189f0e240de92ae}", "ReedCipher{eadbe9eb68777e52}");
        //for Feign Test this.putArgs("logging.level.org.reed.service.LanguageService", "debug");
        putArgs("ReedCipher{15063b6c3a5bcc0062a266b99fa08ff7fb4f607fbed423fcaf07c0ec7ee32cb7293610b8d18878f5bbf6f995006d261426ef0ff90b963bde}", "ReedCipher{61d221975f122fc9}");
        //默认不启用向Eureka服务中心注册
        disableServiceRegister();
        //默认暂不开启服务追踪
        disableServiceTrace();
        //默认不开启管理actuator
        disableAdminClient();
    }

    private void disableServiceRegister(){
        //this.putArgs("eureka.client.enabled", "false");
        putArgs("ReedCipher{6d4f3c92cdc14b1038fdbc8f317b61b3c189f0e240de92ae}", "ReedCipher{676b7279f7e33a7c}");
        //this.putArgs("spring.cloud.discovery.enabled", "false");
        putArgs("ReedCipher{b56c0223134d4bb53031b58ac5a7d83b17ea9e4a5d391e5127389de5556ea80e}", "ReedCipher{676b7279f7e33a7c}");

        putArgs("spring.cloud.nacos.discovery.enabled", "false");
    }

    private void disableServiceTrace(){
        //client id
        putArgs("ReedCipher{dddfa6c50b44a14c904aa27146ad411ebb9d4d033c24b74c}", this.getModuleName());
        //stream auto startup
        putArgs("ReedCipher{dddfa6c50b44a14c0392804342b175c2ea78d6aa9c59db5ba83462823d24580dea4c7d224f114525}", "ReedCipher{676b7279f7e33a7c}");
    }

    private void disableAdminClient(){
        //Nothing to do, Admin Client Feature Is Disabled By Default
    }

    private void disableUnifiedConfiguration(){
        //nacos configuration
        //this.putArgs("spring.cloud.nacos.config.enabled", "false");
        putArgs("ReedCipher{b56c0223134d4bb5a7ec4a5c814dab0aed9483ffc66a6cd249abcaf705f762ad5ef3aa6d915f1e43}", "ReedCipher{676b7279f7e33a7c}");
        //auto refreshed
        //this.putArgs("spring.cloud.bus.refresh.enabled", "false");
        putArgs("ReedCipher{b56c0223134d4bb57b5def6bbcd405c05da3143c5e11ca5094a2cd3b552ad948feb959b7d4642fcb}", "ReedCipher{676b7279f7e33a7c}");
    }

    private void setDefaultInfo(){
        putArgs("info.ModuleName", getModuleName());
        putArgs("info.Server", ReedContext.sysInfo());
    }

    /**
     * 注册
     *
     * @param args
     */
    @Override
    public void register(DiscoverArg args) {
        ReedLogger.trace(EnderUtil.devInfo()+" - DiscoverArgs="+args.toString());
        switch (args.getDiscoveryProduction()){
            case Eureka: {
                ReedLogger.info(EnderUtil.devInfo()+" - Eureka Discovery Configuration Setting...");
                //this.putArgs("eureka.client.enabled", "true");
                putArgs("ReedCipher{6d4f3c92cdc14b1038fdbc8f317b61b3c189f0e240de92ae}", "ReedCipher{eadbe9eb68777e52}");
                //this.putArgs("spring.cloud.discovery.enabled", "true");
                putArgs("ReedCipher{b56c0223134d4bb53031b58ac5a7d83b17ea9e4a5d391e5127389de5556ea80e}", "ReedCipher{eadbe9eb68777e52}");
                //this.putArgs("eureka.client.serviceUrl.defaultZone", args.getServerUrl());
                putArgs("ReedCipher{6d4f3c92cdc14b1002fb64d4c2afc9e30a03e344c630af76d19e542e22a7b6902923cd376571093f}", args.getServerUrl());
                //this.putArgs("eureka.instance.non-secure-port-enabled", "true");
                putArgs("ReedCipher{6d478cbc88d2f34dca2094c74e7f3062c0628b854f6038b8a1c6c7189f890083d97bfef3e68b3099}", "ReedCipher{eadbe9eb68777e52}");
                //this.putArgs("eureka.instance.secure-port-enabled", "false");
                putArgs("ReedCipher{6d478cbc88d2f34dca2094c74e7f30621e75ebf6277b9d00fc858b9740a72b33bf39d10ddb51221a}", "ReedCipher{676b7279f7e33a7c}");
                //this.putArgs("eureka.instance.prefer-ip-address", "true");
                putArgs("ReedCipher{6d478cbc88d2f34dca2094c74e7f3062dcbad3d06d628b46c920435bf1f90ba9e73a3fa3a83feb15}", "ReedCipher{eadbe9eb68777e52}");
                //this.putArgs("eureka.instance.status-page-url-path", "/health/info");
                putArgs("ReedCipher{6d478cbc88d2f34dca2094c74e7f3062b74b6586e6a865c45177a143079618d04cfe6a0d68000be7}", "/health/info");
//        toBeInjected.put("eureka.client.serviceUrl.defaultZone", args.getServerUrl());
                ReedLogger.setLevel("com.netflix.discovery.shared.MonitoredConnectionManager", Level.WARN);
                ReedLogger.setLevel("com.netflix.discovery.shared.NamedConnectionPool", Level.WARN);
                ReedLogger.setLevel("com.netflix.discovery.DiscoveryClient", Level.WARN);
                ReedLogger.setLevel("com.netflix.discovery.util.DeserializerStringCache", Level.WARN);
                ReedLogger.setLevel("com.netflix.discovery.shared.resolver.AsyncResolver", Level.WARN);
                break;
            }
            case Nacos:{
                ReedLogger.info(EnderUtil.devInfo()+" - Nacos Discovery Configuration Setting...");
                putArgs("spring.cloud.discovery.enabled", "true");
                putArgs("spring.cloud.nacos.discovery.enabled", "true");
//                this.putArgs("spring.cloud.nacos.discovery.server", args.getServerUrl()); //format as "ip:port"
                putArgs("spring.cloud.nacos.discovery.server-addr", args.getServerUrl()); //format as "ip:port"
//                this.putArgs("spring.cloud.nacos.discovery.service", args.getServiceName());
                putArgs("spring.cloud.nacos.discovery.group", args.getGroup());
                putArgs("spring.cloud.nacos.discovery.namespace", args.getNamespace());
//                this.putArgs("spring.cloud.nacos.discovery.username", "nacos");
//                this.putArgs("spring.cloud.nacos.discovery.password", "reednacos");

                //for dubbo
                if(this.getClass().getAnnotation(EnableDubbo.class)==null){
                    putArgs("dubbo.enabled", "false");
                }
                putArgs("dubbo.application.name", getModuleName());
                putArgs("dubbo.application.qos-enable", "false");
                putArgs("dubbo.application.qos-accept-foreign-ip", "false");
                putArgs("dubbo.registry.address", "nacos://"+args.getServerUrl());
                putArgs("dubbo.registry.parameters.namespace", args.getNamespace());
                putArgs("dubbo.registry.parameters.group", args.getGroup());
                putArgs("dubbo.protocol.name", "dubbo");
                putArgs("dubbo.protocol.port", "-1");

                ReedLogger.setLevel("com.alibaba.cloud", Level.DEBUG);
                ReedLogger.setLevel("com.alibaba.nacos.common.http.client", Level.INFO);
                break;
            }
            default: {
                ReedLogger.warn(EnderUtil.devInfo()+" - Unsupported Discovery Production: "+args.getDiscoveryProduction());
            }
        }

    }

    /**
     * 调用链追踪
     * @param traceArg
     */
    @Override
    public void startServiceTrace(TraceArg traceArg) {
        ReedLogger.trace(EnderUtil.devInfo()+" - TraceArg="+traceArg.toString());
        putArgs("ReedCipher{326ba31e6864b523e07702d208a9781b2af7278d39fb289c}", traceArg.getTraceServer());
        putArgs("ReedCipher{326ba31e6864b523ecd4d8a4087edffc364a8ffe051d8143623b0e6fabaef384}", "ReedCipher{eadbe9eb68777e52}");
        putArgs("ReedCipher{326ba31e6864b523ecd4d8a4087edffcd4a626174ea259412db7ab4c4394b248246c7c9c66bba889}", String.valueOf(traceArg.getPercentage()));
        if(traceArg.getTraceType().equals(TraceType.KAFKA)){
            //spring.zipkin.sender.type:kafka
            putArgs("ReedCipher{326ba31e6864b52380c90a8c8fcd61b850d92ddd433aa823af1eef4628d36499}", "ReedCipher{2aef0c46ef255db5}");
            putArgs("ReedCipher{dddfa6c50b44a14c0392804342b175c2ea78d6aa9c59db5ba83462823d24580dea4c7d224f114525}", "ReedCipher{eadbe9eb68777e52}");
            putArgs("ReedCipher{326ba31e6864b52346c162d41bf7ea75c4a29e934ca035b6769f5147ef6b9783}", traceArg.getKafka().getTopic());
            StringBuffer strBuf = new StringBuffer();
            for(HostColonPort host : traceArg.getKafka().getHosts()){
                strBuf.append(host.toSimpleString()+",");
            }
            putArgs("ReedCipher{dddfa6c50b44a14cb76080b741637bffa6f959cbde3bee493ae91d268499e398}", strBuf.substring(0, strBuf.length()));
            putArgs("ReedCipher{3e483352313a195a9073a902de4828fb4a86f69a7aba469a3866718236e08b43170bae1f530e3aa4c189f0e240de92ae}", "ReedCipher{676b7279f7e33a7c}");
        }
        ReedLogger.setLevel("com.netflix.loadbalancer.DynamicServerListLoadBalancer", Level.WARN);
        ReedLogger.setLevel("com.netflix.loadbalancer.BaseLoadBalancer", Level.WARN);
    }

    @Override
    public void config(Configration configration) {
        //for actuator and admin
        //this.putArgs("management.endpoint.health.show-details", "always");
        putArgs("ReedCipher{77271c895162116a4baa39415886a7a25affab4e1f15fffc781776fc7d7a3f3682e70f9d8567a22f}", "ReedCipher{b2a4a1feaa502fb7}");
        //this.putArgs("management.endpoints.web.exposure.include", "*");
        putArgs("ReedCipher{77271c895162116a4baa39415886a7a2b2c276c9897a646d52ee9b31a471eeba3d20b413d00b7fc8af1eef4628d36499}", "ReedCipher{bf298e221a20d81e}");
        //this.putArgs("spring.boot.admin.client.url", configration.getServerUrl());
        putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c3b6ff660761ccd85}", configration.getServerUrl());
        //ignore authorization configuration, hardcode first
//        if(!StringUtil.isEmpty(configration.getUsername())){
//            //this.putArgs("spring.boot.admin.client.username", configration.getUsername());
//            this.putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c582bd4a8d9d86ebdaf1eef4628d36499}", configration.getUsername());
//            //this.putArgs("spring.boot.admin.client.instance.metadata.user.name", configration.getUsername());
//            this.putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c478aecd3c42e8ce477103e534d5cae3717ec21cad48553c50eefa52edcf87d63}", configration.getUsername());
//        }
//        if(!StringUtil.isEmpty(configration.getPassword())){
//            //this.putArgs("spring.boot.admin.client.password", configration.getPassword());
//            this.putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c2d58fef4385eb5575ef3aa6d915f1e43}", configration.getPassword());
//            //this.putArgs("spring.boot.admin.client.instance.metadata.user.password", configration.getPassword());
//            this.putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c478aecd3c42e8ce477103e534d5cae3717ec21cad48553c58a65e0e80532b5fafeb959b7d4642fcb}", configration.getPassword());
//        }
        //this.putArgs("spring.boot.admin.client.username", "reed");
        putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c582bd4a8d9d86ebdaf1eef4628d36499}", "ReedCipher{028c60107305bc40}");
        //this.putArgs("spring.boot.admin.client.password", "Eui2560@%^)");
        putArgs("ReedCipher{a3d3afefb0eafbbdb0c2789b8b11538912870c81d60f421c2d58fef4385eb5575ef3aa6d915f1e43}", "ReedCipher{2f8ab711c0397b48c7e8bab352a8977e}");
        //logfile viewer
        putArgs("ReedCipher{15063b6c3a5bcc0058fded3aa11c98d7}", ReedContext.getString("user.dir")+ File.separator+"reedlog"+File.separator+getModuleName()+".log");
        putArgs("ReedCipher{15063b6c3a5bcc007c9d2cfee9bb105858fded3aa11c98d7}", ReedLogger.DEFAULT_PATTERN);
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port  = event.getWebServer().getPort();
        ReedLogger.info("WebServerInitialized port is: "+port);
        WebApplicationContext webApplicationContext = (WebApplicationContext) event.getApplicationContext();
        ReedLogger.trace(webApplicationContext.toString());
        RequestMappingHandlerMapping mapping = webApplicationContext.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
        ReedLogger.info("Following Methods Allowed By This Service:");
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> m : map.entrySet()) {
            Map<String, String> map1 = new HashMap<String, String>();
            RequestMappingInfo info = m.getKey();
            HandlerMethod method = m.getValue();
            PatternsRequestCondition p = info.getPatternsCondition();
            for (String url : p.getPatterns()) {
                map1.put("url", url);
            }
            map1.put("className", method.getMethod().getDeclaringClass().getName()); // 类名
            map1.put("method", method.getMethod().getName()); // 方法名
            map1.put("args", Arrays.toString(method.getMethodParameters()));
            RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
            for (RequestMethod requestMethod : methodsCondition.getMethods()) {
                map1.put("type", requestMethod.toString());
            }

            list.add(map1);
        }
        ReedLogger.info(CollectionUtil.toString(list));
    }

    private void autoTranslate(){
        Annotation annotation = this.getClass().getSuperclass().getAnnotation(ReedAutoTranslate.class);
        if(annotation != null && ((ReedAutoTranslate)annotation).enable()){
            this.isAutoTranslate = true;
        }
    }


    @Autowired
    LanguageService languageService;

    /**
     * Whether this component supports the given controller method return type
     * and the selected {@code HttpMessageConverter} type.
     *
     * @param returnType    the return type
     * @param converterType the selected converter type
     * @return {@code true} if {@link #beforeBodyWrite} should be invoked;
     * {@code false} otherwise
     */
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
//        Annotation annotation = this.getClass().getSuperclass().getAnnotation(ReedAutoTranslate.class);
//        if(annotation == null ||
//                !((ReedAutoTranslate)annotation).enable()){
//            return false;
//        }

        return returnType.getMethod().getReturnType() == ReedResult.class;
    }

    /**
     * Invoked after an {@code HttpMessageConverter} is selected and just before
     * its write method is invoked.
     *
     * @param body                  the body to be written
     * @param returnType            the return type of the controller method
     * @param selectedContentType   the content type selected through content negotiation
     * @param selectedConverterType the converter type selected to write to the response
     * @param request               the current request
     * @param response              the current response
     * @return the body that was passed in or a modified (possibly new) instance
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        ReedLogger.trace(EnderUtil.devInfo()+" - Reed ReedResult Language Handler Start");
        if(body instanceof ReedResult){
            ReedResult result = (ReedResult)body;
            if(isAutoTranslate){
                String language = getLanguage(request);
                ReedLogger.trace(EnderUtil.devInfo()+ " - asking language:"+language);
//            ((ReedResult) body).setMessage(request.getHeaders().getAcceptLanguage()+"{"+((ReedResult) body).getMessage()+"}");
                ReedResult<String> languageResult = languageService.getMessageByLanguageCode(result.getCode(),
                        language);
                if(languageResult.getCode() == BaseErrorCode.SUCCESS_OPERATE){
                    if(StringUtil.isEmpty(languageResult.getData())){
                        ReedLogger.warn(EnderUtil.devInfo()+" - No Message Found With Query{"+this.getModuleName()+
                                ", "+result.getCode()+", "+language+"}");
                    }else{
                        result.setMessage(languageResult.getData());
                    }
                }else{
                    ReedLogger.error(EnderUtil.getDevInfo()+" - Language Result = "+languageResult.toString());
                }
            }
            if(StringUtil.isEmpty(result.getMessage())){
                result.setMessage(CodeDescTranslator.explain(result.getCode()));
            }
        }
        return body;
    }

    private String getLanguage(ServerHttpRequest request){
        List<Locale.LanguageRange> list = request.getHeaders().getAcceptLanguage();
        list.forEach( range -> ReedLogger.trace(EnderUtil.devInfo()+" - range: {"+range.getRange()+", "+range.getWeight()+"}"));
        return list.size()>0?list.get(0).getRange().split("-")[0]:Locale.CHINA.getLanguage();
    }

}

/**
 * E5Projects @ org.reed.listener/FrameworkListener.java
 */
package org.reed.listener;

import org.reed.exceptions.EnderRuntimeException;
import org.reed.interceptor.ReedInterceptor;
import org.reed.log.ReedLogger;
import org.reed.system.ReedContext;
import org.reed.utils.EnderUtil;
import org.reed.utils.StringUtil;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * @author chenxiwen
 * @createTime 2020年07月31日 上午11:04
 * @description
 */
public class FrameworkListener implements SpringApplicationRunListener {

    //springboot starts first, if spring-cloud included, there will be another scope!
    //Reference:org.springframework.cloud.bootstrap.BootstrapApplicationListener#bootstrapServiceContext
    private final BootupScope bootupScope;

    @Autowired(required = false)
    private NacosAutoServiceRegistration nacosAutoServiceRegistration;

    public FrameworkListener(SpringApplication application, String[] args){
        boolean isEmptyEnv = true;
        try {
            Field environment = application.getClass().getDeclaredField("environment");
            if(environment != null){
                environment.setAccessible(true);
                isEmptyEnv = environment.get(application)==null;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if(isEmptyEnv && application.getWebApplicationType()!=WebApplicationType.NONE){
            bootupScope = BootupScope.springboot;
        }else{
            bootupScope = BootupScope.springcloud;
        }

        ReedLogger.debug(EnderUtil.devInfo()+
                " - #"+bootupScope.name()+
                "# found main class: "+(application.getMainApplicationClass()==null?"NULL(application.mainClass)":application.getMainApplicationClass().getName()));

        //i don't like the Dubbo AD. so...  ~_~'
//        application.setListeners(application.getListeners().stream()
//                .filter(listener -> !listener.getClass().getName()
//                        .equalsIgnoreCase("org.apache.dubbo.spring.boot.context.event.WelcomeLogoApplicationListener"))
//                .collect(Collectors.toSet()));

        Set<ApplicationListener<?>> set = application.getListeners().stream().filter(listener -> listener.getClass().getName()
                .equalsIgnoreCase("org.apache" +
                        ".dubbo.spring.boot.context.event.WelcomeLogoApplicationListener")).collect(Collectors.toSet());
        set.forEach(listener -> {
            try {
                Field field = listener.getClass().getDeclaredField("processed");
                field.setAccessible(true);
                AtomicBoolean processed = (AtomicBoolean) field.get(listener);
                processed.compareAndSet(false, true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e){
                e.printStackTrace();
            }
        });
    }

    /**
     * Called immediately when the run method has first started. Can be used for very
     * early initialization.
     */
//    @Override
    public void starting() {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("internal framework detect there is a springboot application is starting...");
        }
    }

    /**
     * Called once the environment has been prepared, but before the
     * {@link ApplicationContext} has been created.
     *
     * @param environment the environment
     */
//    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application has already prepared it's own environment: "+environment.toString());
        }
    }

    /**
     * Called once the {@link ApplicationContext} has been created and prepared, but
     * before sources have been loaded.
     *
     * @param context the application context
     */
    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application has already prepared it's own context: "+context.getId()+
                    " - "+context.getApplicationName());
        }
    }

    /**
     * Called once the application context has been loaded but before it has been
     * refreshed.
     *
     * @param context the application context
     */
    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application has already loaded it's own context: "+context.getId()+" - "+context.getApplicationName());
        }
    }

    /**
     * The context has been refreshed and the application has started but
     * {@link CommandLineRunner CommandLineRunners} and {@link ApplicationRunner
     * ApplicationRunners} have not been called.
     *
     * @param context the application context.
     * @since 2.0.0
     */
    @Override
    public void started(ConfigurableApplicationContext context) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application has already started with context: "+context.getId()+" - "+context.getApplicationName());
        }
    }

    /**
     * Called immediately before the run method finishes, when the application context has
     * been refreshed and all {@link CommandLineRunner CommandLineRunners} and
     * {@link ApplicationRunner ApplicationRunners} have been called.
     *
     * @param context the application context.
     * @since 2.0.0
     */
    @Override
    public void running(ConfigurableApplicationContext context) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application is going to finish 'Run' method, context: "+context.getId()+" - "+context.getApplicationName()+" has been refreshed!");
        }

        if(bootupScope == BootupScope.springboot){
            ReedLogger.debug("Dubbo protocol service detected as below: ");
            Collection<Exporter<?>> exporters = DubboProtocol.getDubboProtocol().getExporters();
            exporters.stream().forEach(exporter -> ReedLogger.debug(exporter.getInvoker().getUrl().toFullString()));
        }

        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            Map<String, WebMvcConfigurer> webMvcConfigurerBeans = context.getBeansOfType(WebMvcConfigurer.class);
            webMvcConfigurerBeans.forEach((k,v) -> System.err.println("Found Spring WebMvcConfigurerBean: "+k+"@"+v.getClass().getName()));
            System.err.println("--------------------------------------------");
            Map<String, HandlerInterceptor> handlerInterceptorMap = context.getBeansOfType(HandlerInterceptor.class);
            handlerInterceptorMap.forEach((k,v)->System.err.println("Found Spring HandlerInterceptorBean: "+k+"@"+v.getClass().getName()));
            handlerInterceptorMap.forEach((k,v)->{
                if(v instanceof ReedInterceptor){
                    System.err.println("ReedInterceptor found@"+v.getClass().getName());
                }else if(v.getClass().getPackage().toString().contains("org.springframework")){
                    System.err.println("SpringFramework Interceptor found@"+v.getClass().getName());
                }else{
                    throw new RuntimeException("Invalidate Interceptor found@"+v.getClass().getName());
                }
            });
        }
        //check if developer did not follow ReedInterceptor
        Map<String, HandlerInterceptor> handlerInterceptorMap = context.getBeansOfType(HandlerInterceptor.class);
//        handlerInterceptorMap.forEach((k,v)->System.out.println("Found Spring HandlerInterceptorBean: "+k+"@"+v.getClass().getName()));
        handlerInterceptorMap.forEach((k,v)->{
            if(v instanceof ReedInterceptor){
                ReedLogger.debug("ReedInterceptor found@"+v.getClass().getName());
            }else if(v.getClass().getPackage().toString().contains("org.springframework")){
                ReedLogger.debug("SpringFramework Interceptor found@"+v.getClass().getName());
            }else{
                throw new EnderRuntimeException("Invalidate Interceptor found@"+v.getClass().getName());
            }
        });

    }

    /**
     * Called when a failure occurs when running the application.
     *
     * @param context   the application context or {@code null} if a failure occurred before
     *                  the context was created
     * @param exception the failure
     * @since 2.0.0
     */
    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        if(!StringUtil.isEmpty(ReedContext.getString("mode")) && ReedContext.getString("mode").equalsIgnoreCase("debug")){
            System.err.println("#"+bootupScope.name()+"# internal framework detect there is a springboot application is dying with context: "+context.getId()+" - "+context.getApplicationName()+", because of: "+exception.getMessage());
        }
    }

    public enum BootupScope{
        springboot, springcloud
    }
}

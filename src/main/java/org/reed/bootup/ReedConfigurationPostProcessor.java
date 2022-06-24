/**
 * E5Projects @ org.reed.bootup/ReedConfigFilePreProcessor.java
 */
package org.reed.bootup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author chenxiwen
 * @createTime 2019年12月31日 下午3:57
 * @description
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@Configuration
@EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
public class ReedConfigurationPostProcessor implements EnvironmentPostProcessor {
    public static final String REED_CONFIG_FILE_NAME = "reed";

    private final YamlPropertySourceLoader reedLoader = new YamlPropertySourceLoader();
    private final Resource reedConfig = new ClassPathResource(REED_CONFIG_FILE_NAME + ".yml");

    /**
     * Post-process the given {@code environment}.
     *
     * @param environment the environment to post-process
     * @param application the application to which the environment belongs
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        //deal with reed.yml first
        PropertySource<?> reedPropertySource = loadConfigration(reedConfig);
        if(reedPropertySource != null){
            org.reed.bootup.ReedPropertySourceHandler.ignorePrefixAndItems((MapPropertySource)reedPropertySource);
//            ReedPropertySourceHandler.attachEnvironment((MapPropertySource)reedPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.handleReedPlaceHolders((MapPropertySource)reedPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.fillSystemProperties(reedPropertySource);
            environment.getPropertySources().addFirst(reedPropertySource);
        }

        //deal with bootstrap.yml
        List<PropertySource<?>> bootstrapPropertySources = getPropertySources(environment,
        OriginTrackedMapPropertySource.class, BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME);
        for(PropertySource<?> bootstrapPropertySource : bootstrapPropertySources){
            org.reed.bootup.ReedPropertySourceHandler.ignorePrefixAndItems((MapPropertySource)bootstrapPropertySource);
//            ReedPropertySourceHandler.attachEnvironment((MapPropertySource)bootstrapPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.override((MapPropertySource)bootstrapPropertySource, (MapPropertySource)reedPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.handleReedPlaceHolders((MapPropertySource)bootstrapPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.fillSystemProperties(bootstrapPropertySource);
        }

//        if(!isCustomEnvironment(application)){
//            //deal with applicaiton.yml
//            List<PropertySource<?>> applicationPropertySources = getPropertySources(environment, OriginTrackedMapPropertySource.class, "classpath:/application:");
//            for(PropertySource<?> applicationPropertySource : applicationPropertySources){
//                ReedPropertySourceHandler.ignorePrefixAndItems((MapPropertySource)applicationPropertySource);
//                ReedPropertySourceHandler.override((MapPropertySource)applicationPropertySource, (MapPropertySource)reedPropertySource);
//                ReedPropertySourceHandler.handleReedPlaceHolders((MapPropertySource)applicationPropertySource);
//                ReedPropertySourceHandler.fillSystemProperties(applicationPropertySource);
//            }
//
//            ReedStarter.reedApplication.handleReedUnifiedConfiguration();
//        }else{
//            ReedStarter.reedApplication.handleReedUnifiedConfiguration();
//        }

        //deal with applicaiton.yml
        List<PropertySource<?>> applicationPropertySources = getPropertySources(environment, OriginTrackedMapPropertySource.class, "classpath:/application:");
        for(PropertySource<?> applicationPropertySource : applicationPropertySources){
            org.reed.bootup.ReedPropertySourceHandler.ignorePrefixAndItems((MapPropertySource)applicationPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.override((MapPropertySource)applicationPropertySource, (MapPropertySource)reedPropertySource);
            org.reed.bootup.ReedPropertySourceHandler.handleReedPlaceHolders((MapPropertySource)applicationPropertySource);
            ReedPropertySourceHandler.fillSystemProperties(applicationPropertySource);
        }

        application.setEnvironment(environment);

        ReedStarter.reedApplication.handleReedUnifiedConfiguration();

    }

    private PropertySource<?> loadConfigration(Resource resource){
        if(resource.exists()){
            try{
                List<PropertySource<?>> propertySourceList = this.reedLoader.load("reed", resource);
                if(propertySourceList.size()>0){
                    PropertySource<?> propertySource = propertySourceList.get(0);
                    return propertySource;
                }else{
                    return null;
                }
            }catch(IOException e){
                throw new IllegalStateException("Failed to load yaml configuration from reed.yml", e);
            }
        }else {
            System.out.println("Reed-Framework Cannot Found reed.yaml. Do Nothing In This Section!");
        }
        return null;
    }

    private List<PropertySource<?>> getPropertySources(final ConfigurableEnvironment environment, final Class<?> propertySourceType, final String keyWord){
        List<PropertySource<?>> list = new ArrayList<>();
        for(PropertySource<?> propertySource : environment.getPropertySources()){
            if(propertySource.getName().contains(keyWord) && propertySource.getClass() == propertySourceType){
                list.add(propertySource);
            }
        }
        return list;
    }

    private boolean isCustomEnvironment(final SpringApplication application){
        boolean result = false;
        try {
            Field field = application.getClass().getDeclaredField("isCustomEnvironment");
            field.setAccessible(true);
            result = field.getBoolean(application);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return result;
    }
}

/**
 * E5Projects @ org.reed.bootup/ReedConfigurationInitializer.java
 */
package org.reed.bootup;

import org.reed.log.ReedLogger;
import org.reed.utils.EnderUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.cloud.bootstrap.config.PropertySourceBootstrapProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chenxiwen
 * @createTime 2019年12月31日 下午4:51
 * @description
 */
@Configuration
@EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
public class ReedConfigurationInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered{
    private final int order = Ordered.LOWEST_PRECEDENCE;

    /**
     * Initialize the given application context.
     *
     * @param applicationContext the application to configure
     */
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MutablePropertySources mps =  environment.getPropertySources();
        if(null == mps || mps.size() == 0){
            throw new RuntimeException("MutablePropertySources from environment is empty!");
        }
        PropertySource reedPropertyResource = mps.get("reed");
//        PropertySource<?> bootstrapPropertySource = mps.get(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME
//                + "Properties");
        PropertySource<?> bootstrapPropertySource = getBootstrapPropertySrouce(mps);
        if(bootstrapPropertySource != null && bootstrapPropertySource instanceof CompositePropertySource){
            bootstrapPropertySource = bootstrapPropertySource;
            Collection<PropertySource<?>> compositeSources =  ((CompositePropertySource) bootstrapPropertySource).getPropertySources();
            for(PropertySource propertySource : compositeSources){
                if(propertySource instanceof CompositePropertySource && propertySource.getName().contains("NACOS")){
                    ReedLogger.info(EnderUtil.devInfo()+" - Found NACOS Configuration -> "+propertySource.getName());
                    Collection<PropertySource<?>> nacosPropertySources = ((CompositePropertySource) propertySource).getPropertySources();
                    for(PropertySource nacosPropertySource : nacosPropertySources){
                        org.reed.bootup.ReedPropertySourceHandler.ignorePrefixAndItems((MapPropertySource)nacosPropertySource);
//                        ReedPropertySourceHandler.attachEnvironment((MapPropertySource)nacosPropertySource);
                        org.reed.bootup.ReedPropertySourceHandler.override((MapPropertySource)nacosPropertySource, (MapPropertySource)reedPropertyResource);
                        org.reed.bootup.ReedPropertySourceHandler.handleReedPlaceHolders((MapPropertySource)nacosPropertySource);
                        ReedPropertySourceHandler.fillSystemProperties(nacosPropertySource, true);
                    }
                }
            }
        }
        ReedStarter starter = ReedStarter.reedApplication;
        starter.verifyReedFramework(starter.getModuleName());
    }


    private PropertySource<?> getBootstrapPropertySrouce(final MutablePropertySources mps){
        PropertySource<?> propertySource = mps.get(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME+"Properties");
        if(propertySource == null){
            CompositePropertySource compositePropertySource = new CompositePropertySource(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME+"Properties");
            Map<String, String> remoteConfig = new LinkedHashMap<>();
            mps.stream().filter(ps ->
                    ps.getName()
                            .startsWith(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME+"Properties"))
                    .collect(Collectors.toList())
                    .stream()
                    .forEach(ps -> {
                        if(ps !=null &&  ps.getSource() instanceof Map){
                            Map map = (Map) ps.getSource();
                            CompositePropertySource cps = new CompositePropertySource("NACOS-"+ps.getName());
                            cps.addPropertySource(new MapPropertySource(ps.getName(), map));
                            compositePropertySource.addPropertySource(cps);
                        }
                    });
            return compositePropertySource;
        }else{
            return propertySource;
        }
    }


    /**
     * Get the order value of this object.
     * <p>Higher values are interpreted as lower priority. As a consequence,
     * the object with the lowest value has the highest priority (somewhat
     * analogous to Servlet {@code load-on-startup} values).
     * <p>Same order values will result in arbitrary sort positions for the
     * affected objects.
     *
     * @return the order value
     * @see #HIGHEST_PRECEDENCE
     * @see #LOWEST_PRECEDENCE
     */
    @Override
    public int getOrder() {
        return order;
    }
}

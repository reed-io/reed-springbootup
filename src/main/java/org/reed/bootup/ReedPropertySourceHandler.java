/**
 * E5Projects @ org.reed.bootup/ReedPropertySourceHandler.java
 */
package org.reed.bootup;

import org.reed.system.ReedContext;
import org.reed.utils.ArrayUtil;
import org.reed.utils.DESUtil;
import org.reed.utils.EnderUtil;
import org.reed.utils.StringUtil;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author chenxiwen
 * @createTime 2019年12月31日 下午2:49
 * @description
 */
public final class ReedPropertySourceHandler {
    private static final String mode = ReedContext.getString("mode", "");
    private static final String UNIFIED_ENVIRONMENT_CONFIG_DATAID_KEY = "cloud.nacos.config.ext-config[0].data-id";
    private static final String UNIFIED_ENVIRONMENT_CONFIG_DATAID_VALUE = "environment";
    private static final String UNIFIED_ENVIRONMENT_CONFIG_GROUP_KEY = "cloud.nacos.config.ext-config[0].group";
    private static final String UNIFIED_ENVIRONMENT_CONFIG_GROUP_VALUE = "parent";

    protected static void ignorePrefix(final MapPropertySource propertySource){
        Map<String, Object> config = propertySource.getSource();
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            //ignore config starts with some prefix
            for(String prefix : org.reed.bootup.SpringBootBootup.IGNORE_PREFIX){
                if(key.startsWith(prefix+ org.reed.bootup.SpringBootBootup.DOT) || key.equalsIgnoreCase(prefix)){
                    Object value = config.get(key);
                    iterator.remove();
                    if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                        System.out.println(EnderUtil.getDevInfo() + " - Reed Framework Has Removed [" + key + "=" + value + "] From Configuration File When It Loaded!");
                    }
                    break;
                }
            }
        }
    }

    protected static void ignoreItems(final MapPropertySource propertySource){
        Map<String, Object> config = propertySource.getSource();
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            //ignore config starts with some prefix
            for(String prefix : org.reed.bootup.SpringBootBootup.IGNORE_ITEMS){
                if(key.equalsIgnoreCase(prefix)){
                    Object value = config.get(key);
                    iterator.remove();
                    if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                        System.out.println(EnderUtil.getDevInfo() + " - Reed Framework Has Removed [" + key + "=" + value + "] From Configuration File When It Loaded!");
                    }
                    break;
                }
            }
        }
    }

    protected static void ignorePrefixAndItems(final MapPropertySource propertySource){
        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
            System.out.println(EnderUtil.getDevInfo()+" - ignorePrefixAndItems, Property Source:["+propertySource.getName()+"]");
        }
        Collection<String> ignores = ArrayUtil.merge(org.reed.bootup.SpringBootBootup.IGNORE_PREFIX, org.reed.bootup.SpringBootBootup.IGNORE_ITEMS);
        Map<String, Object> config = propertySource.getSource();
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            //ignore config starts with some prefix
            for(String prefix : ignores){
                if(key.startsWith(prefix+ SpringBootBootup.DOT) || key.equalsIgnoreCase(prefix)){
                    Object value = config.get(key);
                    iterator.remove();
                    if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                        System.out.println(EnderUtil.getDevInfo() + " - Reed Framework Has Removed [" + key + "=" + value + "] From Configuration File When It Loaded!");
                    }
                    break;
                }
            }
        }
    }

    protected static void override(final MapPropertySource target, final MapPropertySource source){
        if(target == null || source == null){
            return;
        }
        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
            System.out.println(EnderUtil.getDevInfo()+" - Reset Reed Framework Property Source:["+target.getName()+"]");
        }
        if(target == null || target.getSource().size()==0 || source == null || source.getSource().size() == 0){
            return;
        }
        Map<String, Object> config = target.getSource();
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            if(source.containsProperty(key)){
                if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                    System.out.println(EnderUtil.getDevInfo()+" - override {"+key+", "+config.get(key).toString()+"} to {"+key+", "+source.getSource().get(key)+"}");
                }
                updateMap(target.getSource(), key, source.getProperty(key).toString());
            }
        }
    }

    private static void updateMap(final Map m, final String key, final String value){
        synchronized (m){
            m.put(key, value);
        }
    }


    protected static void handleReedPlaceHolders(final MapPropertySource propertySource){
        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
            System.out.println(EnderUtil.getDevInfo()+" - Handle ReedFramework Placeholders, Property Source:["+propertySource.getName()+"]");
        }
        Map<String, Object> config = propertySource.getSource();
        Iterator<Map.Entry<String, Object>> iterator = config.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            if(config.get(key) == null){
                System.out.println(EnderUtil.getDevInfo() + " - there is no value found with key: "+key+", ignore this config!!!");
                continue;
            }
            String value = config.get(key).toString();
            if(StringUtil.isContains(StringUtil.Reed_ENV, value)){ //读取自定义标识ReedEnv{} 匹配成功则表示要去环境变量中读取ReedEnv{}中的值
                if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                    System.out.println(EnderUtil.getDevInfo() + " - found Matched Pattern[" + StringUtil.Reed_ENV.toString() + "] in " + value);
                }
                List<String> matchedList = StringUtil.getMatched(StringUtil.Reed_ENV, value);
                for(String str : matchedList){
                    if(!StringUtil.isEmpty(str)){
                        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                            System.out.println(EnderUtil.getDevInfo() + " - placeholder[" + str + "]");
                        }
                        String envKey = StringUtil.extractVal(str);
                        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                            System.out.println(EnderUtil.getDevInfo() + " - extract environment key from placeholder[" + str + "] as " + envKey);
                        }
                        String envVal = System.getenv(envKey);
                        envVal = envVal == null?"":envVal;
                        if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                            System.out.println(EnderUtil.getDevInfo() + " - get environment: " + envKey + "=" + envVal);
                        }
                        if(!StringUtil.isEmpty(envVal)){
                            if(StringUtil.isMatched(StringUtil.Reed_CIPHER, envVal)){
                                envVal = StringUtil.decryptCiphertext(envVal, DESUtil.DEFAULT_SECURITY_CODE);
                            }
                            value = value.replace(str, envVal);
                            updateMap(config, key, value);
                            if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                                System.out.println(EnderUtil.getDevInfo() + " After SetEnv -> EnvKey:" + key + ", EnvVal:" + config.get(key).toString() + ", EnvValInstance:" + config.get(key).getClass());
                            }
                        }else{
                            if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")){
                                System.out.println(EnderUtil.getDevInfo()+" - "+envKey+" is empty skip this line!");
                            }
                        }
                    }
                }

            }
            if(StringUtil.isContains(StringUtil.Reed_CIPHER, value)){  //匹配到了加密串ReedCipher{xxx}
                value = StringUtil.decryptCiphertext(value, DESUtil.DEFAULT_SECURITY_CODE);
                updateMap(config, key, value);
                if(!StringUtil.isEmpty(mode) && mode.equalsIgnoreCase("debug")) {
                    System.out.println(EnderUtil.getDevInfo() + " After SetEnv -> EnvKey:" + key + ", EnvVal:" + config.get(key).toString() + ", EnvValInstance:" + config.get(key).getClass());
                }
            }

        }
    }

    protected static void fillSystemProperties(final PropertySource<?> propertySource){
        fillSystemProperties(propertySource, false);
    }

    protected static void fillSystemProperties(final PropertySource<?> propertySource, boolean force){
        for(String Reed_CONFIG : ReedStarter.FRAMEWORK_CONFIG){
            if(propertySource.containsProperty(Reed_CONFIG)){
                if(force || !System.getProperties().containsKey(Reed_CONFIG)){
//                    ReedStarter.putArgs(Reed_CONFIG, propertySource.getProperty(Reed_CONFIG).toString());
                    ReedStarter.reedEnvMap.put(Reed_CONFIG, propertySource.getProperty(Reed_CONFIG).toString());
                }
            }
        }
    }


    protected static void attachEnvironment(final MapPropertySource propertySource){
        Map<String, Object> map = propertySource.getSource();
        updateMap(map, UNIFIED_ENVIRONMENT_CONFIG_DATAID_KEY, UNIFIED_ENVIRONMENT_CONFIG_DATAID_VALUE);
        updateMap(map, UNIFIED_ENVIRONMENT_CONFIG_GROUP_KEY, UNIFIED_ENVIRONMENT_CONFIG_GROUP_VALUE);
    }
}

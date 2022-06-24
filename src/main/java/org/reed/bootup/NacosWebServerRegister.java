/**
 * E5Projects @ org.reed.bootup/NacosWebServerRegister.java
 */
package org.reed.bootup;

import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.stereotype.Component;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import java.lang.management.ManagementFactory;
import java.util.Set;

/**
 * @author chenxiwen
 * @createTime 2020年08月25日 上午11:27
 * @description
 */
@Component
@ConditionalOnDiscoveryEnabled
public class NacosWebServerRegister implements ApplicationRunner {

    @Autowired(required = false)
    private NacosAutoServiceRegistration nacosAutoServiceRegistration;
    /**
     * Callback used to run the bean.
     *
     * @param args incoming application arguments
     * @throws Exception on error
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(nacosAutoServiceRegistration == null){
            throw new RuntimeException("Register Component Not Found!");
        }
//        ReedLogger.debug("WebServer port from ServiceRegistration:"+nacosAutoServiceRegistration.getClass().getDeclaredMethod("getPort").invoke(nacosAutoServiceRegistration));
        nacosAutoServiceRegistration.setPort(getWebServerWorkingPort());
        nacosAutoServiceRegistration.start();
    }


    private int getWebServerWorkingPort() throws MalformedObjectNameException {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"),
                Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
        String port = objectNames.iterator().next().getKeyProperty("port");

        return Integer.parseInt(port);
    }
}

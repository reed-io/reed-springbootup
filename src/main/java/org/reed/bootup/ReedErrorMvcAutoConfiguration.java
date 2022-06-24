/**
 * E5Projects @ org.reed.bootup/ReedErrorMvcAutoConfiguration.java
 */
package org.reed.bootup;

import org.reed.utils.StringUtil;
import org.reed.utils.TimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
//import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.DefaultErrorViewResolver;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.util.HtmlUtils;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chenxiwen
 * @createTime 2019年12月08日 下午4:41
 * @description
 */
//@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
// Load before the main WebMvcAutoConfiguration so that the error View is available
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
//@EnableConfigurationProperties({ ServerProperties.class, ResourceProperties.class, WebMvcProperties.class })
@EnableConfigurationProperties({ ServerProperties.class, WebMvcProperties.class })
public class ReedErrorMvcAutoConfiguration {

    private final ServerProperties serverProperties;

    private final DispatcherServletPath dispatcherServletPath;

    private final List<ErrorViewResolver> errorViewResolvers;

    public ReedErrorMvcAutoConfiguration(ServerProperties serverProperties, DispatcherServletPath dispatcherServletPath,
                                     ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        this.serverProperties = serverProperties;
        this.dispatcherServletPath = dispatcherServletPath;
        this.errorViewResolvers = errorViewResolvers.orderedStream().collect(Collectors.toList());
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    public DefaultErrorAttributes errorAttributes() {
//        return new DefaultErrorAttributes(this.serverProperties.getError().isIncludeException());
        return new DefaultErrorAttributes();
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes) {
        return new BasicErrorController(errorAttributes, this.serverProperties.getError(), this.errorViewResolvers);
    }

    @Bean
    public ReedErrorPageCustomizer reedErrorPageCustomizer() {
        return new ReedErrorPageCustomizer(this.serverProperties, this.dispatcherServletPath);
    }

    @Bean
    public static ReedPreserveErrorControllerTargetClassPostProcessor reedPreserveErrorControllerTargetClassPostProcessor() {
        return new ReedPreserveErrorControllerTargetClassPostProcessor();
    }

//    @Configuration(proxyBeanMethods = false)
//    @EnableConfigurationProperties({WebProperties.class, WebMvcProperties.class})
    @EnableConfigurationProperties({WebMvcProperties.class})
    static class DefaultErrorViewResolverConfiguration {

        private final ApplicationContext applicationContext;

//        private final WebProperties.Resources resources;

        private final ResourceProperties resourceProperties;

        DefaultErrorViewResolverConfiguration(ApplicationContext applicationContext,
                                              ResourceProperties resourceProperties) {
            this.applicationContext = applicationContext;
            this.resourceProperties = resourceProperties;
        }
//        DefaultErrorViewResolverConfiguration(ApplicationContext applicationContext,
//                                              WebProperties.Resources resources) {
//            this.applicationContext = applicationContext;
//            this.resources = resources;
//        }

        @Bean
        @ConditionalOnBean(DispatcherServlet.class)
        @ConditionalOnMissingBean
        public DefaultErrorViewResolver conventionErrorViewResolver() {
            return new DefaultErrorViewResolver(this.applicationContext, this.resourceProperties);
//            return new DefaultErrorViewResolver(this.applicationContext, this.resources);
        }

    }

    @Configuration
    @ConditionalOnProperty(prefix = "server.error.whitelabel", name = "enabled", matchIfMissing = true)
    @Conditional(ErrorTemplateMissingCondition.class)
    protected static class WhitelabelErrorViewConfiguration {

        private final StaticView defaultErrorView = new StaticView();

        @Bean(name = "error")
        @ConditionalOnMissingBean(name = "error")
        public View defaultErrorView() {
            return this.defaultErrorView;
        }

        // If the user adds @EnableWebMvc then the bean name view resolver from
        // WebMvcAutoConfiguration disappears, so add it back in to avoid disappointment.
        @Bean
        @ConditionalOnMissingBean
        public BeanNameViewResolver beanNameViewResolver() {
            BeanNameViewResolver resolver = new BeanNameViewResolver();
            resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
            return resolver;
        }

    }

    /**
     * {@link SpringBootCondition} that matches when no error template view is detected.
     */
    private static class ErrorTemplateMissingCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("ErrorTemplate Missing");
            TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(context.getClassLoader());
            TemplateAvailabilityProvider provider = providers.getProvider("error", context.getEnvironment(),
                    context.getClassLoader(), context.getResourceLoader());
            if (provider != null) {
                return ConditionOutcome.noMatch(message.foundExactly("template from " + provider));
            }
            return ConditionOutcome.match(message.didNotFind("error template view").atAll());
        }

    }

    /**
     * Simple {@link View} implementation that writes a default HTML error page.
     */
    private static class StaticView implements View {

        private static final Log logger = LogFactory.getLog(StaticView.class);

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
                throws Exception {
            if (response.isCommitted()) {
                String message = getMessage(model);
                logger.error(message);
                return;
            }
            StringBuilder builder = new StringBuilder();
            Date timestamp = (Date) model.get("timestamp");
            Object message = model.get("message");
            Object trace = model.get("trace");
            Object data = model.get("data");
            Object reedResult = model.get("reedResult");
            if (response.getContentType() == null) {
                response.setContentType(getContentType());
            }
            builder.append("<html><body><h1>Reed Error Page</h1>").append(
                    "<p>There is something wrong with your request! Just provide the following information to chenxiwenender@163.com</p>");
            builder.append("<div>Service Standard Response Format：<br/>").append(reedResult).append("</div>");
            builder.append("Request Info: <br/>");
            builder.append("<div>Protocol: "+request.getProtocol()+"</div>");
            Enumeration<String> headers = request.getHeaderNames();
            while(headers.hasMoreElements()){
                String header = headers.nextElement();
                builder.append("<div>&nbsp;&nbsp;Header: ").append(header+":"+request.getHeader(header)).append("</div>");
            }
            Enumeration<String> parameters = request.getParameterNames();
            while(parameters.hasMoreElements()){
//                String param = parameters.nextElement();
//                builder.append("<div>&nbsp;&nbsp;Parameter: ").append(param+":"+request.getParameter(param)).append("</div>");
                String param = parameters.nextElement();
                String paramValue = request.getParameter(param);
                if (StringUtil.isEmpty(paramValue)) {
                    paramValue = "";
                }
//                paramValue = paramValue.replace("<", "&lt;").replace(">", "&gt;");
                paramValue = HtmlUtils.htmlEscape(paramValue);
                builder.append("<div>&nbsp;&nbsp;Parameter: ").append(param + ":" + paramValue)
                        .append("</div>");

            }
            builder.append("<div id='created'> Happend Time: ").append(TimeUtil.getDateTime(timestamp)).append("</div>")
            .append("<div>There was an unexpected error (type=").append(htmlEscape(model.get("error")))
            .append(", status=").append(htmlEscape(model.get("status"))).append(").</div>");
            if (message != null) {
                builder.append("<div>Message: ").append(htmlEscape(message)).append("</div>");
            }
            if (trace != null) {
                builder.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(trace)).append("</div>");
            }
            builder.append("</body></html>");
            response.getWriter().append(builder.toString());
        }

        private String htmlEscape(Object input) {
            return (input != null) ? HtmlUtils.htmlEscape(input.toString()) : null;
        }

        private String getMessage(Map<String, ?> model) {
            Object path = model.get("path");
            String message = "Cannot render error page for request [" + path + "]";
            if (model.get("message") != null) {
                message += " and exceptions [" + model.get("message") + "]";
            }
            message += " as the response has already been committed.";
            message += " As a result, the response may have the wrong status code.";
            return message;
        }

        @Override
        public String getContentType() {
            return "text/html";
        }

    }

    /**
     * {@link WebServerFactoryCustomizer} that configures the server's error pages.
     */
    private static class ReedErrorPageCustomizer implements ErrorPageRegistrar, Ordered {

        private final ServerProperties properties;

        private final DispatcherServletPath dispatcherServletPath;

        protected ReedErrorPageCustomizer(ServerProperties properties, DispatcherServletPath dispatcherServletPath) {
            this.properties = properties;
            this.dispatcherServletPath = dispatcherServletPath;
        }

        @Override
        public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
            ErrorPage errorPage = new ErrorPage(
                    this.dispatcherServletPath.getRelativePath(this.properties.getError().getPath()));
            errorPageRegistry.addErrorPages(errorPage);
        }

        @Override
        public int getOrder() {
            return 0;
        }

    }

    /**
     * {@link BeanFactoryPostProcessor} to ensure that the target class of ErrorController
     * MVC beans are preserved when using AOP.
     */
    static class ReedPreserveErrorControllerTargetClassPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            String[] errorControllerBeans = beanFactory.getBeanNamesForType(ErrorController.class, false, false);
            for (String errorControllerBean : errorControllerBeans) {
                try {
                    beanFactory.getBeanDefinition(errorControllerBean)
                            .setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
                }
                catch (Throwable ex) {
                    // Ignore
                }
            }
        }

    }

}


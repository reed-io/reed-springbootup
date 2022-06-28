/**
 * IdeaProject @ org.reed.log/LogbackConfigurator.java
 */
package org.reed.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chenxiwen
 * @createTime 2019年07月29日 下午4:38
 * @description
 */
class LogbackConfigurator {
    private final LoggerContext context;

    LogbackConfigurator(LoggerContext context) {
        Assert.notNull(context, "Context must not be null");
        this.context = context;
    }

    public LoggerContext getContext() {
        return this.context;
    }

    public Object getConfigurationLock() {
        return this.context.getConfigurationLock();
    }

    public void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
        Assert.hasLength(conversionWord, "Conversion word must not be empty");
        Assert.notNull(converterClass, "Converter class must not be null");
        Map<String, String> registry = (Map)this.context.getObject("PATTERN_RULE_REGISTRY");
        if (registry == null) {
            registry = new HashMap();
            this.context.putObject("PATTERN_RULE_REGISTRY", registry);
        }

        ((Map)registry).put(conversionWord, converterClass.getName());
    }

    public void appender(String name, Appender<?> appender) {
        appender.setName(name);
        this.start(appender);
    }

    public void logger(String name, Level level) {
        this.logger(name, level, true);
    }

    public void logger(String name, Level level, boolean additive) {
        this.logger(name, level, additive, null);
    }

    public void logger(String name, Level level, boolean additive, Appender<ILoggingEvent> appender) {
        Logger logger = this.context.getLogger(name);
        if (level != null) {
            logger.setLevel(level);
        }

        logger.setAdditive(additive);
        if (appender != null) {
            logger.addAppender(appender);
        }

    }

    @SafeVarargs
    public final void root(Level level, Appender... appenders) {
        Logger logger = this.context.getLogger("ROOT");
        if (level != null) {
            logger.setLevel(level);
        }

        Appender[] var4 = appenders;
        int var5 = appenders.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Appender<ILoggingEvent> appender = var4[var6];
            logger.addAppender(appender);
        }

    }

    public void start(LifeCycle lifeCycle) {
        if (lifeCycle instanceof ContextAware) {
            ((ContextAware)lifeCycle).setContext(this.context);
        }

        lifeCycle.start();
    }
}

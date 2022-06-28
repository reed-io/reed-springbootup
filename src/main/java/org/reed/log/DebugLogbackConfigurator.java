/**
 * IdeaProject @ org.reed.log/DebugLogbackConfigurator.java
 */
package org.reed.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.InfoStatus;
import org.reed.log.LogbackConfigurator;

/**
 * @author chenxiwen
 * @createTime 2019年07月29日 下午4:39
 * @description
 */
final class DebugLogbackConfigurator extends org.reed.log.LogbackConfigurator {
    DebugLogbackConfigurator(LoggerContext context) {
        super(context);
    }

    public void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
        this.info("Adding conversion rule of type '" + converterClass.getName() + "' for word '" + conversionWord);
        super.conversionRule(conversionWord, converterClass);
    }

    public void appender(String name, Appender<?> appender) {
        this.info("Adding appender '" + appender + "' named '" + name + "'");
        super.appender(name, appender);
    }

    public void logger(String name, Level level, boolean additive, Appender<ILoggingEvent> appender) {
        this.info("Configuring logger '" + name + "' with level '" + level + "'. Additive: " + additive);
        if (appender != null) {
            this.info("Adding appender '" + appender + "' to logger '" + name + "'");
        }

        super.logger(name, level, additive, appender);
    }

    public void start(LifeCycle lifeCycle) {
        this.info("Starting '" + lifeCycle + "'");
        super.start(lifeCycle);
    }

    private void info(String message) {
        this.getContext().getStatusManager().add(new InfoStatus(message, this));
    }
}
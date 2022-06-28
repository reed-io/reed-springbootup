/**
 * IdeaProject @ org.reed.log/LogbackLoggingSystem.java
 */
package org.reed.log;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusListenerConfigHelper;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.reed.log.SpringBootJoranConfigurator;
import org.slf4j.ILoggerFactory;
import org.slf4j.Marker;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.*;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * @author chenxiwen
 * @createTime 2019年07月29日 下午4:29
 * @description
 */
public final class LogbackLoggingSystem  extends Slf4JLoggingSystem {
    private static final String CONFIGURATION_FILE_PROPERTY = "logback.configurationFile";
    private static final AbstractLoggingSystem.LogLevels<Level> LEVELS = new AbstractLoggingSystem.LogLevels();
    private static final TurboFilter FILTER;

    public LogbackLoggingSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    protected String[] getStandardConfigLocations() {
        return new String[]{"logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml"};
    }

    public void beforeInitialize() {
        LoggerContext loggerContext = this.getLoggerContext();
        if (!this.isAlreadyInitialized(loggerContext)) {
            super.beforeInitialize();
            loggerContext.getTurboFilterList().add(FILTER);
        }
    }

    public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
        LoggerContext loggerContext = this.getLoggerContext();
        if (!this.isAlreadyInitialized(loggerContext)) {
            super.initialize(initializationContext, configLocation, logFile);
            loggerContext.getTurboFilterList().remove(FILTER);
            this.markAsInitialized(loggerContext);
            if (StringUtils.hasText(System.getProperty("logback.configurationFile"))) {
                this.getLogger(org.springframework.boot.logging.logback.LogbackLoggingSystem.class.getName()).warn("Ignoring 'logback.configurationFile' system property. Please use 'logging.config' instead.");
            }

        }
    }

    protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
        LoggerContext context = this.getLoggerContext();
        this.stopAndReset(context);
        boolean debug = Boolean.getBoolean("logback.debug");
        if (debug) {
            StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
        }

//        LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context) : new LogbackConfigurator(context);
//        Environment environment = initializationContext.getEnvironment();
//        context.putProperty("LOG_LEVEL_PATTERN", environment.resolvePlaceholders("${logging.pattern.level:${LOG_LEVEL_PATTERN:%5p}}"));
//        context.putProperty("LOG_DATEFORMAT_PATTERN", environment.resolvePlaceholders("${logging.pattern.dateformat:${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}"));
//        (new DefaultLogbackConfiguration(initializationContext, logFile)).apply((LogbackConfigurator)configurator);
        context.setPackagingDataEnabled(true);
    }

    protected void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
        super.loadConfiguration(initializationContext, location, logFile);
        LoggerContext loggerContext = this.getLoggerContext();
        this.stopAndReset(loggerContext);

        try {
            this.configureByResourceUrl(initializationContext, loggerContext, ResourceUtils.getURL(location));
        } catch (Exception var9) {
            throw new IllegalStateException("Could not initialize Logback logging from " + location, var9);
        }

        List<Status> statuses = loggerContext.getStatusManager().getCopyOfStatusList();
        StringBuilder errors = new StringBuilder();
        Iterator var7 = statuses.iterator();

        while(var7.hasNext()) {
            Status status = (Status)var7.next();
            if (status.getLevel() == 2) {
                errors.append(errors.length() > 0 ? String.format("%n") : "");
                errors.append(status.toString());
            }
        }

        if (errors.length() > 0) {
            throw new IllegalStateException(String.format("Logback configuration error detected: %n%s", errors));
        }
    }

    private void configureByResourceUrl(LoggingInitializationContext initializationContext, LoggerContext loggerContext, URL url) throws JoranException {
        if (url.toString().endsWith("xml")) {
            JoranConfigurator configurator = new SpringBootJoranConfigurator(initializationContext);
            configurator.setContext(loggerContext);
            configurator.doConfigure(url);
        } else {
            (new ContextInitializer(loggerContext)).configureByResource(url);
        }

    }

    private void stopAndReset(LoggerContext loggerContext) {
//        loggerContext.stop();
//        loggerContext.reset();
        if (this.isBridgeHandlerInstalled()) {
            this.addLevelChangePropagator(loggerContext);
        }

    }

    private boolean isBridgeHandlerInstalled() {
        if (!this.isBridgeHandlerAvailable()) {
            return false;
        } else {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            return handlers.length == 1 && handlers[0] instanceof SLF4JBridgeHandler;
        }
    }

    private void addLevelChangePropagator(LoggerContext loggerContext) {
        LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
        levelChangePropagator.setResetJUL(true);
        levelChangePropagator.setContext(loggerContext);
        loggerContext.addListener(levelChangePropagator);
    }

    public void cleanUp() {
        LoggerContext context = this.getLoggerContext();
        this.markAsUninitialized(context);
        super.cleanUp();
        context.getStatusManager().clear();
        context.getTurboFilterList().remove(FILTER);
    }

    protected void reinitialize(LoggingInitializationContext initializationContext) {
        this.getLoggerContext().reset();
        this.getLoggerContext().getStatusManager().clear();
        this.loadConfiguration(initializationContext, this.getSelfInitializationConfig(), null);
    }

    public List<LoggerConfiguration> getLoggerConfigurations() {
        List<LoggerConfiguration> result = new ArrayList();
        Iterator var2 = this.getLoggerContext().getLoggerList().iterator();

        while(var2.hasNext()) {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)var2.next();
            result.add(this.getLoggerConfiguration(logger));
        }

        result.sort(CONFIGURATION_COMPARATOR);
        return result;
    }

    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        return this.getLoggerConfiguration(this.getLogger(loggerName));
    }

    private LoggerConfiguration getLoggerConfiguration(ch.qos.logback.classic.Logger logger) {
        if (logger == null) {
            return null;
        } else {
            LogLevel level = LEVELS.convertNativeToSystem(logger.getLevel());
            LogLevel effectiveLevel = LEVELS.convertNativeToSystem(logger.getEffectiveLevel());
            String name = logger.getName();
            if (!StringUtils.hasLength(name) || "ROOT".equals(name)) {
                name = "ROOT";
            }

            return new LoggerConfiguration(name, level, effectiveLevel);
        }
    }

    public Set<LogLevel> getSupportedLogLevels() {
        return LEVELS.getSupported();
    }

    public void setLogLevel(String loggerName, LogLevel level) {
        ch.qos.logback.classic.Logger logger = this.getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(LEVELS.convertSystemToNative(level));
        }

    }

    public Runnable getShutdownHandler() {
        return new ShutdownHandler();
    }

    private ch.qos.logback.classic.Logger getLogger(String name) {
        LoggerContext factory = this.getLoggerContext();
        if (StringUtils.isEmpty(name) || "ROOT".equals(name)) {
            name = "ROOT";
        }

        return factory.getLogger(name);
    }

    private LoggerContext getLoggerContext() {
        ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        Assert.isInstanceOf(LoggerContext.class, factory, String.format("LoggerFactory is not a Logback LoggerContext but Logback is on the classpath. Either remove Logback or the competing implementation (%s loaded from %s). If you are using WebLogic you will need to add 'org.slf4j' to prefer-application-packages in WEB-INF/weblogic.xml", factory.getClass(), this.getLocation(factory)));
        return (LoggerContext)factory;
    }

    private Object getLocation(ILoggerFactory factory) {
        try {
            ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                return codeSource.getLocation();
            }
        } catch (SecurityException var4) {
        }

        return "unknown location";
    }

    private boolean isAlreadyInitialized(LoggerContext loggerContext) {
        return loggerContext.getObject(LoggingSystem.class.getName()) != null;
    }

    private void markAsInitialized(LoggerContext loggerContext) {
        loggerContext.putObject(LoggingSystem.class.getName(), new Object());
    }

    private void markAsUninitialized(LoggerContext loggerContext) {
        loggerContext.removeObject(LoggingSystem.class.getName());
    }

    static {
        LEVELS.map(LogLevel.TRACE, Level.TRACE);
        LEVELS.map(LogLevel.TRACE, Level.ALL);
        LEVELS.map(LogLevel.DEBUG, Level.DEBUG);
        LEVELS.map(LogLevel.INFO, Level.INFO);
        LEVELS.map(LogLevel.WARN, Level.WARN);
        LEVELS.map(LogLevel.ERROR, Level.ERROR);
        LEVELS.map(LogLevel.FATAL, Level.ERROR);
        LEVELS.map(LogLevel.OFF, Level.OFF);
        FILTER = new TurboFilter() {
            public FilterReply decide(Marker marker, ch.qos.logback.classic.Logger logger, Level level, String format, Object[] params, Throwable t) {
                return FilterReply.DENY;
            }
        };
    }

    private final class ShutdownHandler implements Runnable {
        private ShutdownHandler() {
        }

        public void run() {
            LogbackLoggingSystem.this.getLoggerContext().stop();
        }
    }

    protected static class LogLevels<T> {
        private final Map<LogLevel, T> systemToNative = new EnumMap(LogLevel.class);
        private final Map<T, LogLevel> nativeToSystem = new HashMap();

        public LogLevels() {
        }

        public void map(LogLevel system, T nativeLevel) {
            if (!this.systemToNative.containsKey(system)) {
                this.systemToNative.put(system, nativeLevel);
            }

            if (!this.nativeToSystem.containsKey(nativeLevel)) {
                this.nativeToSystem.put(nativeLevel, system);
            }

        }

        public LogLevel convertNativeToSystem(T level) {
            return this.nativeToSystem.get(level);
        }

        public T convertSystemToNative(LogLevel level) {
            return this.systemToNative.get(level);
        }

        public Set<LogLevel> getSupported() {
            return new LinkedHashSet(this.nativeToSystem.values());
        }
    }
}

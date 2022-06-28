/**
 * IdeaProject @ org.reed.log/SpringPropertyAction.java
 */
package org.reed.log;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import org.springframework.core.env.Environment;
import org.xml.sax.Attributes;

/**
 * @author chenxiwen
 * @createTime 2019年07月29日 下午4:49
 * @description
 */
public final class SpringPropertyAction extends Action {
    private static final String SOURCE_ATTRIBUTE = "source";
    private static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";
    private final Environment environment;

    SpringPropertyAction(Environment environment) {
        this.environment = environment;
    }

    public void begin(InterpretationContext context, String elementName, Attributes attributes) throws ActionException {
        String name = attributes.getValue("name");
        String source = attributes.getValue("source");
        ActionUtil.Scope scope = ActionUtil.stringToScope(attributes.getValue("scope"));
        String defaultValue = attributes.getValue("defaultValue");
        if (OptionHelper.isEmpty(name) || OptionHelper.isEmpty(source)) {
            this.addError("The \"name\" and \"source\" attributes of <springProperty> must be set");
        }

        ActionUtil.setProperty(context, name, this.getValue(source, defaultValue), scope);
    }

    private String getValue(String source, String defaultValue) {
        if (this.environment == null) {
            this.addWarn("No Spring Environment available to resolve " + source);
            return defaultValue;
        } else {
            String value = this.environment.getProperty(source);
            if (value != null) {
                return value;
            } else {
                int lastDot = source.lastIndexOf(46);
                if (lastDot > 0) {
                    String prefix = source.substring(0, lastDot + 1);
                    return this.environment.getProperty(prefix + source.substring(lastDot + 1), defaultValue);
                } else {
                    return defaultValue;
                }
            }
        }
    }

    public void end(InterpretationContext context, String name) throws ActionException {
    }
}

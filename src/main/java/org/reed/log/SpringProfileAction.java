/**
 * IdeaProject @ org.reed.log/SpringProfileAction.java
 */
package org.reed.log;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.util.OptionHelper;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenxiwen
 * @createTime 2019年07月29日 下午4:50
 * @description
 */
final class SpringProfileAction extends Action implements InPlayListener {
    private final Environment environment;
    private int depth = 0;
    private boolean acceptsProfile;
    private List<SaxEvent> events;

    SpringProfileAction(Environment environment) {
        this.environment = environment;
    }

    public void begin(InterpretationContext ic, String name, Attributes attributes) throws ActionException {
        ++this.depth;
        if (this.depth == 1) {
            ic.pushObject(this);
            this.acceptsProfile = this.acceptsProfiles(ic, attributes);
            this.events = new ArrayList();
            ic.addInPlayListener(this);
        }
    }

    private boolean acceptsProfiles(InterpretationContext ic, Attributes attributes) {
        if (this.environment == null) {
            return false;
        } else {
            String[] profileNames = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(attributes.getValue("name")));
            if (profileNames.length == 0) {
                return false;
            } else {
                for(int i = 0; i < profileNames.length; ++i) {
                    profileNames[i] = OptionHelper.substVars(profileNames[i], ic, this.context);
                }

                return this.environment.acceptsProfiles(Profiles.of(profileNames));
            }
        }
    }

    public void end(InterpretationContext ic, String name) throws ActionException {
        --this.depth;
        if (this.depth == 0) {
            ic.removeInPlayListener(this);
            this.verifyAndPop(ic);
            if (this.acceptsProfile) {
                this.addEventsToPlayer(ic);
            }

        }
    }

    private void verifyAndPop(InterpretationContext ic) {
        Object o = ic.peekObject();
        Assert.state(o != null, "Unexpected null object on stack");
        Assert.isInstanceOf(SpringProfileAction.class, o, "logback stack error");
        Assert.state(o == this, "ProfileAction different than current one on stack");
        ic.popObject();
    }

    private void addEventsToPlayer(InterpretationContext ic) {
        Interpreter interpreter = ic.getJoranInterpreter();
        this.events.remove(0);
        this.events.remove(this.events.size() - 1);
        interpreter.getEventPlayer().addEventsDynamically(this.events, 1);
    }

    public void inPlay(SaxEvent event) {
        this.events.add(event);
    }
}
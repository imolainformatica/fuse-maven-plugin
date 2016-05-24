package it.imolinfo.maven.plugins.jboss.fuse.options;

import java.io.File;
import java.util.Properties;

/**
 *
 * @author giacomo
 */
public class Cfg {
    public enum Option {
        APPEND,
        REPLACE,
        COPY
    }
    
    private Option option; 
    private File source;
    private String destination;
    private String target;
    private String replacement;
    private Properties properties;

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public Option getOption() {
        return option;
    }

    public void setOption(Option option) {
        this.option = option;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
    
}

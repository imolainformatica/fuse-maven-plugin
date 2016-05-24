package it.imolinfo.maven.plugins.jboss.fuse.utils;

import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 * @author giacomo
 */
public class ExceptionManager {
    
    private ExceptionManager() {
        
    }
    
    public static void throwMojoExecutionExceptionIfNull(Object object, String message) throws MojoExecutionException{
        throwMojoExecutionException(object == null, message);
    }
    
    public static void throwMojoExecutionException(Boolean condition, String message) throws MojoExecutionException {
        if (condition) {
            throw new MojoExecutionException(message);
        }
    }
}

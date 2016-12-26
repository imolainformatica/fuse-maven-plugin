/*
 * Copyright 2016 Imola Informatica S.P.A..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

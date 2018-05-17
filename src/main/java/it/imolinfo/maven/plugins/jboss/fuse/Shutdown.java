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
package it.imolinfo.maven.plugins.jboss.fuse;



import it.imolinfo.maven.plugins.jboss.fuse.utils.KarafJMXConnector;
import java.io.IOException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mojo(name = "shutdown", requiresProject = false, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class Shutdown extends AbstractGoal  {
    private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);
    
    @Parameter
    private String skip;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip != null && "true".equalsIgnoreCase(skip)) {
        	LOG.info("Shutdown jboss-fuse goal skipped from configuration");
        } else {
	        LOG.info("Shutdown jboss-fuse");
	        try {
	            KarafJMXConnector jMXConnector = KarafJMXConnector.getInstance(DEFAULT_STOP_TIMEOUT);
	            jMXConnector.shutdown();
	        } catch (IOException | ReflectionException | MBeanException | InstanceNotFoundException | MalformedObjectNameException ex) {
	            LOG.error(ex.getMessage(), ex);
	            throw new MojoExecutionException(ex.getMessage(), ex);
	        }
        }
    }
}

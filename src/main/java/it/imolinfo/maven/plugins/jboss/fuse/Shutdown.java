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



import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mojo(name = "shutdown", requiresProject = false, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class Shutdown extends AbstractGoal  {
    private static final Logger LOG = LoggerFactory.getLogger(Shutdown.class);
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Stop jboss-fuse");
        try {
            Runtime runtime = Runtime.getRuntime();
            Process stop = runtime.exec(STOP_CMD);
            stop.waitFor(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}

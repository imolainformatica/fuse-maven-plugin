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

import com.jcraft.jsch.JSchException;
import static it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal.JBOSS_FUSE_DEPLOY_DIRECTORY;
import static it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal.LIST_CMD;
import it.imolinfo.maven.plugins.jboss.fuse.options.Deployment;
import it.imolinfo.maven.plugins.jboss.fuse.utils.ExceptionManager;
import it.imolinfo.maven.plugins.jboss.fuse.utils.SSHUtility;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.awaitility.Awaitility.await;

@Mojo(name = "deploy", requiresProject = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class Deploy extends AbstractGoal {

    private static final Logger LOG = LoggerFactory.getLogger(Deploy.class);
    @Parameter
    private List<Deployment> deployments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (deployments != null) {
            LOG.info("Start deploy {} items", deployments.size());
            for (Deployment deployment : deployments) {
                ExceptionManager.throwMojoExecutionExceptionIfNull(deployment.getSource(), "Null source");
                ExceptionManager.throwMojoExecutionException(!deployment.getSource().exists(), String.format("%s not exists", deployment.getSource().getAbsolutePath()));
                ExceptionManager.throwMojoExecutionException(deployment.getSource().isDirectory(), String.format("%s is directory", deployment.getSource().getAbsolutePath()));
                deploy(deployment);
            }
        }
    }

    private void deploy(Deployment deployment) throws MojoExecutionException, MojoFailureException {
        try {
            FileUtils.copyFileToDirectory(deployment.getSource(), JBOSS_FUSE_DEPLOY_DIRECTORY);
            if (deployment.getWaitTime() != null) {
                Thread.sleep(deployment.getWaitTime());
            }
            checkDeployStatus(deployment);
        } catch (IOException | InterruptedException | MojoExecutionException | JSchException ex) {
            new Shutdown().execute();
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void checkDeployStatus(Deployment deployment) throws IOException, InterruptedException, MojoExecutionException, JSchException {
        String deploymentName = deployment.getDeploymentName() != null ? deployment.getDeploymentName() : deployment.getSource().getName();
        final Long timeout = deployment.getTimeout() != null ? deployment.getTimeout() : DEFAULT_DEPLOY_TIMEOUT;
        String regex = deployment.getExpectedStatus() == null ? Deployment.DEFAULT_EXPECTED_STATUS : deployment.getExpectedStatus();
        if (deployment.getExpectedContextStatus() != null) {
            regex = String.format("%s.*%s", regex, deployment.getExpectedContextStatus());
        }
        regex = String.format("%s.*%s", regex, deploymentName);

        final Pattern pattern = Pattern.compile(regex);
        try {
            await().atMost(timeout, TimeUnit.MILLISECONDS).until((Callable<Boolean>) () -> {
                String deploymentStatusStr = SSHUtility.executeCmd(LIST_CMD);
                System.out.write(String.format("\r %s", deploymentStatusStr).getBytes());

                Matcher matcher = pattern.matcher(deploymentStatusStr);
                return !(matcher.find() || matcher.matches());
            });
        } catch (Exception ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            throw new MojoExecutionException("Deploy timeout");
        }
    }
}

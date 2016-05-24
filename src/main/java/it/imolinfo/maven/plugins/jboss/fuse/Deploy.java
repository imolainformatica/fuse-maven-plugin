package it.imolinfo.maven.plugins.jboss.fuse;

import com.jcraft.jsch.JSchException;
import static it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal.JBOSS_FUSE_DEPLOY_DIRECTORY;
import it.imolinfo.maven.plugins.jboss.fuse.options.Deployment;
import it.imolinfo.maven.plugins.jboss.fuse.utils.ExceptionManager;
import it.imolinfo.maven.plugins.jboss.fuse.utils.SSHUtility;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deploy", requiresProject = false, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class Deploy extends AbstractGoal {

    @Parameter
    private List<Deployment> deployments;

    public Deploy() {
        super();
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Start deploy");
        if (deployments != null) {
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
            LOG.info(String.format("Deploy %s in %s", deployment.getSource().getAbsolutePath(), JBOSS_FUSE_DEPLOY_DIRECTORY.getAbsolutePath()));
            FileUtils.copyFileToDirectory(deployment.getSource(), JBOSS_FUSE_DEPLOY_DIRECTORY);
            if (deployment.getWaitTime() != null) {
                Thread.sleep(deployment.getWaitTime());
            }
            checkDeployStatus(deployment);
        } catch (Exception ex) {
            new Shutdown().execute();
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
    
    private void checkDeployStatus(Deployment deployment) throws IOException, InterruptedException, MojoExecutionException, JSchException {
        String deploymentName = deployment.getDeploymentName() != null ? deployment.getDeploymentName() : deployment.getSource().getName();
        Long timeout = deployment.getTimeout() != null ? deployment.getTimeout() : DEFAULT_DEPLOY_TIMEOUT;
        String regex = deployment.getExpectedStatus() == null ? Deployment.DEFAULT_EXPECTED_STATUS : deployment.getExpectedStatus();
        if (deployment.getExpectedContextStatus() != null) {
            regex = String.format("%s.*%s", regex, deployment.getExpectedContextStatus());
        }
        regex = String.format("%s.*%s", regex, deploymentName);
        
        Pattern pattern = Pattern.compile(regex);
        Runtime runtime = Runtime.getRuntime();
        String deploymentStatusStr;
        Long startTime = System.currentTimeMillis();
        Boolean started;
        do {
            deploymentStatusStr = SSHUtility.executeCmd(LIST_CMD);
            Matcher matcher = pattern.matcher(deploymentStatusStr);
            started = matcher.find() || matcher.matches();
            LOG.info(String.format("Wait for bundle %s. Status: %s", deploymentName, deploymentStatusStr));
            Thread.sleep(SLEEP);
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new MojoExecutionException("Timeout durante l'avvio...");
            }
        } while (!started);
        LOG.info(String.format("%s started", deploymentName));
    }
}

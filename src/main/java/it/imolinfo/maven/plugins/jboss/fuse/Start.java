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

import static it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal.LOCALHOST;
import static it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal.SSH_TIMEOUT;
import it.imolinfo.maven.plugins.jboss.fuse.options.Cfg;
import it.imolinfo.maven.plugins.jboss.fuse.utils.ExceptionManager;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.awaitility.Awaitility.await;

@Mojo(name = "start", requiresProject = false, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class Start extends AbstractGoal {

    private static final Logger LOG = LoggerFactory.getLogger(Start.class);

    private static final String USER_PROPERTIES_FILE_NAME = "users.properties";
    private static final String DEFAULT_ADMIN_CONFIG = "#admin=admin,admin,manager,viewer,Monitor, Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser";
    private static final String ADMIN_CONFIG = "admin=admin,admin,manager,viewer,Monitor, Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser";

    @Parameter
    private Long timeout;

    @Parameter
    private List<Cfg> cfg;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Start jboss-fuse");
        timeout = timeout == null ? SSH_TIMEOUT : timeout;
        download();
        initBinDirectory();
        disableAdminPassword();
        configure();
        startJbosFuse();
    }

    private void startJbosFuse() throws MojoExecutionException, MojoFailureException {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(START_CMD).waitFor();
            LOG.info("Trying connect to ssh:{}@{}:{} ...", SSH_USER, LOCALHOST, SSH_PORT);

            checkSSHPort(timeout.intValue());

            LOG.info("Connected to ssh:{}@{}:{}...", SSH_USER, LOCALHOST, SSH_PORT);
            deployDependencies();
        } catch (Exception ex) {
            new Shutdown().execute();
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private void configure() throws MojoExecutionException, MojoFailureException {
        if (cfg != null) {
            for (Cfg configuration : cfg) {
                try {
                    configure(configuration);
                } catch (IOException ex) {
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        }
    }

    private void deployDependencies() throws IOException {
        LOG.info("Deploy plugin dependencies");
        PluginDescriptor pluginDescriptor = (PluginDescriptor) super.getPluginContext().get("pluginDescriptor");
        String pluginGroupId = pluginDescriptor.getGroupId();
        String pluginArtifactiId = pluginDescriptor.getArtifactId();
        for (Plugin plugin : project.getBuild().getPlugins()) {
            if (plugin.getGroupId().equals(pluginGroupId) && plugin.getArtifactId().equals(pluginArtifactiId)) {
                for (Dependency dependency : plugin.getDependencies()) {
                    LOG.info("Deploy {}:{}:{}", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                    String path = String.format("%s/%s/%s/%s", super.settings.getLocalRepository(), dependency.getGroupId().replaceAll("\\.", "/"),
                            dependency.getArtifactId(), dependency.getVersion());
                    File dependencyDirectory = new File(path);
                    for (File dependencyFile : dependencyDirectory.listFiles()) {
                        if (FilenameUtils.getExtension(dependencyFile.getAbsolutePath()).toLowerCase().equals(JAR)) {
                            FileUtils.copyFileToDirectory(dependencyFile, JBOSS_FUSE_DEPLOY_DIRECTORY);
                        }
                    }
                }
            }
        }
    }

    private static void configure(Cfg configuration) throws IOException, MojoExecutionException {
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getOption(), "Null option");
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getDestination(), "Null destination");
        File destination = new File(String.format("%s/%s", JBOSS_FUSE_DIRECTORY, configuration.getDestination()));
        ExceptionManager.throwMojoExecutionException(!destination.exists(), String.format("%s not exists", destination.getAbsolutePath()));
        switch (configuration.getOption()) {
            case COPY:
                copy(configuration, destination);
                break;
            case APPEND:
                append(configuration, destination);
                break;
            case REPLACE:
                replace(configuration, destination);
                break;
            default:
                throw new MojoExecutionException("Invalid option");
        }
    }

    private static void disableAdminPassword() throws MojoExecutionException {
        LOG.info("Disable admin password");
        File usersFile = new File(String.format("%s/%s", JBOSS_FUSE_ETC_DIRECTORY.getAbsolutePath(), USER_PROPERTIES_FILE_NAME));
        replace(usersFile, DEFAULT_ADMIN_CONFIG, ADMIN_CONFIG);
    }

    private static void copy(Cfg configuration, File destination) throws MojoExecutionException {
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getSource(), "Null source File");
        ExceptionManager.throwMojoExecutionException(!configuration.getSource().exists(), "Source file not exists");
        ExceptionManager.throwMojoExecutionException(!destination.isDirectory(), String.format("%s is file", destination.getAbsolutePath()));
        LOG.info("Add {} in {}", configuration.getSource().getAbsolutePath(), configuration.getDestination());
        try {
            FileUtils.copyFileToDirectory(configuration.getSource(), destination);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static void append(Cfg configuration, File destination) throws MojoExecutionException {
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getProperties(), "Null properties");
        ExceptionManager.throwMojoExecutionException(destination.isDirectory(), String.format("%s is directory", destination.getAbsolutePath()));
        LOG.info("Append properties in {}", destination.getAbsolutePath());
        try {
            StringBuilder sb = new StringBuilder(FileUtils.readFileToString(destination, "UTF-8"));
            configuration.getProperties().keySet().stream().forEach((key) -> {
                String propertyName = String.valueOf(key);
                sb.append(String.format("%s = %s\n", key, configuration.getProperties().getProperty(propertyName)));
            });
            FileUtils.write(destination, sb.toString(), "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static void replace(Cfg configuration, File destination) throws MojoExecutionException {
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getTarget(), "Null target");
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getReplacement(), "Null replacement");
        ExceptionManager.throwMojoExecutionException(destination.isDirectory(), String.format("%s is directory", destination.getAbsolutePath()));
        LOG.info("Replace {} with {} in {}", configuration.getTarget(), configuration.getReplacement(), destination.getAbsolutePath());
        replace(destination, configuration.getTarget(), configuration.getReplacement());
    }

    private static void replace(File destination, String target, String replacement) throws MojoExecutionException {
        try {
            String text = FileUtils.readFileToString(destination, "UTF-8").replace(target, replacement);
            FileUtils.write(destination, text, "UTF-8");
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static void checkSSHPort(final Integer connectTimeout) throws MojoExecutionException {
        try {
            await().atMost(connectTimeout, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS).until((Callable<Boolean>) () -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(LOCALHOST, SSH_PORT), connectTimeout);
                    return Boolean.TRUE;
                } catch (IOException ex) {
                    LOG.trace(ex.getMessage(), ex);
                    return Boolean.FALSE;
                }
            });
        } catch (Exception ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
            throw new MojoExecutionException(String.format("SSH port %d unavailable", SSH_PORT));

        }
    }
}

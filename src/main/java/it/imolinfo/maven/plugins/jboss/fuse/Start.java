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

import it.imolinfo.maven.plugins.jboss.fuse.model.Bundle;
import it.imolinfo.maven.plugins.jboss.fuse.options.Cfg;
import it.imolinfo.maven.plugins.jboss.fuse.utils.ExceptionManager;
import it.imolinfo.maven.plugins.jboss.fuse.utils.KarafJMXConnector;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DebugResolutionListener;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Parameter
    private String etc;

    @Parameter
    private String features;

    @Parameter
    private String bundles;

    @Component
    private RepositorySystem repository;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Start jboss-fuse");
        timeout = timeout == null ? TIMEOUT : timeout;
        download();
        initBinDirectory();
        disableAdminPassword();
        configure();
        etc();
        startJbosFuse();
        features();
        deployDependencies();
        if (project.getArtifact().getFile() != null) {
            deploy(project.getArtifact().getFile(), timeout);
        }
        list(timeout);
    }

    private void startJbosFuse() throws MojoExecutionException, MojoFailureException {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(START_CMD).waitFor();
        } catch (IOException | InterruptedException ex) {
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

    private void etc() throws MojoExecutionException, MojoFailureException {
        if (etc != null) {
            for (String cfgFile : etc.split(",")) {
                try {
                    cfgFile = cfgFile.trim();
                    LOG.info("Copy {} in {}", cfgFile, JBOSS_FUSE_ETC_DIRECTORY.getAbsolutePath());
                    FileUtils.copyFileToDirectory(new File(cfgFile), JBOSS_FUSE_ETC_DIRECTORY);
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                    new Shutdown().execute();
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        }
    }

    private void features() throws MojoExecutionException, MojoFailureException {
        if (features != null) {
            for (String feature : features.split(",")) {
                feature = feature.trim();
                LOG.info("Deploy feature {}", feature);
                try {
                    KarafJMXConnector jMXConnector = KarafJMXConnector.getInstance(timeout);
                    jMXConnector.featureInstall(feature);
                } catch (ReflectionException | MBeanException | InstanceNotFoundException | IOException | MalformedObjectNameException ex) {
                    LOG.error(ex.getMessage(), ex);
                    new Shutdown().execute();
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        }

    }

    private void deployDependencies() throws MojoExecutionException, MojoFailureException {
        if (features != null) {
            for (String bundle : bundles.split(",")) {
                bundle = bundle.trim();
                LOG.info("Deploy bundle {}", bundle);
                if (bundle.startsWith("mvn:")) {
                    installArtifact(bundle.replace("mvn:", ""));
                } else if (bundle.startsWith("file://")) {
                    deploy(new File(bundle.replace("file://", "")), timeout);
                } else {
                    throw new MojoExecutionException(String.format("Budnle syntax error: %s", bundle));
                }
            }
        }
    }

    private Long installArtifact(String bundle) throws MojoExecutionException, MojoFailureException {
        String[] bundleInfo = bundle.trim().split("/");
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        Artifact artifact = new DefaultArtifact(bundleInfo[0], bundleInfo[1], bundleInfo[2],
                null, "jar", "", artifactHandler);
        request.setArtifact(artifact);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        request.setManagedVersionMap(project.getManagedVersionMap());
        request.setForceUpdate(false);
        request.setResolveTransitively(Boolean.FALSE);
        List<ResolutionListener> resolutionListeners = new ArrayList<>();
        ResolutionListener resolutionListener = new DebugResolutionListener(new ConsoleLogger());
        resolutionListeners.add(resolutionListener);
        request.setListeners(resolutionListeners);
        ArtifactResolutionResult result = repository.resolve(request);
        File bundleFile = result.getArtifacts().iterator().next().getFile();
        return deploy(bundleFile, timeout);
    }

    private static void configure(Cfg configuration) throws IOException, MojoExecutionException {
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getOption(), "Null option");
        File destination = new File(String.format("%s/%s", JBOSS_FUSE_DIRECTORY, configuration.getDestination()));
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
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getDestination(), "Null destination");
        ExceptionManager.throwMojoExecutionException(!destination.exists(), String.format("%s not exists", destination.getAbsolutePath()));
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
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getDestination(), "Null destination");
        ExceptionManager.throwMojoExecutionException(!destination.exists(), String.format("%s not exists", destination.getAbsolutePath()));
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
        ExceptionManager.throwMojoExecutionExceptionIfNull(configuration.getDestination(), "Null destination");
        ExceptionManager.throwMojoExecutionException(!destination.exists(), String.format("%s not exists", destination.getAbsolutePath()));
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

    private static Long deploy(File deployment, Long timeout) throws MojoExecutionException, MojoFailureException {
        try {
            final KarafJMXConnector fuseJMXConnector = KarafJMXConnector.getInstance(timeout);
            final Long bundleId = fuseJMXConnector.install(deployment);
            fuseJMXConnector.start(bundleId);
            Bundle bundle = fuseJMXConnector.getBundle(bundleId);
            waitForBundleState(fuseJMXConnector, bundle);
            LOG.info("[ {} ] {}.{} {}, [ {} ] [ {} ]",
                    bundle.getId(),
                    bundle.getName(),
                    bundle.getVersion(),
                    bundle.getState(),
                    bundle.getBlueprintState() != null ? bundle.getBlueprintState() : "",
                    bundle.getSpringState() != null ? bundle.getSpringState() : "");
            return bundleId;
        } catch (IOException | MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException ex) {
            new Shutdown().execute();
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static void list(Long timeout) throws MojoExecutionException, MojoFailureException {
        try {
            KarafJMXConnector karafJMXConnector = KarafJMXConnector.getInstance(timeout);
            for (Bundle bundle : karafJMXConnector.list()) {
                LOG.info("[ {} ] {}.{} {}, [ {} ] [ {} ]",
                        bundle.getId(),
                        bundle.getName(),
                        bundle.getVersion(),
                        bundle.getState(),
                        bundle.getBlueprintState() != null ? bundle.getBlueprintState() : "",
                        bundle.getSpringState() != null ? bundle.getSpringState() : "");
            }
        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException | IOException ex) {
            new Shutdown().execute();
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static void waitForBundleState(final KarafJMXConnector fuseJMXConnector, final Bundle bundle) {
        try {
            if (!bundle.getState().equals(Bundle.State.ACTIVE)) {
                Awaitility.await().atMost(Duration.TEN_SECONDS).until((Callable<Boolean>) () -> {
                    LOG.debug("Wait for bundle {} state", bundle.getId());
                    return fuseJMXConnector.getBundle(bundle.getId()).getState().equals(Bundle.State.ACTIVE);
                });
            }
        } catch (ConditionTimeoutException ex) {
            LOG.debug(ex.getMessage());
        }
    }
}

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

import it.imolinfo.maven.plugins.jboss.fuse.utils.ArchiveManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.awaitility.Awaitility.await;

/**
 *
 * @author giacomo
 */
public abstract class AbstractGoal extends AbstractMojo {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGoal.class);

    public static final String LOCALHOST = "127.0.0.1";
    public static final Integer JMX_PORT = 1099;
    
    public static final String FILE_PREFIX;
    public static final String UNIX_FILE_PREFIX = "file://";
    public static final String WINDOWS_FILE_PREFIX = "file:///";
    static {
        FILE_PREFIX = System.getProperty("os.name").toLowerCase().contains("windows")
                ? WINDOWS_FILE_PREFIX : UNIX_FILE_PREFIX;

    }
    
    protected static final String JBOSS_FUSE_ZIP_FILE = "jboss-fuse-full-6.2.1.redhat-083.zip";
    protected static final String JBOSS_FUSE_DOWNLOAD_DIRECTORY = "it/imolinfo/maven/plugins/jboss-fuse-maven-plugin";
    protected static final String JBOSS_FUSE_DIRECTORY_NAME = "jboss-fuse-6.2.1.redhat-083";
    protected static final File TARGET_DIRECTORY = new File("target");
    protected static final File JBOSS_FUSE_DIRECTORY = new File(String.format("target/%s", JBOSS_FUSE_DIRECTORY_NAME));
    protected static final File JBOSS_FUSE_ETC_DIRECTORY = new File(String.format("%s/etc", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final File JBOSS_FUSE_LOG_DIRECTORY = new File(String.format("%s/data/log", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final File JBOSS_FUSE_BIN_DIRECTORY = new File(String.format("%s/bin", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final File JBOSS_FUSE_DEPLOY_DIRECTORY = new File(String.format("%s/deploy", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final Integer TIMEOUT = 60000;
    //TIMEOUT
    protected static final Long DEFAULT_STOP_TIMEOUT = 20000L;
    protected static final Long DOWNLOAD_SLEEP = 1000L;
    //CMD
    protected static final String START_CMD = SystemUtils.IS_OS_WINDOWS ? String.format("%s/start.bat", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath()) : String.format("%s/start", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath());
    protected static final String STOP_CMD = SystemUtils.IS_OS_WINDOWS ? String.format("%s/stop.bat", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath()) : String.format("%s/stop", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath());

    protected static final String JAR = "jar";
    private static final Long MB = 1024 * 1024L;

    protected static File JBOSS_FUSE_REPOSITORY_DIRECTORY;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;
    @Parameter(defaultValue = "https://repository.jboss.org/nexus/content/groups/ea/org/jboss/fuse/jboss-fuse-full/6.2.1.redhat-083/jboss-fuse-full-6.2.1.redhat-083.zip", required = true, readonly = true)
    protected String jbossFuseDownloadUrl;

    private Boolean downloadCompleted = Boolean.FALSE;

    protected void initBinDirectory() {
        for (File binFile : JBOSS_FUSE_BIN_DIRECTORY.listFiles()) {
            binFile.setExecutable(Boolean.TRUE);
        }
    }

    protected void download() throws MojoExecutionException {
        String localRepository = settings.getLocalRepository();
        String fuseDownloadDirectoryPath = String.format("%s/%s", localRepository, JBOSS_FUSE_DOWNLOAD_DIRECTORY);
        File fuseZipFile = new File(String.format("%s/%s", fuseDownloadDirectoryPath, JBOSS_FUSE_ZIP_FILE));
        if (!fuseZipFile.exists()) {
            try {
                download(fuseZipFile);
            } catch (IOException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            }
        }

        extractArchive(fuseZipFile);
    }

    private void download(File fuseZipFile) throws IOException {
        File tmpFile = File.createTempFile("fuse", ".temp");
        LOG.info("Download {} in {}...", jbossFuseDownloadUrl, tmpFile.getAbsolutePath());
        URL url = new URL(jbossFuseDownloadUrl);
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream(); FileOutputStream downloadOutputStream = new FileOutputStream(tmpFile)) {
            Long contentLength = connection.getContentLengthLong();
            new Thread(new DownloadProgress(tmpFile, contentLength)).start();
            IOUtils.copyLarge(inputStream, downloadOutputStream);
            downloadOutputStream.flush();
        } finally {
            downloadCompleted = Boolean.TRUE;
        }
        FileUtils.moveFile(tmpFile, fuseZipFile);
        LOG.info("Download completed");
    }

    private static void extractArchive(File zipFile) throws MojoExecutionException {
        try {
            ArchiveManager.extract(zipFile.getAbsolutePath(), TARGET_DIRECTORY.getAbsolutePath());
        } catch (MojoExecutionException ex) {
            LOG.error(ex.getMessage(), ex);
            zipFile.delete();
            throw ex;
        }
    }

    class DownloadProgress implements Runnable {

        private final File downloadFile;
        private final Long contentLength;

        public DownloadProgress(File downloadFile, Long contentLength) {
            this.downloadFile = downloadFile;
            this.contentLength = contentLength;
        }

        @Override
        public void run() {

            await().forever().pollDelay(DOWNLOAD_SLEEP, TimeUnit.MILLISECONDS).until((Callable<Boolean>) () -> {
                Long perc = (100 * downloadFile.length()) / contentLength;
                System.out.write(String.format("\r  %d%% %d/%dMB", perc, downloadFile.length() / (MB), contentLength / (MB)).getBytes());
                return downloadCompleted;
            });
        }

    }

}

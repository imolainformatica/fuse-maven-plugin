package it.imolinfo.maven.plugins.jboss.fuse;

import it.imolinfo.maven.plugins.jboss.fuse.utils.UnzipUtility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomo
 */
public abstract class AbstractGoal extends AbstractMojo {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGoal.class);
    
    public static final String LOCALHOST = "127.0.0.1";
    public static final Integer SSH_PORT = 8101;
    public static final String SSH_USER = "admin";
    public static final String SSH_PASSWORD = "admin";
    
    protected static final String JBOSS_FUSE_DOWNLOAD_URL = "https://repository.jboss.org/nexus/content/groups/ea/org/jboss/fuse/jboss-fuse-full/6.2.1.redhat-083/jboss-fuse-full-6.2.1.redhat-083.zip";
    protected static final String JBOSS_FUSE_ZIP_FILE = "jboss-fuse-full-6.2.1.redhat-083.zip";
    protected static final String JBOSS_FUSE_DOWNLOAD_DIRECTORY = "it/imolinfo/maven/plugins/jboss-fuse-maven-plugin";
    protected static final String JBOSS_FUSE_DIRECTORY_NAME = "jboss-fuse-6.2.1.redhat-083";
    protected static final File TARGET_DIRECTORY = new File("target");
    protected static final File JBOSS_FUSE_DIRECTORY = new File(String.format("target/%s", JBOSS_FUSE_DIRECTORY_NAME));
    protected static final File JBOSS_FUSE_ETC_DIRECTORY = new File(String.format("%s/etc", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final File JBOSS_FUSE_BIN_DIRECTORY = new File(String.format("%s/bin", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final File JBOSS_FUSE_DEPLOY_DIRECTORY = new File(String.format("%s/deploy", JBOSS_FUSE_DIRECTORY.getAbsolutePath()));
    protected static final Integer SSH_TIMEOUT = 60000;
    //TIMEOUT
    protected static final Long DEFAULT_START_TIMEOUT = 20000L;
    protected static final Long DEFAULT_STATUS_TIMEOUT = 10000L;
    protected static final Long DEFAULT_CLIENT_TIMEOUT = 10000L;
    protected static final Long DEFAULT_STOP_TIMEOUT = 20000L;
    protected static final Long DEFAULT_DEPLOY_TIMEOUT = 60000L;
    protected static final Long SLEEP = 1000L;
    protected static final Long DOWNLOAD_SLEEP = 1000L;
    //CMD
    protected static final String START_CMD =  SystemUtils.IS_OS_WINDOWS ? String.format("%s/start.bat", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath()) : String.format("%s/start", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath());
    protected static final String STOP_CMD = SystemUtils.IS_OS_WINDOWS ? String.format("%s/stop.bat", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath()) : String.format("%s/stop", JBOSS_FUSE_BIN_DIRECTORY.getAbsolutePath());
    protected static final String LIST_CMD = "list";
    //STATUS
    protected static final String RUNNING = "Running ...";

    private static final Long MB = 1024 * 1024L;

    protected static File JBOSS_FUSE_REPOSITORY_DIRECTORY;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;
    @Parameter
    protected String downloadUrl;

    private Boolean downloadCompleted = Boolean.FALSE;


    protected void initBinDirectory() {
        for (File binFile : JBOSS_FUSE_BIN_DIRECTORY.listFiles()) {
            binFile.setExecutable(Boolean.TRUE);
        }
    }

    protected void download() throws MojoExecutionException {
        downloadUrl = System.getProperty("downloadUrl") != null ? System.getProperty("downloadUrl") : downloadUrl == null ? JBOSS_FUSE_DOWNLOAD_URL : downloadUrl;
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

        unzip(fuseZipFile);
    }

    private void download(File fuseZipFile) throws IOException {
        LOG.info("Download {} in {}...", downloadUrl, fuseZipFile.getAbsolutePath());
        URL url = new URL(downloadUrl);
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream()) {
            Long contentLength = connection.getContentLengthLong();
            try (FileOutputStream downloadOutputStream = new FileOutputStream(fuseZipFile)) {
                new Thread(new DownloadProgress(fuseZipFile, contentLength)).start();
                IOUtils.copyLarge(inputStream, downloadOutputStream);
                downloadOutputStream.flush();
            } finally {
                downloadCompleted = Boolean.TRUE;
            }
        }
        LOG.info("Download completed");
    }

    private static void unzip(File zipFile) throws MojoExecutionException {
        try {
            UnzipUtility.unzip(zipFile.getAbsolutePath(), TARGET_DIRECTORY.getAbsolutePath());
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
            while (!downloadCompleted) {
                try {
                    Long perc = (100 * downloadFile.length()) / contentLength;
                    System.out.write(String.format("\r  %d%% %d/%dMB", perc, downloadFile.length() / (1024 * 1024), contentLength / (1024 * 1024)).getBytes());
                } catch (IOException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
                try {
                    Thread.sleep(DOWNLOAD_SLEEP);
                } catch (InterruptedException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            }
        }

    }

}

package it.imolinfo.maven.plugins.jboss.fuse;



import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;


@Mojo(name = "shutdown", requiresProject = false, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class Shutdown extends AbstractGoal  {
    
    public Shutdown() {
        super();
    }
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LOG.info("Stop jboss-fuse");
        try {
            Runtime runtime = Runtime.getRuntime();
            Process stop = runtime.exec(STOP_CMD);
            stop.waitFor(DEFAULT_STOP_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

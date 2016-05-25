package it.imolinfo.maven.plugins.jboss.fuse.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author giacomo
 */
public class SSHUtility {


    private SSHUtility() {

    }

    public static String executeCmd(String command) throws JSchException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(AbstractGoal.SSH_USER, AbstractGoal.LOCALHOST, AbstractGoal.SSH_PORT);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(AbstractGoal.SSH_PASSWORD);
        session.connect();
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.connect();
        String output = IOUtils.toString(in);
        channelExec.disconnect();
        session.disconnect();
        return output;
    }

}

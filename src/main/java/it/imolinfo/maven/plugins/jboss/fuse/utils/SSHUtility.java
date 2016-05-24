package it.imolinfo.maven.plugins.jboss.fuse.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author giacomo
 */
public class SSHUtility {

    private static final String SSH_HOST = "127.0.0.1";
    private static final Integer SSH_PORT = 8101;
    private static final String SSH_USER = "admin";
    private static final String SSH_PASSWORD = "admin";

    private SSHUtility() {

    }

    public static String executeCmd(String command) throws JSchException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(SSH_PASSWORD);
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

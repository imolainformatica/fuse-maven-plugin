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
        String output = IOUtils.toString(in, "UTF-8");
        channelExec.disconnect();
        session.disconnect();
        return output;
    }

}

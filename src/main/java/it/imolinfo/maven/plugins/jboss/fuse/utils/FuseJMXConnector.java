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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import static org.awaitility.Awaitility.await;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomo
 */
public class FuseJMXConnector {

    private static final Logger LOG = LoggerFactory.getLogger(FuseJMXConnector.class);
    private static FuseJMXConnector instance;

    public static FuseJMXConnector getInstance(Long timeout) throws IOException, MalformedURLException, MalformedObjectNameException {
        return instance = (instance != null ? instance : new FuseJMXConnector(timeout));
    }
    
    private Long timeout;
    private MBeanServerConnection connection;
    private ObjectName karafBundles;
    private ObjectName karafFeatures;

    private FuseJMXConnector(Long timeout) throws IOException, MalformedURLException, MalformedObjectNameException {
        this.timeout = timeout;
        this.init();
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    public Long install(File bundleFile) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Long) connection.invoke(karafBundles, "install",
                new Object[]{String.format("file://%s", bundleFile.getAbsolutePath())},
                new String[]{"java.lang.String"});
    }

    public Object start(Long bundleId) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Long) connection.invoke(karafBundles, "start",
                new Object[]{String.valueOf(bundleId)},
                new String[]{"java.lang.String"});
    }

    public TabularDataSupport list() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (TabularDataSupport) connection.invoke(karafBundles, "list", null, null);
    }

    public Object featureInstall(String feature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (TabularDataSupport) connection.invoke(karafFeatures, "installFeature",
                new Object[]{feature},
                new String[]{"java.lang.String"});
    }

    private void init() throws MalformedURLException {
        JMXServiceURL jmxUrl = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://localhost:1099/karaf-root");
        Map<String, ?> env = Collections.singletonMap(
                javax.management.remote.JMXConnector.CREDENTIALS,
                new String[]{"admin", "admin"});
        await().atMost(timeout, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS).until((Callable<Boolean>) () -> {
            try {
                if (connection == null) {
                    JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, env);
                    connection = jmxConnector.getMBeanServerConnection();
                }
                karafBundles = new ObjectName("org.apache.karaf:type=bundles,name=root");
                karafFeatures = new ObjectName("org.apache.karaf:type=features,name=root");
                connection.getAttribute(karafFeatures, "Features");
                return Boolean.TRUE;
            } catch (IOException | MalformedObjectNameException | InstanceNotFoundException ex) {
                LOG.trace(ex.getMessage(), ex);
                return Boolean.FALSE;
            }
        });
    }

}

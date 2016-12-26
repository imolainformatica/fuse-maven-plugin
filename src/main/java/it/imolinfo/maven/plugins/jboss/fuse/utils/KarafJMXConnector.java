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

import it.imolinfo.maven.plugins.jboss.fuse.model.Bundle;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.TabularDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.awaitility.Awaitility.await;

/**
 *
 * @author giacomo
 */
public class KarafJMXConnector {

    private static final Logger LOG = LoggerFactory.getLogger(KarafJMXConnector.class);
    private static KarafJMXConnector instance;

    public static KarafJMXConnector getInstance(Long timeout) throws IOException, MalformedURLException, MalformedObjectNameException {
        return instance = (instance != null ? instance : new KarafJMXConnector(timeout));
    }

    private final Long timeout;
    private MBeanServerConnection connection;
    private ObjectName osgiFramework;
    private ObjectName osgiBundleState;
    private ObjectName karafBundles;
    private ObjectName karafFeatures;
    private ObjectName karafSystem;

    private KarafJMXConnector(Long timeout) throws IOException, MalformedURLException, MalformedObjectNameException {
        this.timeout = timeout;
        this.init();
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    public Long install(File bundleFile) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Long) connection.invoke(osgiFramework, "installBundle",
                new Object[]{String.format("file://%s", bundleFile.getAbsolutePath())},
                new String[]{"java.lang.String"});
    }

    public void start(Long bundleId) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        LOG.info("Start bundle {}", bundleId);
        connection.invoke(osgiFramework, "startBundle",
                new Object[]{bundleId},
                new String[]{long.class.getName()});
    }

    public List<Bundle> list() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        TabularDataSupport tabularDataSupport = (TabularDataSupport) connection.invoke(karafBundles, "list", null, null);
        List<Bundle> bundles = new ArrayList<>();
        for (Map.Entry entry : tabularDataSupport.entrySet()) {
            Bundle bundle = new Bundle();
            CompositeDataSupport karafInfo = (CompositeDataSupport) entry.getValue();
            bundle.setId(Long.parseLong(String.valueOf(karafInfo.get("ID"))));
            bundle.setName(String.valueOf(karafInfo.get("Name")));
            bundle.setVersion(String.valueOf(karafInfo.get("Version")));
            bundle.setState(Bundle.State.valueOf(String.valueOf(karafInfo.get("State"))));
            if (karafInfo.containsKey("Blueprint")) {
                String blueprintState = String.valueOf(karafInfo.get("Blueprint"));
                bundle.setBlueprintState(blueprintState);
            }
            if (karafInfo.containsKey("Spring")) {
                String springState = String.valueOf(karafInfo.get("Spring"));
                bundle.setSpringState(springState);
            }
            bundles.add(bundle);
        }
        return bundles;
    }

    public Bundle getBundle(Long bundleId) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        Bundle bundle = new Bundle();
        CompositeDataSupport osgiInfo = (CompositeDataSupport) connection.invoke(osgiBundleState, "getBundle", new Object[]{bundleId},
                new String[]{long.class.getName()});
        TabularDataSupport karafList = (TabularDataSupport) connection.invoke(karafBundles, "list", null, null);
        CompositeDataSupport karafInfo = (CompositeDataSupport) karafList.get(new Object[]{bundleId});
        String name = String.valueOf(osgiInfo.get("SymbolicName"));
        String state = String.valueOf(osgiInfo.get("State"));
        String version = String.valueOf(osgiInfo.get("Version"));
        bundle.setId(bundleId);
        bundle.setName(name);
        bundle.setVersion(version);
        bundle.setState(Bundle.State.valueOf(state));
        if (karafInfo.containsKey("Blueprint")) {
            String blueprintState = String.valueOf(karafInfo.get("Blueprint"));
            bundle.setBlueprintState(blueprintState);
        }
        if (karafInfo.containsKey("Spring")) {
            String springState = String.valueOf(karafInfo.get("Spring"));
            bundle.setSpringState(springState);
        }
        return bundle;
    }

    public Object featureInstall(String feature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (TabularDataSupport) connection.invoke(karafFeatures, "installFeature",
                new Object[]{feature},
                new String[]{"java.lang.String"});
    }

    public void shutdown() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        connection.invoke(karafSystem, "shutdown", null, null);
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
                karafSystem = new ObjectName("org.apache.karaf:type=system,name=root");
                Boolean result = !connection.queryMBeans(new ObjectName("osgi.core:type=framework,*"), null).isEmpty()
                        && !connection.queryMBeans(new ObjectName("osgi.core:type=bundleState,*"), null).isEmpty()
                        && !connection.getObjectInstance(karafFeatures).getClassName().isEmpty()
                        && !connection.getObjectInstance(karafSystem).getClassName().isEmpty()
                        && !connection.getObjectInstance(karafBundles).getClassName().isEmpty();
                if (result) {
                    osgiFramework = connection.queryMBeans(new ObjectName("osgi.core:type=framework,*"), null).iterator().next().getObjectName();
                    osgiBundleState = connection.queryMBeans(new ObjectName("osgi.core:type=bundleState,*"), null).iterator().next().getObjectName();
                    connection.addNotificationListener(osgiBundleState, new OsgiBundleNotificationListener(), null, null);
                    connection.addNotificationListener(karafFeatures, new KarafFeaturesNotificationListener(), null, null);
                }
                return result;
            } catch (IOException | MalformedObjectNameException | InstanceNotFoundException ex) {
                LOG.trace(ex.getMessage(), ex);
                return Boolean.FALSE;
            }
        });
    }

    class OsgiBundleNotificationListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification.getUserData() != null) {
                CompositeDataSupport compositeDataSupport = (CompositeDataSupport) notification.getUserData();
                LOG.info("{} {} {}: {}",
                        compositeDataSupport.get("Identifier"),
                        compositeDataSupport.get("SymbolicName"),
                        compositeDataSupport.get("Location"),
                        notification.getMessage());
            }
        }
    }

    class KarafFeaturesNotificationListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (notification.getUserData() != null) {
                CompositeDataSupport compositeDataSupport = (CompositeDataSupport) notification.getUserData();
                LOG.info("{}.{} {}: {}",
                        compositeDataSupport.get("Name"),
                        compositeDataSupport.get("Version"),
                        compositeDataSupport.get("Type"),
                        notification.getMessage());
            }
        }
    }

}

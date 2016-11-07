package it.imolinfo.maven.plugins.samples.it;

import it.imolinfo.camelcmissample.ws.v1.CamelCmisSampleV1;
import it.imolinfo.camelcmissample.ws.v1.CamelCmisSampleV1_Service;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomo
 */
public class AbstractIT {

    private static final Integer JETTY_PORT = 7070;
    private static final String JETTY_CONTEXT_PATH = "/chemistry-opencmis-server-inmemory";
    private static final String CMIS_WAR_PATH = "target/webapp-tmp/chemistry-opencmis-server-inmemory-1.0.0.war";

    private static Server JETTY_SERVER;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIT.class);

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void startJettyServer() throws Exception {

        JETTY_SERVER = new Server(JETTY_PORT);
        JETTY_SERVER.setHandler(new WebAppContext(CMIS_WAR_PATH, JETTY_CONTEXT_PATH));
        ((LifeCycle) JETTY_SERVER).start();
    }

    protected CamelCmisSampleV1 getPort() {
        return getPort(Boolean.TRUE);
    }

    protected CamelCmisSampleV1 getPort(Boolean mtomEnabled) {
        CamelCmisSampleV1_Service service = new CamelCmisSampleV1_Service();
        CamelCmisSampleV1 camelCmisSampleV1 = service.getCamelCmisSampleSoapV1();
        BindingProvider bindingProvider = (BindingProvider) camelCmisSampleV1;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://127.0.0.1:8181/cxf/camelCmisSample_v1");
        SOAPBinding binding = (SOAPBinding) bindingProvider.getBinding();
        LOG.info("MTOM: {}", mtomEnabled);
        binding.setMTOMEnabled(mtomEnabled);
        return camelCmisSampleV1;
    }

    @AfterClass
    public static void stopJettyServer() throws Exception {
        ((LifeCycle) JETTY_SERVER).stop();
    }
}

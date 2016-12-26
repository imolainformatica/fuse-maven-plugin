package it.imolinfo.maven.plugins.samples.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomo
 */
public class RestIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(RestIT.class);
    
    @Rule
    public TestName testName = new TestName();
    
    
    /**
     * Test feature http4 and etc property file
     * @throws MalformedURLException
     * @throws IOException 
     */
    @Test
    public void https4Test() throws MalformedURLException, IOException {
        LOG.info("Start test {}", testName.getMethodName());
        URL url = new URL("http://127.0.0.1:8282/sample/v1");
        URLConnection urlConnection = url.openConnection();
        InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();
        Assert.assertEquals("{\"result\": \"OK\"}", line);
        
    }
    
}

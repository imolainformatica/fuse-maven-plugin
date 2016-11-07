package it.imolinfo.maven.plugins.samples.it;

import it.imolinfo.camelcmissample.schema.v1.CreateDocumentResponseType;
import it.imolinfo.camelcmissample.schema.v1.CreateDocumentType;
import it.imolinfo.camelcmissample.schema.v1.CreateDocumentsResponseType;
import it.imolinfo.camelcmissample.schema.v1.CreateDocumentsType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.activation.DataHandler;
import org.junit.Assert;
import org.junit.Rule;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author giacomo
 */
public class CamelCmisIT extends AbstractIT {

    private static final Logger LOG = LoggerFactory.getLogger(CamelCmisIT.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void createDocumentsTest() throws IOException {
        LOG.info("Start test {}", testName.getMethodName());
        File document1 = new File("./src/test/resources/doc1.pdf");
        File document2 = new File("./src/test/resources/doc2.pdf");
        String documentName1 = System.nanoTime() + "doc1.pdf";
        String documentName2 = System.nanoTime() + "doc2.pdf";
        CreateDocumentsType createDocumentsType =new CreateDocumentsType();
        CreateDocumentType createDocumentType1 = createDocumentRequest(documentName1, "/", document1, "cmis:document");
        CreateDocumentType createDocumentType2 = createDocumentRequest(documentName2, "/", document2, "cmis:document");
        createDocumentsType.getDocuments().add(createDocumentType1);
        createDocumentsType.getDocuments().add(createDocumentType2);
        CreateDocumentsResponseType createDocumentsResponseType = super.getPort().createDocuments(createDocumentsType);
        Assert.assertEquals("Dimensione lista documenti errata", 2, createDocumentsResponseType.getDocuments().size());
        for (CreateDocumentResponseType createDocumentResponseType : createDocumentsResponseType.getDocuments()) {
            LOG.info("Creato documento {}:{}", createDocumentResponseType.getDocumentId(), createDocumentResponseType.getDocumentName());
        }
        Assert.assertEquals("Nome non corretto", documentName1, createDocumentsResponseType.getDocuments().get(0).getDocumentName());
        Assert.assertEquals("Nome non corretto", documentName2, createDocumentsResponseType.getDocuments().get(1).getDocumentName());
    }


    private CreateDocumentType createDocumentRequest(String documentName, String folder, File document, String documentClass) throws MalformedURLException {
        CreateDocumentType createDocumentRequest = new CreateDocumentType();

        createDocumentRequest.setDocumentName(documentName);
        createDocumentRequest.setDocumentContentType("application/pdf");
        createDocumentRequest.setFolderPath(folder);
        createDocumentRequest.setDocumentClass(documentClass);
        
        URL url = new URL(String.format("file://%s", document.getAbsolutePath()));
        DataHandler dataHandler = new DataHandler(url);
        createDocumentRequest.setDocumentFile(dataHandler);
        return createDocumentRequest;
    }

}

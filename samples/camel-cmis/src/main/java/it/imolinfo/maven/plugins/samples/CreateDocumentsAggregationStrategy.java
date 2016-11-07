package it.imolinfo.maven.plugins.samples;

import it.imolinfo.camelcmissample.schema.v1.CreateDocumentResponseType;
import it.imolinfo.camelcmissample.schema.v1.CreateDocumentsResponseType;
import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

/**
 *
 * @author giacomo
 */
public class CreateDocumentsAggregationStrategy implements AggregationStrategy {


    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String documentId = newExchange.getIn().getBody(String.class);
        CreateDocumentResponseType createDocumentResponseType = new CreateDocumentResponseType();
        createDocumentResponseType.setDocumentId(documentId);
        createDocumentResponseType.setDocumentName(newExchange.getProperty("documentName", String.class));
        CreateDocumentsResponseType createDocumentsResponseType = (oldExchange == null) ? new CreateDocumentsResponseType() : 
                oldExchange.getIn().getBody(CreateDocumentsResponseType.class);
        createDocumentsResponseType.getDocuments().add(createDocumentResponseType);
        newExchange.getIn().setBody(createDocumentsResponseType);
        return newExchange;
    }

}

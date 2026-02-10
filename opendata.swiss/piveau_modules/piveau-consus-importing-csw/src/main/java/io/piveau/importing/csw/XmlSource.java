package io.piveau.importing.csw;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.StringReader;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlSource {
    private static final Logger logger = LoggerFactory.getLogger(XmlSource.class);
    private final String baseURL;

    private int startPosition = 1;
    public int totalRecords = 0;
    private boolean finished = false;
    private HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    public static Namespace cswNamespace = Namespace.getNamespace("csw", "http://www.opengis.net/cat/csw/2.0.2");
    public static Namespace dcNamespace = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    // Setter for the HTTP client to facilitate testing with a mock client.
    public void setClient(HttpClient client) {
        this.client = client;
    }

    public XmlSource(String address, String typeNames) {
        this.baseURL = address + "?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results&typeNames=" + typeNames;
    }

    Stream<List<Element>> getRecordsStream() {
        return Stream.generate(() -> {
            try {
                return getNextRecords();
            } catch (IOException | InterruptedException | org.jdom2.JDOMException e) {
                logger.error("Error fetching records: " + e.getMessage(), e);
                return List.<Element>of(); // Return an empty list on error to continue the stream
            }
        }).takeWhile(records -> !records.isEmpty());
    }

    public List<Element> getNextRecords() throws IOException, InterruptedException, org.jdom2.JDOMException {
        if (finished) {
            return List.<Element>of(); // Return an empty list to indicate no more records
        }
        String requestUrl = this.baseURL + "&startPosition=" + startPosition;
        logger.info(requestUrl);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(requestUrl))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch XML data: HTTP " + response.statusCode());
        }
        logger.info("Successfully fetched XML:");
        logger.info("-------------------------");

        String xmlContent = response.body();
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(new StringReader(xmlContent));
        Element rootElement = document.getRootElement();

        // This assumes the records are under
        // <csw:GetRecordsResponse>/<csw:SearchResults>
        Element records = rootElement.getChild("SearchResults", cswNamespace);
        if (records == null) {
            logger.warn("No 'SearchResults' element found in the CSW response for " + requestUrl + ". Skipping.");
            return List.<Element>of(); // Return an empty list to indicate no records found         
        }
        List<Element> recordsList = records.getChildren("Record", cswNamespace);
        logger.info("Found " + recordsList.size() + " records");

        totalRecords = records.getAttributeValue("numberOfRecordsMatched") != null
            ? Integer.parseInt(records.getAttributeValue("numberOfRecordsMatched"))
            : 0;
        int nextRecord = records.getAttributeValue("nextRecord") != null
            ? Integer.parseInt(records.getAttributeValue("nextRecord"))
            : 0;
        logger.info("Total records: " + totalRecords);
        logger.info("Next record: " + nextRecord);
        if(totalRecords == 0 || nextRecord == 0 || nextRecord > totalRecords) {
            logger.info("No more records to fetch. Ending stream.");
            finished = true;
        }

        startPosition = nextRecord;
        return recordsList;
    }
}

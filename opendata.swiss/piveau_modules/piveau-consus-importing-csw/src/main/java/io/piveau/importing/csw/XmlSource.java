package io.piveau.importing.csw;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    private boolean finished = false;
    private int startPosition = 1;
    private int totalRecords = 0;

    public int getTotalRecords() {
        return totalRecords;
    }

    public static Namespace cswNamespace = Namespace.getNamespace("csw", "http://www.opengis.net/cat/csw/2.0.2");
    public static Namespace dcNamespace = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    private HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(120))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();


    // Setter for the HTTP client to facilitate testing with a mock client.
    public void setClient(HttpClient client) {
        this.client = client;
    }

    public XmlSource(String address, String typeNames, int pageSize) {
        this.baseURL = address + "?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results&typeNames=" + typeNames + "&maxRecords=" + pageSize;
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
        Element records = parseResponse(getResponseText(requestUrl));
        if (records == null) {
            logger.warn("No 'SearchResults' element found in the CSW response for " + requestUrl + ". Skipping.");
            return List.<Element>of(); // Return an empty list to indicate no records found         
        }

        setTotalRecords(records);
        setStartPosition(records);

        return records.getChildren("Record", cswNamespace);
    }

    private String getResponseText(String requestUrl) throws IOException, InterruptedException {
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

        return response.body();
    }

    private Element parseResponse(String xml) throws org.jdom2.JDOMException, IOException {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(new StringReader(xml));
        Element rootElement = document.getRootElement();
        // This assumes the records are under
        // <csw:GetRecordsResponse>/<csw:SearchResults>
        return rootElement.getChild("SearchResults", cswNamespace);      
    }

    // number of records is expected to be the same for all pages
    public void setTotalRecords(Element records) {
        int numberOfRecordsMatched = records.getAttributeValue("numberOfRecordsMatched") != null
            ? Integer.parseInt(records.getAttributeValue("numberOfRecordsMatched"))
            : 0;
        if (this.totalRecords > 0 && this.totalRecords != numberOfRecordsMatched) {
            throw new RuntimeException("numberOfRecordsMatched changed from " + this.totalRecords + " to " + numberOfRecordsMatched);
        }
        this.totalRecords = numberOfRecordsMatched;
        logger.info("Total records: " + this.totalRecords);
    }

    public void setStartPosition(Element records) {
        int nextRecord = records.getAttributeValue("nextRecord") != null
            ? Integer.parseInt(records.getAttributeValue("nextRecord"))
            : 0;
        // often in the last page nextRecord is totalRecords + 1
        if(totalRecords == 0 || nextRecord == 0 || nextRecord > totalRecords) {
            logger.info("No more records to fetch. Ending stream.");
            finished = true;
        }
        // ensure that nextRecord is always greater than startPosition to avoid infinite loop
        if(nextRecord > 0 && nextRecord <= startPosition) {
            logger.warn("nextRecord is " + nextRecord + ", startPosition is " + startPosition + ". This may indicate an issue with the CSW service. Ending stream to avoid infinite loop.");
            finished = true;
        }

        this.startPosition = nextRecord;
        logger.info("Next record: " + this.startPosition);
    }
}

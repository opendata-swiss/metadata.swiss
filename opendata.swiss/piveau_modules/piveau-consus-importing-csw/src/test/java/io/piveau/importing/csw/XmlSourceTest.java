package io.piveau.importing.csw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;

public class XmlSourceTest {

    @Test
    public void swisstopo() {
        test("https://geocat.ch/geonetwork/swisstopo/ger/csw", "swisstopo-harvester");
    }

    @Test
    public void aargau_kt() {
        test("https://geocat.ch/geonetwork/ag/ger/csw", "aargau-kt");
    }

    @Test
    public void amt_fur_geoinformation_kt_sh() {
        test("https://www.geocat.ch/geonetwork/sh/ger/csw", "amt-fur-geoinformation-kt-sh");
    }

    @Test
    public void glarus_kt() {
        test("https://www.geocat.ch/geonetwork/gl/ger/csw", "glarus-kt");
    }

    void test(String address, String catalogue) {
        AtomicInteger index = new AtomicInteger(1);
        XmlSource xmlSource = new XmlSource(address);
        xmlSource.getRecordsStream()
            .flatMap(records -> records.stream())
            .forEach(record -> print(record, index.getAndIncrement()));
        assertTrue(xmlSource.getTotalRecords() > 0);
        assertEquals(index.get(), xmlSource.getTotalRecords() + 1);
    }

    JSONObject getConfig(Path path) {
        try {
            String content = Files.readString(path);
            JSONArray segments = new JSONObject(content)
                .getJSONObject("body")
                .getJSONArray("segments");
            for (int i = 0; i < segments.length(); i++) {
                JSONObject segment = segments.getJSONObject(i);
                if (segment.getJSONObject("header").getString("name").equals("piveau-consus-importing-csw")) {
                    return segment.getJSONObject("body").getJSONObject("config");
                }
            }
            throw new RuntimeException("Config not found in file: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Config not found in file: " + path);        
        }
    }

    static void print(Element record, Integer index) {
        System.out.println("-------------- " + index + " -------------------------------");
        String xmlString = new XMLOutputter().outputString(record);
        System.out.println(xmlString);
    }
}
package io.piveau.importing.csw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;

public class XmlSourceTest {

    @Test
    public void aargau_kt() {
        test("https://geocat.ch/geonetwork/ag/ger/csw", "dcat", "aargau-kt");
    }

    @Test
    public void amt_fur_geoinformation_kt_sh() {
        test("https://www.geocat.ch/geonetwork/sh/ger/csw", "dcat", "amt-fur-geoinformation-kt-sh");
    }

        @Test
    public void glarus_kt() {
        test("https://www.geocat.ch/geonetwork/gl/ger/csw", "dcat", "glarus-kt");
    }

    @Test // takes a lot of time
    public void testAll() throws Exception {
        File dir = new File("../../metadata/piveau_pipes");
        System.out.println("Looking for test files in " + dir.getAbsolutePath());
        Files.list(dir.toPath())
            .filter(path -> path.toString().endsWith(".json"))
            .sorted()
            .peek(path -> System.out.println("Testing file " + path))
            .map(path -> getConfig(path))
            .filter(config -> !skipList.contains(config.getString("catalogue")))
            .forEach(config -> test(config.getString("address"), config.getString("typeNames"), config.getString("catalogue")));
    }

    void test(String address, String typeNames, String catalogue) {
        AtomicInteger index = new AtomicInteger(1);
        XmlSource xmlSource = new XmlSource(address, typeNames);
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

    // both geocat-ge and sitg have a very large number of records (about 17K, suspiciously the exact same number)
    // which makes the test take a very long time. They also have the same issue: once we reach 15K, the server response is an error
    // https://www.geocat.ch/geonetwork/GE/ger/csw?service=CSW&version=2.0.2&request=GetRecords&elementsetname=full&resultType=results&typeNames=dcat&startPosition=15001
    Set<String> skipList = Set.of(
        "geocat-ge",
        "sitg"
    );

    static void print(Element record, Integer index) {
        System.out.println("-------------- " + index + " -------------------------------");
        String xmlString = new XMLOutputter().outputString(record);
        System.out.println(xmlString);

    }
}
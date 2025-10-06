# Geoharvesters - metadata transformation

The following fields are fetched from geocat.ch APIs:

|Input property (.xml)|Output property (.rdf)|Output class (.rdf)|
|:---|:---|:---|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:URI`|`dct:accessURL`|`dcat:Distribution`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:date`|unused||
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:description`|`dct:description`|`dcat:Dataset`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:format`|`dct:format`|`dcat:Distribution`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:identifier`|`dct:identifier`|`dcat:Distribution`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:language`|`dct:title`<br>`dct:description`<br>`dcat:keyword`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:language`|`dct:language`|`dcat:Distribution`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:subject`|`dcat:keyword`|`dcat:Dataset`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:title`|`dct:title`|`dcat:Distribution`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dc:type`|unused||
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dct:abstract`|unused||
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/dct:modified`|`dct:issued`<br>`dct:modified`|`dcat:Dataset`|
|`csw:GetRecordsResponse/csw:SearchResults/csw:Record/ows:BoundingBox`|`dct:spatial`|`dcat:Dataset`|


Where in .xml files:
```
<csw:Record xmlns:csw="http://www.opengis.net/cat/csw/2.0.2>
<csw:Record xmlns:dc="http://purl.org/dc/elements/1.1/>
```

and in .rdf files:
```
PREFIX dcat: <http://www.w3.org/ns/dcat#>
PREFIX dct:  <http://purl.org/dc/terms/>
```

Example output:
```
PREFIX dcat: <http://www.w3.org/ns/dcat#>
PREFIX dct:  <http://purl.org/dc/terms/>
PREFIX locn: <http://www.w3.org/ns/locn#>
PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

<https://piveau.io/set/data/0b254869-1783-edd3-e063-5015630add48>
        rdf:type           dcat:Dataset;
        dct:description    "Beschreibung"@de;
        dct:issued         "2025-09-23T00:00:00";
        dct:modified       "2025-09-23T00:00:00";
        dct:spatial        [ rdf:type       dct:Location;
                             locn:geometry  "{\"type\": \"Polygon\", \"coordinates\": [[[8.64513528,47.44191401],[8.83354431,47.44191401],[8.83354431,47.551913],[8.64513528,47.551913],[8.64513528,47.44191401]]]}"^^<https://replacement.io/assignments/media-types/application/vnd.geo+json>
                           ];
        dct:title          "Titel"@de;
        dcat:distribution  <https://piveau.io/set/distribution/fa9e8f3d-a7d4-4bc0-b724-cdb9850f1ff5>;
        dcat:keyword       "Kunstbauten"@de , "Bauwerke"@de.

<https://piveau.io/set/distribution/fa9e8f3d-a7d4-4bc0-b724-cdb9850f1ff5>
        rdf:type        dcat:Distribution;
        dct:format      <http://publications.europa.eu/input/authority/file-type/SDF>;
        dct:identifier  "0b254869-1783-edd3-e063-5015630add48";
        dct:accessURL   <https://example.com>;
        dct:language    "de"^^dct:LinguisticSystem.

```

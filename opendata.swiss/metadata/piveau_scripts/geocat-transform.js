function transforming(input) {

    if (typeof input === 'string') {
        input = JSON.parse(input);
    }


    input = input["csw:Record"]

    var output = {
        "@context": {
            "Agent": "http://xmlns.com/foaf/0.1/Agent",
            "Catalog": "http://www.w3.org/ns/dcat#Catalog",
            "CatalogRecord": "http://www.w3.org/ns/dcat#CatalogRecord",
            "Checksum": "http://spdx.org/rdf/terms#Checksum",
            "Concept": "http://www.w3.org/2004/02/skos/core#Concept",
            "ConceptScheme": "http://www.w3.org/2004/02/skos/core#ConceptScheme",
            "Dataset": "http://www.w3.org/ns/dcat#Dataset",
            "Distribution": "http://www.w3.org/ns/dcat#Distribution",
            "Document": "http://xmlns.com/foaf/0.1/Document",
            "Frequency": "http://purl.org/dc/terms/Frequency",
            "Identifier": "http://www.w3.org/ns/adms#Identifier",
            "Kind": "http://www.w3.org/2006/vcard/ns#Kind",
            "LicenseDocument": "http://purl.org/dc/terms/LicenseDocument",
            "LinguisticSystem": "http://purl.org/dc/terms/LinguisticSystem",
            "Literal": "http://www.w3.org/2000/01/rdf-schema#Literal",
            "Location": "http://purl.org/dc/terms/Location",
            "MediaTypeOrExtent": "http://purl.org/dc/terms/MediaTypeOrExtent",
            "PeriodOfTime": "http://purl.org/dc/terms/PeriodOfTime",
            "ProvenanceStatement": "http://purl.org/dc/terms/ProvenanceStatement",
            "Resource": "http://www.w3.org/2000/01/rdf-schema#Resource",
            "RightsStatement": "http://purl.org/dc/terms/RightsStatement",
            "Standard": "http://purl.org/dc/terms/Standard",

            "accessRights": {
                "@id": "http://purl.org/dc/terms/accessRights",
                "@type": "http://purl.org/dc/terms/RightsStatement"
            },
            "accessURL": {
                "@id": "http://www.w3.org/ns/dcat#accessURL",
                "@type": "http://www.w3.org/2000/01/rdf-schema#Resource"
            },
            "accrualPeriodicity": {
                "@id": "http://purl.org/dc/terms/accrualPeriodicity",
                "@type": "http://purl.org/dc/terms/Frequency"
            },
            "algorithm": {
                "@id": "http://spdx.org/rdf/terms#algorithm",
                "@type": "http://spdx.org/rdf/terms#checksumAlgorithm_sha1"
            },
            "application_profile": {
                "@id": "http://purl.org/dc/terms/conformsTo",
                "@type": "http://www.w3.org/2000/01/rdf-schema#Resource"
            },
            "byteSize": {
                "@id": "http://www.w3.org/ns/dcat#byteSize",
                "@type": "http://www.w3.org/2001/XMLSchema#decimal"
            },
            "checksum": {
                "@id": "http://spdx.org/rdf/terms#checksum",
                "@type": "http://spdx.org/rdf/terms#Checksum"
            },
            "checksumValue": {
                "@id": "http://spdx.org/rdf/terms#checksumValue",
                "@type": "http://www.w3.org/2001/XMLSchema#hexBinary"
            },
            "conforms_to": {
                "@id": "http://purl.org/dc/terms/conformsTo",
                "@type": "http://purl.org/dc/terms/Standard"
            },
            "contactPoint": {
                "@id": "http://www.w3.org/ns/dcat#contactPoint",
                "@type": "http://www.w3.org/2006/vcard/ns#Kind"
            },
            "dataset": {
                "@id": "http://www.w3.org/ns/dcat#dataset",
                "@type": "http://www.w3.org/ns/dcat#Dataset"
            },
            "description": {
                "@id": "http://purl.org/dc/terms/description",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "distribution": {
                "@id": "http://www.w3.org/ns/dcat#distribution",
                "@type": "http://www.w3.org/ns/dcat#Distribution"
            },
            "downloadURL": {
                "@id": "http://www.w3.org/ns/dcat#downloadURL",
                "@type": "http://www.w3.org/2000/01/rdf-schema#Resource"
            },
            "endDate": {
                "@id": "http://schema.org/endDate"
            },
            "format": {
                "@id": "http://purl.org/dc/terms/format",
                "@type": "http://purl.org/dc/terms/MediaTypeOrExtent"
            },
            "hasPart": {
                "@id": "http://purl.org/dc/terms/hasPart",
                "@type": "http://www.w3.org/ns/dcat#Catalog"
            },
            "hasVersion": {
                "@id": "http://purl.org/dc/terms/hasVersion",
                "@type": "http://www.w3.org/ns/dcat#Dataset"
            },
            "homepage": {
                "@id": "http://xmlns.com/foaf/0.1/homepage",
                "@type": "http://xmlns.com/foaf/0.1/Document"
            },
            "identifier": {
                "@id": "http://purl.org/dc/terms/identifier",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "isPartOf": {
                "@id": "http://purl.org/dc/terms/isPartOf",
                "@type": "http://www.w3.org/ns/dcat#Catalog"
            },
            "issued": {
                "@id": "http://purl.org/dc/terms/issued"
            },
            "isVersionOf": {
                "@id": "http://purl.org/dc/terms/isVersionOf",
                "@type": "http://www.w3.org/ns/dcat#Dataset"
            },
            "keyword": {
                "@id": "http://www.w3.org/ns/dcat#keyword",
                "@type": "http://www.w3.org/2000/01/rdf-schema#Literal"
            },
            "landingPage": {
                "@id": "http://www.w3.org/ns/dcat#landingPage",
                "@type": "http://xmlns.com/foaf/0.1/Document"
            },
            "language": {
                "@id": "http://purl.org/dc/terms/language",
                "@type": "http://purl.org/dc/terms/LinguisticSystem"
            },
            "license": {
                "@id": "http://purl.org/dc/terms/license",
                "@type": "http://purl.org/dc/terms/LicenseDocument"
            },
            "linked_schemas": {
                "@id": "http://purl.org/dc/terms/conformsTo",
                "@type": "http://purl.org/dc/terms/Standard"
            },
            "mediaType": {
                "@id": "http://www.w3.org/ns/dcat#mediaType",
                "@type": "http://purl.org/dc/terms/MediaTypeOrExtent"
            },
            "modified": {
                "@id": "http://purl.org/dc/terms/modified"
            },
            "name": {
                "@id": "http://xmlns.com/foaf/0.1/name",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "notation": {
                "@id": "http://www.w3.org/2004/02/skos/core#notation",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "other_identifier": {
                "@id": "http://www.w3.org/ns/adms#identifier",
                "@type": "http://www.w3.org/ns/adms#Identifier"
            },
            "page": {
                "@id": "http://xmlns.com/foaf/0.1/page",
                "@type": "http://xmlns.com/foaf/0.1/Document"
            },
            "prefLabel": {
                "@id": "http://www.w3.org/2004/02/skos/core#prefLabel",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "primaryTopic": {
                "@id": "http://xmlns.com/foaf/0.1/primaryTopic",
                "@type": "http://www.w3.org/ns/dcat#Dataset"
            },
            "provenance": {
                "@id": "http://purl.org/dc/terms/provenance",
                "@type": "http://purl.org/dc/terms/ProvenanceStatement"
            },
            "publisher": {
                "@id": "http://purl.org/dc/terms/publisher",
                "@type": "http://xmlns.com/foaf/0.1/Agent"
            },
            "record": {
                "@id": "http://www.w3.org/ns/dcat#record",
                "@type": "http://www.w3.org/ns/dcat#CatalogRecord"
            },
            "relation": {
                "@id": "http://purl.org/dc/terms/relation",
                "@type": "http://www.w3.org/2000/01/rdf-schema#Resource"
            },
            "rights": {
                "@id": "http://purl.org/dc/terms/rights",
                "@type": "http://purl.org/dc/terms/RightsStatement"
            },
            "sample": {
                "@id": "http://www.w3.org/ns/adms#sample",
                "@type": "http://www.w3.org/ns/dcat#Distribution"
            },
            "source": {
                "@id": "http://purl.org/dc/terms/source",
                "@type": "http://www.w3.org/ns/dcat#Dataset"
            },
            "source_metadata": {
                "@id": "http://purl.org/dc/terms/source",
                "@type": "http://www.w3.org/ns/dcat#CatalogRecord"
            },
            "spatial": {
                "@id": "http://purl.org/dc/terms/spatial",
                "@type": "http://purl.org/dc/terms/Location"
            },
            "startDate": {
                "@id": "http://schema.org/startDate"
            },
            "status": {
                "@id": "http://www.w3.org/ns/adms#status",
                "@type": "http://www.w3.org/2004/02/skos/core#Concept"
            },
            "temporal": {
                "@id": "http://purl.org/dc/terms/temporal",
                "@type": "http://purl.org/dc/terms/PeriodOfTime"
            },
            "theme": {
                "@id": "http://www.w3.org/ns/dcat#theme",
                "@type": "http://www.w3.org/2004/02/skos/core#Concept"
            },
            "themeTaxonomy": {
                "@id": "http://www.w3.org/ns/dcat#themeTaxonomy",
                "@type": "http://www.w3.org/2004/02/skos/core#ConceptScheme"
            },
            "title": {
                "@id": "http://purl.org/dc/terms/title",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "type": {
                "@id": "http://purl.org/dc/terms/type",
                "@type": "http://www.w3.org/2004/02/skos/core#Concept"
            },
            "versionInfo": {
                "@id": "http://www.w3.org/2002/07/owl#versionInfo",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            },
            "versionNotes": {
                "@id": "http://www.w3.org/ns/adms#versionNotes",
                "@type": "http://www.w3.org/2001/XMLSchema#string"
            }
        }
    };

    const languageMap = {
        "ger": "de",
        "fra": "fr",
        "fre": "fr",
        "ita": "it",
        "eng": "en",
        "de": "de",
        "fr": "fr",
        "it": "it",
        "en": "en",
        "ger + fre": "de",
        "ger + ita": "de",
        "fra + ita": "fr",
        "ger + fra + ita": "de"
    };

    // Required
    output["@type"] = "Dataset";


    output.publisher = {
        "@type": "Agent",
        "name": "Stadt Winterthur",
        "type": {
            "@id": "http://purl.org/adms/publishertype/LocalAuthority"
        }
    };


    let id
    if (input["dc:identifier"]) {
        id = input["dc:identifier"]
    } else {
        id = 0
    }

    let record_language;
    if (input["dc:language"]) {
        // Get the raw language string, which might be an array or a single value.
        let rawLang = Array.isArray(input["dc:language"]) ? input["dc:language"][0] : input["dc:language"];

        let langCode = rawLang; // Default to the raw value

        if (typeof rawLang === 'string' && rawLang.trim()) {
            // Split the string by comma or plus, and any surrounding whitespace.
            // e.g., "ger + fre" becomes ["ger", "fre"]
            const langParts = rawLang.split(/\s*[,\+]\s*/);

            langCode = langParts[0].trim();
        }

        if (!(langCode in languageMap)) {
            console.log(langCode + " not in language map, using as is.");
        }
        record_language = languageMap[langCode] || langCode;
    } else {
        record_language = params.default_language;
    }

    let format = input["dc:format"];

    if (Array.isArray(format)) {
        format = format[0];
    }

    output.distribution = [];
    let dist = {
        "@type": "Distribution",
        "identifier": id,
        "format": [{
            "@id": encodeURI("http://publications.europa.eu/input/authority/file-type/" + format)
        }],

    };

    if (input["dc:title"]) {
        output.title = [
        {
            "@value": input["dc:title"],
            "@language": params.defaultLanguage
        }    ];
        output.distribution.title = output.title;

    }

    if (input["dc:description"]) {
        output.description = [
        {
            "@value": input["dc:description"],
            "@language": params.defaultLanguage
        }    ];
        output.distribution.description = output.description;

    } else if (input["dc:abstract"]) {
        output.description = [
        {
            "@value": input["dc:abstract"],
            "@language": params.defaultLanguage
        }    ];
        output.distribution.description = output.description;
    }

    if (input["dc:URI"]) {
        if (!Array.isArray(input["dc:URI"])) {
            dist["accessUrl"] = input["dc:URI"]["content"]
        } else {
            dist["accessUrl"] = input["dc:URI"].map(item => ({
                "@id": encodeURI(item.content)
            }))
        }

    }
    output.keyword = [];
    if (input["dc:subject"]) {
        const subjects = Array.isArray(input["dc:subject"]) ? input["dc:subject"] : [input["dc:subject"]];

        for (const tag of subjects) {
            if (typeof tag === 'string' && tag.trim()) {
                output.keyword.push({
                    "@value": tag,
                    "@language": record_language
                });
            }
        }
    }

    if (input["ows:BoundingBox"]) {
        if (input["ows:BoundingBox"]["ows:LowerCorner"] && input["ows:BoundingBox"]["ows:UpperCorner"]) {
            const minLat = Number(input["ows:BoundingBox"]["ows:LowerCorner"].split(" ")[0]);
            const minLon = Number(input["ows:BoundingBox"]["ows:LowerCorner"].split(" ")[1]);
            const maxLat = Number(input["ows:BoundingBox"]["ows:UpperCorner"].split(" ")[0]);
            const maxLon = Number(input["ows:BoundingBox"]["ows:UpperCorner"].split(" ")[1]);

            const geojson_points =
                [
                    [minLon, minLat],
                    [maxLon, minLat],
                    [maxLon, maxLat],
                    [minLon, maxLat],
                    [minLon, minLat]
                ]


            output.spatial = {
                "@type": "Location", "http://www.w3.org/ns/locn#geometry": [
                    {
                        "@value": `{\"type\": \"Polygon\", \"coordinates\": [${JSON.stringify(geojson_points)}]}`,
                        "@type": "https://replacement.io/assignments/media-types/application/vnd.geo+json"
                    }
                ]
            };
        }
    }

    dist.language = record_language
    if (input["dct:modified"]) {
        output.issued = `${input["dct:modified"]}T00:00:00`;
        output.modified = `${input["dct:modified"]}T00:00:00`;
    }

    dist.mediaType = input["dc:format"];
    dist.license = "http://dcat-ap.de/def/licenses/dl-by-de/2.0"

    output.distribution.push(dist);
    // console.log("Transformed output:");

    //const { "@context": context, ...outputWithoutContext } = output;
//
    //console.log(JSON.stringify(outputWithoutContext, null, 2));
//
    //console.log("Transformation completed successfully. Returning output.");
    return output
}

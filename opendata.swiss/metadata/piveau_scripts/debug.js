function transforming(input){


    if (typeof input === 'string') {
      input = JSON.parse(input);
    }


  input = input["csw:Record"]
  console.log("input:");
  console.log(JSON.stringify(input, null, 2));

  let output = dcatapContext;

  // Required
  output["@type"] = "Dataset";


    output.title = [
        {
            "@value": "Test title",
            "@language": params.defaultLanguage
        }
    ];

    output.description = [
        {
            "@value": "Test description",
            "@language": params.defaultLanguage
        }
    ];



    output.publisher = {
            "@type": "Agent",
            "name": "Test name",
            "type": {
                "@id": "http://purl.org/adms/publishertype/NationalAuthority"
            }
        };


  output.distribution = [];


    let resource = input;

    let id
    if (resource["dc:identifier"]) {
      id = resource["dc:identifier"]
    } else {
      id = 0
    }

    let record_language
    if (resource["dc:language"]) {
      if (resource["dc:language"] === "ger") {
        record_language = "de"
      } else {
        record_language = input["dc:language"];
      }
    } else {
      record_language = params.default_language;
    }

    let format = resource["dc:format"];

    if (Array.isArray(format)) {
      format = format[0];
    }

    let dist = {
      "@type": "Distribution",
      "identifier": id,
      "accessUrl": [{
        "@id": encodeURI("https://example.eu/set/distribution/" + id)
      }],
      "description": [{
        "@value": "Distribution description " + id,
        "@language": params.defaultLanguage
      }],
      "title": [
        {
          "@value": "Data",
          "@language": "en"
        }
      ],
      "format": [{
        "@id": encodeURI(decodeURI("http://publications.europa.eu/resource/authority/file-type/" + format))
      }],

    };


    dist.language = record_language
    output.issued = resource["dct:modified"]
    output.modified = resource["dct:modified"]
    dist.mediaType = "text/csv"
    dist.license = "http://dcat-ap.de/def/licenses/dl-by-de/2.0"

    if (resource.title) {
      dist.title = [{
        "@value": resource.title,
        "@language": record_language
      }];
    }

    output.distribution.push(dist);

  console.log("Transformed output:");

  const { "@context": context, ...outputWithoutContext } = output;

  console.log(JSON.stringify(outputWithoutContext, null, 2));

  console.log("Transformation completed successfully. Returning output.");
  return output
}


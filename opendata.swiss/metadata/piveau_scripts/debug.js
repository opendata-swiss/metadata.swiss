function transforming(jsonBody) {


  let input;
  try {
    if (typeof jsonBody === 'string') {
      input = JSON.parse(jsonBody);
    } else {
      input = jsonBody;
    }

    console.log("Successfully loaded input for transformation");
    //outputString = JSON.stringify(input, null, 2)
    //console.log(outputString); // Pretty-print the object
  } catch (e) {
    // If the input is not valid JSON, fail gracefully.
    console.error("Failed to parse or process input:", e);
  }



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

  //for (let i = 0; i < input["csw:SearchResults"]["csw:Record"].slice(0,1); i++) {

    let resource = input["csw:SearchResults"]["csw:Record"][0];

    let i = 0
    let id
    if (resource["dc:identifier"]) {
      id = resource["dc:identifier"]
    } else {
      id = i
    }

    output.identifier = id
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


    let dist = {
      "@type": "Distribution",
      "identifier": id,
      "accessUrl": [{
        "@id": encodeURI("https://example.eu/set/distribution/" + id)
      }],
      "description": [{
        "@value": "Distribution description " + i,
        "@language": params.defaultLanguage
      }],
      "title": [
        {
          "@value": "Data",
          "@language": "en"
        }
      ]

    };

    dist.format = "CSV"//resource["dc:format"][0]
    dist.id = mapSingleLiteral(id)
    dist.language = record_language
    dist.issued = resource["dct:modified"]
    dist.mediaType = "text/csv"
    dist.license = "http://dcat-ap.de/def/licenses/dl-by-de/2.0"

    if (resource.title) {
      dist.title = [{
        "@value": resource.title,
        "@language": record_language
      }];
    }

    if (resource.modified) {
      dist.issued = resource.modified
    }
    output.distribution.push(dist);
  //}

  console.log("Transformed output:");

  const { "@context": context, ...outputWithoutContext } = output;

  console.log(JSON.stringify(outputWithoutContext, null, 2));

  return output
}


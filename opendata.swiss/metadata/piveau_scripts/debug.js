function transforming(jsonBody) {

  let input;
  try {
    if (typeof input === 'string') {
      input = JSON.parse(jsonBody);
    } else {
      input = jsonBody;
    }

    console.log("Successfully loaded object:");
    outputString = JSON.stringify(input, null, 2)
    console.log(outputString); // Pretty-print the object
  } catch (e) {
    // If the input is not valid JSON, fail gracefully.
    console.error("Failed to parse or process input:", e);
  }



  let output = dcatapContext;

  // Required
  output["@type"] = "Dataset";

  output.title = piveau.mapLiteral(input.title, output.title, params.default_language)
  output.description = piveau.mapLiteral(input.description, output.description, params.default_language)


  //dc:identifier
  //dc:subject
  //ows:BoundingBox
  //ows:BoundingBox/ows:LowerCorner
  //ows:BoundingBox/ows:UpperCorner

  output.distribution = [];
  for (const resource of input["csw:SearchResults"]["csw:Record"]) {

    if (false) {
    //if (resource.identifier) {

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
        "accessUrl": [{
          "@id": encodeURI("https://example.eu/set/distribution/" + resource.id)
        }],
        "description": [{
          "@value": resource.description,
          "@language": params.defaultLanguage
        }]
      };
      dist.identifier = mapSingleLiteral(resource.id)

      if (resource.name) {
        dist.title = [{
          "@value": resource.title,
          "@language": record_language
        }];
      }

      if (resource["modified"]) {
        dist.issued = resource["modified"]
      }
      output.distribution.push(dist);
    }
  }



  //dc:URI
  //dc:date
  //dc:description
  //dc:format
  //dc:identifier
  //dc:subject
  //dc:type
  //dct:abstract
  //dct:modified
  //ows:BoundingBox
  //ows:BoundingBox/ows:LowerCorner
  //ows:BoundingBox/ows:UpperCorner

  console.log("Transformed output:");
  console.log(JSON.stringify(output, null, 2));

  // Use the callback to pass the result to the next step.
  // This is the correct asynchronous pattern.
  // callback(null, jsonBody);

  return output
}


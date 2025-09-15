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

    let language;
    if (input["dc:language"]) {
      if (input["dc:language"] === "ger") {
        language = "de"
      } else {
        language = input["dc:language"];
      }
    } else
      language = params.defaultLanguage;


    output.title = piveau.mapLiteral(input["dc:title"], output.title, language)
    output.description = piveau.mapLiteral(input["description"], output.description, language)

    // Optional
    output.page = piveau.mapReference(input["dc:URI"], output.page, "Document")


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


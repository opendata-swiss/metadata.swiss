const fs = require('fs');

function transforming(input, callback) {

  try {
    let jsonBody;
    if (typeof input === 'string') {
      jsonBody = JSON.parse(input);
    } else {
      jsonBody = input;
    }


    console.log("Successfully processed object:");
    console.log(JSON.stringify(jsonBody, null, 2)); // Pretty-print the object
    fs.appendFileSync('/app/output.log', outputString + '\n');

    // Use the callback to pass the result to the next step.
    // This is the correct asynchronous pattern.
    callback(null, jsonBody);
  } catch (e) {
    // If the input is not valid JSON, fail gracefully.
    console.error("Failed to parse or process input:", e);
    callback(e, null);
  }
}

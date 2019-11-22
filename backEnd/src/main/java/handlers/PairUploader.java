package handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import imports.DevTestingManager;

import utilities.IOStreamsHelper;
import utilities.JsonParsers;

public class PairUploader implements RequestStreamHandler {

  private final DevTestingManager devTestingManager = new DevTestingManager();

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);

      // upload the data
      if (!jsonMap.isEmpty()) {
        this.devTestingManager.insertNewValuePairs(jsonMap);
        IOStreamsHelper.writeToOutput(outputStream, "Data inserted successfully!");
      } else {
        IOStreamsHelper.writeToOutput(outputStream, "No data entered.");
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      IOStreamsHelper.writeToOutput(outputStream, "Unable to handle request.\n");
    }
  }
}
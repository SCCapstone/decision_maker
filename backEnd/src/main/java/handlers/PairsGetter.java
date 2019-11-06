package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DevTestingManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import utilities.IOStreamsHelper;

public class PairsGetter implements RequestStreamHandler {
  private final DevTestingManager devTestingManager = new DevTestingManager();

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    try {
      String jsonPairsData = this.devTestingManager.getJsonStringOfTableDataForFrontEnd();

      IOStreamsHelper.writeToOutput(outputStream,jsonPairsData);
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      IOStreamsHelper.writeToOutput(outputStream,"Unable to handle request.\n");
    }
  }
}

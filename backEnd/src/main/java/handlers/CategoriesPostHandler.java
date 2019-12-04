package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.CategoriesManager;
import utilities.IOStreamsHelper;
import utilities.JsonParsers;
import utilities.ResultStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class CategoriesPostHandler implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);

      if (!jsonMap.isEmpty()) {
        if (jsonMap.containsKey("action") && jsonMap.containsKey("payload")) {
          try {
            String action = (String) jsonMap.get("action");
            Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");

            ResultStatus resultStatus;

            if (action.equals("newCategory")) {
              resultStatus = CategoriesManager.addNewCategory(payloadJsonMap);
            } else if (action.equals("editCategory")) {
              resultStatus = CategoriesManager.editCategory(payloadJsonMap);
            } else if (action.equals("getCategories")) {
              resultStatus = CategoriesManager.getCategories(payloadJsonMap);
            } else if (action.equals("deleteCategory")) {
              resultStatus = CategoriesManager.deleteCategory(payloadJsonMap);
            } else {
              resultStatus = new ResultStatus(false, "Error: Invalid action entered");
            }

            IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
          } catch (Exception e) {
            IOStreamsHelper.writeToOutput(outputStream,
                new ResultStatus(false, "Error: Unable to parse request.").toString());
          }
        } else {
          //probably want to log this somewhere as front end validation shouldn't have let this through
          //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
          IOStreamsHelper.writeToOutput(outputStream,
              new ResultStatus(false, "Error: No action/payload entered.").toString());
        }
      } else {
        //probably want to log this somewhere as front end validation shouldn't have let this through
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        IOStreamsHelper.writeToOutput(outputStream,
            new ResultStatus(false, "Error: No data entered.").toString());
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      IOStreamsHelper.writeToOutput(outputStream,
          new ResultStatus(false, "Error: Unable to handle request.").toString());
    }
  }
}

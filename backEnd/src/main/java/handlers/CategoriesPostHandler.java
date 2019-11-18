package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.CategoriesManager;
import imports.UsersManager;
import utilities.IOStreamsHelper;
import utilities.JsonParsers;
import utilities.ResultStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class CategoriesPostHandler implements RequestStreamHandler {

  private final CategoriesManager categoriesManager = new CategoriesManager();
  private final UsersManager usersManager = new UsersManager();
  public static final String USERNAME_FIELD = "Username";
  public static final String GET_ALL_FIELD = "GetAll";

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
              resultStatus = this.categoriesManager.addNewCategory(payloadJsonMap);
            } else if (action.equals("editCategory")) {
              resultStatus = this.categoriesManager.editCategory(payloadJsonMap);
            } else if (action.equals("getCategories")) {
              String username = (String) payloadJsonMap.get(USERNAME_FIELD);
              String getAllString = (String) payloadJsonMap.get(GET_ALL_FIELD);
              if (username == null || getAllString == null) {
                IOStreamsHelper.writeToOutput(outputStream, new ResultStatus(false,
                    "Error: Username or getAll parameter invalid ").toString());
                return;
              }

              boolean getAll = Boolean.parseBoolean(getAllString);
              if (getAll) {
                ArrayList<String> categoryIds = this.usersManager.getAllCategoryIds(username);
                resultStatus = new ResultStatus(true,
                    this.categoriesManager.getAllCategories(categoryIds));
              } else {
                resultStatus = new ResultStatus(true, "Not yet implemented");
              }
            } else {
              resultStatus = new ResultStatus(false, "Error: Invalid action entered");
            }

            IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
          } catch (Exception e) {
            IOStreamsHelper.writeToOutput(outputStream,
                new ResultStatus(false, "Error: Unable to parse request. Exception message: " + e).toString());
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

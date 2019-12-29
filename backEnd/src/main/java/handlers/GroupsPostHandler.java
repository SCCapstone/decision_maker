package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import imports.DatabaseManagers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import utilities.IOStreamsHelper;
import utilities.JsonParsers;
import utilities.ResultStatus;

public class GroupsPostHandler implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    System.out.println("top of handle requests in GroupsPostHandler");

    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(inputStream);

      if (!jsonMap.isEmpty()) {
        if (jsonMap.containsKey("action") && jsonMap.containsKey("payload")) {
          try {
            String action = (String) jsonMap.get("action");
            Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");

            ResultStatus resultStatus;

            if (action.equals("getGroups")) {
              resultStatus = DatabaseManagers.GROUPS_MANAGER.getGroups(payloadJsonMap);
            } else if (action.equals("createNewGroup")) {
              resultStatus = DatabaseManagers.GROUPS_MANAGER.createNewGroup(payloadJsonMap);
            } else if (action.equals("editGroup")) {
              resultStatus = DatabaseManagers.GROUPS_MANAGER.editGroup(payloadJsonMap);
            } else if (action.equals("newEvent")) {
              resultStatus = DatabaseManagers.GROUPS_MANAGER.newEvent(payloadJsonMap);
            } else if (action.equals("optUserInOut")) {
              resultStatus = DatabaseManagers.GROUPS_MANAGER.optInOutOfEvent(payloadJsonMap);
            } else {
              resultStatus = new ResultStatus(false, "Error: Invalid action entered");
            }

            IOStreamsHelper.writeToOutput(outputStream, resultStatus.toString());
          } catch (Exception e) {
            IOStreamsHelper.writeToOutput(outputStream,
                new ResultStatus(false, "Error: Unable to parse request in handler.").toString());
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

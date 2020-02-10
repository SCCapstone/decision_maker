package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import imports.DatabaseManagers;
import java.util.Map;
import utilities.GetActiveUser;
import utilities.JsonParsers;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GroupsPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    Metrics metrics = new Metrics(context.getAwsRequestId());
    LambdaLogger lambdaLogger = context.getLogger();

    try {
      Map<String, Object> jsonMap = JsonParsers.parseInput(request.getBody());

      if (!jsonMap.isEmpty()) {
        if (jsonMap.containsKey("action") && jsonMap.containsKey("payload")) {
          try {
            String testpoint0 = "0\n";
            String testpoint1 = "1\n";
            String testpoint2 = "2\n";
            String testpoint3 = "3\n";
            String testpoint4 = "4\n";
            
            resultStatus.resultMessage += testpoint0;
            Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");
            resultStatus.resultMessage += testpoint1; 
            
            if (!payloadJsonMap.containsKey(RequestFields.ACTIVE_USER)) {
              if (jsonMap.containsKey(RequestFields.TESTER_ADMIN)) {
                payloadJsonMap
                  .put(RequestFields.ACTIVE_USER, RequestFields.TESTER_ADMIN);
              } else {
                payloadJsonMap
                    .put(RequestFields.ACTIVE_USER,
                       GetActiveUser.getActiveUserFromRequest(request, context));
              }
              resultStatus.resultMessage += testpoint2;

              String action = (String) jsonMap.get("action");
              
              resultStatus.resultMessage += testpoint3;

              if (action.equals("getGroups")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .getGroups(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("createNewGroup")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER.createNewGroup(payloadJsonMap);
              } else if (action.equals("editGroup")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER.editGroup(payloadJsonMap);
              } else if (action.equals("newEvent")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .newEvent(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("optUserInOut")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER.optInOutOfEvent(payloadJsonMap);
              } else if (action.equals("voteForChoice")) {
                resultStatus.resultMessage += testpoint4;
                resultStatus = DatabaseManagers.GROUPS_MANAGER.voteForChoice(payloadJsonMap);
              } else if (action.equals("warmingEndpoint")) {
                resultStatus = new ResultStatus(true, "Warming groups endpoint.");
              } else {
                resultStatus.resultMessage += "Error: Invalid action entered";
              }
            } else {
              resultStatus.resultMessage += "Error: Invalid key in payload."; 
            }
          } catch (Exception e) {
            resultStatus.resultMessage += "Error: Unable to parse request in handler. Message: "+ e;
          }
        } else {
          //probably want to log this somewhere as front end validation shouldn't have let this through
          //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
          resultStatus.resultMessage = "Error: No action/payload entered.";
        }
      } else {
        //probably want to log this somewhere as front end validation shouldn't have let this through
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: No data entered.";
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Unable to handle request.";
    }

    metrics.logMetrics(lambdaLogger);

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}

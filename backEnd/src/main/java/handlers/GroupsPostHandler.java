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
            Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");

            if (!payloadJsonMap.containsKey(RequestFields.ACTIVE_USER)) {
              payloadJsonMap
                  .put(RequestFields.ACTIVE_USER,
                      GetActiveUser.getActiveUserFromRequest(request, context));

              String action = (String) jsonMap.get("action");

              if (action.equals("getGroups")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .getGroups(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("createNewGroup")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .createNewGroup(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("editGroup")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .editGroup(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("newEvent")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .newEvent(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("optUserInOut")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER.optInOutOfEvent(payloadJsonMap);
              } else if (action.equals("voteForChoice")) {
                resultStatus = DatabaseManagers.GROUPS_MANAGER
                    .voteForChoice(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("warmingEndpoint")) {
                resultStatus = new ResultStatus(true, "Warming groups endpoint.");
              } else {
                resultStatus.resultMessage = "Error: Invalid action entered";
              }
            } else {
              resultStatus.resultMessage = "Error: Invalid key in payload.";
            }
          } catch (Exception e) {
            resultStatus.resultMessage = "Error: Unable to parse request in handler.";
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

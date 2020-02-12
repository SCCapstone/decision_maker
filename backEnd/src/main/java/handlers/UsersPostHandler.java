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

public class UsersPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    final Metrics metrics = new Metrics(context.getAwsRequestId());
    final LambdaLogger lambdaLogger = context.getLogger();

    final String classMethod = "UsersPostHandler.handleRequest";
    metrics.commonSetup(classMethod);

    try {
      final Map<String, Object> jsonMap = JsonParsers.parseInput(request.getBody());

      if (jsonMap.containsKey("action") && jsonMap.containsKey("payload")) {
        final Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");

        if (!payloadJsonMap.containsKey(RequestFields.ACTIVE_USER)) {
          payloadJsonMap
              .put(RequestFields.ACTIVE_USER,
                  GetActiveUser.getActiveUserFromRequest(request, context));

          final String action = (String) jsonMap.get("action");

          if (action.equals("updateUserChoiceRatings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .updateUserChoiceRatings(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("getUserRatings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .getUserRatings(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("updateUserSettings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .updateUserSettings(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("getUserData")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .getUserData(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("warmingEndpoint")) {
            resultStatus = new ResultStatus(true, "Warming users endpoint.");
          } else {
            resultStatus.resultMessage = "Error: Invalid action entered";
          }
        } else {
          resultStatus.resultMessage = "Error: Invalid key in payload.";
        }
      } else {
        //probably want to log this somewhere as front end validation shouldn't have let this through
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: No action/payload entered.";
      }
    } catch (Exception e) {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Unable to handle request.";
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics(lambdaLogger);

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}
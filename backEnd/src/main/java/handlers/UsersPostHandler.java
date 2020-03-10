package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import imports.DatabaseManagers;
import java.util.Map;
import utilities.ErrorDescriptor;
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
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
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
                .updateUserChoiceRatings(payloadJsonMap, false, metrics, lambdaLogger);
          } else if (action.equals("getUserRatings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .getUserRatings(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("updateUserSettings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .updateUserSettings(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("getUserData")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .getUserData(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("registerPushEndpoint")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .createPlatformEndpointAndStoreArn(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("unregisterPushEndpoint")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .unregisterPushEndpoint(payloadJsonMap, metrics, lambdaLogger);
          } else if (action.equals("warmingEndpoint")) {
            resultStatus = new ResultStatus(true, "Warming users endpoint.");
          } else {
            resultStatus.resultMessage = "Error: Invalid action entered.";
            lambdaLogger.log(
                new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                    "Invalid action entered.").toString());
          }
        } else {
          resultStatus.resultMessage = "Error: Invalid key in payload.";
          lambdaLogger.log(
              new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                  "Invalid key in payload.").toString());
        }
      } else {
        resultStatus.resultMessage = "Error: No action/payload entered.";
        lambdaLogger.log(
            new ErrorDescriptor<>(jsonMap, classMethod, metrics.getRequestId(),
                "No action/payload entered.").toString());
      }
    } catch (Exception e) {
      resultStatus.resultMessage = "Error: Unable to handle request.";
      lambdaLogger.log(
          new ErrorDescriptor<>(request.getBody(), classMethod, metrics.getRequestId(), e)
              .toString());
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}
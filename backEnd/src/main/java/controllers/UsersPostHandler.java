package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import handlers.DatabaseManagers;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.GetActiveUser;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class UsersPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    final String classMethod = "UsersPostHandler.handleRequest";
    metrics.commonSetup(classMethod);

    try {
      final Map<String, Object> jsonMap = JsonUtils.parseInput(request.getBody());

      if (jsonMap.containsKey("action") && jsonMap.containsKey("payload")) {
        final Map<String, Object> payloadJsonMap = (Map<String, Object>) jsonMap.get("payload");

        if (!payloadJsonMap.containsKey(RequestFields.ACTIVE_USER)) {
          payloadJsonMap
              .put(RequestFields.ACTIVE_USER,
                  GetActiveUser.getActiveUserFromRequest(request, context));

          final String action = (String) jsonMap.get("action");

          if (action.equals("updateUserSettings")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .updateUserSettings(payloadJsonMap, metrics);
          } else if (action.equals("updateSortSetting")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .updateSortSetting(payloadJsonMap, metrics);
          } else if (action.equals("getUserData")) {
            resultStatus = DatabaseManagers.USERS_MANAGER.getUserData(payloadJsonMap, metrics);
          } else if (action.equals("registerPushEndpoint")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .createPlatformEndpointAndStoreArn(payloadJsonMap, metrics);
          } else if (action.equals("unregisterPushEndpoint")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .unregisterPushEndpoint(payloadJsonMap, metrics);
          } else if (action.equals("markEventAsSeen")) {
            resultStatus = DatabaseManagers.USERS_MANAGER.markEventAsSeen(payloadJsonMap, metrics);
          } else if (action.equals("setUserGroupMute")) {
            resultStatus = DatabaseManagers.USERS_MANAGER.setUserGroupMute(payloadJsonMap, metrics);
          } else if (action.equals("markAllEventsSeen")) {
            resultStatus = DatabaseManagers.USERS_MANAGER
                .markAllEventsSeen(payloadJsonMap, metrics);
          } else {
            resultStatus.resultMessage = "Error: Invalid action entered.";
            metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Invalid action entered."));
          }
        } else {
          resultStatus.resultMessage = "Error: Invalid key in payload.";
          metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "Invalid key in payload."));
        }
      } else {
        resultStatus.resultMessage = "Error: No action/payload entered.";
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, "No action/payload entered."));
      }
    } catch (Exception e) {
      resultStatus.resultMessage = "Error: Unable to handle request.";
      metrics.log(new ErrorDescriptor<>(request.getBody(), classMethod, e));
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}
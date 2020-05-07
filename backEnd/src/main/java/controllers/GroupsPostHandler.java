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

public class GroupsPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    final String classMethod = "GroupsPostHandler.handleRequest";
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

          if (action.equals("getGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.getGroup(payloadJsonMap, metrics);
          } else if (action.equals("createNewGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.createNewGroup(payloadJsonMap, metrics);
          } else if (action.equals("editGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.editGroup(payloadJsonMap, metrics);
          } else if (action.equals("deleteGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.deleteGroup(payloadJsonMap, metrics);
          } else if (action.equals("newEvent")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.newEvent(payloadJsonMap, metrics);
          } else if (action.equals("optUserInOut")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.optInOutOfEvent(payloadJsonMap, metrics);
          } else if (action.equals("leaveGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.leaveGroup(payloadJsonMap, metrics);
          } else if (action.equals("rejoinGroup")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.rejoinGroup(payloadJsonMap, metrics);
          } else if (action.equals("voteForChoice")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER.voteForChoice(payloadJsonMap, metrics);
          } else if (action.equals("getBatchOfEvents")) {
            resultStatus = DatabaseManagers.GROUPS_MANAGER
                .handleGetBatchOfEvents(payloadJsonMap, metrics);
          } else {
            resultStatus.resultMessage = "Error: Invalid action entered";
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
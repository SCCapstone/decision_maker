package dbMaintenance.controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import controllers.ApiRequestController;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.GetActiveUser;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class DbMaintenanceController implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private static final Map<String, Class<? extends ApiRequestController>> ACTIONS_TO_CONTROLLERS = Maps
      .newHashMap(ImmutableMap.<String, Class<? extends ApiRequestController>>builder()
          .put("keyUserRatingsByVersion", KeyUserRatingsByVersionController.class)
          .put("addCategoryCreatorToGroup", AddCategoryCreatorToGroupController.class)
          .build());

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    final String classMethod = "DbMaintenanceController.handleRequest";

    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus;

    try {
      String action = request.getPath(); // this should be of the form '/action'
      String[] splitAction = action.split("/"); // remove the prefixed slash

      if (splitAction.length == 2) {
        action = splitAction[1]; // the action is after the '/'

        if (ACTIONS_TO_CONTROLLERS.containsKey(action)) {
          final Map<String, Object> jsonMap = JsonUtils.parseInput(request.getBody());
          metrics.setRequestBody(jsonMap); // attach here for logging before handling action

          if (!jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
            //get the active user from the authorization header and put it in the request payload
            jsonMap.put(RequestFields.ACTIVE_USER,
                GetActiveUser.getActiveUserFromRequest(request, context));

            final Class<? extends ApiRequestController> actionHandlerClass = ACTIONS_TO_CONTROLLERS
                .get(action);
            final Constructor c = actionHandlerClass.getConstructor(); // get the default
            final ApiRequestController apiRequestHandler = (ApiRequestController) c.newInstance();
            resultStatus = apiRequestHandler.processApiRequest(jsonMap, metrics);
          } else {
            //bad request body, log warning
            resultStatus = ResultStatus.failure("Error: Bad request body.");
          }
        } else {
          metrics.log(new WarningDescriptor<>(
              new HashMap<String, Object>() {{
                put("path", request.getPath());
                put("body", request.getBody());
              }},
              classMethod, "Unknown action."));
          resultStatus = ResultStatus.failure("Error: Unknown action.");
        }
      } else {
        metrics.log(new WarningDescriptor<>(
            new HashMap<String, Object>() {{
              put("path", request.getPath());
              put("body", request.getBody());
            }},
            classMethod, "Bad request format."));
        resultStatus = ResultStatus.failure("Error: Bad request format.");
      }
    } catch (final Exception e) {
      metrics.log(new ErrorDescriptor<>(
          new HashMap<String, Object>() {{
            put("path", request.getPath());
            put("body", request.getBody());
          }},
          classMethod, e));
      resultStatus = ResultStatus.failure("Error: Exception occurred.");
    }

    metrics.commonClose(resultStatus.success);
    metrics.logMetrics();

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}

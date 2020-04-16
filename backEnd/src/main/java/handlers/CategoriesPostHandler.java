package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import imports.DatabaseManagers;
import imports.WarmingManager;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.GetActiveUser;
import utilities.JsonParsers;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class CategoriesPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    final String classMethod = "CategoriesPostHandler.handleRequest";
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

          if (action.equals("newCategory")) {
            resultStatus = DatabaseManagers.CATEGORIES_MANAGER.addNewCategory(payloadJsonMap,
                metrics);
          } else if (action.equals("editCategory")) {
            resultStatus = DatabaseManagers.CATEGORIES_MANAGER.editCategory(payloadJsonMap,
                metrics);
          } else if (action.equals("getCategories")) {
            resultStatus = DatabaseManagers.CATEGORIES_MANAGER
                .getCategories(payloadJsonMap, metrics);
          } else if (action.equals("deleteCategory")) {
            resultStatus = DatabaseManagers.CATEGORIES_MANAGER
                .deleteCategory(payloadJsonMap, metrics);
          } else if (action.equals("warmingEndpoint")) {
            resultStatus = new WarmingManager().warmAllConnections(metrics);

            //squelch metrics on warming -> we only want metrics on user impacting cold starts
            metrics.setPrintMetrics(false);
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
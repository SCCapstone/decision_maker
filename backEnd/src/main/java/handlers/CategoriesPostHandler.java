package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import imports.DatabaseManagers;
import utilities.GetActiveUser;
import utilities.JsonParsers;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import java.util.Map;

public class CategoriesPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
    Metrics metrics = new Metrics(context.getAwsRequestId());
    LambdaLogger lambdaLogger = context.getLogger();

    try {
      lambdaLogger.log("From lambda logger!");
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

              if (action.equals("newCategory")) {
                resultStatus = DatabaseManagers.CATEGORIES_MANAGER.addNewCategory(payloadJsonMap);
              } else if (action.equals("editCategory")) {
                resultStatus = DatabaseManagers.CATEGORIES_MANAGER.editCategory(payloadJsonMap);
              } else if (action.equals("getCategories")) {
                resultStatus = DatabaseManagers.CATEGORIES_MANAGER
                    .getCategories(payloadJsonMap, metrics, lambdaLogger);
              } else if (action.equals("deleteCategory")) {
                resultStatus = DatabaseManagers.CATEGORIES_MANAGER.deleteCategory(payloadJsonMap);
              } else {
                resultStatus.resultMessage = "Error: Invalid action entered";
              }
            } else {
              resultStatus.resultMessage = "Error: Invalid key in payload.";
            }
          } catch (Exception e) {
            resultStatus.resultMessage = "Error: Unable to parse request.";
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

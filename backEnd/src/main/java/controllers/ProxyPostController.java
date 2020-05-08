package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import managers.DbAccessManager;
import modules.PocketPollModule;
import utilities.ErrorDescriptor;
import utilities.GetActiveUser;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class ProxyPostController implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final Map<String, Class<? extends ApiRequestController>> actionsToHandlers = Maps
      .newHashMap(ImmutableMap.<String, Class<? extends ApiRequestController>>builder()
          .put("addNewCategory", AddNewCategoryController.class)
//          .put("editCategory", EditCategoryController.class)
//          .put("getCategories", GetCategoriesController.class)
//          .put("deleteCategory", DeleteCategoryController.class)
//          .put("warmingEndpoint", WarmingController.class)
//          .put("updateUserChoiceRatings", UpdateUserChoiceRatingsController.class)
          .build());

  private final Class[] defaultConstructor = new Class[]{}; // the default constructor

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    final String classMethod = "ProxyPostHandler.handleRequest";

    ResultStatus resultStatus;
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());
    metrics.commonSetup(classMethod);

    //Setup the static reference for injection. This might not work because I don't think this is thread safe...
    PocketPollModule.metrics = metrics;

    try {
      String action = request.getPath(); // this should be of the form '/action'
      String[] splitAction = action.split("/"); // remove the prefixed slash

      if (splitAction.length == 2) {
        action = splitAction[1]; // the action is after the '/'

        if (this.actionsToHandlers.containsKey(action)) {
          final Map<String, Object> jsonMap = JsonUtils.parseInput(request.getBody());
          metrics.setRequestBody(jsonMap);

          if (!jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
            //get the active user from the authorization header and put it in the request payload
            jsonMap.put(RequestFields.ACTIVE_USER,
                GetActiveUser.getActiveUserFromRequest(request, context));

            //TODO use DI for this part (inject the db access manager).
            final Class<? extends ApiRequestController> actionHandlerClass = this.actionsToHandlers
                .get(action);
            final Constructor c = actionHandlerClass.getConstructor(this.defaultConstructor);
            final ApiRequestController apiRequestHandler = (ApiRequestController) c.newInstance();
            resultStatus = apiRequestHandler.processApiRequest(jsonMap, metrics);
          } else {
            //bad request body, log warning
            resultStatus = ResultStatus.failure("Error: Bad request body.");
          }
        } else {
          //bad action, log warning
          resultStatus = ResultStatus.failure("Error: Invalid action.");
        }
      } else {
        //bad request, log warning
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

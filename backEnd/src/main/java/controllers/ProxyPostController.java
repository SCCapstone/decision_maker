package controllers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import handlers.AddNewCategoryHandler;
import managers.DbAccessManager;
import handlers.ApiRequestHandler;
import handlers.DeleteCategoryHandler;
import handlers.EditCategoryHandler;
import handlers.GetCategoriesHandler;
import handlers.UpdateUserChoiceRatingsHandler;
import handlers.WarmingHandler;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import utilities.ErrorDescriptor;
import utilities.GetActiveUser;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class ProxyPostController implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final Map<String, Class<? extends ApiRequestHandler>> actionsToHandlers = Maps
      .newHashMap(ImmutableMap.<String, Class<? extends ApiRequestHandler>>builder()
          .put("addNewCategory", AddNewCategoryHandler.class)
          .put("editCategory", EditCategoryHandler.class)
          .put("getCategories", GetCategoriesHandler.class)
          .put("deleteCategory", DeleteCategoryHandler.class)
          .put("warmingEndpoint", WarmingHandler.class)
          .put("updateUserChoiceRatings", UpdateUserChoiceRatingsHandler.class)
          .build());

  private final Class[] defaultConstructor = new Class[]{DbAccessManager.class, Map.class,
      Metrics.class};

  private DbAccessManager dbAccessManager = new DbAccessManager();

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    final String classMethod = "ProxyPostHandler.handleRequest";

    ResultStatus resultStatus;
    final Metrics metrics = new Metrics(context.getAwsRequestId(), context.getLogger());

    try {
      String action = request.getPath(); // this should be of the form '/action'
      String[] splitAction = action.split("/"); // remove the prefixed slash

      if (splitAction.length == 2) {
        action = splitAction[1]; // the action is after the '/'

        if (this.actionsToHandlers.containsKey(action)) {
          final Map<String, Object> jsonMap = JsonUtils.parseInput(request.getBody());

          if (!jsonMap.containsKey(RequestFields.ACTIVE_USER)) {
            //get the active user from the authorization header and put it in the request payload
            jsonMap.put(RequestFields.ACTIVE_USER,
                GetActiveUser.getActiveUserFromRequest(request, context));

            //TODO use DI for this part (inject the db access manager).
            Class<? extends ApiRequestHandler> actionHandlerClass = this.actionsToHandlers
                .get(action);
            Constructor c = actionHandlerClass.getConstructor(this.defaultConstructor);
            ApiRequestHandler apiRequestHandler = (ApiRequestHandler) c
                .newInstance(this.dbAccessManager, jsonMap, metrics);
            resultStatus = apiRequestHandler.handle();
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
          new HashMap<String, Object>(){{
            put("path", request.getPath());
            put("body", request.getBody());
          }},
          classMethod, e));
      resultStatus = ResultStatus.failure("Error: Exception occurred.");
    }

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}

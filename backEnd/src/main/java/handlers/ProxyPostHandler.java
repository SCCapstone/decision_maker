package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import imports.AddNewCategoryHandler;
import imports.DbAccessManager;
import imports.Handler;
import java.lang.reflect.Constructor;
import java.util.Map;
import utilities.GetActiveUser;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class ProxyPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final Map<String, Class<? extends Handler>> actionsToHandlers = Maps
      .newHashMap(ImmutableMap.<String, Class<? extends Handler>>builder()
          .put("addNewCategory", AddNewCategoryHandler.class)
          .build());

  private final Class[] defaultConstructor = new Class[]{DbAccessManager.class};

  private DbAccessManager dbAccessManager = new DbAccessManager();

  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request,
      Context context) {
    ResultStatus resultStatus = new ResultStatus();
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

            //TODO use DI for this part.
            Class<? extends Handler> actionHandlerClass = this.actionsToHandlers.get(action);
            Constructor c = actionHandlerClass.getConstructor(this.defaultConstructor);
            Handler handler = (Handler) c.newInstance(this.dbAccessManager);
            handler.handle(jsonMap, metrics);
          } else {
            //bad request body, log warning
          }
        } else {
          //bad action, log warning
        }
      } else {
        //bad request, log warning
      }
    } catch (final Exception e) {

    } 

    return new APIGatewayProxyResponseEvent().withBody(resultStatus.toString());
  }
}

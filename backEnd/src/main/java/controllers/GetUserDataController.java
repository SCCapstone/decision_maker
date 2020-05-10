package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.GetUserDataHandler;
import java.util.Map;
import utilities.Metrics;
import utilities.ResultStatus;

public class GetUserDataController implements ApiRequestController {

  @Inject
  public GetUserDataHandler getUserDataHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    return null;
  }
}

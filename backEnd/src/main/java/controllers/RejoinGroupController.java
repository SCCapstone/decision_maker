package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.RejoinGroupHandler;
import java.util.Map;
import javax.inject.Inject;
import utilities.Metrics;
import utilities.ResultStatus;

public class RejoinGroupController implements ApiRequestController {

  @Inject
  public RejoinGroupHandler rejoinGroupHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    return null;
  }
}

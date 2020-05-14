package controllers;

import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import utilities.Metrics;
import utilities.ResultStatus;

public interface ApiRequestController {

  ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException;
}

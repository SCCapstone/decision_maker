package handlers;

import managers.DbAccessManager;
import utilities.Metrics;
import utilities.ResultStatus;

public class RejoinGroupHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  public RejoinGroupHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {

  }
}

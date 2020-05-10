package handlers;

import managers.DbAccessManager;
import utilities.Metrics;
import utilities.ResultStatus;

public class GetUserDataHandler implements ApiRequestHandler {

  private DbAccessManager dbAccessManager;
  private Metrics metrics;

  @Inject
  public GetUserDataHandler(final DbAccessManager dbAccessManager, final Metrics metrics) {
    this.dbAccessManager = dbAccessManager;
    this.metrics = metrics;
  }

  public ResultStatus handle() {

  }
}

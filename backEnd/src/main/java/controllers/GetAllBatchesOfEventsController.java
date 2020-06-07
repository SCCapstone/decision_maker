package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.GetAllBatchesOfEventsHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Group;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class GetAllBatchesOfEventsController implements ApiRequestController {

  @Inject
  public GetAllBatchesOfEventsHandler getAllBatchesOfEventsHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "GetAllBatchesOfEventsController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, RequestFields.BATCH_INDEXES,
            RequestFields.MAX_MATCHES);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final Map<String, Integer> batchIndexes = (Map<String, Integer>) jsonMap
            .get(RequestFields.BATCH_INDEXES);
        final Integer maxBatches = (Integer) jsonMap.get(RequestFields.MAX_MATCHES);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.getAllBatchesOfEventsHandler
            .handle(activeUser, groupId, batchIndexes, maxBatches);
      } catch (final Exception e) {
        metrics.log(new ErrorDescriptor<>(jsonMap, classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}

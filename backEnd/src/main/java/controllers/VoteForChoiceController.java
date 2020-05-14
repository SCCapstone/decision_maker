package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.VoteForChoiceHandler;
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

public class VoteForChoiceController implements ApiRequestController {

  @Inject
  public VoteForChoiceHandler voteForChoiceHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "VoteForChoiceController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(Group.GROUP_ID, RequestFields.EVENT_ID, RequestFields.CHOICE_ID,
            RequestFields.VOTE_VALUE, RequestFields.ACTIVE_USER);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String eventId = (String) jsonMap.get(RequestFields.EVENT_ID);
        final String choiceId = (String) jsonMap.get(RequestFields.CHOICE_ID);
        final Integer voteValue = (Integer) jsonMap.get(RequestFields.VOTE_VALUE);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.voteForChoiceHandler
            .handle(activeUser, groupId, eventId, choiceId, voteValue);
      } catch (final Exception e) {
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}

package controllers;

import exceptions.MissingApiRequestKeyException;
import handlers.NewEventHandler;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import models.Category;
import models.Event;
import models.Group;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;

public class NewEventController implements ApiRequestController {

  @Inject
  public NewEventHandler newEventHandler;

  @Override
  public ResultStatus processApiRequest(final Map<String, Object> jsonMap, final Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "NewEventController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, Event.EVENT_NAME, Category.CATEGORY_ID,
            Event.RSVP_DURATION, Event.EVENT_START_DATE_TIME, Event.VOTING_DURATION,
            Event.UTC_EVENT_START_SECONDS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String eventName = (String) jsonMap.get(Event.EVENT_NAME);
        final String categoryId = (String) jsonMap.get(Category.CATEGORY_ID);
        final Integer rsvpDuration = (Integer) jsonMap.get(Event.RSVP_DURATION);
        final Integer votingDuration = (Integer) jsonMap.get(Event.VOTING_DURATION);
        final String eventStartDateTime = (String) jsonMap.get(Event.EVENT_START_DATE_TIME);
        final Integer utcStartSeconds = (Integer) jsonMap.get(Event.UTC_EVENT_START_SECONDS);

        Injector.getInjector(metrics).inject(this);
        resultStatus = this.newEventHandler
            .handle(activeUser, groupId, eventName, categoryId, rsvpDuration, votingDuration,
                eventStartDateTime, utcStartSeconds);
      } catch (Exception e) {
        metrics.logWithBody(new ErrorDescriptor<>(classMethod, e));
        resultStatus = ResultStatus.failure("Exception in " + classMethod);
      }
    } else {
      throw new MissingApiRequestKeyException(requiredKeys);
    }

    return resultStatus;
  }
}

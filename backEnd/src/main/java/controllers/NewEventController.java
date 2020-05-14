package controllers;

import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.google.common.collect.ImmutableMap;
import exceptions.MissingApiRequestKeyException;
import handlers.DatabaseManagers;
import handlers.NewEventHandler;
import handlers.PendingEventsManager;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import models.Category;
import models.Event;
import models.EventWithCategoryChoices;
import models.Group;
import models.GroupForApiResponse;
import models.User;
import modules.Injector;
import utilities.ErrorDescriptor;
import utilities.JsonUtils;
import utilities.Metrics;
import utilities.RequestFields;
import utilities.ResultStatus;
import utilities.WarningDescriptor;

public class NewEventController implements ApiRequestController {

  @Inject
  public NewEventHandler newEventHandler;

  @Override
  public ResultStatus processApiRequest(Map<String, Object> jsonMap, Metrics metrics)
      throws MissingApiRequestKeyException {
    final String classMethod = "NewEventController.processApiRequest";

    ResultStatus resultStatus;

    final List<String> requiredKeys = Arrays
        .asList(RequestFields.ACTIVE_USER, Group.GROUP_ID, Event.EVENT_NAME, Event.CATEGORY_ID,
            Event.RSVP_DURATION, Event.EVENT_START_DATE_TIME, Event.VOTING_DURATION,
            Event.UTC_EVENT_START_SECONDS);

    if (jsonMap.keySet().containsAll(requiredKeys)) {
      try {
        final String activeUser = (String) jsonMap.get(RequestFields.ACTIVE_USER);
        final String groupId = (String) jsonMap.get(Group.GROUP_ID);
        final String eventName = (String) jsonMap.get(Event.EVENT_NAME);
        final String categoryId = (String) jsonMap.get(Event.CATEGORY_ID);
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

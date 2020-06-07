package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.AttributeValueOutOfRangeException;
import exceptions.InvalidAttributeValueException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class UserForApiResponse extends User {

  public static final String FIRST_LOGIN = "FirstLogin";

  private boolean firstLogin;

  public UserForApiResponse(final User user, final boolean firstLogin)
      throws InvalidAttributeValueException, AttributeValueOutOfRangeException {
    super(user.asMap());
    this.firstLogin = firstLogin;
  }

  public UserForApiResponse(final Item user, final boolean firstLogin)
      throws InvalidAttributeValueException, AttributeValueOutOfRangeException {
    super(user.asMap());
    this.firstLogin = firstLogin;
  }

  public UserForApiResponse(final User user)
      throws InvalidAttributeValueException, AttributeValueOutOfRangeException {
    this(user, false);
  }

  public UserForApiResponse(final Item user)
      throws InvalidAttributeValueException, AttributeValueOutOfRangeException {
    this(user, false);
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    //add the first login key
    modelAsMap.putIfAbsent(FIRST_LOGIN, this.firstLogin);

    //remove the ratings maps all together
    modelAsMap.remove(User.CATEGORY_RATINGS);

    //overwrite the user groups with summaries of said groups
    modelAsMap.put(User.GROUPS, this.getSummarizedGroups());
    return modelAsMap;
  }

  private Map<String, Object> getSummarizedGroups() {
    final Map<String, Object> summarizedGroups = new HashMap<>();
    Map<String, Object> userGroupSummary;
    for (final Map.Entry<String, UserGroup> userGroupEntry : this.getGroups().entrySet()) {
      userGroupSummary = userGroupEntry.getValue().asMap();

      // overwrite the mapping to just be the count
      userGroupSummary.put(User.EVENTS_UNSEEN, userGroupEntry.getValue().getEventsUnseen().size());

      summarizedGroups.put(userGroupEntry.getKey(), userGroupSummary);
    }

    return summarizedGroups;
  }
}

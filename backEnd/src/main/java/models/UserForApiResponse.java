package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import exceptions.InvalidAttributeValueException;
import java.util.Map;
import lombok.Data;

@Data
public class UserForApiResponse extends User {

  public static final String FIRST_LOGIN = "FirstLogin";

  private boolean firstLogin;

  public UserForApiResponse(final User user, final boolean firstLogin)
      throws InvalidAttributeValueException {
    super(user.asMap());
    this.firstLogin = firstLogin;
  }

  public UserForApiResponse(final Item user, final boolean firstLogin)
      throws InvalidAttributeValueException {
    super(user.asMap());
    this.firstLogin = firstLogin;
  }

  public UserForApiResponse(final User user) throws InvalidAttributeValueException {
    this(user, false);
  }

  public UserForApiResponse(final Item user) throws InvalidAttributeValueException {
    this(user, false);
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = super.asMap();
    //add the first login key
    modelAsMap.putIfAbsent(FIRST_LOGIN, this.firstLogin);

    //remove the ratings maps all together
    modelAsMap.remove(User.CATEGORY_RATINGS);

    //TODO remove the events unseen from the user map and add summary (https://github.com/SCCapstone/decision_maker/issues/538)
    return modelAsMap;
  }
}

package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import utilities.ResultStatus;

import java.util.ArrayList;
import java.util.Map;

public class UsersManager extends DatabaseAccessManager {
  public static final String USER_FIELD_USERNAME = "Username";
  public static final String USER_FIELD_FIRSTNAME = "FirstName";
  public static final String USER_FIELD_LASTNAME = "LastName";
  public static final String USER_FIELD_DARK_THEME = "AppSetting_DarkTheme";
  public static final String USER_FIELD_MUTED = "AppSetting_Muted";
  public static final String USER_FIELD_CATEGORIES = "Categories";

  public static final String REQUEST_FIELD_CATEGORYID = "CategoryId";
  public static final String REQUEST_FIELD_RATINGS = "Ratings";

  public static final String DEFAULT_FIRSTNAME = "DefFirstName";
  public static final String DEFAULT_LASTNAME = "DefLastName";
  public static final boolean DEFAULT_DARK_THEME = false;
  public static final boolean DEFAULT_MUTED = false;

  public static final String CATEGORY_FIELD = "Categories";

  public UsersManager() {
    super("users", "Username", Regions.US_EAST_2);
  }

  public ArrayList<String> getAllCategoryIds(String username) {
    Item dbData = super
        .getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), username));
    Map<String, Object> dbDataMap = dbData.asMap(); // specific user record as a map
    Map<String, String> categoryMap = (Map<String, String>) dbDataMap.get(CATEGORY_FIELD);
    ArrayList<String> ids = new ArrayList<String>(categoryMap.keySet());
    return ids;
  }

  public boolean checkUser(String userName) {
    Item newItem;
    try{
      newItem = super.getItem(new GetItemSpec().withPrimaryKey(super.getPrimaryKeyIndex(), userName));
      if (newItem == null) {
        return true;
      }
      return false;
    } catch(ResourceNotFoundException e) {
      return false;
    }
  }

  public ResultStatus addNewUser(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    if (jsonMap.containsKey(USER_FIELD_USERNAME)) {
      try {
        String userName = (String) jsonMap.get(USER_FIELD_USERNAME);

        if(this.checkUser(userName)) {
          Item newUser = new Item()
            .withString(USER_FIELD_USERNAME, userName)
            .withString(USER_FIELD_FIRSTNAME,DEFAULT_FIRSTNAME)
            .withString(USER_FIELD_LASTNAME,DEFAULT_LASTNAME)
            .withBoolean(USER_FIELD_DARK_THEME,DEFAULT_DARK_THEME)
            .withBoolean(USER_FIELD_MUTED,DEFAULT_MUTED);

          PutItemSpec putItemSpec = new PutItemSpec()
            .withItem(newUser);

          super.putItem(putItemSpec);

          resultStatus = new ResultStatus(true, "User added successfully!");
        } else {
          resultStatus.resultMessage = "Error: Username already exists in database";
        }
      } catch (Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: "+e;
      }
    } else {
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }
  
  public ResultStatus updateUserChoiceRatings(Map<String, Object> jsonMap) {
    ResultStatus resultStatus = new ResultStatus();
    
    if (
        jsonMap.containsKey(USER_FIELD_USERNAME) &&
        jsonMap.containsKey(REQUEST_FIELD_CATEGORYID) &&
        jsonMap.containsKey(REQUEST_FIELD_RATINGS)
    ) {
      try {
        String categoryId = (String) jsonMap.get(REQUEST_FIELD_CATEGORYID);
        Map<String,Object> ratings = (Map<String,Object>) jsonMap.get(REQUEST_FIELD_RATINGS);
        String user = (String) jsonMap.get(USER_FIELD_USERNAME);
        
        int nextRatingNo = -1;
        
        for(String ratingNo : ratings.keySet()) {
          if(Integer.parseInt(ratingNo) > nextRatingNo) {
            nextRatingNo = Integer.parseInt(ratingNo);
          }
        }
        
        nextRatingNo++;
        
        String updateExpression = "set " + USER_FIELD_CATEGORIES + ".ID_"+categoryId+ " = :map";
        ValueMap valueMap = new ValueMap().withMap(":map", ratings);
        
        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
          .withPrimaryKey(super.getPrimaryKeyIndex(), user)
          .withUpdateExpression(updateExpression)
          .withValueMap(valueMap);
        
        super.updateItem(updateItemSpec);
        
        resultStatus = new ResultStatus(true, "User ratings updated successfully!");
      }
      catch(Exception e) {
        //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
        resultStatus.resultMessage = "Error: Unable to parse request. Exception message: "+e;
      }
    } else {
      //TODO add log message https://github.com/SCCapstone/decision_maker/issues/82
      resultStatus.resultMessage = "Error: Required request keys not found.";
    }

    return resultStatus;
  }
}

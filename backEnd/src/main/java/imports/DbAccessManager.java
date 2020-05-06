package imports;

import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import java.util.Map;
import models.Category;
import models.User;

public class DbAccessManager {
  public void putUser(final User user) {
    //TODO
  }

  public User getUser(final String username) {
    return null; //TODO
  }

  public void updateUser(final String username, final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey("asdf", username);
    //TODO
  }

  public void putCategory(final Category category) {
    //TODO implement db call
  }

  public Category getCategory(final String categoryId) {
    return null; //TODO
  }

  public Map<String, Object> getCategoryMap(final String categoryId) {
    return null; //TODO
  }

  public void updateCategory(final String categoryId, final UpdateItemSpec updateItemSpec) {
    //TODO
  }

  public void deleteCategory(final String categoryId) {
    final DeleteItemSpec deleteItemSpec = new DeleteItemSpec().withPrimaryKey("asdF", categoryId);
    //TODO
  }

  public void describeTables() {
    //TODO
  }
}

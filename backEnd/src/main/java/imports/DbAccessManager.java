package imports;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import exceptions.InvalidAttributeValueException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.Category;
import models.Group;
import models.User;

public class DbAccessManager {

  private final Table groupsTable;
  private final Table usersTable;
  private final Table categoriesTable;
  private final Table pendingEventsTable;
  private final String groupsPrimaryKeyIndex;
  private final String usersPrimaryKeyIndex;
  private final String categoriesPrimaryKeyIndex;
  private final String pendingEventsPrimaryKeyIndex;
  private final Regions region;
  private final AmazonDynamoDBClient client;
  private final DynamoDB dynamoDb;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss");

  public DbAccessManager() {
    this.region = Regions.US_EAST_2;
    this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
        .withRegion(this.region)
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
    this.dynamoDb = new DynamoDB(this.client);

    this.groupsTable = this.dynamoDb.getTable("groups");
    this.groupsPrimaryKeyIndex = Group.GROUP_ID;

    this.usersTable = this.dynamoDb.getTable("users");
    this.usersPrimaryKeyIndex = User.USERNAME;

    this.categoriesTable = this.dynamoDb.getTable("categories");
    this.categoriesPrimaryKeyIndex = Category.CATEGORY_ID;

    this.pendingEventsTable = this.dynamoDb.getTable("pending_events");
    this.pendingEventsPrimaryKeyIndex = "ScannerId";
  }

  //Users table methods
  public PutItemOutcome putUser(final Item user) {
    return this.usersTable.putItem(user);
  }

  public User getUser(final String username)
      throws NullPointerException, InvalidAttributeValueException {
    return new User(this.usersTable.getItem(new PrimaryKey(this.usersPrimaryKeyIndex, username)));
  }

  public UpdateItemOutcome updateUser(final String username, final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey("asdf", username);
    return this.usersTable.updateItem(updateItemSpec);
  }

  //Categories table methods
  public PutItemOutcome putCategory(final Category category) {
    return this.categoriesTable.putItem(category.asItem());
  }

  public Category getCategory(final String categoryId) throws NullPointerException {
    return new Category(
        this.categoriesTable.getItem(new PrimaryKey(this.categoriesPrimaryKeyIndex, categoryId)));
  }

  public Map<String, Object> getCategoryMap(final String categoryId) {
    return this.getCategory(categoryId).asMap();
  }

  public UpdateItemOutcome updateCategory(final String categoryId,
      final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(this.categoriesPrimaryKeyIndex, categoryId);
    return this.categoriesTable.updateItem(updateItemSpec);
  }

  public DeleteItemOutcome deleteCategory(final String categoryId) {
    return this.categoriesTable
        .deleteItem(new PrimaryKey(this.categoriesPrimaryKeyIndex, categoryId));
  }

  //Groups table methods
  public PutItemOutcome putGroup(final Group group) {
    return this.groupsTable.putItem(group.asItem());
  }

  public Group getGroup(final String groupId) {
    return new Group(this.groupsTable.getItem(new PrimaryKey(this.groupsPrimaryKeyIndex, groupId)));
  }

  public UpdateItemOutcome updateGroup(final String groupId,
      final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(this.groupsPrimaryKeyIndex, groupId);
    return this.groupsTable.updateItem(updateItemSpec);
  }

  public DeleteItemOutcome deleteGroup(final String groupId) {
    return this.groupsTable.deleteItem(new PrimaryKey(this.groupsPrimaryKeyIndex, groupId));
  }

  //for warming
  public List<TableDescription> describeTables() {
    final ArrayList<TableDescription> descriptions = new ArrayList<>();
    descriptions.add(this.groupsTable.describe());
    descriptions.add(this.usersTable.describe());
    descriptions.add(this.categoriesTable.describe());
    return descriptions;
  }
}

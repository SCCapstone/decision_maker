package managers;

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
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import exceptions.InvalidAttributeValueException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.Category;
import models.Group;
import models.User;
import utilities.UpdateItemData;

public class DbAccessManager {

  public static final String USERS_TABLE_NAME = "users";
  public static final String GROUPS_TABLE_NAME = "groups";
  public static final String CATEGORIES_TABLE_NAME = "categories";
  public static final String PENDING_EVENTS_TABLE_NAME = "pending_events";

  public static final String CATEGORIES_PRIMARY_KEY = Category.CATEGORY_ID;
  public static final String GROUPS_PRIMARY_KEY = Group.GROUP_ID;
  public static final String USERS_PRIMARY_KEY = User.USERNAME;
  public static final String PENDING_EVENTS_PRIMARY_KEY = "ScannerId";

  public static final String NUMBER_OF_PARTITIONS_ENV_KEY = "NUMBER_OF_PARTITIONS";
  public static final String DELIM = ";";

  private final Table groupsTable;
  private final Table usersTable;
  private final Table categoriesTable;
  private final Table pendingEventsTable;

  private final Regions region;
  private final AmazonDynamoDBClient client;
  private final DynamoDB dynamoDb;
  private final DateTimeFormatter dateTimeFormatter;

  public DbAccessManager() {
    this.region = Regions.US_EAST_2;
    this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
        .withRegion(this.region)
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
    this.dynamoDb = new DynamoDB(this.client);

    this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    this.groupsTable = this.dynamoDb.getTable(GROUPS_TABLE_NAME);
    this.usersTable = this.dynamoDb.getTable(USERS_TABLE_NAME);
    this.categoriesTable = this.dynamoDb.getTable(CATEGORIES_TABLE_NAME);
    this.pendingEventsTable = this.dynamoDb.getTable(PENDING_EVENTS_TABLE_NAME);
  }

  public String now() {
    return LocalDateTime.now(ZoneId.of("UTC")).format(this.dateTimeFormatter);
  }

  //Users table methods
  public PutItemOutcome putUser(final Item user) {
    return this.usersTable.putItem(user);
  }

  public User getUser(final String username)
      throws NullPointerException, InvalidAttributeValueException {
    return new User(this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username)));
  }

  public Item getUserItem(final String username) throws NullPointerException {
    return this.usersTable.getItem(new PrimaryKey(USERS_PRIMARY_KEY, username));
  }

  public UpdateItemOutcome updateUser(final String username, final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(USERS_PRIMARY_KEY, username);
    return this.usersTable.updateItem(updateItemSpec);
  }

  public UpdateItemOutcome updateUser(final UpdateItemData updateItemData) throws Exception {
    return this.usersTable.updateItem(updateItemData.asUpdateItemSpec());
  }

  //Categories table methods
  public PutItemOutcome putCategory(final Category category) {
    return this.categoriesTable.putItem(category.asItem());
  }

  public Category getCategory(final String categoryId) throws NullPointerException {
    return new Category(
        this.categoriesTable.getItem(new PrimaryKey(CATEGORIES_PRIMARY_KEY, categoryId)));
  }

  public Map<String, Object> getCategoryMap(final String categoryId) {
    return this.getCategory(categoryId).asMap();
  }

  public UpdateItemOutcome updateCategory(final String categoryId,
      final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(CATEGORIES_PRIMARY_KEY, categoryId);
    return this.categoriesTable.updateItem(updateItemSpec);
  }

  public UpdateItemOutcome updateCategory(final UpdateItemData updateItemData) throws Exception {
    return this.categoriesTable.updateItem(updateItemData.asUpdateItemSpec());
  }

  public DeleteItemOutcome deleteCategory(final String categoryId) {
    return this.categoriesTable
        .deleteItem(new PrimaryKey(CATEGORIES_PRIMARY_KEY, categoryId));
  }

  //Groups table methods
  public PutItemOutcome putGroup(final Group group) {
    return this.groupsTable.putItem(group.asItem());
  }

  public Group getGroup(final String groupId) {
    return new Group(this.groupsTable.getItem(new PrimaryKey(GROUPS_PRIMARY_KEY, groupId)));
  }

  public UpdateItemOutcome updateGroup(final String groupId,
      final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(GROUPS_PRIMARY_KEY, groupId);
    return this.groupsTable.updateItem(updateItemSpec);
  }

  public UpdateItemOutcome updateGroup(final UpdateItemData updateItemData) throws Exception {
    return this.groupsTable.updateItem(updateItemData.asUpdateItemSpec());
  }

  public DeleteItemOutcome deleteGroup(final String groupId) {
    return this.groupsTable.deleteItem(new PrimaryKey(GROUPS_PRIMARY_KEY, groupId));
  }

  //Pending events table methods
  public Item getPendingEvents(final String scannerId) {
    return this.pendingEventsTable.getItem(new PrimaryKey(PENDING_EVENTS_PRIMARY_KEY, scannerId));
  }

  public UpdateItemOutcome updatePendingEvent(final String scannerId,
      final UpdateItemSpec updateItemSpec) {
    updateItemSpec.withPrimaryKey(PENDING_EVENTS_PRIMARY_KEY, scannerId);
    return this.pendingEventsTable.updateItem(updateItemSpec);
  }

  public UpdateItemOutcome updatePendingEvent(final UpdateItemData updateItemData)
      throws Exception {
    return this.pendingEventsTable.updateItem(updateItemData.asUpdateItemSpec());
  }

  //for warming
  public List<TableDescription> describeTables() {
    final ArrayList<TableDescription> descriptions = new ArrayList<>();
    descriptions.add(this.groupsTable.describe());
    descriptions.add(this.usersTable.describe());
    descriptions.add(this.categoriesTable.describe());
    return descriptions;
  }

  //transactions
  public TransactWriteItemsResult executeWriteTransaction(final List<TransactWriteItem> actions) {
    final TransactWriteItemsRequest transactWriteItemsRequest = new TransactWriteItemsRequest()
        .withTransactItems(actions);
    return this.client.transactWriteItems(transactWriteItemsRequest);
  }

  public TransactGetItemsResult executeGetTransaction(
      final TransactGetItemsRequest transactGetItemsRequest) {
    return this.client.transactGetItems(transactGetItemsRequest);
  }

  public static String getKeyIndex(final String tableName) throws Exception {
    if (tableName.equals(DbAccessManager.CATEGORIES_TABLE_NAME)) {
      return CATEGORIES_PRIMARY_KEY;
    } else if (tableName.equals(DbAccessManager.USERS_TABLE_NAME)) {
      return USERS_PRIMARY_KEY;
    } else if (tableName.equals(DbAccessManager.GROUPS_TABLE_NAME)) {
      return GROUPS_PRIMARY_KEY;
    } else {
      throw new Exception("Invalid table name: " + tableName);
    }
  }
}

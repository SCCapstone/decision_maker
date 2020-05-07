package handlers;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import models.Model;

public class DatabaseAccessManager {

  private final DynamoDB dynamoDb;
  private final String tableName;
  private final String primaryKeyIndex;
  private final Regions region;
  private final AmazonDynamoDBClient client;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss");

  public DatabaseAccessManager(final String tableName, final String primaryKeyIndex,
      final Regions regions) {
    this.tableName = tableName;
    this.primaryKeyIndex = primaryKeyIndex;
    this.region = regions;
    this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
        .withRegion(this.region)
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
    this.dynamoDb = new DynamoDB(this.client);
  }

  public DatabaseAccessManager(final String tableName, final String primaryKeyIndex,
      final Regions regions, final DynamoDB dynamoDB) {
    this.tableName = tableName;
    this.primaryKeyIndex = primaryKeyIndex;
    this.region = regions;
    this.client = (AmazonDynamoDBClient) AmazonDynamoDBClient.builder()
        .withRegion(this.region)
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .build();
    this.dynamoDb = dynamoDB;
  }

  public String getPrimaryKeyIndex() {
    return this.primaryKeyIndex;
  }

  public DateTimeFormatter getDateTimeFormatter() {
    return this.dateTimeFormatter;
  }


  public Item getItemByPrimaryKey(final String primaryKey) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName)
        .getItem(new GetItemSpec().withPrimaryKey(this.primaryKeyIndex, primaryKey));
  }

  public Map<String, Object> getMapByPrimaryKey(final String primaryKey)
      throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName)
        .getItem(new GetItemSpec().withPrimaryKey(this.primaryKeyIndex, primaryKey)).asMap();
  }

  public UpdateItemOutcome updateItem(final UpdateItemSpec updateItemSpec)
      throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).updateItem(updateItemSpec);
  }

  public UpdateItemOutcome updateItem(final String key, final UpdateItemSpec updateItemSpec)
      throws NullPointerException {
    updateItemSpec.withPrimaryKey(this.primaryKeyIndex, key);
    return this.dynamoDb.getTable(this.tableName).updateItem(updateItemSpec);
  }

  public PutItemOutcome putItem(final Item item) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).putItem(new PutItemSpec().withItem(item));
  }

  public PutItemOutcome putItem(final Model model) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName)
        .putItem(new PutItemSpec().withItem(model.asItem()));
  }

  public DeleteItemOutcome deleteItem(final DeleteItemSpec deleteItemSpec)
      throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).deleteItem(deleteItemSpec);
  }

  public DeleteItemOutcome deleteItem(final String key)
      throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName)
        .deleteItem(new DeleteItemSpec().withPrimaryKey(this.primaryKeyIndex, key));
  }

  //Commenting these until they are used. Will be imperative for adding transactions in the future
//  public TransactWriteItemsResult executeWriteTransaction(
//      final TransactWriteItemsRequest transactWriteItemsRequest) {
//    return this.client.transactWriteItems(transactWriteItemsRequest);
//  }
//
//  public TransactGetItemsResult executeGetTransaction(
//      final TransactGetItemsRequest transactGetItemsRequest) {
//    return this.client.transactGetItems(transactGetItemsRequest);
//  }

  public TableDescription describeTable() {
    return this.dynamoDb.getTable(this.tableName).describe();
  }
}
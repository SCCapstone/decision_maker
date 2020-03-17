package imports;

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
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.format.DateTimeFormatter;

public class DatabaseAccessManager {

  private final DynamoDB dynamoDb;
  private final String tableName;
  private final String primaryKeyIndex;
  private final Regions region;
  private final AmazonDynamoDBClient client;
  private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

  public String getTableName() {
    return this.tableName;
  }

  public String getPrimaryKeyIndex() {
    return this.primaryKeyIndex;
  }

  public DateTimeFormatter getDateTimeFormatter() {
    return this.dateTimeFormatter;
  }

  public Item getItem(final GetItemSpec getItemSpec) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).getItem(getItemSpec);
  }

  public Item getItemByPrimaryKey(final String primaryKey) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName)
        .getItem(new GetItemSpec().withPrimaryKey(this.primaryKeyIndex, primaryKey));
  }

  public UpdateItemOutcome updateItem(final UpdateItemSpec updateItemSpec) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).updateItem(updateItemSpec);
  }

  public PutItemOutcome putItem(final PutItemSpec putItemSpec) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).putItem(putItemSpec);
  }

  public PutItemOutcome putItem(final Item item) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).putItem(new PutItemSpec().withItem(item));
  }

  public DeleteItemOutcome deleteItem(final DeleteItemSpec deleteItemSpec) throws NullPointerException {
    return this.dynamoDb.getTable(this.tableName).deleteItem(deleteItemSpec);
  }

  public TransactWriteItemsResult executeWriteTransaction(
      final TransactWriteItemsRequest transactWriteItemsRequest) {
    return this.client.transactWriteItems(transactWriteItemsRequest);
  }

  public TransactGetItemsResult executeGetTransaction(
      final TransactGetItemsRequest transactGetItemsRequest) {
    return this.client.transactGetItems(transactGetItemsRequest);
  }
}

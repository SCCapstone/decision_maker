package imports;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;

public class DatabaseAccessManager {
  private final DynamoDB dynamoDb;
  private final String tableName;
  private final String primaryKeyIndex;
  private final Regions region;

  public DatabaseAccessManager(String tableName, String primaryKeyIndex, Regions regions) {
    this.tableName = tableName;
    this.primaryKeyIndex = primaryKeyIndex;
    this.region = regions;

    AmazonDynamoDBClient amazonDynamoDBClient =
        (AmazonDynamoDBClient) AmazonDynamoDBClient.builder().withRegion(this.region).build();
    this.dynamoDb = new DynamoDB(amazonDynamoDBClient);
  }

  public String getTableName() {
    return this.tableName;
  }

  public String getPrimaryKeyIndex() {
    return this.primaryKeyIndex;
  }

  public Item getItem(GetItemSpec getItemSpec) {
    return this.dynamoDb.getTable(this.tableName).getItem(getItemSpec);
  }

  public UpdateItemOutcome updateItem(UpdateItemSpec updateItemSpec) {
    return this.dynamoDb.getTable(this.tableName).updateItem(updateItemSpec);
  }

  public PutItemOutcome putItem(PutItemSpec putItemSpec) {
    return this.dynamoDb.getTable(this.tableName).putItem(putItemSpec);
  }
}

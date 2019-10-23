import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.apache.commons.codec.Charsets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class PairsGetter implements RequestStreamHandler {
  private DynamoDB dynamoDb;
  private static final String DYNAMODB_TABLE_NAME = "testing_tables";
  private static final Regions REGION = Regions.US_EAST_2;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    try {
      // get the DynamoDB table connection
      this.initDynamoDbClient();

      Item dbData = this.getTableData();
      Map<String, Object> dbDataMap = dbData.asMap();

      // this will be a json string representing an array of objects
      String outputString = "[";

      for (String s: dbDataMap.keySet()) {
        outputString += "{\"key\": \"" + s + "\", \"value\": \"" + dbDataMap.get(s).toString() + "\"},";
      }

      outputString = outputString.substring(0, outputString.length() - 1) + "]";

      this.writeToOutput(outputStream,outputString);
    } catch (Exception e) {
      this.writeToOutput(outputStream,"Unable to handle request.\n");
    }
  }

  private Item getTableData() {
    GetItemSpec getItemSpec = new GetItemSpec()
        .withPrimaryKey("DevName", "John");

    return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME).getItem(getItemSpec);
  }

  private void initDynamoDbClient() {
    AmazonDynamoDBClient amazonDynamoDBClient =
        (AmazonDynamoDBClient) AmazonDynamoDBClient.builder().withRegion(REGION).build();
    this.dynamoDb = new DynamoDB(amazonDynamoDBClient);
  }

  private void writeToOutput(OutputStream outputStream, String message) throws IOException {
    outputStream.write(message.getBytes(Charsets.UTF_8));
  }
}

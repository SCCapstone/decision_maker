
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.Charsets;

public class PairUploader implements RequestStreamHandler {
  //public static final String DYNAMO_ARN = "arn:aws:dynamodb:us-east-2:871532548613:table/testing_tables";

  private DynamoDB dynamoDb;
  private static final String DYNAMODB_TABLE_NAME = "testing_tables";
  private static final Regions REGION = Regions.US_EAST_2;

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    try {
      // prepare the input
      ObjectMapper objectMapper = new ObjectMapper();
      Map<String, Object> jsonMap = objectMapper.readValue(inputStream,
          new TypeReference<Map<String, Object>>() {
          });

      // get the DynamoDB table connection
      this.initDynamoDbClient();

      // upload the data
      if (!jsonMap.isEmpty()) {
        this.insertData(jsonMap);
      } else {
        writeToOutput(outputStream,"No data entered.");
      }

      this.writeToOutput(outputStream,"Data inserted successfully!");
    } catch (Exception e) {
      this.writeToOutput(outputStream,"Unable to handle request.\n");
//      dataOutputStream.writeUTF(e.toString() + "\n");
//      StackTraceElement[] stackTraceElements = e.getStackTrace();
//      StringBuilder stringBuilder = new StringBuilder();
//      for (int i = 0; i < stackTraceElements.length; i++) {
//        stringBuilder.append(stackTraceElements[i].toString() + "\n");
//      }
//      dataOutputStream.writeUTF(stringBuilder.toString());
    }
  }

  private UpdateItemOutcome insertData(Map<String, Object> inputMap) throws ConditionalCheckFailedException {

    String updateExpression = "set ";
    ValueMap valueMap = new ValueMap();
    char a = 'a';

    for (String s: inputMap.keySet()) {
      updateExpression += s + " = :" + a + ",";
      valueMap.with(":" + a, inputMap.get(s));
      a++;
    }

    updateExpression = updateExpression.substring(0, updateExpression.length() - 1);

    UpdateItemSpec updateItemSpec = new UpdateItemSpec()
        .withPrimaryKey("DevName", "John")
        .withUpdateExpression(updateExpression)
        .withValueMap(valueMap); // only modifying my row

    return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME).updateItem(updateItemSpec);
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
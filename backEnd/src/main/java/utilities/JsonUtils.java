package utilities;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonUtils {
  public static Item getItemFromFile(final String fileName) throws IOException {
    File dataFile = new File("src/test/json/" + fileName);
    InputStream dataStream = new FileInputStream(dataFile);
    return Item.fromMap(JsonUtils.parseInput(dataStream));
  }

  public static Item getItemFromFilePath(final String filePath) throws IOException {
    File dataFile = new File(filePath);
    InputStream dataStream = new FileInputStream(dataFile);
    return Item.fromMap(JsonUtils.parseInput(dataStream));
  }

  //////////////endregion
  // decoding //
  //////////////region

  public static Map<String, Object> parseInput(InputStream inputStream) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {
    });
  }

  public static Map<String, Object> parseInput(String jsonString) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
    });
  }

  //////////////endregion
  // encoding //
  //////////////region

  public static String convertObjectToJson(Object value) {
    StringBuilder outputString = new StringBuilder();

    if (value instanceof Map) {
      //we should potentially get the first key and then see what it's value is and map that,
      //but I think for now it's safe to assume it is a string or something can be made a string
      outputString.append(JsonUtils.convertMapToJson((Map) value));
    } else if (value instanceof String) {
      outputString.append(JsonUtils.convertStringToJson((String) value));
    } else if (value instanceof Iterable) {
      outputString.append(JsonUtils.convertIterableToJson((Iterable) value));
    } else if (value instanceof Number || value instanceof Boolean) {
      outputString.append(value.toString());
    } else {
      outputString.append("null"); // assuming null pointer
    }

    return outputString.toString();
  }

  public static String convertIterableToJson(Iterable value) {
    StringBuilder outputString = new StringBuilder();

    outputString.append("[");
    for (Object data : value) {
      outputString.append(JsonUtils.convertObjectToJson(data));
      outputString.append(",");
    }

    IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
    outputString.append("]");

    return outputString.toString();
  }

  public static String convertMapToJson(Map<String, Object> value) {
    StringBuilder outputString = new StringBuilder();

    outputString.append("{");
    for (String key : value.keySet()) {
      Object data = value.get(key);

      outputString.append(JsonUtils.convertStringToJson(key));
      outputString.append(":");
      outputString.append(JsonUtils.convertObjectToJson(data));
      outputString.append(",");
    }

    IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
    outputString.append("}");

    return outputString.toString();
  }

  public static String convertStringToJson(String value) {
    return "\\\"" + value + "\\\"";
  }

  //endregion
}

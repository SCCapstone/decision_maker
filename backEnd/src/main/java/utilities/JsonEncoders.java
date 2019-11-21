package utilities;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.List;
import java.util.Map;

public class JsonEncoders {

  public static String convertObjectToJson(Object value) {
    StringBuilder outputString = new StringBuilder();

    if (value instanceof Map) {
      //we should potentially get the first key and then see what it's value is and map that,
      //but I think for now it's safe to assume it is a string or something can be made a string
      outputString.append(JsonEncoders.convertMapToJson((Map) value));
    } else if (value instanceof String) {
      outputString.append(JsonEncoders.convertStringToJson((String) value));
    } else if (value instanceof List) {
      outputString.append(JsonEncoders.convertListToJson((List) value));
    } else if (value instanceof Number) {
      outputString.append(JsonEncoders.convertStringToJson(value.toString()));
    } else {
      outputString.append("null"); // assuming null pointer
    }

    return outputString.toString();
  }

  public static String convertListToJson(List value) {
    StringBuilder outputString = new StringBuilder();

    outputString.append("[");
    for (Object data : value) {
      outputString.append(JsonEncoders.convertObjectToJson(data));
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

      outputString.append(JsonEncoders.convertStringToJson(key));
      outputString.append(":");
      outputString.append(JsonEncoders.convertObjectToJson(data));
      outputString.append(",");
    }

    IOStreamsHelper.removeLastInstanceOf(outputString, ','); // remove the last comma
    outputString.append("}");

    return outputString.toString();
  }

  public static String convertStringToJson(String value) {
    return "\"" + value + "\"";
  }
}

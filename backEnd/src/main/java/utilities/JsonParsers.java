package utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonParsers {
  public static Map<String, Object> parseInput(InputStream inputStream) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
  }

  public static Map<String, Object> parseInput(String jsonString) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
  }
}

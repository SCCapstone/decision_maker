package utilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.Charsets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class IOStreamsHelper {


  public static void writeToOutput(OutputStream outputStream, String message) throws IOException {
    outputStream.write(message.getBytes(Charsets.UTF_8));
  }

  public static Map<String, Object> parseInput(InputStream inputStream) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
  }
}

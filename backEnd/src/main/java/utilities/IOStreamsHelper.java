package utilities;

import org.apache.commons.codec.Charsets;

import java.io.IOException;
import java.io.OutputStream;

public class IOStreamsHelper {
  public static void writeToOutput(OutputStream outputStream, String message) throws IOException {
    outputStream.write(message.getBytes(Charsets.UTF_8));
  }
}

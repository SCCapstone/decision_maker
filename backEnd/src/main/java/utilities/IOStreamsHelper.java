package utilities;

import org.apache.commons.codec.Charsets;

import java.io.IOException;
import java.io.OutputStream;

public class IOStreamsHelper {

  public static void writeToOutput(OutputStream outputStream, String message) throws IOException {
    outputStream.write(message.getBytes(Charsets.UTF_8));
  }

  //Caution: this actually modifies the reference so a return isn't necessary but keeping for clarity
  public static void removeLastInstanceOf(StringBuilder input, char toRemove) {
    String inputString = input.toString();
    int index = inputString.lastIndexOf(toRemove);

    if (index >= 0) {
      input.deleteCharAt(index);
    }
  }
}

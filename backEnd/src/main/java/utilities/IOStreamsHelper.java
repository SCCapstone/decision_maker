package utilities;

import java.util.List;
import java.util.Map;
import org.apache.commons.codec.Charsets;

import java.io.IOException;
import java.io.OutputStream;

public class IOStreamsHelper {

  public static void writeToOutput(final OutputStream outputStream, final String message)
      throws IOException {
    outputStream.write(message.getBytes(Charsets.UTF_8));
  }

  //Caution: this actually modifies the reference so a return isn't necessary but keeping for clarity
  public static void removeLastInstanceOf(final StringBuilder input, final char toRemove) {
    if (input.lastIndexOf(toRemove + "") >= 0) {
      input.deleteCharAt(input.lastIndexOf(toRemove + ""));
    }
  }

  public static void removeAllInstancesOf(final StringBuilder input, final char toRemove) {
    while (input.lastIndexOf(toRemove + "") != -1) {
      input.deleteCharAt(input.lastIndexOf(toRemove + ""));
    }
  }

  public static boolean allKeysContained(final Map<String, Object> inputMap,
      final List<String> keys) {
    boolean allKeysContained = true;
    for (String k : keys) {
      allKeysContained = allKeysContained && inputMap.containsKey(k);
    }

    return allKeysContained;
  }
}

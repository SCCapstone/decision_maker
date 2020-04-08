package utilities;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {
  public static Item getItemFromFile(final String filePath) throws IOException {
    File dataFile = new File(filePath);
    InputStream dataStream = new FileInputStream(dataFile);
    return Item.fromMap(JsonParsers.parseInput(dataStream));
  }
}

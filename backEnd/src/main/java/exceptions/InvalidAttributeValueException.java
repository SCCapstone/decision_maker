package exceptions;

import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InvalidAttributeValueException extends Exception {

  private String attributeName;
  private List validAttributeValues;
  private Object attemptedValue;

  @Override
  public String getMessage() {
    return String
        .format("Invalid attribute value for %s\nValue entered was: %s\nValid values are: %s",
            this.attributeName,
            this.attemptedValue.toString(),
            this.validAttributeValues.toString());
  }
}

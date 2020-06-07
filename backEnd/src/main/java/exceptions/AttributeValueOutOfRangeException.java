package exceptions;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AttributeValueOutOfRangeException extends Exception {

  private String attributeName;
  private Object lowValue;
  private Object highValue;
  private Object attemptedValue;

  @Override
  public String getMessage() {
    return String.format(
        "Attribute value out of range for %s\nValue entered was: %s\nValid values range is: [%s - %s]",
        this.attributeName, this.attemptedValue.toString(), this.lowValue.toString(),
        this.highValue.toString());
  }
}

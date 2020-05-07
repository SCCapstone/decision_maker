package exceptions;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MissingApiRequestKeyException extends Exception {

  private final List<String> requiredKeys;

  @Override
  public String getMessage() {
    return "Missing request key. The following keys are required for this action: "
        + this.requiredKeys.toString();
  }
}

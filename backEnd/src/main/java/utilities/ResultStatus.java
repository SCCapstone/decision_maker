package utilities;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ResultStatus<T> {

  public boolean success;
  public T data;
  public String resultMessage;

  public ResultStatus() {
    this.success = false;
    this.data = null;
    this.resultMessage = "Error.";
  }

  public ResultStatus(boolean success, String resultMessage) {
    this.success = success;
    this.resultMessage = resultMessage;
  }

  public String toString() {
    return "{\"success\":\"" + (this.success ? "true" : "false")
        + "\",\"resultMessage\":\"" + this.resultMessage + "\"}";
  }

  public ResultStatus applyResultStatus(ResultStatus otherStatus) {
    this.success = this.success && otherStatus.success;
    this.resultMessage += "\n\n" + otherStatus.resultMessage;
    return this;
  }

  public static ResultStatus successful(final String resultMessage) {
    return new ResultStatus(true, resultMessage);
  }

  public static ResultStatus failure(final String resultMessage) {
    return new ResultStatus(false, resultMessage);
  }
}

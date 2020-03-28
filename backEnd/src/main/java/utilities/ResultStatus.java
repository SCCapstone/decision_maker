package utilities;

public class ResultStatus {

  public boolean success;
  public String resultMessage;

  public ResultStatus() {
    this.success = false;
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
}

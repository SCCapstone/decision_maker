package utilities;

public class ErrorDescriptor<T> {

  private T input;
  private String classMethod;
  private String requestId;
  private Exception exception;
  private String developerMessage;

  public ErrorDescriptor(T input, String classMethod, String requestId,
      Exception exception) {
    this.input = input;
    this.classMethod = classMethod;
    this.requestId = requestId;
    this.exception = exception;
  }

  public ErrorDescriptor(T input, String classMethod, Exception exception) {
    this.input = input;
    this.classMethod = classMethod;
    this.exception = exception;
  }

  public ErrorDescriptor(T input, String classMethod, String requestId,
      String developerMessage) {
    this.input = input;
    this.classMethod = classMethod;
    this.requestId = requestId;
    this.developerMessage = developerMessage;
  }

  public ErrorDescriptor(T input, String classMethod, String developerMessage) {
    this.input = input;
    this.classMethod = classMethod;
    this.developerMessage = developerMessage;
  }

  public ErrorDescriptor withRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public String toString() {
    //maybe update this print four separate lines? Or like one log message with 4 lines \n\r
    String retString = "[ERROR] requestId: " + this.requestId;

    if (this.classMethod != null) {
      retString += "\n\tlocation: " + this.classMethod;
    }

    if (this.input != null) {
      retString += "\n\tinput: " + JsonEncoders.convertObjectToJson(this.input);
    }

    if (this.developerMessage != null) {
      retString += "\n\tmessage: " + this.developerMessage;
    }

    if (this.exception != null) {
      retString += "\n\texception: " + ExceptionHelper.getStackTrace(this.exception);
    }

    return retString;
  }
}

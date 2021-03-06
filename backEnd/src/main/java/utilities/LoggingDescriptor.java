package utilities;

public class LoggingDescriptor<T> {

  private String loggingLevel;
  private T input;
  private String classMethod;
  private String requestId;
  private Exception exception;
  private String developerMessage;

  public LoggingDescriptor(String loggingLevel, T input, String classMethod, Exception exception) {
    this.loggingLevel = loggingLevel;
    this.input = input;
    this.classMethod = classMethod;
    this.exception = exception;
  }

  public LoggingDescriptor(String loggingLevel, String classMethod, Exception exception) {
    this.loggingLevel = loggingLevel;
    this.input = null;
    this.classMethod = classMethod;
    this.exception = exception;
  }

  public LoggingDescriptor(String loggingLevel, T input, String classMethod,
      String developerMessage) {
    this.loggingLevel = loggingLevel;
    this.input = input;
    this.classMethod = classMethod;
    this.developerMessage = developerMessage;
  }

  public LoggingDescriptor(String loggingLevel, String classMethod,
      String developerMessage) {
    this.loggingLevel = loggingLevel;
    this.input = null;
    this.classMethod = classMethod;
    this.developerMessage = developerMessage;
  }

  public LoggingDescriptor withRequestId(String requestId) {
    this.requestId = requestId;
    return this;
  }

  public LoggingDescriptor withInput(T input) {
    this.input = input;
    return this;
  }

  public String toString() {
    //maybe update this print four separate lines? Or like one log message with 4 lines \n\r
    String retString = "[" + this.loggingLevel + "] requestId: " + this.requestId;

    if (this.classMethod != null) {
      retString += "\n\tlocation: " + this.classMethod;
    }

    if (this.input != null) {
      retString += "\n\tinput: " + JsonUtils.convertObjectToJson(this.input);
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

package utilities;

public class ErrorDescriptor<T> extends LoggingDescriptor<T> {

  public ErrorDescriptor(T input, String classMethod, Exception exception) {
    super("ERROR", input, classMethod, exception);
  }

  public ErrorDescriptor(String classMethod, Exception exception) {
    super("ERROR", classMethod, exception);
  }

  public ErrorDescriptor(T input, String classMethod, String developerMessage) {
    super("ERROR", input, classMethod, developerMessage);
  }

  public ErrorDescriptor(String classMethod, String developerMessage) {
    super("ERROR", classMethod, developerMessage);
  }
}

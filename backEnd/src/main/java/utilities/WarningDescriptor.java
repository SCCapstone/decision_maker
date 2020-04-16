package utilities;

public class WarningDescriptor<T> extends LoggingDescriptor<T> {

  public WarningDescriptor(T input, String classMethod, Exception exception) {
    super("WARNING", input, classMethod, exception);
  }

  public WarningDescriptor(T input, String classMethod, String developerMessage) {
    super("WARNING", input, classMethod, developerMessage);
  }
}

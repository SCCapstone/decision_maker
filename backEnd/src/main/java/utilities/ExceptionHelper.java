package utilities;

public class ExceptionHelper {

  public static String getStackTrace(Exception e) {
    StringBuilder ret = new StringBuilder(e.toString() + "\n");

    StackTraceElement[] stackTraceElements = e.getStackTrace();
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      ret.append(stackTraceElement.toString());
      ret.append("\n");
    }

    return ret.toString();
  }
}

package utilities;

public class ExceptionHelper {
  public static void LogException(Exception e) {
    //TODO instead of using dataOutputStream, that should be our log file or something like that. https://github.com/SCCapstone/decision_maker/issues/82
//      dataOutputStream.writeUTF(e.toString() + "\n");
//      StackTraceElement[] stackTraceElements = e.getStackTrace();
//      StringBuilder stringBuilder = new StringBuilder();
//      for (int i = 0; i < stackTraceElements.length; i++) {
//        stringBuilder.append(stackTraceElements[i].toString() + "\n");
//      }
//      dataOutputStream.writeUTF(stringBuilder.toString());
  }

  public static String getStackTrace(Exception e) {
    String ret = e.getMessage() + '\n';

    StackTraceElement[] stackTraceElements = e.getStackTrace();
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < stackTraceElements.length; i++) {
      stringBuilder.append(stackTraceElements[i].toString() + "\n");
    }

    ret += stringBuilder.toString();

    return ret;
  }
}

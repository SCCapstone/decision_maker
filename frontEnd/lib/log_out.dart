import 'imports/globals.dart';
import 'imports/user_tokens_manager.dart';

// Clear and reset everything stored locally.
void logOutUser() {
  Globals.clearGlobals();
  clearTokens();
}

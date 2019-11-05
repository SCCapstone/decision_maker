import 'package:flutter/material.dart';
import 'package:milestone_3/imports/user_tokens_manager.dart';

import 'utilities/input_field.dart';

class SignInPage extends StatefulWidget {
  @override
  _SignInState createState() => _SignInState();
}

class _SignInState extends State<SignInPage> {
  final InputField usernameInput = new InputField("Username", TextInputType.text);
  final InputField passwordInput = new InputField("Password", TextInputType.text);
  final InputField emailInput = new InputField("Email Address", TextInputType.emailAddress);

  bool _signUp = true;

  @override
  void dispose() {
    usernameInput.controller.dispose();
    passwordInput.controller.dispose();
    emailInput.controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomPadding: false,
      appBar: AppBar(
        title: Text(getTitle(_signUp)),
      ),
      body: Container(
        width: MediaQuery.of(context).size.width,
        height: MediaQuery.of(context).size.height,
        child: Column(
          children: <Widget>[
            Padding(
              padding: EdgeInsets.all(25.0),
            ),
            Container(
                child: Visibility(
                  child: Text("Email"),
                  visible: _signUp,
                )
            ),
            Container(
                child: Visibility(
                  child: emailInput,
                  visible: _signUp,
                )
            ),
            Text("Username"),
            usernameInput,
            Text("Password"),
            passwordInput,
            Padding(
              padding: EdgeInsets.all(5.0),
            ),
            RaisedButton(
              onPressed: (){}, // TODO implement
              child: Text(
                "Forgot Password?",
                style: TextStyle(
                  fontSize: 20
                ),
              ),
            ),
            Padding(
              padding: EdgeInsets.all(5.0),
            ),
            SizedBox(
              width: 300.0,
              height: 50.0,
              child: RaisedButton(
                color: Colors.blue,
                onPressed: (){
                  if(validatePassword(passwordInput,_signUp)==null){
                    registerNewUser();
                    }
                  else{
                      showErrorDialog(context,usernameInput,passwordInput,_signUp);
                    }
                  }, // TODO implement
                child: Text(
                  getSubmitButtonMsg(_signUp),
                  style: TextStyle(
                      fontSize: 30
                  ),
                ),
              ),
            ),
            Padding(
              padding: EdgeInsets.all(25.0),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: <Widget>[
                Flexible(
                  fit: FlexFit.loose,
                  child: Text(getMessage(_signUp)),
                ),
                Flexible(
                  fit: FlexFit.tight,
                  child: RaisedButton(
                    onPressed: (){setState(() {
                      _signUp = !_signUp;
                    });},
                    child: Text(getButtonMessage(_signUp)),
                  ),
                )
              ],
            )
          ],
        ),
      ),
    );
  }
}

String getTitle(bool signUp){
  if(signUp){
    return "Sign Up";
  }
  else{
    return "Sign In";
  }
}

String getSubmitButtonMsg(bool signUp){
  if(signUp){
    return "Sign Up";
  }
  else{
    return "Sign In";
  }
}

String getButtonMessage(bool signUp){
  if(signUp){
    return "Sign in";
  }
  else{
    return "Sign up";
  }
}

String getMessage(bool signUp){
  if(signUp){
    return "Already have an account?";
  }
  else{
    return "Create an account?";
  }
}

String validatePassword(InputField passwordInput, bool signUp){
  String password = passwordInput.controller.text;
  String retVal;
  if (password.isEmpty ) {
    retVal = "Password cannot be empty!";
  }
  else if(password.length<8 && signUp){
    retVal = "Password must be greater than 8 characters";
  }
  return retVal;
}

String validateUsernameInput(InputField inputField) {
  String name = inputField.controller.text;
  String retVal;
  if (name.isEmpty ) {
    retVal = "Username cannot be empty!";
  }
  return retVal;
}

void showErrorDialog(BuildContext context, InputField usernameInput, InputField passwordInput, bool signUp) {
  String usernameErrorMsg = validateUsernameInput(usernameInput);
  String passwordErrorMsg = validatePassword(passwordInput, signUp);
  String errorMsg;
  if(usernameErrorMsg == null){
    // the username is valid
    errorMsg = passwordErrorMsg;
  }
  else if(passwordErrorMsg == null){
    // the password is valid
    errorMsg = usernameErrorMsg;
  }
  else{
    // both invalid
    errorMsg = usernameErrorMsg+"\n"+passwordErrorMsg;
  }
  showDialog(
    context: context,
    builder: (BuildContext context) {
      // return object of type Dialog
      return AlertDialog(
        title: new Text("Invalid input!"),
        content: new Text(errorMsg),
        actions: <Widget>[
          new FlatButton(
            child: new Text("Close"),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
        ],
      );
    },
  );
}
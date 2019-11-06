import 'package:flutter/material.dart';
import 'package:milestone_3/imports/response_item.dart';
import 'package:milestone_3/imports/user_tokens_manager.dart';

import 'utilities/input_field.dart';

class SignInPage extends StatefulWidget {
  @override
  _SignInState createState() => _SignInState();
}

class _SignInState extends State<SignInPage> {
  final InputField usernameInput = new InputField("Username", TextInputType.text, false);
  final InputField passwordInput = new InputField("Password", TextInputType.text, true);
  final InputField emailInput = new InputField("Email Address", TextInputType.emailAddress, false);

  bool _signUp = false;

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
        title: Text(getPageTitle(_signUp)),
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
            Padding(
              padding: EdgeInsets.all(5.0),
            ),
            SizedBox(
              width: 300.0,
              height: 50.0,
              child: RaisedButton(
                color: const Color(0xff106126),
                textColor: Colors.white,
                onPressed: (){
                  if(_signUp){
                    attemptSignUp(context,passwordInput,usernameInput,emailInput,_signUp);
                  }
                  else{
                    attemptSignIn(context,passwordInput,usernameInput,emailInput,_signUp);
                  }
                  },
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
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                Flexible(
                  fit: FlexFit.loose,
                  child: Text(getQuestion(_signUp)),
                ),
                Flexible(
                  child: RaisedButton(
                    color: Colors.blue,
                    onPressed: (){setState(() {
                      // reload widget and switch to either sign up or sign in
                      passwordInput.controller.clear();
                      usernameInput.controller.clear();
                      emailInput.controller.clear();
                      _signUp = !_signUp;
                    });},
                    child: Text(getNavButtonMessage(_signUp)),
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

String getPageTitle(bool signUp){
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

String getNavButtonMessage(bool signUp){
  if(signUp){
    return "Sign in";
  }
  else{
    return "Sign up";
  }
}

String getQuestion(bool signUp){
  if(signUp){
    return "Already have an account?";
  }
  else{
    return "Create an account?";
  }
}

String validatePassword(InputField passwordInput, bool signUp){
  /*
    Validates a password with a given inputField object. If the user is not signing up, only check if it is empty
   */
  String password = passwordInput.controller.text;
  String retVal="";
  if (password.isEmpty ) {
    retVal = "Password cannot be empty!";
  }
  else if(password.length<8 && signUp){
    // if not signing up, don't need to remind the user of length of password
    retVal = "Password must be greater than 8 characters";
  }
  return retVal;
}

String validateUsernameInput(InputField inputField) {
  String name = inputField.controller.text;
  String retVal="";
  if (name.isEmpty ) {
    retVal = "Username cannot be empty!";
  }
  return retVal;
}

String validateEmailInput(InputField inputField){
  String email = inputField.controller.text;
  String retVal = "";
  if (email.isEmpty ) {
    retVal = "Email cannot be empty!";
  }
  else if(!email.contains("@")){
    retVal = "Enter a valid email address!";
  }
  return retVal;
}

void attemptSignIn(BuildContext context, InputField passwordInput, InputField usernameInput,
    InputField emailInput, bool signUp){
  if(validatePassword(passwordInput,signUp)=="" && validateUsernameInput(usernameInput)==""){
    // TODO trim the input
    ResponseItem response = logUserIn();
    if(response.getSuccess()){
      // sign up success, go to next stage
    }
    else{
      // sign in was not successful, show error
      print("Sign in not successful");
    }
  }
  else{
    showErrorDialog(context,usernameInput,passwordInput,emailInput,signUp);
  }
}

void attemptSignUp(BuildContext context, InputField passwordInput, InputField usernameInput,
    InputField emailInput, bool signUp){
  if(validatePassword(passwordInput,signUp)=="" && validateEmailInput(emailInput)==""
      && validateUsernameInput(usernameInput)==""){
    // no errors in the input, so attempt to sign up in cognito
    String email = emailInput.controller.text.trim();
    String username = usernameInput.controller.text.trim();
    String password = passwordInput.controller.text.trim();
    ResponseItem response = registerNewUser(email,username,password);
    if(response.getSuccess()){
      // sign up success, go to next stage
    }
    else{
      // sign up was not successful, show error
      print("Sign up not successful");
    }
  }
  else{
    // error
    showErrorDialog(context,usernameInput,passwordInput,emailInput,signUp);
  }
}

void showErrorDialog(BuildContext context, InputField usernameInput, InputField passwordInput,
    InputField emailInput, bool signUp) {
  List errorMsgs = new List();
  if(signUp){
    errorMsgs.add(validateEmailInput(emailInput));
  }
  errorMsgs.add(validateUsernameInput(usernameInput));
  errorMsgs.add(validatePassword(passwordInput, signUp));

  String errorMsg = "";
  if(errorMsgs.length==1){
    errorMsg = errorMsgs[0];
  }
  else{
    // more than one error msg, so create message with appropriate new lines separating each error message
    for(int i=0;i<errorMsgs.length;i++){
      errorMsg += errorMsgs[i]+"\n";
    }
  }
  errorMsg.trim(); // gets rid of extra new line

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
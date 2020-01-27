import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'imports/globals.dart';
import 'imports/response_item.dart';

bool mutexLock = false;

class SignInPage extends StatefulWidget {
  @override
  _SignInState createState() => _SignInState();
}

class _SignInState extends State<SignInPage> {
  final formKey = GlobalKey<FormState>();
  final TextEditingController usernameController = new TextEditingController();
  final TextEditingController passwordController = new TextEditingController();
  final TextEditingController emailController = new TextEditingController();
  final Future<ResponseItem> responseValue = null;
  bool signUp = false;
  bool autoValidate = false;
  String email;
  String username;
  String password;

  @override
  void dispose() {
    usernameController.dispose();
    passwordController.dispose();
    emailController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    signUp = false;
    autoValidate = false;
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomPadding: false,
      appBar: AppBar(title: Text((signUp) ? "Sign Up" : "Sign In")),
      body: Form(
        key: formKey,
        autovalidate: autoValidate,
        child: Container(
          width: MediaQuery.of(context).size.width,
          height: MediaQuery.of(context).size.height,
          child: ListView(
            padding: EdgeInsets.all(25.0),
            children: <Widget>[
              Visibility(
                  visible: signUp,
                  child: TextFormField(
                    controller: emailController,
                    validator: validEmail,
                    onSaved: (String arg) {
                      email = arg;
                    },
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.8),
                    decoration: InputDecoration(labelText: "Email"),
                  )),
              TextFormField(
                maxLength: 30,
                controller: usernameController,
                validator: validUsername,
                onSaved: (String arg) {
                  username = arg;
                },
                style: TextStyle(
                    fontSize:
                        DefaultTextStyle.of(context).style.fontSize * 0.8),
                decoration:
                    InputDecoration(labelText: "Username", counterText: ""),
              ),
              TextFormField(
                obscureText: true,
                controller: passwordController,
                validator: (signUp) ? validNewPassword : validPassword,
                onSaved: (String arg) {
                  password = arg;
                },
                style: TextStyle(
                    fontSize:
                        DefaultTextStyle.of(context).style.fontSize * 0.8),
                decoration: InputDecoration(labelText: "Password"),
              ),
              Padding(
                padding: EdgeInsets.all(5.0),
              ),
              Padding(
                padding: EdgeInsets.all(5.0),
              ),
              SizedBox(
                height: MediaQuery.of(context).size.width * .12,
                child: RaisedButton(
                  color: Globals.secondaryColor,
                  textColor: Colors.white,
                  onPressed: () {
                    if (!mutexLock) {
                      // prevents user from spamming the login button if HTTP request is being processed
                      validateInput();
                    }
                  },
                  child: Text(
                    (signUp) ? "Sign Up" : "Sign In",
                    style: TextStyle(
                        fontSize:
                            DefaultTextStyle.of(context).style.fontSize * 0.7),
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
                    child: Text((signUp)
                        ? "Already have an account?"
                        : "Create an account?"),
                  ),
                  Flexible(
                    child: RaisedButton(
                      color: Colors.green,
                      onPressed: () {
                        setState(() {
                          // reload widget and switch to either sign up or sign in
                          passwordController.clear();
                          usernameController.clear();
                          emailController.clear();
                          signUp = !signUp;
                          autoValidate = false;
                        });
                      },
                      child: Text((!signUp) ? "Sign Up" : "Sign In"),
                    ),
                  )
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  void validateInput() {
    final form = formKey.currentState;
    if (formKey.currentState.validate()) {
      print("Valid");
      form.save();
      // all input is valid. Attempt sign in / sign up
      if (signUp) {
        attemptSignUp();
      } else {
        attemptSignIn();
      }
    } else {
      print("invalid");
      setState(() => autoValidate = true);
    }
  }

  void attemptSignIn() {
    showLoadingDialog(context, "Loading..."); // show loading dialog
    mutexLock = true;
//    ResponseItem response = await logUserIn(context, username, password);
    Navigator.pop(context); // dismiss loading dialog
    mutexLock = false;
//    if (response.success) {
//      // sign in success, go to next stage
//    } else {
//      // sign in was not successful, show error
//    }
  }

  void attemptSignUp() {
    showLoadingDialog(context, "Loading..."); // show loading dialog
    mutexLock = true;
//    ResponseItem response = await logUserIn(context, username, password);
    Navigator.pop(context); // dismiss loading dialog
    mutexLock = false;
//    if (response.success) {
//      // sign up success, go to next stage
//    } else {
//      // sign up was not successful, show error
//    }
  }
}

void showLoadingDialog(BuildContext context, String msg) {
  showDialog(
      barrierDismissible: false,
      context: context,
      builder: (context) {
        return WillPopScope(
          // prevents the dialog from being exited by the back button
          onWillPop: () async => false,
          child: AlertDialog(
            content: Flex(
              direction: Axis.horizontal,
              children: <Widget>[
                CircularProgressIndicator(),
                Padding(
                  padding: EdgeInsets.all(20),
                ),
                Flexible(
                  flex: 8,
                  child: Text(msg),
                )
              ],
            ),
          ),
        );
      });
}
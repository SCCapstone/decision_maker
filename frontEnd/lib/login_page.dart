import 'package:amazon_cognito_identity_dart/cognito.dart';
import 'package:flutter/material.dart';
import 'package:frontEnd/main.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:url_launcher/url_launcher.dart';

import 'imports/globals.dart';
import 'imports/response_item.dart';
import 'imports/user_tokens_manager.dart';

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
                key: new Key("username"),
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
                key: new Key("password"),
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
                key: new Key("signInOrUp"),
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
                padding: EdgeInsets.all(10.0),
              ),
              Visibility(
                visible: !signUp,
                child: InkWell(
                  child: Center(
                    child: Text(
                      "Forgot Password? Click here to reset.",
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize * 0.4,
                          decoration: TextDecoration.underline),
                    ),
                  ),
                  onTap: () async {
                    if (await canLaunch(Globals.resetUrl)) {
                      await launch(Globals.resetUrl);
                    } else {
                      throw 'Could not launch ${Globals.resetUrl}';
                    }
                  },
                ),
              ),
              Padding(
                padding: EdgeInsets.all(10.0),
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
                          formKey.currentState.reset();
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
      form.save();
      // all input is valid. Attempt sign in / sign up
      if (signUp) {
        attemptSignUp();
      } else {
        attemptSignIn();
      }
    } else {
      setState(() => autoValidate = true);
    }
  }

  void attemptSignIn() async {
    showLoadingDialog(context, "Loading..."); // show loading dialog
    mutexLock = true;

    bool signedIn = false;

    final userPool = new CognitoUserPool('us-east-2_ebbPP76nO',
        '7eh4otm1r5p351d1u9j3h3rf1o'); //TODO put in config
    final cognitoUser = new CognitoUser(this.usernameController.text, userPool);
    final authDetails = new AuthenticationDetails(
        username: this.usernameController.text,
        password: this.passwordController.text);
    CognitoUserSession session;
    try {
      session = await cognitoUser.authenticateUser(authDetails);

      await storeUserTokens(session.getAccessToken().jwtToken,
          session.getRefreshToken().getToken(), session.getIdToken().jwtToken);

      signedIn = true;
//    } on CognitoUserNewPasswordRequiredException catch (e) {
//      // handle New Password challenge
//    } on CognitoUserMfaRequiredException catch (e) {
//      // handle SMS_MFA challenge
//    } on CognitoUserSelectMfaTypeException catch (e) {
//      // handle SELECT_MFA_TYPE challenge
//    } on CognitoUserMfaSetupException catch (e) {
//      // handle MFA_SETUP challenge
//    } on CognitoUserTotpRequiredException catch (e) {
//      // handle SOFTWARE_TOKEN_MFA challenge
//    } on CognitoUserCustomChallengeException catch (e) {
//      // handle CUSTOM_CHALLENGE challenge
//    } on CognitoUserConfirmationNecessaryException catch (e) {
//      // handle User Confirmation Necessary
    } catch (e) {
      print(e);
    }

    Navigator.of(context, rootNavigator: true)
        .pop('dialog'); // dismiss loading dialog
    mutexLock = false;

    if (signedIn) {
      Navigator.pushReplacement(
          context, new MaterialPageRoute(builder: (context) => MyApp()));
    }
  }

  void attemptSignUp() async {
    showLoadingDialog(context, "Loading..."); // show loading dialog
    mutexLock = true;

    final userPool = new CognitoUserPool('us-east-2_ebbPP76nO',
        '7eh4otm1r5p351d1u9j3h3rf1o'); //TODO put in config
    final userAttributes = [
      new AttributeArg(name: 'email', value: this.emailController.text),
    ];

    var data;
    try {
      data = await userPool.signUp(
          this.usernameController.text, this.passwordController.text,
          userAttributes: userAttributes);
    } catch (e) {
      print(e);
    }

    Navigator.of(context, rootNavigator: true)
        .pop('dialog'); // dismiss loading dialog
    mutexLock = false;

    if (data != null) {
      attemptSignIn();
    }
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

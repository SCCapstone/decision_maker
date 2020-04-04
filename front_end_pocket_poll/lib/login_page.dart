import 'package:amazon_cognito_identity_dart/cognito.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/main.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
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
      appBar: AppBar(
          centerTitle: true,
          title: AutoSizeText(
            "Pocket Poll",
            style: TextStyle(fontSize: 40),
            maxLines: 1,
            minFontSize: 20,
          )),
      body: GestureDetector(
        onTap: () {
          hideKeyboard(context);
        },
        child: Form(
          key: formKey,
          autovalidate: autoValidate,
          child: ListView(
            padding: EdgeInsets.all(25.0),
            children: <Widget>[
              Visibility(
                  visible: signUp,
                  child: TextFormField(
                    keyboardType: TextInputType.emailAddress,
                    controller: emailController,
                    maxLength: Globals.maxEmailLength,
                    validator: validEmail,
                    onSaved: (String arg) {
                      email = arg.trim();
                    },
                    style: TextStyle(fontSize: 32),
                    decoration:
                        InputDecoration(labelText: "Email", counterText: ""),
                  )),
              TextFormField(
                key: new Key("username"),
                maxLength: Globals.maxUsernameLength,
                controller: usernameController,
                validator: validUsername,
                onSaved: (String arg) {
                  username = arg.trim();
                },
                style: TextStyle(fontSize: 32),
                decoration:
                    InputDecoration(labelText: "Username", counterText: ""),
              ),
              TextFormField(
                key: new Key("password"),
                obscureText: true,
                autocorrect: false,
                // don't allow user's passwords to be saved in their keyboard
                maxLength: Globals.maxPasswordLength,
                controller: passwordController,
                validator: (signUp) ? validNewPassword : validPassword,
                onSaved: (String arg) {
                  password = arg.trim();
                },
                style: TextStyle(fontSize: 32),
                decoration:
                    InputDecoration(labelText: "Password", counterText: ""),
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
                  onPressed: () {
                    if (!mutexLock) {
                      // prevents user from spamming the login button if HTTP request is being processed
                      validateInput();
                    }
                  },
                  child: Text(
                    (signUp) ? "Sign Up" : "Sign In",
                    style: TextStyle(fontSize: 32),
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
                    child: AutoSizeText(
                      "Forgot Password? Click here to reset.",
                      maxLines: 1,
                      minFontSize: 14,
                      style: TextStyle(
                          color: Colors.blue,
                          fontSize: 20,
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
                    child: AutoSizeText(
                      (signUp)
                          ? "Already have an account?"
                          : "Create an account?",
                      maxLines: 1,
                      minFontSize: 12,
                      style: TextStyle(fontSize: 16),
                    ),
                  ),
                  Flexible(
                    child: RaisedButton(
                      onPressed: () {
                        setState(() {
                          // reload widget and switch to either sign up or sign in
                          formKey.currentState.reset();
                          passwordController.clear();
                          usernameController.clear();
                          emailController.clear();
                          signUp = !signUp;
                          autoValidate = false;
                          hideKeyboard(context);
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
    showLoadingDialog(context, "Loading...", false); // show loading dialog
    mutexLock = true;

    bool signedIn = false;

    final userPool = new CognitoUserPool('us-east-2_ebbPP76nO',
        '7eh4otm1r5p351d1u9j3h3rf1o'); //TODO put in config
    final cognitoUser = new CognitoUser(this.username, userPool);
    final authDetails = new AuthenticationDetails(
        username: this.username, password: this.password);
    CognitoUserSession session;
    try {
      session = await cognitoUser.authenticateUser(authDetails);

      await storeUserTokens(session.getAccessToken().jwtToken,
          session.getRefreshToken().getToken(), session.getIdToken().jwtToken);

      signedIn = true;
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      mutexLock = false;
    } catch (e) {
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      mutexLock = false;
      showErrorMessage("Sign In Error", e.message, context);
    }

    if (signedIn) {
      Navigator.pushReplacement(
          context, new MaterialPageRoute(builder: (context) => MyApp()));
    }
  }

  void attemptSignUp() async {
    showLoadingDialog(context, "Loading...", false); // show loading dialog
    mutexLock = true;

    final userPool = new CognitoUserPool('us-east-2_ebbPP76nO',
        '7eh4otm1r5p351d1u9j3h3rf1o'); //TODO put in config
    final userAttributes = [
      new AttributeArg(name: 'email', value: this.email),
    ];

    var data;
    try {
      data = await userPool.signUp(this.username, this.password,
          userAttributes: userAttributes);
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      mutexLock = false;
    } catch (e) {
      Navigator.of(context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      mutexLock = false;
      showErrorMessage("Sign Up Error", e.message, context);
    }

    if (data != null) {
      attemptSignIn();
    }
  }
}

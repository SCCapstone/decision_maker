import 'package:amazon_cognito_identity_dart/cognito.dart';
import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/main.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/imports/response_item.dart';
import 'package:front_end_pocket_poll/imports/user_tokens_manager.dart';

class LoginPage extends StatefulWidget {
  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final TextEditingController usernameController = new TextEditingController();
  final TextEditingController passwordController = new TextEditingController();
  final TextEditingController emailController = new TextEditingController();
  final Future<ResponseItem> responseValue = null;
  bool signUp;
  bool autoValidate;
  bool showPassword;
  String email;
  String username;
  String password;
  bool loading;
  FocusNode usernameFocus;
  FocusNode passwordFocus;

  @override
  void dispose() {
    this.usernameController.dispose();
    this.passwordController.dispose();
    this.emailController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    this.usernameFocus = new FocusNode();
    this.passwordFocus = new FocusNode();
    this.showPassword = false;
    this.loading = false;
    this.signUp = false;
    this.autoValidate = false;
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
      key: Key("login_page:scaffold"),
      body: GestureDetector(
        onTap: () {
          hideKeyboard(context);
        },
        child: Form(
          key: this.formKey,
          autovalidate: this.autoValidate,
          child: ListView(
            padding: EdgeInsets.all(25.0),
            children: <Widget>[
              Visibility(
                  visible: this.signUp,
                  child: Row(
                    children: <Widget>[
                      Expanded(
                        child: TextFormField(
                          keyboardType: TextInputType.emailAddress,
                          controller: this.emailController,
                          maxLength: Globals.maxEmailLength,
                          validator: validEmail,
                          onSaved: (String arg) {
                            this.email = arg.trim();
                          },
                          textInputAction: TextInputAction.next,
                          onFieldSubmitted: (form) {
                            // when user hits the next button on their keyboard, takes them to the username field
                            FocusScope.of(context)
                                .requestFocus(this.usernameFocus);
                          },
                          style: TextStyle(fontSize: 32),
                          decoration: InputDecoration(
                              labelText: "Email", counterText: ""),
                        ),
                      ),
                      IconButton(
                        icon: Icon(Icons.help_outline),
                        tooltip: "Help",
                        onPressed: () {
                          showHelpMessage(
                              "Email Help",
                              "Currently the email is only used for resetting passwords.",
                              context);
                        },
                      )
                    ],
                  )),
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextFormField(
                      key: Key("login_page:username_input"),
                      maxLength: Globals.maxUsernameLength,
                      controller: this.usernameController,
                      validator: validUsername,
                      onSaved: (String arg) {
                        this.username = arg.trim();
                      },
                      focusNode: usernameFocus,
                      textInputAction: TextInputAction.next,
                      onFieldSubmitted: (form) {
                        // when user hits the next button on their keyboard, takes them to the password field
                        FocusScope.of(context).requestFocus(this.passwordFocus);
                      },
                      style: TextStyle(fontSize: 32),
                      decoration: InputDecoration(
                          labelText: "Username", counterText: ""),
                    ),
                  ),
                  Visibility(
                    visible: this.signUp,
                    child: IconButton(
                      icon: Icon(Icons.help_outline),
                      tooltip: "Help",
                      onPressed: () {
                        showHelpMessage(
                            "Username Help",
                            "The username uniquely identifies an account within the app. "
                                "You will use this to login to your account, so don't forget it! "
                                "Other users will use this to add you to groups.",
                            context);
                      },
                    ),
                  )
                ],
              ),
              Row(
                children: <Widget>[
                  Expanded(
                    child: TextFormField(
                      key: Key("login_page:password_input"),
                      obscureText: !this.showPassword,
                      autocorrect: false,
                      // don't allow user's passwords to be saved in their keyboard
                      maxLength: Globals.maxPasswordLength,
                      controller: this.passwordController,
                      focusNode: passwordFocus,
                      validator:
                          (this.signUp) ? validNewPassword : validPassword,
                      onSaved: (String arg) {
                        this.password = arg.trim();
                      },
                      onFieldSubmitted: (form) {
                        // when user hits the next button on their keyboard, tries to sign in/up
                        validateInput();
                      },
                      style: TextStyle(fontSize: 32),
                      decoration: InputDecoration(
                          labelText: "Password", counterText: ""),
                    ),
                  ),
                  IconButton(
                    icon: (this.showPassword)
                        ? Icon(Icons.visibility_off)
                        : Icon(Icons.remove_red_eye),
                    tooltip:
                        (this.showPassword) ? "Hide Password" : "Show password",
                    onPressed: () {
                      setState(() {
                        this.showPassword = !this.showPassword;
                      });
                    },
                  )
                ],
              ),
              Padding(
                padding: EdgeInsets.all(5.0),
              ),
              Padding(
                padding: EdgeInsets.all(5.0),
              ),
              SizedBox(
                key: Key("login_page:sign_in_button"),
                height: MediaQuery.of(context).size.width * .12,
                child: RaisedButton(
                  onPressed: () {
                    if (!this.loading) {
                      // prevents user from spamming the login button if HTTP request is being processed
                      validateInput();
                    }
                  },
                  child: Text(
                    (this.signUp) ? "Sign Up" : "Sign In",
                    style: TextStyle(fontSize: 32),
                  ),
                ),
              ),
              Padding(
                padding: EdgeInsets.all(10.0),
              ),
              Visibility(
                visible: !this.signUp,
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
                    if (await canLaunch(Globals.resetPasswordUrl)) {
                      await launch(Globals.resetPasswordUrl);
                    } else {
                      throw 'Could not launch ${Globals.resetPasswordUrl}';
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
                      (this.signUp)
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
                          this.formKey.currentState.reset();
                          this.passwordController.clear();
                          this.usernameController.clear();
                          this.emailController.clear();
                          this.signUp = !this.signUp;
                          this.autoValidate = false;
                          hideKeyboard(context);
                        });
                      },
                      child: Text((!this.signUp) ? "Sign Up" : "Sign In"),
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

  // attempt to validate all user input. Highlight errors if they arise
  void validateInput() {
    final form = formKey.currentState;
    if (this.formKey.currentState.validate()) {
      form.save();
      // all input is valid. Attempt sign in / sign up
      if (this.signUp) {
        attemptSignUp();
      } else {
        attemptSignIn();
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }

  void attemptSignIn() async {
    this.loading = true;
    showLoadingDialog(this.context, "Loading...", false); // show loading dialog

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
      this.loading = false;
      Navigator.of(this.context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
    } catch (e) {
      this.loading = false;
      Navigator.of(this.context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      showErrorMessage("Sign In Error", e.message, this.context);
    }

    if (signedIn) {
      Navigator.pushReplacement(
          this.context, new MaterialPageRoute(builder: (context) => MyApp()));
    }
  }

  void attemptSignUp() async {
    this.loading = true;
    showLoadingDialog(this.context, "Loading...", false); // show loading dialog

    final userPool = new CognitoUserPool('us-east-2_ebbPP76nO',
        '7eh4otm1r5p351d1u9j3h3rf1o'); //TODO put in config
    final userAttributes = [
      new AttributeArg(name: 'email', value: this.email),
    ];

    try {
      var data = await userPool.signUp(this.username, this.password,
          userAttributes: userAttributes);
      this.loading = false;
      Navigator.of(this.context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      if (data != null) {
        // we do this in order to get the user data from the DB
        attemptSignIn();
      }
    } catch (e) {
      this.loading = false;
      Navigator.of(this.context, rootNavigator: true)
          .pop('dialog'); // dismiss loading dialog
      showErrorMessage("Sign Up Error", e.message, this.context);
    }
  }
}

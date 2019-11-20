import 'package:flutter/material.dart';
import 'package:frontEnd/models/group.dart';
import 'package:frontEnd/utilities/validator.dart';

class GroupSettings extends StatefulWidget {
  final Group group;

  GroupSettings({Key key, this.group}) : super(key: key);

  @override
  _GroupSettingsState createState() => _GroupSettingsState();
}

class _GroupSettingsState extends State<GroupSettings> {
  bool _autoValidate = false;
  bool _validGroupIcon = true; // assume user
  bool _editing = false;
  String _groupName;
  String _groupIcon;
  int _pollPassPercent;
  int _pollDuration;

  final _formKey = GlobalKey<FormState>();
  final _groupNameController = TextEditingController();
  final _groupIconController = TextEditingController();
  final _pollPassController = TextEditingController();
  final _pollDurationController = TextEditingController();

  @override
  void dispose() {
    _groupNameController.dispose();
    _groupIconController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    _groupIcon = widget.group
        .groupIcon; // assume icon isn't changed. This will change if user clicks on popup
    _groupNameController.text = widget.group.groupName;
    _pollPassController.text = widget.group.defaultPollPassPercent.toString();
    _pollDurationController.text = widget.group.defaultPollDuration.toString();
    // TODO get all categories from the users that are in this group
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: true,
      appBar: AppBar(
        title: Text("${widget.group.groupName} Settings"),
      ),
      body: Form(
        key: _formKey,
        autovalidate: _autoValidate,
        child: ListView(
          padding: EdgeInsets.all(10.0),
          children: <Widget>[
            Column(
              children: [
                TextFormField(
                  controller: _groupNameController,
                  validator: validGroupName,
                  onChanged: (String arg) {
                    // the moment the user starts making changes, display the save button
                    if (!(arg == widget.group.groupName)) {
                      setState(() {
                        _editing = true;
                      });
                    } else {
                      setState(() {
                        _editing = false;
                      });
                    }
                  },
                  onSaved: (String arg) {
                    _groupName = arg;
                  },
                  style: TextStyle(fontSize: 35),
                  decoration: InputDecoration(labelText: "Group Name"),
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .01),
                ),
                Container(
                  width: MediaQuery.of(context).size.width * .5,
                  height: MediaQuery.of(context).size.height * .3,
                  alignment: Alignment.topRight,
                  decoration: BoxDecoration(
                      image: DecorationImage(
                          fit: BoxFit.fitHeight,
                          image: NetworkImage(widget.group.groupIcon))),
                  child: Container(
                    decoration: BoxDecoration(
                        color: Colors.grey.withOpacity(0.7),
                        shape: BoxShape.circle),
                    child: IconButton(
                      icon: Icon(Icons.edit),
                      color: Colors.blueAccent,
                      onPressed: () {
                        groupIconPopup(context);
                      },
                    ),
                  ),
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .004),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: <Widget>[
                    Text(
                      "Default poll duration (mins)",
                      style: TextStyle(fontSize: 20),
                    ),
                    Container(
                      width: MediaQuery.of(context).size.width * .25,
                      child: TextFormField(
                        keyboardType: TextInputType.number,
                        validator: validPollDuration,
                        controller: _pollDurationController,
                        onChanged: (String arg) {
                          int num = int.parse(arg);
                          // the moment the user starts making changes, display the save button
                          if (!(num == widget.group.defaultPollDuration)) {
                            setState(() {
                              _editing = true;
                            });
                          } else {
                            setState(() {
                              _editing = false;
                            });
                          }
                        },
                        onSaved: (String arg) {
                          _pollDuration = int.parse(arg);
                        },
                        decoration:
                            InputDecoration(border: OutlineInputBorder()),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding:
                      EdgeInsets.all(MediaQuery.of(context).size.height * .004),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: <Widget>[
                    Text(
                      "Default pass percentage     ",
                      style: TextStyle(fontSize: 20),
                    ),
                    Container(
                      width: MediaQuery.of(context).size.width * .25,
                      child: TextFormField(
                        controller: _pollPassController,
                        keyboardType: TextInputType.number,
                        validator: validPassPercentage,
                        onChanged: (String arg) {
                          int num = int.parse(arg);
                          // the moment the user starts making changes, display the save button
                          if (!(num == widget.group.defaultPollPassPercent)) {
                            setState(() {
                              _editing = true;
                            });
                          } else {
                            setState(() {
                              _editing = false;
                            });
                          }
                        },
                        onSaved: (String arg) {
                          _pollPassPercent = int.parse(arg);
                        },
                        decoration:
                            InputDecoration(border: OutlineInputBorder()),
                      ),
                    )
                  ],
                ),
                Visibility(
                  visible: _editing,
                  child: RaisedButton.icon(
                      onPressed: validateInput,
                      icon: Icon(Icons.save),
                      label: Text("Save")),
                )
              ],
            ),
          ],
        ),
      ),
    );
  }

  void validateInput() {
    final form = _formKey.currentState;
    if (form.validate() && _validGroupIcon) {
      // b/c url is entered in a popup dialog, can't share the same form so must use another flag
      form.save();
      print(
          "New name: $_groupName New Duration: $_pollDuration New percentage: $_pollPassPercent");
      // TODO edit the Group object and update to DB ()
      // should also go back to last page and pass in the new Group object in constructor to avoid logic errors
      // (e.g. group name changed, so previous page would have the old group name when going back)
    } else {
      setState(() => _autoValidate = true);
    }
  }

  void groupIconPopup(BuildContext context) {
    // displays a popup for editing the group icon's url
    // putting this method in this class because need to set state when url is validated

    final formKey = GlobalKey<FormState>();
    showDialog(
        context: context,
        builder: (context) {
          return Form(
            autovalidate: _autoValidate,
            key: formKey,
            child: AlertDialog(
              title: Text("Edit Icon url"),
              actions: <Widget>[
                FlatButton(
                  child: Text("Cancel"),
                  onPressed: () {
                    _groupIconController.clear();
                    Navigator.of(context).pop();
                  },
                ),
                FlatButton(
                  child: Text("Submit"),
                  onPressed: () {
                    if (formKey.currentState.validate()) {
                      setState(() {
                        print("is valid");
                        _groupIcon = _groupIconController.text;
                        _groupIconController.clear();
                        _editing = true;
                        _validGroupIcon = true;
                        _autoValidate = true;
                        Navigator.of(context).pop();
                      });
                    }
                  },
                ),
              ],
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  TextFormField(
                      controller: _groupIconController,
                      validator: validGroupIcon,
                      keyboardType: TextInputType.url,
                      decoration: InputDecoration(
                        labelText: "Enter a icon link",
                      )),
                ],
              ),
            ),
          );
        });
  }
}

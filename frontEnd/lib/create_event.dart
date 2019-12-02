import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';

import 'imports/categories_manager.dart';
import 'imports/groups_manager.dart';
import 'imports/globals.dart';
import 'models/category.dart';
import 'models/event.dart';
import 'models/group.dart';

class CreateEvent extends StatefulWidget {
  @required
  final Group group;

  CreateEvent({Key key, this.group}) : super(key: key);

  @override
  _CreateEventState createState() => _CreateEventState();
}

class _CreateEventState extends State<CreateEvent> {
  String eventName;
  String pollDuration;
  String pollPassPercent;
  final TextEditingController eventNameController = TextEditingController();

  // These TextEditingControllers must be non-final so that I can initialize
  // them with the default values in initState() while still having them be
  // accessible for the whole class
  TextEditingController pollDurationController;
  TextEditingController passPercentageController;

  Future<List<Category>> categoriesInGroup;
  bool autoValidate = false;
  final formKey = GlobalKey<FormState>();
  final List<Category> categoriesSelected = new List<Category>();
  final DateTime currentDate = DateTime.now();
  final TimeOfDay currentTime = TimeOfDay.now();

  // Using these strings both to display on the page and to eventually
  // make the API request for making the event
  String eventStartDate;
  String eventStartTime;

  String convertDateToString(DateTime date) {
    return date.toString().substring(0, 10);
  }

  String convertTimeToString(TimeOfDay time) {
    return time.toString().substring(10, 15);
  }

  int getHour(String time) {
    return int.parse(time.substring(0, 2));
  }

  int getMinute(String time) {
    return int.parse(time.substring(3, 5));
  }

  @override
  void dispose() {
    eventNameController.dispose();
    pollDurationController.dispose();
    passPercentageController.dispose();
    super.dispose();
  }

  @override
  void initState() {
    categoriesInGroup =
        CategoriesManager.getAllCategoriesFromGroup(widget.group.groupId);
    eventStartDate = convertDateToString(currentDate);
    eventStartTime = (currentTime.hour + 1).toString() + ":00";
    pollDurationController = new TextEditingController(
        text: widget.group.defaultPollDuration.toString());
    passPercentageController = new TextEditingController(
        text: widget.group.defaultPollPassPercent.toString());
    super.initState();
  }

  Future selectStartDate(BuildContext context) async {
    DateTime selectedDate = await showDatePicker(
        context: context,
        initialDate: DateTime.parse(eventStartDate),
        firstDate: new DateTime(currentDate.year),
        lastDate: new DateTime(currentDate.year + 30));
    if ((selectedDate.isAfter(currentDate) ||
        (selectedDate.day == currentDate.day) &&
            (selectedDate.year == currentDate.year) &&
            (selectedDate.month == currentDate.month))) {
      setState(() {
        eventStartDate = convertDateToString(selectedDate);
      });
    } else {
      showPopupMessage("Start date cannot be before today's date.", context);
    }
  }

  Future selectStartTime(BuildContext context) async {
    final TimeOfDay selectedTime = await showTimePicker(
        context: context,
        initialTime: new TimeOfDay(
            hour: getHour(eventStartTime), minute: getMinute(eventStartTime)));
    DateTime parsedStartDate = DateTime.parse(eventStartDate);
    if ((parsedStartDate.year == currentDate.year &&
            parsedStartDate.month == currentDate.month &&
            parsedStartDate.day == currentDate.day) &&
        ((selectedTime.hour < currentTime.hour) ||
            (selectedTime.hour == currentTime.hour &&
                selectedTime.minute < currentTime.minute))) {
      showPopupMessage("Start time must be after current time.", context);
    } else {
      setState(() {
        eventStartTime = convertTimeToString(selectedTime);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Create Event")),
      body: Form(
          key: formKey,
          autovalidate: autoValidate,
          child: ListView(
            shrinkWrap: true,
            padding: EdgeInsets.fromLTRB(20, 0, 20, 0),
            children: <Widget>[
              Column(
                children: <Widget>[
                  TextFormField(
                    controller: eventNameController,
                    validator: validEventName,
                    onSaved: (String arg) {
                      eventName = arg;
                    },
                    decoration:
                        InputDecoration(labelText: "Enter an event name"),
                  ),
                  Container(height: 20), // Temporarily using Containers to space out the elements
                  Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: <Widget>[
                        Text("Start date and time for the event",
                            style: TextStyle(fontSize: 16)),
                      ]),
                  Container(height: 10),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: <Widget>[
                        ButtonTheme(
                            minWidth: 110,
                            height: 40,
                            buttonColor: Colors.white60,
                            child: RaisedButton(
                                onPressed: () {
                                  selectStartDate(context);
                                },
                                child: Text(eventStartDate))),
                        ButtonTheme(
                            minWidth: 110,
                            height: 40,
                            buttonColor: Colors.white60,
                            child: RaisedButton(
                                onPressed: () {
                                  selectStartTime(context);
                                },
                                child: Text(eventStartTime))),
                      ]),
                  Container(height: 20),
                  FutureBuilder(
                      future: categoriesInGroup,
                      builder: (BuildContext context, AsyncSnapshot snapshot) {
                        if (snapshot.hasData) {
                          List<Category> categories = snapshot.data;

                          return CategoryDropdown("Select a category",
                              categories, categoriesSelected,
                              callback: (category) => selectCategory(category));
                        } else if (snapshot.hasError) {
                          return Text("Error: ${snapshot.error}");
                        } else {
                          return Center(child: CircularProgressIndicator());
                        }
                      }),
                  Container(height: 10),
                  TextFormField(
                    controller: pollDurationController,
                    validator: validPollDuration,
                    onSaved: (String arg) {
                      pollDuration = arg;
                    },
                    decoration: InputDecoration(
                        labelText: "Poll duration (in minutes)"),
                  ),
                  TextFormField(
                    controller: passPercentageController,
                    validator: validPassPercentage,
                    onSaved: (String arg) {
                      pollPassPercent = arg;
                    },
                    decoration:
                        InputDecoration(labelText: "Poll pass percentage"),
                  ),
                ],
              )
            ],
          )),
      bottomNavigationBar: BottomAppBar(
          child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: <Widget>[
            RaisedButton(
                onPressed: () {
                  Navigator.pop(context);
                },
                child: Text("Cancel")),
            RaisedButton.icon(
                onPressed: () {
                  if (categoriesSelected.isEmpty) {
                    showPopupMessage("Select a category.", context);
                  } else {
                    validateInput();
                  }
                },
                icon: Icon(Icons.add),
                label: Text("Create"))
          ])),
    );
  }

  void validateInput() {
    final form = formKey.currentState;
    if (form.validate()) {
      form.save();
      // We want the initial votingNumbers attribute value to be a map where
      // each key-value pair is the number of the choice and the number of votes
      // that choice has (starting at 0).
      Map<String, dynamic> votingNumbers = new Map<String, dynamic>();
      for (String key in this.categoriesSelected.first.choices.keys) {
        votingNumbers[key] = 0;
      }
      Map<String, dynamic> eventCreator = new Map<String, dynamic>();
      eventCreator.putIfAbsent(
          Globals.username, () => widget.group.members[Globals.username]);
      int nextEventId = widget.group.nextEventId;

      Event event = new Event(
        eventName: this.eventName,
        categoryId: this.categoriesSelected.first.categoryId,
        categoryName: this.categoriesSelected.first.categoryName,
        createdDateTime:
            DateTime.parse(currentDate.toString().substring(0, 19)),
        eventStartDateTime: DateTime.parse(
            this.eventStartDate + ' ' + this.eventStartTime + ':00'),
        type: 0, // all events are non-recurring for now
        pollDuration: int.parse(this.pollDuration),
        pollPassPercent: int.parse(this.pollPassPercent),
        optedIn: widget.group.members,
        tentativeAlgorithmChoices: this.categoriesSelected.first.choices,
        votingNumbers: votingNumbers,
        selectedChoice: "null", // note: we can't add an empty string to database
        eventCreator: eventCreator,
      );

      GroupsManager.addEvent(widget.group.groupId, event, nextEventId, context);
      setState(() {
        autoValidate = false;
      });
    } else {
      setState(() => autoValidate = true);
    }
  }

  void selectCategory(Category category) {
    setState(() {
      if (categoriesSelected.contains(category)) {
        categoriesSelected.remove(category);
      } else if (categoriesSelected.isEmpty) {
        categoriesSelected.add(category);
      } else {
        categoriesSelected.clear();
        categoriesSelected.add(category);
      }
    });
  }
}

import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';

import 'package:frontEnd/imports/categories_manager.dart';
import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/event.dart';

class CreateEvent extends StatefulWidget {
  @required
  CreateEvent({Key key}) : super(key: key);

  @override
  _CreateEventState createState() => _CreateEventState();
}

class _CreateEventState extends State<CreateEvent> {
  String eventName;
  String pollDuration;
  String pollPassPercent;
  final TextEditingController eventNameController = TextEditingController();
  final TextEditingController pollDurationController = TextEditingController();
  final TextEditingController passPercentageController =
      TextEditingController();

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
    categoriesInGroup = CategoriesManager.getAllCategoriesFromGroup(
        Globals.currentGroup.groupId);
    eventStartDate = convertDateToString(currentDate);
    eventStartTime = (currentTime.hour + 1).toString() + ":00";
    if ((currentTime.hour + 1) < 10) {
      eventStartTime = "0" + eventStartTime;
    }
    pollDurationController.text =
        Globals.currentGroup.defaultPollDuration.toString();
    passPercentageController.text =
        Globals.currentGroup.defaultPollPassPercent.toString();
    super.initState();
  }

  Future selectStartDate(BuildContext context) async {
    DateTime selectedDate = await showDatePicker(
        context: context,
        initialDate: DateTime.parse(eventStartDate),
        firstDate: new DateTime(currentDate.year),
        lastDate: new DateTime(currentDate.year + 30));
    DateTime currentDateMinusOneDay =
        currentDate.subtract(new Duration(days: 1));
    if ((selectedDate.isAfter(currentDateMinusOneDay))) {
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
    if (startTimeIsInvalid(parsedStartDate, selectedTime)) {
      showPopupMessage("Start time must be after current time.", context);
    } else {
      setState(() {
        eventStartTime = convertTimeToString(selectedTime);
      });
    }
  }

  bool startTimeIsInvalid(DateTime startDate, TimeOfDay startTime) {
    // Check if the start time is on the earlier in the day than the current
    // time (in other words, same date as current day && earlier time means
    // the start time is invalid)
    return ((startDate.year == currentDate.year &&
            startDate.month == currentDate.month &&
            startDate.day == currentDate.day)) &&
        ((startTime.hour < currentTime.hour) ||
            (startTime.hour == currentTime.hour &&
                startTime.minute < currentTime.minute));
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
                  Container(height: 20),
                  // Temporarily using Containers to space out the elements
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
                    keyboardType: TextInputType.number,
                    validator: validPollDuration,
                    onSaved: (String arg) {
                      pollDuration = arg;
                    },
                    decoration: InputDecoration(
                        labelText: "Poll duration (in minutes)"),
                  ),
                  TextFormField(
                    controller: passPercentageController,
                    keyboardType: TextInputType.number,
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
      Map<String, dynamic> eventCreator = new Map<String, dynamic>();
      eventCreator.putIfAbsent(Globals.username,
          () => Globals.currentGroup.members[Globals.username]);

      Event event = new Event(
        eventName: this.eventName,
        categoryId: this.categoriesSelected.first.categoryId,
        categoryName: this.categoriesSelected.first.categoryName,
        createdDateTime:
            DateTime.parse(currentDate.toString().substring(0, 19)),
        eventStartDateTime: DateTime.parse(
            this.eventStartDate + ' ' + this.eventStartTime + ':00'),
        type: 0,
        // all events are non-recurring for now
        pollDuration: int.parse(this.pollDuration),
        pollPassPercent: int.parse(this.pollPassPercent),
        eventCreator: eventCreator,
      );

      GroupsManager.addEvent(Globals.currentGroup.groupId, event, context);
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

import 'package:flutter/material.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';
import 'package:frontEnd/widgets/category_dropdown.dart';

import 'group_settings.dart';
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
  final eventNameController = TextEditingController();
  TextEditingController pollDurationController = TextEditingController();
  TextEditingController passPercentageController = TextEditingController();

  Future<List<Category>> categoriesTotal;

  bool autoValidate = false;
  final formKey = GlobalKey<FormState>();
  final List<Category> categoriesSelected = new List<Category>();

  final DateTime today = DateTime.now();
  final TimeOfDay currentTime = TimeOfDay.now();
  String eventStartDate;
  String eventEndDate;
  String eventStartTime;
  String eventEndTime;
  String pollDuration;
  String pollPassPercent;

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
    categoriesTotal = CategoriesManager.getAllCategoriesFromGroup(widget.group.groupId);
    eventStartDate = convertDateToString(today);
    eventEndDate = convertDateToString(today);
    eventStartTime = (currentTime.hour + 1).toString() + ":00";
    eventEndTime = (currentTime.hour + 2).toString() + ":00";
    pollDurationController = new TextEditingController(text: widget.group.defaultPollDuration.toString());
    passPercentageController = new TextEditingController(text: widget.group.defaultPollPassPercent.toString());
    super.initState();
  }

  Future selectStartDate(BuildContext context) async {
    DateTime selectedDate = await showDatePicker(
        context: context,
        initialDate: DateTime.parse(eventStartDate),
        firstDate: new DateTime(today.year),
        lastDate: new DateTime(today.year + 30));
    if ((selectedDate.isAfter(today) ||
        (selectedDate.day == today.day) &&
            (selectedDate.year == today.year) &&
            (selectedDate.month == today.month))) {
      setState(() {
        eventStartDate = convertDateToString(selectedDate);
        if (DateTime.parse(eventEndDate).isBefore(DateTime.parse(eventStartDate))) {
          eventEndDate = eventStartDate;
        }
      });
    } else {
      showPopupMessage("Start date cannot be before today's date.", context);
    }
  }

  Future selectEndDate(BuildContext context) async {
    DateTime selectedDate = await showDatePicker(
        context: context,
        initialDate: DateTime.parse(eventEndDate),
        firstDate: new DateTime(today.year),
        lastDate: new DateTime(today.year + 30));
    DateTime start = DateTime.parse(eventStartDate);
    if ((selectedDate.isAfter(start) ||
        (selectedDate.day == start.day) &&
            (selectedDate.year == start.year) &&
            (selectedDate.month == start.month))) {
      setState(() {
        eventEndDate = convertDateToString(selectedDate);
      });
    } else {
      showPopupMessage("End date cannot be before start date.", context);
    }
  }

  Future selectStartTime(BuildContext context) async {
    final TimeOfDay selectedTime = await showTimePicker(
        context: context,
        initialTime: new TimeOfDay(
            hour: getHour(eventStartTime), minute: getMinute(eventStartTime)));
    DateTime s = DateTime.parse(eventStartDate);
    if ((s.year == today.year &&
        s.month == today.month &&
        s.day == today.day) &&
        (selectedTime.hour < currentTime.hour ||
            (selectedTime.hour == currentTime.hour &&
                selectedTime.minute < currentTime.minute))) {
      showPopupMessage("Start time must be after current time.", context);
    } else if (eventStartDate.compareTo(eventEndDate) == 0) {
      if (selectedTime.hour > getHour(eventEndTime)) {
        setState(() {
          eventStartTime = convertTimeToString(selectedTime);
          eventEndTime = convertTimeToString(TimeOfDay(
              hour: getHour(eventStartTime) + 1, minute: getMinute(eventStartTime)));
        });
      } else if (selectedTime.hour == getHour(eventEndTime)) {
        if (selectedTime.minute >= getMinute(eventEndTime)) {
          setState(() {
            eventStartTime = convertTimeToString(selectedTime);
            eventEndTime = convertTimeToString(TimeOfDay(
                hour: getHour(eventStartTime) + 1, minute: getMinute(eventStartTime)));
          });
        } else {
          setState(() {
            eventStartTime = convertTimeToString(selectedTime);
          });
        }
      } else {
        setState(() {
          eventStartTime = convertTimeToString(selectedTime);
        });
      }
    } else {
      setState(() {
        eventStartTime = convertTimeToString(selectedTime);
      });
    }
  }

  Future selectEndTime(BuildContext context) async {
    final TimeOfDay selectedTime = await showTimePicker(
        context: context,
        initialTime:
        new TimeOfDay(hour: getHour(eventEndTime), minute: getMinute(eventEndTime)));
    if (eventStartDate.compareTo(eventEndDate) != 0) {
      setState(() {
        eventEndTime = convertTimeToString(selectedTime);
      });
    } else {
      if (selectedTime.hour >
          TimeOfDay(hour: getHour(eventStartTime), minute: 0).hour) {
        setState(() {
          eventEndTime = convertTimeToString(selectedTime);
        });
      } else if (selectedTime.hour ==
          TimeOfDay(hour: getHour(eventStartTime), minute: 0).hour &&
          selectedTime.minute >
              TimeOfDay(hour: 0, minute: getMinute(eventStartTime)).minute) {
        setState(() {
          eventEndTime = convertTimeToString(selectedTime);
        });
      } else {
        showPopupMessage("End time must be after the start time.", context);
      }
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
                  Container(
                      height: 10
                  ),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: <Widget>[
                        Text("Date and time", style: TextStyle(fontSize: 16)),
                      ]
                  ),
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
                                child: Text(eventStartDate))
                        ),
                        Icon(Icons.chevron_right),
                        ButtonTheme(
                            minWidth: 110,
                            height: 40,
                            buttonColor: Colors.white60,
                            child: RaisedButton(
                                onPressed: () {
                                  if (eventStartDate.compareTo('Pick start date') ==
                                      0) {
                                    showPopupMessage(
                                        "Pick a start date first!", context);
                                  } else {
                                    selectEndDate(context);
                                  }
                                },
                                child: Text(eventEndDate))
                        )
                      ]),
                  Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: <Widget>[
                        ButtonTheme(
                            minWidth: 110,
                            height: 40,
                            buttonColor: Colors.white60,
                            child: RaisedButton(
                                onPressed: () {
                                  selectStartTime(context);
                                },
                                child: Text(eventStartTime))
                        ),
                        Icon(Icons.chevron_right),
                        ButtonTheme(
                            minWidth: 110,
                            height: 40,
                            buttonColor: Colors.white60,
                            child: RaisedButton(
                                onPressed: () {
                                  selectEndTime(context);
                                },
                                child: Text(eventEndTime))
                        )
                      ]
                  ),
                  FutureBuilder(
                      future: categoriesTotal,
                      builder:
                          (BuildContext context, AsyncSnapshot snapshot) {
                        if (snapshot.hasData) {
                          List<Category> categories = snapshot.data;

                          return CategoryDropdown("Select a category",
                              categories, categoriesSelected,
                              callback: (category) =>
                                  selectCategory(category));
                        } else if (snapshot.hasError) {
                          return Text("Error: ${snapshot.error}");
                        } else {
                          return Center(child: CircularProgressIndicator());
                        }
                      }
                  ),
                  Container(
                      height: 10
                  ),
                  TextFormField(
                    controller: pollDurationController,
                    validator: validPollDuration,
                    onSaved: (String arg) {
                      pollDuration = arg;
                    },
                    decoration:
                    InputDecoration(labelText: "Poll duration (in minutes)"),
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
          )
      ),
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
              ]
          )
      ),
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
      eventCreator.putIfAbsent(Globals.username, () => widget.group.members[Globals.username]);
      int nextEventId = widget.group.nextEventId;

      Event event = new Event(
        eventName: this.eventName,
        categoryId: this.categoriesSelected.first.categoryId,
        categoryName: this.categoriesSelected.first.categoryName,
        createdDateTime: DateTime.parse(DateTime.now().toString().substring(0, 19)),
        eventStartDateTime: DateTime.parse(this.eventStartDate + ' ' + this.eventStartTime + ':00'),
        type: 0, // all events are non-recurring for now
        pollDuration: int.parse(this.pollDuration),
        pollPassPercent: int.parse(this.pollPassPercent),
        optedIn: widget.group.members,
        tentativeAlgorithmChoices: this.categoriesSelected.first.choices,
        votingNumbers: votingNumbers,
        selectedChoice: "null", // note: can't add empty string to database
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
      } else if (categoriesSelected.isEmpty){
        categoriesSelected.add(category);
      } else {
        categoriesSelected.clear();
        categoriesSelected.add(category);
      }
    });
  }
}

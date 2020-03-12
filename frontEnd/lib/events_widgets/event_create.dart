import 'dart:async';

import 'package:flutter/material.dart';
import 'package:frontEnd/imports/result_status.dart';
import 'package:frontEnd/utilities/utilities.dart';
import 'package:frontEnd/utilities/validator.dart';

import 'package:frontEnd/imports/groups_manager.dart';
import 'package:frontEnd/imports/globals.dart';
import 'package:frontEnd/models/category.dart';
import 'package:frontEnd/models/event.dart';
import 'package:frontEnd/widgets/category_popup_single.dart';

class CreateEvent extends StatefulWidget {
  @required
  CreateEvent({Key key}) : super(key: key);

  @override
  _CreateEventState createState() => _CreateEventState();
}

class _CreateEventState extends State<CreateEvent> {
  String eventName;
  String votingDuration;
  String considerDuration;
  final TextEditingController eventNameController = TextEditingController();
  final TextEditingController votingDurationController =
      TextEditingController();
  final TextEditingController considerDurationController =
      TextEditingController();

  bool autoValidate = false;
  final formKey = GlobalKey<FormState>();
  final List<Category> categorySelected = new List<Category>();
  DateTime currentDate = DateTime.now();
  TimeOfDay currentTime = TimeOfDay.now();

  // Using these strings both to display on the page and to eventually
  // make the API request for making the event
  String eventStartDate;
  String eventStartTime;
  String eventStartDateFormatted;
  String eventStartTimeFormatted;

  DateTime votingStart;
  DateTime votingEnd;

  final int textFieldLength = 4;
  bool willConsider = true;
  bool willVote = true;
  String considerButtonText;
  String voteButtonText;
  Timer timer;

  @override
  void dispose() {
    eventNameController.dispose();
    votingDurationController.dispose();
    considerDurationController.dispose();
    timer?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    // every 15 seconds refresh the calculated times
    timer = Timer.periodic(Duration(seconds: 15), (Timer t) => refreshTime());
    eventStartDate = convertDateToString(currentDate);
    eventStartTime = (currentTime.hour + 1).toString() + ":00";
    if ((currentTime.hour + 1) < 10) {
      eventStartTime = "0" + eventStartTime;
    } else if ((currentTime.hour + 1) > 23) {
      eventStartTime = "00:00";
      eventStartDate =
          convertDateToString(DateTime.now().add(Duration(days: 1)));
    }
    eventStartDateFormatted =
        Globals.formatter.format(currentDate).substring(0, 10);
    eventStartTimeFormatted = convertTimeToStringFormatted(
        new TimeOfDay(hour: getHour(eventStartTime), minute: 0));
    votingDurationController.text =
        Globals.currentGroup.defaultVotingDuration.toString();
    considerDurationController.text =
        Globals.currentGroup.defaultConsiderDuration.toString();
    considerDuration = considerDurationController.text;
    votingDuration = votingDurationController.text;
    considerButtonText = "Skip Consider";
    voteButtonText = "Skip Voting";
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      // allows for anywhere on the screen to be clicked to lose focus of a textfield
      onTap: () {
        hideKeyboard(context);
      },
      child: Scaffold(
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
                      textCapitalization: TextCapitalization.sentences,
                      onSaved: (String arg) {
                        eventName = arg;
                      },
                      decoration:
                          InputDecoration(labelText: "Enter an event name"),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Row(
                        mainAxisAlignment: MainAxisAlignment.start,
                        children: <Widget>[
                          Text("Start date and time for the event",
                              style: TextStyle(fontSize: 16)),
                        ]),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Row(
                        mainAxisAlignment: MainAxisAlignment.spaceAround,
                        children: <Widget>[
                          RaisedButton(
                              onPressed: () {
                                selectStartDate(context);
                              },
                              child: Text(eventStartDateFormatted)),
                          RaisedButton(
                              onPressed: () {
                                selectStartTime(context);
                              },
                              child: Text(eventStartTimeFormatted)),
                        ]),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    RaisedButton(
                      child: Text(getCategoryButtonMessage()),
                      onPressed: () {
                        showCategoriesPopup();
                      },
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .005),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Visibility(
                          visible: willConsider,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: considerDurationController,
                                keyboardType: TextInputType.number,
                                enabled: willConsider,
                                validator: validConsiderDuration,
                                maxLength: textFieldLength,
                                onChanged: (String arg) {
                                  // if already at max length and you keep typing, setState won't get called again
                                  if (!(arg.length == textFieldLength &&
                                      considerDuration.length ==
                                          textFieldLength)) {
                                    considerDuration = arg;
                                    setState(() {});
                                  }
                                },
                                decoration: InputDecoration(
                                    labelText: "Consider Duration (in minutes)",
                                    counterText: ""),
                              ),
                            ),
                          ),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .3,
                          child: RaisedButton(
                              child: Text(considerButtonText),
                              onPressed: () {
                                willConsider = !willConsider;
                                if (willConsider == false) {
                                  considerDurationController.text = "0";
                                  considerDuration =
                                      considerDurationController.text;
                                  considerButtonText = "Set Consider";
                                } else {
                                  considerDurationController.text = Globals
                                      .currentGroup.defaultConsiderDuration
                                      .toString();
                                  considerDuration =
                                      considerDurationController.text;
                                  considerButtonText = "Skip Consider";
                                }
                                setState(() {});
                              }),
                        ),
                        Padding(
                          // this is hacky af, but otherwise when textfield is hidden the button jumps to the right
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .000),
                        ),
                      ],
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Text(
                      calculateVotingStartDateTime(),
                      textAlign: TextAlign.center,
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.35),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Visibility(
                          visible: willVote,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: votingDurationController,
                                keyboardType: TextInputType.number,
                                validator: validVotingDuration,
                                maxLength: textFieldLength,
                                onChanged: (String arg) {
                                  if (!(arg.length == textFieldLength &&
                                      votingDuration.length ==
                                          textFieldLength)) {
                                    votingDuration = arg;
                                    setState(() {});
                                  }
                                },
                                decoration: InputDecoration(
                                    labelText: "Voting Duration (in minutes)",
                                    counterText: ""),
                              ),
                            ),
                          ),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .3,
                          child: RaisedButton(
                              child: Text(voteButtonText),
                              onPressed: () {
                                willVote = !willVote;
                                if (willVote == false) {
                                  votingDurationController.text = "0";
                                  votingDuration =
                                      votingDurationController.text;
                                  voteButtonText = "Set Voting";
                                } else {
                                  votingDurationController.text = Globals
                                      .currentGroup.defaultVotingDuration
                                      .toString();
                                  votingDuration =
                                      votingDurationController.text;
                                  voteButtonText = "Skip Voting";
                                }
                                setState(() {});
                              }),
                        ),
                        Padding(
                          // this is hacky af, but otherwise when textfield is hidden the button jumps to the right
                          padding: EdgeInsets.all(
                              MediaQuery.of(context).size.height * .000),
                        ),
                      ],
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Text(
                      calculateVotingEndDateTime(),
                      textAlign: TextAlign.center,
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.35),
                    ),
                  ],
                )
              ],
            )),
        bottomNavigationBar: BottomAppBar(
            color: Theme.of(context).scaffoldBackgroundColor,
            child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: <Widget>[
                  RaisedButton.icon(
                      onPressed: () {
                        if (categorySelected.isEmpty) {
                          showPopupMessage("Select a category.", context);
                        } else {
                          validateInput();
                        }
                      },
                      icon: Icon(Icons.add),
                      label: Text("Create"))
                ])),
      ),
    );
  }

  void refreshTime() {
    // used to get the most recent calculated times in case user is just idle on this screen
    setState(() {});
  }

  void showCategoriesPopup() {
    showDialog(
        context: context,
        child: CategoryPopupSingle(
          categorySelected,
          handlePopupClosed: categoryPopupClosed,
        )).then((value) {
      categoryPopupClosed();
    });
  }

  void categoryPopupClosed() {
    hideKeyboard(context);
    setState(() {});
  }

  String getCategoryButtonMessage() {
    if (categorySelected.isEmpty) {
      return "Select Category";
    } else {
      return categorySelected.first.categoryName;
    }
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
      eventStartDate = convertDateToString(selectedDate);
      eventStartDateFormatted =
          Globals.formatter.format(selectedDate).substring(0, 10);
      setState(() {});
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
      eventStartTime = convertTimeToString(selectedTime);
      eventStartTimeFormatted = convertTimeToStringFormatted(selectedTime);
      setState(() {});
    }
  }

  bool startTimeIsInvalid(DateTime startDate, TimeOfDay startTime) {
    /*
      Check if the start time is on the earlier in the day than the current
      time (in other words, same date as current day && earlier time means
      the start time is invalid)
     */
    currentDate = DateTime.now();
    currentTime = TimeOfDay.now();
    return ((startDate.year == currentDate.year &&
            startDate.month == currentDate.month &&
            startDate.day == currentDate.day)) &&
        ((startTime.hour < currentTime.hour) ||
            (startTime.hour == currentTime.hour &&
                startTime.minute < currentTime.minute));
  }

  String calculateVotingStartDateTime() {
    String displayVal;

    if (considerDuration == "0") {
      votingStart = DateTime.now();
      displayVal = Globals.formatter.format(votingStart);
    } else {
      if (validConsiderDuration(considerDuration) != null) {
        displayVal = Globals.formatter.format(votingStart);
      } else {
        votingStart = DateTime.now();
        votingStart =
            votingStart.add(Duration(minutes: int.parse(considerDuration)));
        displayVal = Globals.formatter.format(votingStart);
      }
    }
    return "Consider will end: $displayVal";
  }

  String calculateVotingEndDateTime() {
    String displayVal;

    if (validVotingDuration(votingDuration) != null) {
      displayVal = Globals.formatter.format(votingEnd);
    } else if (votingDuration == "0") {
      votingEnd = votingStart;
      displayVal = Globals.formatter.format(votingEnd);
    } else if (votingStart != null) {
      votingEnd = votingStart.add(Duration(minutes: int.parse(votingDuration)));
      displayVal = Globals.formatter.format(votingEnd);
    } else {
      displayVal = "";
    }
    return "Voting will end: $displayVal";
  }

  void validateInput() async {
    final form = formKey.currentState;
    if (form.validate()) {
      form.save();
      String errorMessage = "";
      if (this.votingStart.isAfter(
          DateTime.parse(this.eventStartDate + " " + this.eventStartTime))) {
        errorMessage +=
            "Consider duration invalid: Consider time will end after the event starts\n\n";
      }
      if (this.votingEnd.isAfter(
          DateTime.parse(this.eventStartDate + " " + this.eventStartTime))) {
        errorMessage +=
            "Voting duration invalid: Voting will end after the event starts";
      }
      if (errorMessage != "") {
        showErrorMessage("Error", errorMessage, context);
      } else {
        Event event = new Event(
            eventName: this.eventName.trim(),
            categoryId: this.categorySelected.first.categoryId,
            categoryName: this.categorySelected.first.categoryName,
            createdDateTime:
                DateTime.parse(DateTime.now().toString().substring(0, 19)),
            eventStartDateTime: DateTime.parse(
                this.eventStartDate + ' ' + this.eventStartTime + ':00'),
            votingDuration: int.parse(this.votingDuration),
            considerDuration: int.parse(this.considerDuration));

        showLoadingDialog(context, "Creating event...", true);
        ResultStatus result =
            await GroupsManager.newEvent(Globals.currentGroup.groupId, event);
        Navigator.of(context, rootNavigator: true).pop('dialog');

        if (result.success) {
          Navigator.of(context).pop();
        } else {
          showErrorMessage("Error", result.errorMessage, context);
        }
      }
    } else {
      setState(() => autoValidate = true);
    }
  }
}

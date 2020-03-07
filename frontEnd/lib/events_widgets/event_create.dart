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
  String rsvpDuration;
  final TextEditingController eventNameController = TextEditingController();
  final TextEditingController votingDurationController =
      TextEditingController();
  final TextEditingController rsvpDurationController = TextEditingController();

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

  final int textFieldLength = 5;
  bool willRsvp = true;
  String rsvpLabelText;
  String skipButtonText;

  @override
  void dispose() {
    eventNameController.dispose();
    votingDurationController.dispose();
    rsvpDurationController.dispose();
    super.dispose();
  }

  @override
  void initState() {
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
    rsvpDurationController.text =
        Globals.currentGroup.defaultRsvpDuration.toString();
    rsvpDuration = rsvpDurationController.text;
    votingDuration = votingDurationController.text;
    rsvpLabelText = "RSVP Duration (in minutes)";
    skipButtonText = "Skip RSVP";
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
                          Container(
                              width: 110,
                              height: 40,
                              child: RaisedButton(
                                  onPressed: () {
                                    selectStartDate(context);
                                  },
                                  child: Text(eventStartDateFormatted))),
                          Container(
                              width: 110,
                              height: 40,
                              child: RaisedButton(
                                  onPressed: () {
                                    selectStartTime(context);
                                  },
                                  child: Text(eventStartTimeFormatted))),
                        ]),
                    Container(height: 20),
                    RaisedButton(
                      child: Text(getCategoryButtonMessage()),
                      onPressed: () {
                        showCategoriesPopup();
                      },
                    ),
                    Container(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: <Widget>[
                        Container(
                          width: MediaQuery.of(context).size.width * .6,
                          child: TextFormField(
                            controller: rsvpDurationController,
                            keyboardType: TextInputType.number,
                            enabled: willRsvp,
                            validator: validRsvpDuration,
                            maxLength: textFieldLength,
                            onChanged: (String arg) {
                              // if already at max length and you keep typing, setState won't get called again
                              if (!(arg.length == textFieldLength &&
                                  rsvpDuration.length == textFieldLength)) {
                                rsvpDuration = arg;
                                setState(() {});
                              }
                            },
                            decoration:
                                InputDecoration(labelText: rsvpLabelText),
                          ),
                        ),
                        RaisedButton(
                            child: Text(skipButtonText),
                            onPressed: () {
                              willRsvp = !willRsvp;
                              if (willRsvp == false) {
                                rsvpDurationController.text = "0";
                                rsvpDuration = rsvpDurationController.text;
                                skipButtonText = "RSVP";
                                rsvpLabelText = "Skipping RSVP phase";
                              } else {
                                rsvpDurationController.text = Globals
                                    .currentGroup.defaultRsvpDuration
                                    .toString();
                                rsvpDuration = rsvpDurationController.text;
                                skipButtonText = "Skip RSVP";
                                rsvpLabelText = "RSVP Duration (in minutes)";
                              }
                              setState(() {});
                            })
                      ],
                    ),
                    TextFormField(
                      controller: votingDurationController,
                      keyboardType: TextInputType.number,
                      validator: validVotingDuration,
                      maxLength: textFieldLength,
                      onChanged: (String arg) {
                        if (!(arg.length == textFieldLength &&
                            votingDuration.length == textFieldLength)) {
                          votingDuration = arg;
                          setState(() {});
                        }
                      },
                      decoration: InputDecoration(
                          labelText: "Voting duration (in minutes)"),
                    ),
                    Text(
                      calculateVotingStartDateTime(),
                      style: TextStyle(
                          fontSize:
                              DefaultTextStyle.of(context).style.fontSize *
                                  0.35),
                    ),
                    Text(
                      calculateVotingEndDateTime(),
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
    // Check if the start time is on the earlier in the day than the current
    // time (in other words, same date as current day && earlier time means
    // the start time is invalid)
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
    if (rsvpDuration == "0") {
      votingStart = DateTime.now();
      return "Voting will start immediately.";
    } else {
      if (validRsvpDuration(rsvpDuration) != null) {
        return "RSVP duration invalid: " + validRsvpDuration(rsvpDuration);
      }
      votingStart = DateTime.now();
      votingStart = votingStart.add(Duration(minutes: int.parse(rsvpDuration)));
      return "Voting will start " + Globals.formatter.format(votingStart);
    }
  }

  String calculateVotingEndDateTime() {
    if (validVotingDuration(votingDuration) != null) {
      return "Voting duration invalid: " + validVotingDuration(votingDuration);
    }
    if (votingStart != null) {
      votingEnd = votingStart.add(Duration(minutes: int.parse(votingDuration)));
      return "Voting will end " + Globals.formatter.format(votingEnd);
    } else {
      return "";
    }
  }

  void validateInput() async {
    final form = formKey.currentState;
    if (form.validate()) {
      form.save();
      String errorMessage = "";
      if (this.votingStart.isAfter(
          DateTime.parse(this.eventStartDate + " " + this.eventStartTime))) {
        errorMessage +=
            "RSVP duration invalid: RSVP will end after the event starts\n";
      }
      if (this.votingEnd.isAfter(
          DateTime.parse(this.eventStartDate + " " + this.eventStartTime))) {
        errorMessage +=
            "Voting duration invalid: voting will end after the event starts";
      }
      if (errorMessage != "") {
        showErrorMessage("Error", errorMessage, context);
      } else {
        Event event = new Event(
            eventName: this.eventName,
            categoryId: this.categorySelected.first.categoryId,
            categoryName: this.categorySelected.first.categoryName,
            createdDateTime:
                DateTime.parse(DateTime.now().toString().substring(0, 19)),
            eventStartDateTime: DateTime.parse(
                this.eventStartDate + ' ' + this.eventStartTime + ':00'),
            votingDuration: int.parse(this.votingDuration),
            rsvpDuration: int.parse(this.rsvpDuration));

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

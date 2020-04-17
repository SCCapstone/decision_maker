import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/result_status.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

import 'package:front_end_pocket_poll/imports/groups_manager.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/event.dart';
import 'package:front_end_pocket_poll/widgets/category_popup_single.dart';

class CreateEvent extends StatefulWidget {
  @required
  CreateEvent({Key key}) : super(key: key);

  @override
  _CreateEventState createState() => _CreateEventState();
}

class _CreateEventState extends State<CreateEvent> {
  final TextEditingController eventNameController = TextEditingController();
  final TextEditingController votingDurationController =
      TextEditingController();
  final TextEditingController considerDurationController =
      TextEditingController();
  final TextEditingController hrController = new TextEditingController();
  final TextEditingController minController = new TextEditingController();
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  final List<Category> categorySelected = new List<Category>(); // only 1 item

  Timer timer;
  String eventName;
  String votingDuration;
  String considerDuration;
  String eventStartDateFormatted;
  String considerButtonText;
  String voteButtonText;
  bool autoValidate;
  bool willConsider;
  bool willVote;
  bool am;
  DateTime votingStart;
  DateTime votingEnd;
  DateTime proposedEventDateTime;
  int proposedHr;
  int proposedMin;
  int proposedYear;
  int proposedMonth;
  int proposedDay;
  FocusNode minuteInputFocus;

  @override
  void dispose() {
    this.eventNameController.dispose();
    this.votingDurationController.dispose();
    this.considerDurationController.dispose();
    this.hrController.dispose();
    this.minController.dispose();
    this.timer?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    this.minuteInputFocus = new FocusNode();
    this.autoValidate = false;
    this.willConsider = true;
    this.willVote = true;

    // if user is in PM or almost near PM show PM instead of AM initially
    this.am =
        (TimeOfDay.now().hour + 1 < 12) || (TimeOfDay.now().hour + 1 > 24);
    // provide default values initially, this is never shown to the user however
    DateTime initialTime = DateTime.now().add(Duration(hours: 1));
    this.proposedHr = convertMilitaryHrToMeridian(initialTime.hour);
    this.proposedMin = initialTime.minute;

    // every 15 seconds refresh the calculated times
    timer = Timer.periodic(Duration(seconds: 15), (Timer t) => refreshTime());

    // if close to the end of the current day, then add one to the default proposed day
    if ((TimeOfDay.now().hour + 1) > 23) {
      this.proposedEventDateTime = DateTime.now().add(Duration(days: 1));
    } else {
      this.proposedEventDateTime = DateTime.now();
    }
    this.proposedYear = this.proposedEventDateTime.year;
    this.proposedMonth = this.proposedEventDateTime.month;
    this.proposedDay = this.proposedEventDateTime.day;
    this.eventStartDateFormatted =
        Globals.dateFormatter.format(this.proposedEventDateTime);

    this.votingDurationController.text =
        Globals.currentGroup.defaultVotingDuration.toString();
    this.considerDurationController.text =
        Globals.currentGroup.defaultConsiderDuration.toString();
    this.considerDuration = this.considerDurationController.text;
    this.votingDuration = this.votingDurationController.text;
    if (this.considerDuration == "0") {
      this.considerButtonText = "Set Consider";
      this.willConsider = false;
    } else {
      this.considerButtonText = "Skip Consider";
    }
    if (this.votingDuration == "0") {
      this.voteButtonText = "Set Voting";
      this.willVote = false;
    } else {
      this.voteButtonText = "Skip Voting";
    }
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
        key: Key("event_create:scaffold"),
        body: Form(
            key: this.formKey,
            autovalidate: this.autoValidate,
            child: ListView(
              shrinkWrap: true,
              padding: EdgeInsets.fromLTRB(20, 0, 20, 0),
              children: <Widget>[
                Column(
                  children: <Widget>[
                    TextFormField(
                      maxLength: Globals.maxEventNameLength,
                      controller: this.eventNameController,
                      validator: validEventName,
                      textCapitalization: TextCapitalization.sentences,
                      onSaved: (String arg) {
                        this.eventName = arg.trim();
                      },
                      key: Key("event_create:event_name_input"),
                      decoration: InputDecoration(
                          labelText: "Enter an event name", counterText: ""),
                    ),
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: GestureDetector(
                            onTap: () {
                              showCategoriesPopup();
                            },
                            child: AutoSizeText(
                              getCategoryActionMessage(),
                              minFontSize: 10,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(fontSize: 24),
                            ),
                          ),
                        ),
                        IconButton(
                          icon: Icon(Icons.add_circle_outline),
                          iconSize: 40,
                          key: Key("event_create:add_category_button"),
                          onPressed: () {
                            showCategoriesPopup();
                          },
                        )
                      ],
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
                      children: <Widget>[
                        Expanded(
                          child: GestureDetector(
                            onTap: () {
                              selectStartDate();
                            },
                            child: AutoSizeText(
                              this.eventStartDateFormatted,
                              maxLines: 1,
                              minFontSize: 14,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(fontSize: 30),
                            ),
                          ),
                        ),
                        IconButton(
                          icon: Icon(Icons.date_range),
                          iconSize: 40,
                          onPressed: () {
                            selectStartDate();
                          },
                        )
                      ],
                    ),
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: TextFormField(
                            keyboardType: TextInputType.number,
                            controller: this.hrController,
                            textInputAction: TextInputAction.next,
                            validator: (input) {
                              return validMeridianHour(this.hrController.text);
                            },
                            key: Key("event_create:hour_input"),
                            textAlign: TextAlign.center,
                            enableInteractiveSelection: false,
                            maxLength: 2,
                            decoration: InputDecoration(
                                hintText: "HH", counterText: ""),
                            onFieldSubmitted: (form) {
                              // when user hits the next button on their keyboard, takes them to the minutes field
                              FocusScope.of(context)
                                  .requestFocus(this.minuteInputFocus);
                            },
                            onChanged: (val) {
                              // as the user types the hour is automatically saved, assuming it is valid
                              if (val.isNotEmpty) {
                                // this check needs to be here otherwise user can't backspace
                                try {
                                  int newVal = int.parse(val.trim());
                                  if (newVal > 12) {
                                    this.hrController.clear();
                                    this.hrController.text =
                                        this.proposedHr.toString();
                                    FocusScope.of(context)
                                        .requestFocus(this.minuteInputFocus);
                                  } else if (val.trim().length == 2) {
                                    if (newVal == 0) {
                                      // can't have 00 for hour in AM/PM format
                                      this.proposedHr = 1;
                                      this.hrController.text =
                                          this.proposedHr.toString();
                                    } else {
                                      this.proposedHr = int.parse(val.trim());
                                    }
                                    FocusScope.of(context)
                                        .requestFocus(this.minuteInputFocus);
                                  } else {
                                    this.proposedHr = int.parse(val.trim());
                                  }
                                } catch (e) {
                                  // in case somehow a non number gets inputted
                                  this.hrController.clear();
                                  this.hrController.text =
                                      this.proposedHr.toString();
                                  FocusScope.of(context)
                                      .requestFocus(this.minuteInputFocus);
                                }
                              }
                            },
                          ),
                        ),
                        Text(":"),
                        Expanded(
                          child: TextFormField(
                            textAlign: TextAlign.center,
                            keyboardType: TextInputType.number,
                            textInputAction: TextInputAction.done,
                            controller: this.minController,
                            focusNode: this.minuteInputFocus,
                            validator: (val) {
                              return validMinute(this.minController.text);
                            },
                            key: Key("event_create:minute_input"),
                            enableInteractiveSelection: false,
                            maxLength: 2,
                            decoration: InputDecoration(
                                hintText: "MM", counterText: ""),
                            onChanged: (val) {
                              // as user types the minute is automatically saved, assuming it is valid
                              if (val.isNotEmpty) {
                                // this needs to be here otherwise user can't backspace
                                try {
                                  int newVal = int.parse(val.trim());
                                  if (newVal > 59) {
                                    this.minController.clear();
                                    this.minController.text =
                                        this.proposedMin.toString();
                                    hideKeyboard(context);
                                  } else if (newVal.toString().length == 2 ||
                                      val.length == 2) {
                                    this.proposedMin = int.parse(val.trim());
                                    hideKeyboard(context);
                                  } else {
                                    this.proposedMin = int.parse(val.trim());
                                  }
                                } catch (e) {
                                  // in case somehow a non number gets inputted
                                  this.minController.clear();
                                  this.minController.text =
                                      this.proposedMin.toString();
                                  hideKeyboard(context);
                                }
                              }
                            },
                          ),
                        ),
                        Expanded(
                          child: RaisedButton(
                            child: Text((this.am) ? "AM" : "PM"),
                            onPressed: () {
                              this.am = !this.am;
                              setState(() {});
                            },
                          ),
                        ),
                        IconButton(
                          icon: Icon(Icons.access_time),
                          iconSize: 40,
                          onPressed: () {
                            selectStartTimePopup();
                          },
                        )
                      ],
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .005),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .005),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Visibility(
                          visible: this.willConsider,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: this.considerDurationController,
                                keyboardType: TextInputType.number,
                                enabled: this.willConsider,
                                validator: (value) {
                                  return validConsiderDuration(value, true);
                                },
                                key: Key("event_create:consider_input"),
                                maxLength: Globals.maxConsiderDigits,
                                onChanged: (String arg) {
                                  // if already at max length and you keep typing, setState won't get called again
                                  if (!(arg.length ==
                                          Globals.maxConsiderDigits &&
                                      this.considerDuration.length ==
                                          Globals.maxConsiderDigits)) {
                                    this.considerDuration = arg;
                                    setState(() {});
                                  }
                                },
                                decoration: InputDecoration(
                                    labelText: "Consider Duration (mins)",
                                    counterText: ""),
                              ),
                            ),
                          ),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .3,
                          child: RaisedButton(
                              child: AutoSizeText(
                                this.considerButtonText,
                                minFontSize: 10,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 15),
                              ),
                              key: Key("event_create:skip_consider_button"),
                              onPressed: () {
                                this.willConsider = !this.willConsider;
                                if (!this.willConsider) {
                                  this.considerDurationController.text = "0";
                                  this.considerDuration =
                                      this.considerDurationController.text;
                                  this.considerButtonText = "Set Consider";
                                } else {
                                  this.considerDurationController.text = Globals
                                      .currentGroup.defaultConsiderDuration
                                      .toString();
                                  this.considerDuration =
                                      this.considerDurationController.text;
                                  this.considerButtonText = "Skip Consider";
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
                    AutoSizeText(
                      calculateVotingStartDateTime(),
                      maxLines: 1,
                      minFontSize: 12,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 18),
                    ),
                    Padding(
                      padding: EdgeInsets.all(
                          MediaQuery.of(context).size.height * .01),
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Visibility(
                          visible: this.willVote,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: this.votingDurationController,
                                keyboardType: TextInputType.number,
                                validator: (value) {
                                  return validVotingDuration(value, true);
                                },
                                key: Key("event_create:vote_input"),
                                maxLength: Globals.maxVotingDigits,
                                onChanged: (String arg) {
                                  if (!(arg.length ==
                                          Globals.maxConsiderDigits &&
                                      this.votingDuration.length ==
                                          Globals.maxConsiderDigits)) {
                                    this.votingDuration = arg;
                                    setState(() {});
                                  }
                                },
                                decoration: InputDecoration(
                                    labelText: "Voting Duration (mins)",
                                    counterText: ""),
                              ),
                            ),
                          ),
                        ),
                        Container(
                          width: MediaQuery.of(context).size.width * .3,
                          child: RaisedButton(
                              child: AutoSizeText(
                                this.voteButtonText,
                                minFontSize: 10,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 15),
                              ),
                              key: Key("event_create:skip_vote_button"),
                              onPressed: () {
                                this.willVote = !this.willVote;
                                if (!this.willVote) {
                                  this.votingDurationController.text = "0";
                                  this.votingDuration =
                                      this.votingDurationController.text;
                                  this.voteButtonText = "Set Voting";
                                } else {
                                  this.votingDurationController.text = Globals
                                      .currentGroup.defaultVotingDuration
                                      .toString();
                                  this.votingDuration =
                                      this.votingDurationController.text;
                                  this.voteButtonText = "Skip Voting";
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
                    AutoSizeText(
                      calculateVotingEndDateTime(),
                      minFontSize: 10,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 18),
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
                        if (this.categorySelected.isEmpty) {
                          showErrorMessage(
                              "Error", "Select a category.", context);
                        } else {
                          attemptSave();
                        }
                      },
                      key: Key("event_create:save_event_button"),
                      icon: Icon(Icons.add),
                      label: Text("Create"))
                ])),
      ),
    );
  }

  /*
      Get the most recent calculated times in case user is just idle on this screen.

      Is called by a timer on a fixed interval.
   */
  void refreshTime() {
    setState(() {});
  }

  // display a popup for picking a category for this event
  void showCategoriesPopup() {
    showDialog(
        context: this.context,
        child: CategoryPopupSingle(
          this.categorySelected,
          handlePopupClosed: categoryPopupClosed,
        )).then((value) {
      categoryPopupClosed();
    });
  }

  // callback method for whenever popup is closed. Currently just closes keyboard if open
  void categoryPopupClosed() {
    hideKeyboard(this.context);
    setState(() {});
  }

  // if the user has selected a category, then display that name instead of default message
  String getCategoryActionMessage() {
    if (this.categorySelected.isEmpty) {
      return "Select Category";
    } else {
      return this.categorySelected.first.categoryName;
    }
  }

  /*
      Displays a GUI for selecting the month/day/year for the event start time
   */
  Future selectStartDate() async {
    hideKeyboard(this.context);
    DateTime selectedDate = await showDatePicker(
        context: this.context,
        initialDate: this.proposedEventDateTime,
        firstDate: DateTime.now().subtract(Duration(days: 1)),
        lastDate: DateTime(DateTime.now().year + 50));
    if (selectedDate != null) {
      // would be null if user clicks cancel or clicks outside of the popup
      this.proposedYear = selectedDate.year;
      this.proposedMonth = selectedDate.month;
      this.proposedDay = selectedDate.day;
      this.proposedEventDateTime =
          new DateTime(this.proposedYear, this.proposedMonth, this.proposedDay);
      this.eventStartDateFormatted = Globals.dateFormatter.format(selectedDate);
      setState(() {});
    }
  }

  /*
      Displays a GUI for selecting the hr/min for the event start time.

      The time picker requires an initial time. When the user first opens the page,
      we must provide some random initial time to the GUI.
      For now just using 1 hour past current time
   */
  Future selectStartTimePopup() async {
    hideKeyboard(this.context);
    DateTime initialTime = DateTime.now().add(Duration(hours: 1));
    final TimeOfDay selectedTime = await showTimePicker(
        context: this.context,
        initialTime: new TimeOfDay(
            hour: (this.proposedHr < 1)
                ? initialTime.hour
                : convertMeridianHrToMilitary(this.proposedHr, this.am),
            minute: (this.proposedMin < 1)
                ? initialTime.minute
                : this.proposedMin));
    if (selectedTime != null) {
      // would be null if user clicks cancel or clicks outside of the popup
      this.am = ((selectedTime.period == DayPeriod.am));
      this.proposedHr = selectedTime.hourOfPeriod;
      if (this.proposedHr == 0) {
        // if the user puts it to 12:00 AM the future will return 0 for the hour
        this.proposedHr = 12;
      }
      this.proposedMin = selectedTime.minute;
      setState(() {
        this.hrController.text = this.proposedHr.toString();
        this.minController.text = this.proposedMin.toString();
      });
    }
  }

  // calculate a string to show when the vote start time would be given current input
  String calculateVotingStartDateTime() {
    String displayVal;
    if (this.considerDuration == "0") {
      this.votingStart = DateTime.now();
      displayVal = Globals.formatterWithTime.format(this.votingStart);
    } else {
      if (validConsiderDuration(this.considerDuration, true) != null) {
        displayVal = Globals.formatterWithTime.format(this.votingStart);
      } else {
        this.votingStart = DateTime.now();
        this.votingStart = this
            .votingStart
            .add(Duration(minutes: int.parse(this.considerDuration)));
        displayVal = Globals.formatterWithTime.format(this.votingStart);
      }
    }
    return "Consider will end: $displayVal";
  }

  // calculate a string to show when the consider start time would be given current input
  String calculateVotingEndDateTime() {
    String displayVal;
    if (validVotingDuration(this.votingDuration, true) != null) {
      displayVal = Globals.formatterWithTime.format(this.votingEnd);
    } else if (this.votingDuration == "0") {
      this.votingEnd = this.votingStart;
      displayVal = Globals.formatterWithTime.format(this.votingEnd);
    } else if (this.votingStart != null) {
      this.votingEnd = this
          .votingStart
          .add(Duration(minutes: int.parse(this.votingDuration)));
      displayVal = Globals.formatterWithTime.format(this.votingEnd);
    } else {
      displayVal = "";
    }
    return "Voting will end: $displayVal";
  }

  /*
    Attempts to create the event if all input is valid.

    Once it is created the page is popped. Errors are highlighted if present.
   */
  void attemptSave() async {
    hideKeyboard(this.context);
    final form = this.formKey.currentState;
    if (form.validate()) {
      form.save();
      // convert the hour to military time
      int formattedHr = convertMeridianHrToMilitary(this.proposedHr, this.am);
      this.proposedEventDateTime = new DateTime(this.proposedYear,
          this.proposedMonth, this.proposedDay, formattedHr, this.proposedMin);

      String errorMessage = "";
      if (DateTime.now().isAfter(this.proposedEventDateTime)) {
        errorMessage += "Start time must be after current time\n\n";
      } else {
        if (this.votingStart.isAfter(this.proposedEventDateTime)) {
          errorMessage +=
              "Consider duration invalid: Consider time will end after the event starts\n\n";
        }
        if (this.votingEnd.isAfter(this.proposedEventDateTime)) {
          errorMessage +=
              "Voting duration invalid: Voting will end after the event starts";
        }
      }

      if (errorMessage != "") {
        showErrorMessage("Error", errorMessage.trim(), this.context);
      } else {
        Event event = new Event(
            eventName: this.eventName.trim(),
            categoryId: this.categorySelected.first.categoryId,
            categoryName: this.categorySelected.first.categoryName,
            createdDateTime:
                DateTime.parse(DateTime.now().toString().substring(0, 19)),
            eventStartDateTime: this.proposedEventDateTime,
            votingDuration: int.parse(this.votingDuration),
            considerDuration: int.parse(this.considerDuration));

        showLoadingDialog(context, "Creating event...", true);
        ResultStatus result =
            await GroupsManager.newEvent(Globals.currentGroup.groupId, event);
        Navigator.of(this.context, rootNavigator: true).pop('dialog');

        if (result.success) {
          Navigator.of(this.context).pop();
        } else {
          showErrorMessage("Error", result.errorMessage, this.context);
        }
      }
    } else {
      setState(() => this.autoValidate = true);
    }
  }
}

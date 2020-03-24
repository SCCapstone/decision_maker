import 'dart:async';

import 'package:auto_size_text/auto_size_text.dart';
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
  final TextEditingController eventNameController = TextEditingController();
  final TextEditingController votingDurationController =
      TextEditingController();
  final TextEditingController considerDurationController =
      TextEditingController();
  final TextEditingController hrController = new TextEditingController();
  final TextEditingController minController = new TextEditingController();
  final int textFieldLength = 4;
  final formKey = GlobalKey<FormState>();
  final List<Category> categorySelected = new List<Category>();

  Timer timer;
  String eventName;
  String votingDuration;
  String considerDuration;
  String eventStartDateFormatted;
  String considerButtonText;
  String voteButtonText;
  bool autoValidate = false;
  bool willConsider = true;
  bool willVote = true;
  bool am;
  bool timeSet;
  DateTime votingStart;
  DateTime votingEnd;
  DateTime proposedEventDateTime;
  int proposedHr;
  int proposedMin;
  int proposedYear;
  int proposedMonth;
  int proposedDay;
  FocusNode minuteFocus = new FocusNode();

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
    // if user is in PM or almost near PM show PM instead of AM
    this.am = (TimeOfDay.now().hour + 1 < 12);
    // provide default values initially, this is never shown to the user however
    this.timeSet = false;
    DateTime initialTime = DateTime.now().add(Duration(hours: 1));
    this.proposedHr = initialTime.hour;
    this.proposedMin = initialTime.minute;

    // every 15 seconds refresh the calculated times
    timer = Timer.periodic(Duration(seconds: 15), (Timer t) => refreshTime());

    // if close to the end of the current day, then add one to the default proposed day
    if ((TimeOfDay.now().hour + 1) > 23) {
      proposedEventDateTime = DateTime.now().add(Duration(days: 1));
    } else {
      proposedEventDateTime = DateTime.now();
    }
    proposedYear = proposedEventDateTime.year;
    proposedMonth = proposedEventDateTime.month;
    proposedDay = proposedEventDateTime.day;
    eventStartDateFormatted =
        Globals.formatter.format(proposedEventDateTime).substring(0, 10);

    votingDurationController.text =
        Globals.currentGroup.defaultVotingDuration.toString();
    considerDurationController.text =
        Globals.currentGroup.defaultConsiderDuration.toString();
    considerDuration = considerDurationController.text;
    votingDuration = votingDurationController.text;
    if (considerDuration == "0") {
      considerButtonText = "Set Consider";
      willConsider = false;
    } else {
      considerButtonText = "Skip Consider";
    }
    if (votingDuration == "0") {
      voteButtonText = "Set Voting";
      willVote = false;
    } else {
      voteButtonText = "Skip Voting";
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
                      maxLength: Globals.maxEventNameLength,
                      controller: eventNameController,
                      validator: validEventName,
                      textCapitalization: TextCapitalization.sentences,
                      onSaved: (String arg) {
                        eventName = arg;
                      },
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
                              getCategoryButtonMessage(),
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
                              eventStartDateFormatted,
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
                            controller: hrController,
                            textInputAction: TextInputAction.next,
                            validator: (input) {
                              return validMeridianHour(hrController.text);
                            },
                            textAlign: TextAlign.center,
                            enableInteractiveSelection: false,
                            maxLength: 2,
                            decoration: InputDecoration(
                                hintText: "HH", counterText: ""),
                            onFieldSubmitted: (form) {
                              // when user hits the next button on their keyboard, takes them to the minutes field
                              FocusScope.of(context).requestFocus(minuteFocus);
                            },
                            onChanged: (val) {
                              // as user types the hour is automatically saved, assuming it is valid
                              if (val.isNotEmpty) {
                                // this check needs to be here otherwise user can't backspace
                                try {
                                  int newVal = int.parse(val.trim());
                                  if (newVal > 12) {
                                    hrController.clear();
                                    hrController.text = proposedHr.toString();
                                    FocusScope.of(context)
                                        .requestFocus(minuteFocus);
                                  } else if (val.trim().length == 2) {
                                    if (newVal == 0) {
                                      // can't have 00 for hour in AM/PM format
                                      proposedHr = 1;
                                      hrController.text = proposedHr.toString();
                                    } else {
                                      proposedHr = int.parse(val.trim());
                                    }
                                    FocusScope.of(context)
                                        .requestFocus(minuteFocus);
                                  } else {
                                    this.timeSet = true;
                                    proposedHr = int.parse(val.trim());
                                  }
                                } catch (e) {
                                  // in case somehow a non number gets inputted
                                  hrController.clear();
                                  hrController.text = proposedHr.toString();
                                  FocusScope.of(context)
                                      .requestFocus(minuteFocus);
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
                            controller: minController,
                            focusNode: minuteFocus,
                            validator: (val) {
                              return validMinute(minController.text);
                            },
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
                                    minController.clear();
                                    minController.text = proposedMin.toString();
                                    hideKeyboard(context);
                                  } else if (newVal.toString().length == 2 ||
                                      val.length == 2) {
                                    proposedMin = int.parse(val.trim());
                                    hideKeyboard(context);
                                  } else {
                                    this.timeSet = true;
                                    proposedMin = int.parse(val.trim());
                                  }
                                } catch (e) {
                                  // in case somehow a non number gets inputted
                                  minController.clear();
                                  minController.text = proposedMin.toString();
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
                          visible: willConsider,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: considerDurationController,
                                keyboardType: TextInputType.number,
                                enabled: willConsider,
                                validator: (value) {
                                  return validConsiderDuration(value, true);
                                },
                                maxLength: Globals.maxConsiderDigits,
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
                                considerButtonText,
                                minFontSize: 10,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 15),
                              ),
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
                          visible: willVote,
                          child: Expanded(
                            child: Container(
                              child: TextFormField(
                                controller: votingDurationController,
                                keyboardType: TextInputType.number,
                                validator: (value) {
                                  return validVotingDuration(value, true);
                                },
                                maxLength: Globals.maxVotingDigits,
                                onChanged: (String arg) {
                                  if (!(arg.length == textFieldLength &&
                                      votingDuration.length ==
                                          textFieldLength)) {
                                    votingDuration = arg;
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
                                voteButtonText,
                                minFontSize: 10,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(fontSize: 15),
                              ),
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
                        if (categorySelected.isEmpty) {
                          showErrorMessage(
                              "Error", "Select a category.", context);
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

  Future selectStartDate() async {
    /*
      Displays a GUI for selecting the month/day/year for the event start time
     */
    hideKeyboard(context);
    DateTime selectedDate = await showDatePicker(
        context: context,
        initialDate: proposedEventDateTime,
        firstDate: DateTime.now().subtract(Duration(days: 1)),
        lastDate: DateTime(DateTime.now().year + 50));
    if (selectedDate != null) {
      // would be null if user clicks cancel or clicks outside of the popup
      this.proposedYear = selectedDate.year;
      this.proposedMonth = selectedDate.month;
      this.proposedDay = selectedDate.day;
      this.proposedEventDateTime =
          new DateTime(this.proposedYear, this.proposedMonth, this.proposedDay);
      eventStartDateFormatted =
          Globals.formatter.format(selectedDate).substring(0, 10);
      setState(() {});
    }
  }

  Future selectStartTimePopup() async {
    /*
      Displays a GUI for selecting the hr/min for the event start time.
      The time picker requires an initial time. When the user first opens the page,
      there is no initial time (it's set to a sentinel value of -1) so we must provide
      some random initial time to the GUI. For now just using 1 hour past current time
     */
    hideKeyboard(context);
    DateTime initialTime = DateTime.now().add(Duration(hours: 1));
    final TimeOfDay selectedTime = await showTimePicker(
        context: context,
        initialTime: new TimeOfDay(
            hour: (this.proposedHr < 1)
                ? initialTime.hour
                : convertMeridianHrToMilitary(proposedHr, am),
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

  String calculateVotingStartDateTime() {
    String displayVal;

    if (considerDuration == "0") {
      votingStart = DateTime.now();
      displayVal = Globals.formatter.format(votingStart);
    } else {
      if (validConsiderDuration(considerDuration, true) != null) {
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

    if (validVotingDuration(votingDuration, true) != null) {
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
    if (form.validate() && this.timeSet) {
      form.save();
      // convert the hour to military time
      int formattedHr = convertMeridianHrToMilitary(proposedHr, am);
      proposedEventDateTime = new DateTime(this.proposedYear,
          this.proposedMonth, this.proposedDay, formattedHr, proposedMin);

      String errorMessage = "";
      if (DateTime.now().isAfter(proposedEventDateTime)) {
        errorMessage += "Start time must be after current time\n\n";
      } else {
        if (this.votingStart.isAfter(proposedEventDateTime)) {
          errorMessage +=
              "Consider duration invalid: Consider time will end after the event starts\n\n";
        }
        if (this.votingEnd.isAfter(proposedEventDateTime)) {
          errorMessage +=
              "Voting duration invalid: Voting will end after the event starts";
        }
      }

      if (errorMessage != "") {
        showErrorMessage("Error", errorMessage.trim(), context);
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

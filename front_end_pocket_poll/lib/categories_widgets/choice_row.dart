import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';

class ChoiceRow extends StatefulWidget {
  final int choiceNumber;
  final bool isOwner;
  final TextEditingController labelController;
  final TextEditingController rateController;
  final Function deleteChoice;
  final VoidCallback checkForChange;
  final FocusNode focusNode;
  final String originalRating;
  final String originalLabel;
  final bool isNewChoice;
  final bool displayLabelHelpText;
  final bool displayRateHelpText;
  final Map<String, String> unratedChoices; // only used if not the owner

  ChoiceRow(this.choiceNumber, this.isOwner, this.labelController,
      this.rateController,
      {Key key,
      this.deleteChoice,
      this.focusNode,
      this.checkForChange,
      this.isNewChoice,
      this.displayLabelHelpText,
      this.displayRateHelpText,
      this.originalLabel,
      this.unratedChoices,
      this.originalRating})
      : super(key: key);

  // opens up the keyboard to this specific widget
  void requestFocus(BuildContext context) {
    FocusScope.of(context).requestFocus(focusNode);
  }

  @override
  _ChoiceRowState createState() => new _ChoiceRowState();
}

class _ChoiceRowState extends State<ChoiceRow> {
  FocusNode ratingsFocus;
  int rating;
  bool changed;
  bool choiceNotRated;
  String labelHelpText;
  final String ratingRegex =
      "[${Globals.minChoiceRating}-${Globals.maxChoiceRating}]";

  @override
  void initState() {
    print(widget.key.toString() + widget.choiceNumber.toString() + widget.focusNode.toString());
    this.changed = false;
    // used in editing a category as the owner
    if (widget.displayLabelHelpText) {
      if (widget.isNewChoice) {
        this.labelHelpText = "(New)";
      } else {
        this.labelHelpText = "(Modified)";
      }
    } else {
      // if you aren't the owner, you shouldn't ever get text underneath the label text input
      this.labelHelpText = " ";
    }

    // used for when updating ratings from an event
    if (widget.displayRateHelpText) {
      this.labelHelpText = "(Modified)";
    }
    // do this check since choice row can get destroyed at any point, if destroyed still want to show if new/modified
    if (widget.labelController.text.toString() != widget.originalLabel ||
        widget.rateController.text.toString() != widget.originalRating) {
      this.changed = true;
    }

    if (widget.unratedChoices != null &&
        widget.unratedChoices.containsKey(widget.originalLabel)) {
      this.choiceNotRated = true;
    } else {
      this.choiceNotRated = false;
    }

    this.rating = int.parse(widget.rateController.text);
    this.ratingsFocus = new FocusNode();
    this.ratingsFocus.addListener(() {
      // highlight the rating any time the rating field gains focus
      widget.rateController.value = widget.rateController.value.copyWith(
        selection: TextSelection(baseOffset: 0, extentOffset: 1),
        composing: TextRange.empty,
      );
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      children: <Widget>[
        Flexible(
          child: TextFormField(
            onTap: () {
              setState(() {
                this.choiceNotRated = false;
              });
            },
            onChanged: (val) {
              widget.checkForChange();
              if (val == widget.originalLabel) {
                setState(() {
                  this.changed = false;
                });
              } else {
                setState(() {
                  this.changed = true;
                });
              }
            },
            validator: validChoiceName,
            maxLength: Globals.maxChoiceNameLength,
            readOnly: !widget.isOwner,
            textCapitalization: TextCapitalization.sentences,
            focusNode: widget.focusNode,
            controller: widget.labelController,
            textInputAction: TextInputAction.next,
            decoration: InputDecoration(
                labelStyle: TextStyle(fontSize: 15),
                labelText: "Choice",
                counterText: "",
                helperText: (widget.displayLabelHelpText && this.changed)
                    ? this.labelHelpText
                    : " "),
            key: Key("choice_row:choice_name_input:${widget.choiceNumber}"),
            onFieldSubmitted: (val) {
              FocusScope.of(context).requestFocus(this.ratingsFocus);
            },
          ),
        ),
        Container(
          width: MediaQuery.of(context).size.width * .23,
          child: TextFormField(
            onChanged: (val) {
              if (val.isEmpty) {
                // if the user puts in an invalid number, it will be empty, so just use the last rating saved
                widget.rateController.text = this.rating.toString();
                hideKeyboard(context);
              } else {
                this.rating = int.parse(val);
                hideKeyboard(context);
                widget.checkForChange();
              }
              if (this.rating.toString() == widget.originalRating) {
                setState(() {
                  this.changed = false;
                });
              } else {
                setState(() {
                  this.changed = true;
                });
              }
            },
            onTap: () {
              setState(() {
                this.choiceNotRated = false;
              });
            },
            validator: validChoiceRating,
            focusNode: this.ratingsFocus,
            controller: widget.rateController,
            maxLength: Globals.maxChoiceRatingDigits,
            maxLines: 1,
            inputFormatters: [
              WhitelistingTextInputFormatter(RegExp(this.ratingRegex))
            ],
            enableInteractiveSelection: false,
            keyboardType: TextInputType.number,
            key: Key("choice_row:rating_input:${widget.choiceNumber}"),
            decoration: InputDecoration(
                labelStyle: TextStyle(fontSize: 15),
                labelText:
                    "Rating (${Globals.minChoiceRating}-${Globals.maxChoiceRating})",
                counterText: "",
                helperText: (widget.displayRateHelpText && this.changed)
                    ? this.labelHelpText
                    : " "),
          ),
        ),
        Visibility(
          // if user is not the category owner, they cannot delete choices
          visible: widget.isOwner,
          child: IconButton(
            color: Colors.red,
            icon: Icon(Icons.cancel),
            tooltip: "Delete Choice",
            key: Key("choice_row:delete_button:${widget.choiceNumber}"),
            onPressed: () {
              widget.deleteChoice(widget);
            },
          ),
        ),
        Visibility(
          // if user is not the category owner, they cannot delete choices
          visible: this.choiceNotRated,
          child: IconButton(
            color: Colors.greenAccent,
            icon: Icon(Icons.priority_high),
            tooltip: "Unrated Choice",
            key: Key("choice_row:new_choice_button:${widget.choiceNumber}"),
            onPressed: () {
              setState(() {
                this.choiceNotRated = false;
              });
            },
          ),
        )
      ],
    );
  }
}

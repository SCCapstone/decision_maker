import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/utilities/utilities.dart';
import 'package:front_end_pocket_poll/utilities/validator.dart';
import 'package:numberpicker/numberpicker.dart';

class ChoiceRow extends StatefulWidget {
  final String choiceNumber;
  final bool isOwner;
  final TextEditingController labelController;
  final TextEditingController rateController;
  final Function deleteChoice;
  final VoidCallback checkForChange;
  final FocusNode focusNode;

  ChoiceRow(this.choiceNumber, this.isOwner, this.labelController,
      this.rateController,
      {this.deleteChoice, this.focusNode, this.checkForChange});

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

  @override
  void initState() {
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
            onChanged: (val) {
              widget.checkForChange();
            },
            validator: validChoiceName,
            maxLength: Globals.maxChoiceNameLength,
            enabled: widget.isOwner,
            textCapitalization: TextCapitalization.sentences,
            focusNode: widget.focusNode,
            controller: widget.labelController,
            textInputAction: TextInputAction.next,
            decoration: InputDecoration(
                labelText: "Choice", counterText: "", helperText: " "),
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
            },
            validator: validChoiceRating,
            focusNode: this.ratingsFocus,
            controller: widget.rateController,
            maxLength: 1,
            maxLines: 1,
            inputFormatters: [WhitelistingTextInputFormatter(RegExp("[0-5]"))],
            enableInteractiveSelection: false,
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
                labelText: "Rating (0-5)", counterText: "", helperText: " "),
          ),
        ),
//        RaisedButton.icon(
//          icon: Icon(Icons.edit),
//          label: Text("Rating: " + widget.rateController.text),
//          key: Key("choice_row:ratings_button:${widget.choiceNumber}"),
//          onPressed: () {
//            displayRateSelector();
//          },
//        ),
        Visibility(
          // if user is not the category owner, they cannot delete choices
          visible: widget.isOwner,
          child: IconButton(
            color: Colors.red,
            icon: Icon(Icons.cancel),
            key: Key("choice_row:delete_button:${widget.choiceNumber}"),
            onPressed: () {
              widget.deleteChoice(widget);
            },
          ),
        )
      ],
    );
  }

  // displays a popup for allowing the users to pick a rating for the given choice
  void displayRateSelector() {
    showDialog<int>(
        context: this.context,
        builder: (BuildContext context) {
          return NumberPickerDialog.integer(
            minValue: 0,
            maxValue: 5,
            title: Text("Rate this choice:"),
            initialIntegerValue: int.parse(widget.rateController.text),
          );
        }).then((int value) {
      if (value != null) {
        setState(() => widget.rateController.text = value.toString());
        widget.checkForChange();
      }
    });
  }
}

import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';

class Sorter {
  // sorts choice rows based on a given sort value. Empty choices always put on top
  static void sortChoiceRows(List<ChoiceRow> choiceRows, int sortVal) {
    if (sortVal == Globals.defaultChoiceSort) {
      // sort by choice id. we want the highest choice id on the top
      choiceRows.sort((a, b) => b.choiceNumber.compareTo(a.choiceNumber));
    } else if (sortVal == Globals.alphabeticalSort) {
      choiceRows.sort((a, b) => a.labelController.text
          .toString()
          .toLowerCase()
          .compareTo(b.labelController.text.toString().toLowerCase()));
    } else if (sortVal == Globals.alphabeticalReverseSort) {
      choiceRows.sort((a, b) => b.labelController.text
          .toString()
          .toLowerCase()
          .compareTo(a.labelController.text.toString().toLowerCase()));
    } else if (sortVal == Globals.choiceRatingAscending) {
      // smallest rating at top of the list
      choiceRows.sort((a, b) => a.rateController.text
          .toString()
          .compareTo(b.rateController.text.toString()));
    } else if (sortVal == Globals.choiceRatingDescending) {
      // smallest rating at top of the list
      choiceRows.sort((a, b) => b.rateController.text
          .toString()
          .compareTo(a.rateController.text.toString()));
    }
    for (ChoiceRow choiceRow in choiceRows) {
      if (choiceRow.labelController.text.isEmpty) {
        // we want all empty labeled choice rows to always be at the top
        choiceRows.remove(choiceRow);
        choiceRows.insert(0, choiceRow);
      }
    }
  }
}

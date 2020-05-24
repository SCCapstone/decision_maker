import 'package:front_end_pocket_poll/categories_widgets/choice_row.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:front_end_pocket_poll/models/category.dart';
import 'package:front_end_pocket_poll/models/group_interface.dart';
import 'package:front_end_pocket_poll/models/user_group.dart';

class Sorter {
  // sorts choice rows based on a given sort value. Empty choices always put on top
  static void sortChoiceRows(List<ChoiceRow> choiceRows, int sortVal) {
    if (sortVal == Globals.alphabeticalSort) {
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

  // sorts a list of groups by date modified (ascending)
  static void sortGroupRowsByDateNewest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(b.lastActivity)
        .compareTo(DateTime.parse(a.lastActivity)));
  }

  // sorts a list of groups by date modified (descending)
  static void sortGroupRowsByDateOldest(List<UserGroup> groups) {
    groups.sort((a, b) => DateTime.parse(a.lastActivity)
        .compareTo(DateTime.parse(b.lastActivity)));
  }

  // sorts a list of groups alphabetically (ascending)
  static void sortGroupRowsByAlphaAscending(List<GroupInterface> groups) {
    groups.sort((a, b) => a
        .getGroupName()
        .toUpperCase()
        .compareTo(b.getGroupName().toUpperCase()));
  }

  // sorts a list of groups alphabetically (descending)
  static void sortGroupRowsByAlphaDescending(List<GroupInterface> groups) {
    groups.sort((a, b) => b
        .getGroupName()
        .toUpperCase()
        .compareTo(a.getGroupName().toUpperCase()));
  }

  // sorts a list of categories alphabetically (ascending)
  static void sortCategoriesByAlphaAscending(List<Category> categories) {
    categories.sort((a, b) =>
        a.categoryName.toLowerCase().compareTo(b.categoryName.toLowerCase()));
  }

  // sorts a list of categories alphabetically (descending)
  static void sortCategoriesByAlphaDescending(List<Category> categories) {
    categories.sort((a, b) =>
        b.categoryName.toLowerCase().compareTo(a.categoryName.toLowerCase()));
  }
}

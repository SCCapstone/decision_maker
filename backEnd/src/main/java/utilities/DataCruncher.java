package utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import models.Category;
import models.Group;
import models.User;
import utilities.ErrorDescriptor;

public class DataCruncher {

  private LinkedHashMap<String, Map<Integer, Integer>> controlRatingCountsByChoice;
  private LinkedHashMap<String, Map<Integer, Integer>> allRatingCountsByChoice;

  private Group group;
  private Category category;
  private Map<String, User> allUsers;
  private Float k;
  private int totalDifferentThanControl;
  private int totalRoundsDiffering;
  private int numberDifferingBy1;
  private int numberDifferingBy2;
  private int numberDifferingBy3;

  public DataCruncher(final Group group, final Category category,
      final Map<String, User> allUsers, final Float k) {
    this.group = group;
    this.category = category;
    this.allUsers = allUsers;
    this.k = k;
    this.totalDifferentThanControl = 0;
    this.totalRoundsDiffering = 0;

    this.numberDifferingBy1 = 0;
    this.numberDifferingBy2 = 0;
    this.numberDifferingBy3 = 0;

    //set up the default mappings
    this.resetControlRatingsCountsByChoice();
    this.resetAllRatingsCountsByChoice();
  }

  public void crunch() {
    //first setup the control values
    List<Map<String, Integer>> allCategoryChoiceRatings = new ArrayList<>();
    for (String username : this.group.getMembers().keySet()) {
      try {
        final User user = this.allUsers.get(username);

        final Map<String, Integer> categoryChoiceRatings = user.getCategoryRatings()
            .get(this.category.getCategoryId());

        allCategoryChoiceRatings.add(categoryChoiceRatings);
      } catch (final Exception e) {
        System.out.println(new ErrorDescriptor<>(username, "blah", e));
      }
    }

    //add in the ratings!
    this.clearAndSetControlRatingCountsByChoice(allCategoryChoiceRatings);

    //Now we need to get the random users or at least the random categoryChoiceRating maps
    for (int i = 0; i < Math.ceil(this.k * group.getMembers().size()); i++) {
      allCategoryChoiceRatings.add(this.getRandomUserChoiceRatings());
    }

    this.clearAndSetAllRatingCountsByChoice(allCategoryChoiceRatings);

    this.updateTotalDifferent();
  }

  public void updateTotalDifferent() {
    List<String> control = this.getTopThreeControlChoices(true);
    List<String> all = this.getTopThreeAllChoices(true);

    int numberDiffering = this.getNumberOfChanges(control, all);

    this.totalDifferentThanControl += numberDiffering;

    if (numberDiffering == 1) {
      this.numberDifferingBy1 += 1;
    } else if (numberDiffering == 2) {
      this.numberDifferingBy2 += 1;
    } else if (numberDiffering == 3) {
      this.numberDifferingBy3 += 1;
    }

    if(!all.containsAll(control)) {
      this.totalRoundsDiffering += 1;
    }
  }

  //we put one additional vote into ratings 1 through 5 (so everything besides 0)
  //we then roll a random number and find what rating that correlates to probabilistically
  public Map<String, Integer> getRandomUserChoiceRatings() {
    Map<String, Integer> categoryChoiceRatings = new HashMap<>();
    Random random = new Random();

    for (String choiceId : this.controlRatingCountsByChoice.keySet()) {
      int randInt = random.nextInt(100); // [0, 100)
      int runningProb = 0;
      int totalVotes =
          this.group.getMembers().size() + 5; //we added one vote to each of the choices 1 through 5

      Map<Integer, Integer> ratingCounts = this.controlRatingCountsByChoice.get(choiceId);

      for (int rating = 0; rating < 6; rating++) {
        int controlCount = ratingCounts.get(rating);
        int topOfRange = runningProb;

        //we have to figure out how much percentage this choice had
        if (rating == 0) {
          // we handle zero differently just because
          topOfRange += Math.floor(controlCount * 100.0 / totalVotes);
        } else {
          topOfRange += Math.floor((controlCount + 1) * 100.0 / totalVotes);
        }

        if (randInt < topOfRange) {
          categoryChoiceRatings.putIfAbsent(choiceId, rating);
          break;
        }
        runningProb = topOfRange + 1;
      }

      if (!categoryChoiceRatings.containsKey(choiceId)) {
        System.out.println("didn't contain " + choiceId);
        categoryChoiceRatings.put(choiceId, 3);
      }
    }

    return categoryChoiceRatings;
  }

  private void clearAndSetControlRatingCountsByChoice(
      List<Map<String, Integer>> allCategoryChoiceRatings) {
    this.resetControlRatingsCountsByChoice();

    for (Map<String, Integer> categoryChoiceRatings : allCategoryChoiceRatings) {
      for (String choiceId : this.controlRatingCountsByChoice.keySet()) {
        Integer selectedRating;
        if (categoryChoiceRatings != null && categoryChoiceRatings.containsKey(choiceId)) {
          //increment the old count of this rating value by 1
          selectedRating = categoryChoiceRatings.get(choiceId);
        } else {
          selectedRating = 3;
        }

        //increment the old count of this rating value by 1
        final Integer newCount =
            this.controlRatingCountsByChoice.get(choiceId).get(selectedRating) + 1;

        this.controlRatingCountsByChoice.get(choiceId).replace(selectedRating, newCount);
      }
    }
  }

  private void clearAndSetAllRatingCountsByChoice(
      List<Map<String, Integer>> allCategoryChoiceRatings) {
    this.resetAllRatingsCountsByChoice();

    for (Map<String, Integer> categoryChoiceRatings : allCategoryChoiceRatings) {
      for (String choiceId : this.allRatingCountsByChoice.keySet()) {
        Integer selectedRating;
        if (categoryChoiceRatings != null && categoryChoiceRatings.containsKey(choiceId)) {
          //increment the old count of this rating value by 1
          selectedRating = categoryChoiceRatings.get(choiceId);
        } else {
          selectedRating = 3;
        }

        //increment the old count of this rating value by 1
        final Integer newCount =
            this.allRatingCountsByChoice.get(choiceId).get(selectedRating) + 1;

        this.allRatingCountsByChoice.get(choiceId).replace(selectedRating, newCount);
      }
    }
  }

  private void resetControlRatingsCountsByChoice() {
    this.controlRatingCountsByChoice = new LinkedHashMap<>();
    for (String choiceId : category.getChoices().keySet()) {
      this.controlRatingCountsByChoice
          .putIfAbsent(choiceId, this.getEmptyRatingCountsByChoiceMap());
    }
  }

  private void resetAllRatingsCountsByChoice() {
    this.allRatingCountsByChoice = new LinkedHashMap<>();
    for (String choiceId : category.getChoices().keySet()) {
      this.allRatingCountsByChoice
          .putIfAbsent(choiceId, this.getEmptyRatingCountsByChoiceMap());
    }
  }

  private Map<Integer, Integer> getEmptyRatingCountsByChoiceMap() {
    return new HashMap<Integer, Integer>() {{
      put(0, 0);
      put(1, 0);
      put(2, 0);
      put(3, 0);
      put(4, 0);
      put(5, 0);
    }};
  }

  public int getSumOfRatingsCounts(Map<Integer, Integer> ratingsToCount) {
    int ret = 0;

    for (int rating = 0; rating < 6; rating++) {
      ret += ratingsToCount.get(rating) * rating; // count * rating
    }

    return ret;
  }

  List<String> getTopThreeControlChoices(boolean justKeys) {
    return this.controlRatingCountsByChoice.entrySet().stream()
        .sorted((e1, e2) ->
            this.getSumOfRatingsCounts(e1.getValue()) > this.getSumOfRatingsCounts(e2.getValue())
                ? -1 : 1)
        .limit(3)
        .map((e1) -> justKeys ? e1.getKey() : String.format("%s:%s", e1.getKey(), this.getSumOfRatingsCounts(e1.getValue())))
        .collect(Collectors.toList());
  }

  List<String> getTopThreeAllChoices(boolean justKeys) {
    return this.allRatingCountsByChoice.entrySet().stream()
        .sorted((e1, e2) ->
            this.getSumOfRatingsCounts(e1.getValue()) > this.getSumOfRatingsCounts(e2.getValue())
                ? -1 : 1)
        .limit(3)
        .map((e1) -> justKeys ? e1.getKey() : String.format("%s:%s", e1.getKey(), this.getSumOfRatingsCounts(e1.getValue())))
        .collect(Collectors.toList());
  }

  public String toString() {
    String tableString = String
        .format("%s/%s/%s,", this.group.getGroupId(), this.category.getCategoryId(),
            this.k.toString());
    tableString += String.format("%s,", this.getTopThreeControlChoices(false));
    tableString += String.format("%s", this.getTopThreeAllChoices(false));
    return tableString;
  }

  public String toString2() {
    String tableString = String
        .format("%s/%s/%s,", this.group.getGroupId(), this.category.getCategoryId(),
            this.k.toString());
    List<String> control = this.getTopThreeControlChoices(true);
    List<String> all = this.getTopThreeAllChoices(true);
    tableString += String.format("%s-%s-%s,", control.get(0), control.get(1), control.get(2));
    tableString += String.format("%s-%s-%s,", all.get(0), all.get(1), all.get(2));

//    if (control.containsAll(all)) {
//      tableString += "0";
//    } else {
//      tableString += "1";
//    }

    tableString += this.getNumberOfChanges(control, all);

    return tableString;
  }

  public String getExperimentResults() {
    String tableString = "";
    List<String> control = this.getTopThreeControlChoices(true);
    List<String> all = this.getTopThreeAllChoices(true);
    tableString += String.format("%s-%s-%s,", all.get(0), all.get(1), all.get(2));

    tableString += this.getNumberOfChanges(control, all);

    return tableString;
  }

  public int getTotalDifferentThanControl() {
    return this.totalDifferentThanControl;
  }

  public int getTotalRoundsDiffering() {
    return this.totalRoundsDiffering;
  }

  public String getNumberDifferingDetails() {
    return "(" + this.numberDifferingBy1 + "; " + this.numberDifferingBy2 + "; " + this.numberDifferingBy3 + ")";
  }

  private int getNumberOfChanges(List<String> control, List<String> all) {
    int ret = 0;

    for (String s: all) {
      if (!control.contains(s)) {
        ret++;
      }
    }

    return ret;
  }
}

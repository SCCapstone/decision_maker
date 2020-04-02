package utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import models.EventWithCategoryChoices;
import models.User;

public class DataCruncher {

  private LinkedHashMap<String, Map<Integer, Integer>> controlRatingCountsByChoice;
  private LinkedHashMap<String, Map<Integer, Integer>> allRatingCountsByChoice;

  private EventWithCategoryChoices event;
  private Map<String, User> allUsers;
  private Float k;
  private Metrics metrics;

  public DataCruncher(final EventWithCategoryChoices event, final List<User> users, final Float k,
      final Metrics metrics) {
    this.event = event;
    this.allUsers = users.stream()
        .collect(Collectors.toMap(User::getUsername, u -> u, (u1, u2) -> u2, HashMap::new));
    this.k = k;
    this.metrics = metrics;

    //set up the default mappings
    this.resetControlRatingsCountsByChoice();
    this.resetAllRatingsCountsByChoice();
  }

  public void crunch() {
    //first setup the control values
    List<Map<String, Integer>> allCategoryChoiceRatings = new ArrayList<>();
    for (String username : this.event.getOptedIn().keySet()) {
        allCategoryChoiceRatings.add(
            this.allUsers.get(username).getCategoryRatings().get(this.event.getCategoryId())
        );
    }

    this.clearAndSetControlRatingCountsByChoice(allCategoryChoiceRatings);

    //Now we need to get the 'random' categoryChoiceRating maps based on our control histogram
    for (int i = 0; i < Math.ceil(this.k * this.event.getOptedIn().size()); i++) {
      allCategoryChoiceRatings.add(this.getRandomUserChoiceRatings());
    }

    //last we generate the final histogram using the control maps and the random maps
    this.clearAndSetAllRatingCountsByChoice(allCategoryChoiceRatings);
  }

  //we put one additional vote into ratings 1 through 5 (so everything besides 0)
  //we then roll a random number and find what rating that correlates to probabilistically
  public Map<String, Integer> getRandomUserChoiceRatings() {
    Map<String, Integer> categoryChoiceRatings = new HashMap<>();
    Random random = new Random();

    for (String choiceId : this.controlRatingCountsByChoice.keySet()) {
      int randInt = random.nextInt(100); // [0, 100) aka [0, 99] since it's integers
      int runningProb = 0;
      int totalVotes =
          this.event.getOptedIn().size() + 5; //we added one vote to each of the choices 1 through 5

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
        //this shouldn't happen
        metrics.log(new ErrorDescriptor<>(ratingCounts, "DataCruncher.getRandomUserChoiceRatings",
            "choice not set in random ratings map"));
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
    for (String choiceId : this.event.getCategoryChoices().keySet()) {
      this.controlRatingCountsByChoice
          .putIfAbsent(choiceId, this.getEmptyRatingCountsMap());
    }
  }

  private void resetAllRatingsCountsByChoice() {
    this.allRatingCountsByChoice = new LinkedHashMap<>();
    for (String choiceId : this.event.getCategoryChoices().keySet()) {
      this.allRatingCountsByChoice
          .putIfAbsent(choiceId, this.getEmptyRatingCountsMap());
    }
  }

  private Map<Integer, Integer> getEmptyRatingCountsMap() {
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

  public Map<String, String> getTopXAllChoices(final Integer x) {
    return this.allRatingCountsByChoice.entrySet().stream()
        .sorted((e1, e2) ->
            this.getSumOfRatingsCounts(e1.getValue()) > this.getSumOfRatingsCounts(e2.getValue())
                ? -1 : 1)
        .limit(x)
        .collect(Collectors
            .toMap(Entry::getKey, e -> this.event.getCategoryChoices().get(e.getKey()),
                (e1, e2) -> e2, HashMap::new));
  }
}

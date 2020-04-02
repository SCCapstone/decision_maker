package utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import models.EventWithCategoryChoices;
import models.User;

public class NondeterministicOptimalChoiceSelector {

  private LinkedHashMap<String, Map<Integer, Integer>> ratingCountsByChoice;

  private EventWithCategoryChoices event;
  private Map<String, User> allUsers;
  private Metrics metrics;

  public NondeterministicOptimalChoiceSelector(final EventWithCategoryChoices event, final List<User> users,
      final Metrics metrics) {
    this.event = event;
    this.allUsers = users.stream()
        .collect(Collectors.toMap(User::getUsername, u -> u, (u1, u2) -> u2, HashMap::new));
    this.metrics = metrics;
    this.resetRatingsCountsByChoice(); // setup the default empty histogram
  }

  public void crunch(final Float k) {
    //first setup the control values (just the mapping of the users opted in)
    List<Map<String, Integer>> allCategoryChoiceRatings = new ArrayList<>();
    for (String username : this.event.getOptedIn().keySet()) {
      allCategoryChoiceRatings.add(
          this.allUsers.get(username).getCategoryRatings().get(this.event.getCategoryId())
      );
    }

    //set up the control histogram (this is the histogram of real choice ratings)
    this.resetRatingsCountsByChoice();
    this.calculateAndSetRatingCountsByChoice(allCategoryChoiceRatings);

    //Now we need to get the 'random' categoryChoiceRating maps based on our control histogram
    for (int i = 0; i < Math.ceil(k * this.event.getOptedIn().size()); i++) {
      //getRandomUserChoiceRatings utilizes the data setup in ratingCountsByChoice a few lines up
      allCategoryChoiceRatings.add(this.getRandomUserChoiceRatings());
    }

    //last we generate the final histogram using the user rating maps and the 'random' rating maps
    this.resetRatingsCountsByChoice();
    this.calculateAndSetRatingCountsByChoice(allCategoryChoiceRatings);
  }

  //we put one additional vote into ratings 1 through 5 (so everything besides 0)
  //we then roll a random number and find what rating that correlates to probabilistically
  public Map<String, Integer> getRandomUserChoiceRatings() {
    Map<String, Integer> categoryChoiceRatings = new HashMap<>();
    Random random = new Random();

    for (String choiceId : this.ratingCountsByChoice.keySet()) {
      int randInt = random.nextInt(100); // [0, 100) aka [0, 99] since it's integers
      int runningProb = 0;
      int totalVotes =
          this.event.getOptedIn().size() + 5; //we added one vote to each of the ratings 1 through 5

      Map<Integer, Integer> ratingCounts = this.ratingCountsByChoice.get(choiceId);

      for (int rating = 0; rating < 6; rating++) {
        int controlCount = ratingCounts.get(rating);
        int topOfRange = runningProb;

        //we have to figure out how much percentage this choice had
        if (rating == 0) {
          // we handle zero differently since we didn't add the one to it
          topOfRange += Math.floor(controlCount * 100.0 / totalVotes);
        } else {
          topOfRange += Math.floor((controlCount + 1) * 100.0 / totalVotes);
        }

        if (randInt < topOfRange) { // topOfRange went from being below randInt to above it
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

  private void calculateAndSetRatingCountsByChoice(
      List<Map<String, Integer>> allCategoryChoiceRatings) {
    for (Map<String, Integer> categoryChoiceRatings : allCategoryChoiceRatings) {
      for (String choiceId : this.ratingCountsByChoice.keySet()) {
        Integer selectedRating;
        if (categoryChoiceRatings != null && categoryChoiceRatings.containsKey(choiceId)) {
          selectedRating = categoryChoiceRatings.get(choiceId);
        } else {
          selectedRating = 3;
        }

        //increment the old count of this rating value by 1
        final Integer newCount =
            this.ratingCountsByChoice.get(choiceId).get(selectedRating) + 1;

        this.ratingCountsByChoice.get(choiceId).replace(selectedRating, newCount);
      }
    }
  }

  private void resetRatingsCountsByChoice() {
    this.ratingCountsByChoice = new LinkedHashMap<>();
    for (String choiceId : this.event.getCategoryChoices().keySet()) {
      this.ratingCountsByChoice
          .putIfAbsent(choiceId, this.getEmptyRatingCountsMap());
    }
  }

  private Map<Integer, Integer> getEmptyRatingCountsMap() {
    return IntStream.range(0, 6).boxed()
        .collect(Collectors.toMap(i -> i, i -> 0, (i1, i2) -> i2, HashMap::new));
  }

  //This methods sums the product of all ratings times their counts
  private int getSumOfRatingsCounts(Map<Integer, Integer> ratingsToCount) {
    int ret = 0;

    for (int rating = 0; rating < 6; rating++) {
      ret += ratingsToCount.get(rating) * rating; // count * rating
    }

    return ret;
  }

  public Map<String, String> getTopXChoices(final Integer x) {
    return this.ratingCountsByChoice.entrySet().stream()
        .sorted((e1, e2) ->
            this.getSumOfRatingsCounts(e1.getValue()) > this.getSumOfRatingsCounts(e2.getValue())
                ? -1 : 1)
        .limit(x)
        .collect(Collectors
            .toMap(Entry::getKey, e -> this.event.getCategoryChoices().get(e.getKey()),
                (e1, e2) -> e2, HashMap::new));
  }
}

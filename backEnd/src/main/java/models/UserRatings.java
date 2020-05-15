package models;

import handlers.NewCategoryHandler;
import java.util.HashMap;
import java.util.Map;

public class UserRatings {

  private Map<String, Map<String, Integer>> versionsToRatingMaps;

  public UserRatings(final Map<String, Map<String, Integer>> versionsToRatingMaps) {
    this.versionsToRatingMaps = versionsToRatingMaps;
  }

  public void mergeInNewRatings(final String version, final Map<String, Integer> ratings) {
    //first we need to build the past ratings for this category
    final Map<String, Integer> choiceRatings = new HashMap<>();
    //TODO get versionToRatingMaps key set and sort, then process while lower than version param
    for (int i = NewCategoryHandler.DEFAULT_CATEGORY_VERSION; i < Integer.parseInt(version); i++) {
      if (this.versionsToRatingMaps.containsKey(i)) {
        for (final Map.Entry<String, Integer> choiceRatePair : this.versionsToRatingMaps.get(i + "")
            .entrySet()) {
          //enter everything we find, we always overwrite older version data with newer version data
          choiceRatings.put(choiceRatePair.getKey(), choiceRatePair.getValue());
        }
      }
    }

    //now we have the ratings up until this version that we're merging in
    //look for new choice ids or new choice values
    for (final Map.Entry<String, Integer> choiceRatePair : ratings.entrySet()) {
      //enter everything we find, we always overwrite older version data with newer version data
      choiceRatings.put(choiceRatePair.getKey(), choiceRatePair.getValue());
    }

    //TODO if there are higher versions saved for this category, and we're changing the rating for
    // something here, then we should propagate the prior rating to the higher version so that this
    // changes doesn't implicitly change something at a higher level
    //Maybe not though. Maybe they did want to change that rating and upwards. I think one would
    // have to know the history or the labels on the category. If the label changed, then I think it
    // would need to propagate up, but if not then it wouldn't propagate.
    //What if I didn't have ratings saved for versions x and x+1. There are two events with x and
    // x+1. Versions x and x+1 both had changes to the same choice label.
  }
}

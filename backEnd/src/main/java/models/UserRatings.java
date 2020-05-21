package models;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Data;

@Data
public class UserRatings {

  private Map<String, Map<String, Integer>> versionsToRatingMaps;

  public UserRatings() {
    this.versionsToRatingMaps = new HashMap<>();
  }

  public UserRatings(final Map<String, Object> versionsToRatingMaps) {
    this.setVersionToRatingMapsRaw(versionsToRatingMaps);
  }

  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    for (final Map.Entry<String, Map<String, Integer>> versionRatingsEntry : this.versionsToRatingMaps
        .entrySet()) {
      modelAsMap.putIfAbsent(versionRatingsEntry.getKey(), versionRatingsEntry.getValue());
    }
    return modelAsMap;
  }

  public Set<String> getVersionSet() {
    return this.versionsToRatingMaps.keySet();
  }

  public Map<String, Integer> getRatings(final String version) {
    return this.versionsToRatingMaps.getOrDefault(version, new HashMap<>());
  }

  public boolean contains(final Integer version) {
    return this.versionsToRatingMaps.containsKey(version.toString());
  }

  public Map<String, Integer> getRatings(final Integer version) {
    return this.versionsToRatingMaps.getOrDefault(version.toString(), new HashMap<>());
  }

  public void setRatings(final String version, final Map<String, Integer> ratings) {
    this.versionsToRatingMaps.put(version, ratings);
  }

  private void setVersionToRatingMapsRaw(final Map<String, Object> versionToRatingMapsRaw) {
    this.versionsToRatingMaps = null;
    if (versionToRatingMapsRaw != null) {
      this.versionsToRatingMaps = new HashMap<>();
      for (final Map.Entry<String, Object> versionIdToChoiceRatings : versionToRatingMapsRaw
          .entrySet()) {
        final Map<String, Object> choiceRatingsRaw = (Map<String, Object>) versionIdToChoiceRatings
            .getValue();
        final Map<String, Integer> choiceRatingsConverted = new HashMap<>();
        for (final String choiceId : choiceRatingsRaw.keySet()) {
          choiceRatingsConverted
              .putIfAbsent(choiceId, getIntFromObject(choiceRatingsRaw.get(choiceId)));
        }

        this.versionsToRatingMaps.put(versionIdToChoiceRatings.getKey(), choiceRatingsConverted);
      }
    }
  }

  private Integer getIntFromObject(final Object input) {
    if (input != null) {
      return Integer.parseInt(input.toString());
    }
    return null;
  }
}

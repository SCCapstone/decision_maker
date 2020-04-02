import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import models.Category;
import models.Group;
import models.User;
import utilities.DataCruncher;
import utilities.ErrorDescriptor;

public class Ntsh {

  public static void main(String[] args) {
    //Setup the categories
    final Category smallCategory = new Category(Collections.emptyMap()); // 6 choices
    smallCategory.setCategoryId("smallCategory");
    smallCategory.setChoices(new HashMap<String, String>() {{
      put("a", "a");
      put("b", "b");
      put("c", "c");
      put("d", "d");
      put("e", "e");
      put("f", "f");
    }});

    final Category mediumCategory = new Category(Collections.emptyMap()); // 10 choices
    mediumCategory.setCategoryId("mediumCategory");
    mediumCategory.setChoices(new HashMap<String, String>() {{
      put("a", "a");
      put("b", "b");
      put("c", "c");
      put("d", "d");
      put("e", "e");
      put("f", "f");
      put("g", "g");
      put("h", "h");
      put("i", "i");
      put("j", "j");
    }});

    final Category largeCategory = new Category(Collections.emptyMap()); // 15 choices
    largeCategory.setCategoryId("largeCategory");
    largeCategory.setChoices(new HashMap<String, String>() {{
      put("a", "a");
      put("b", "b");
      put("c", "c");
      put("d", "d");
      put("e", "e");
      put("f", "f");
      put("g", "g");
      put("h", "h");
      put("i", "i");
      put("j", "j");
      put("k", "k");
      put("l", "l");
      put("m", "m");
      put("n", "n");
      put("o", "o");
    }});

    LinkedHashMap<String, User> users = new LinkedHashMap<>();
    Random random = new Random();
    try {
      for (int i = 1; i <= 30; i++) {
        User newUser = new User(Collections.emptyMap());
        newUser.setUsername("user" + i);

        Map<String, Object> userCategoryRatings = new HashMap<>();
        //create random ratings for the 3 above categories
        Map<String, Integer> smallChoiceRatings = new HashMap<>();
        for (String choiceId : smallCategory.getChoices().keySet()) {
          smallChoiceRatings.putIfAbsent(choiceId, random.nextInt(6)); // [0,6)
        }
        userCategoryRatings.putIfAbsent("smallCategory", smallChoiceRatings);

        Map<String, Integer> mediumChoiceRatings = new HashMap<>();
        for (String choiceId : mediumCategory.getChoices().keySet()) {
          mediumChoiceRatings.putIfAbsent(choiceId, random.nextInt(6)); // [0,6)
        }
        userCategoryRatings.putIfAbsent("mediumCategory", mediumChoiceRatings);

        Map<String, Integer> largeChoiceRatings = new HashMap<>();
        for (String choiceId : largeCategory.getChoices().keySet()) {
          largeChoiceRatings.putIfAbsent(choiceId, random.nextInt(6)); // [0,6)
        }
        userCategoryRatings.putIfAbsent("largeCategory", largeChoiceRatings);

        newUser.setCategoryRatings(userCategoryRatings);
        users.putIfAbsent(newUser.getUsername(), newUser);
      }
    } catch (final Exception e) {
      System.out.println(new ErrorDescriptor<>("blah", "blah", e));
    }

    //Setup the groups
    final Group smallGroup = new Group();
    smallGroup.setGroupId("smallGroup");
    Map<String, Object> smallGroupMembers = users.entrySet().stream().limit(10)
        .collect(Collectors.toMap(
            Entry::getKey, (e) -> e.getValue().asMember().asMap(), (e1, e2) -> e2, HashMap::new
        ));
    smallGroup.setMembers(smallGroupMembers);

    final Group mediumGroup = new Group();
    mediumGroup.setGroupId("mediumGroup");
    Map<String, Object> mediumGroupMembers = users.entrySet().stream().limit(20)
        .collect(Collectors.toMap(
            Entry::getKey, (e) -> e.getValue().asMember().asMap(), (e1, e2) -> e2, HashMap::new
        ));
    mediumGroup.setMembers(mediumGroupMembers);

    final Group largeGroup = new Group();
    largeGroup.setGroupId("largeGroup");
    Map<String, Object> largeGroupMembers = users.entrySet().stream().limit(30)
        .collect(Collectors.toMap(
            Entry::getKey, (e) -> e.getValue().asMember().asMap(), (e1, e2) -> e2, HashMap::new
        ));
    largeGroup.setMembers(largeGroupMembers);

    List<Group> groups = new ArrayList<>();
    groups.add(smallGroup);
    groups.add(mediumGroup);
    groups.add(largeGroup);

    List<Category> categories = new ArrayList<>();
    categories.add(smallCategory);
    categories.add(mediumCategory);
    categories.add(largeCategory);

    List<Float> kValues = ImmutableList.of(0.1f, 0.15f, 0.2f, 0.25f, 0.3f);

    int numberOfTests = 100;
    if (numberOfTests < 1) { //one test results will always be in the output
      numberOfTests = 1;
    }
    String tableHeader = "g/c/k,ctrl";

    for (int i = 1; i <= numberOfTests; i++) {
      tableHeader += ",test" + i + ",#changed?";
    }

    tableHeader += ",Total Changed";

    System.out.println(tableHeader);

    DataCruncher dataCruncher;

    for (Group g: groups) {
      for (Category c: categories) {
        for (Float k: kValues) {
          dataCruncher = new DataCruncher(g, c, users, k);
          dataCruncher.crunch();
          String tableRow = dataCruncher.toString2();

          //limited by numberOfTest - 1 since the first test is done in the initial .crunch()
          for (int i = 0; i < numberOfTests - 1; i++) {
            dataCruncher.crunch();
            tableRow += "," + dataCruncher.getExperimentResults();
          }

          tableRow += "," + dataCruncher.getTotalDifferentThanControl() + " over " + dataCruncher.getTotalRoundsDiffering() + " " + dataCruncher.getNumberDifferingDetails();

          System.out.println(tableRow);
        }
        System.out.println();
      }
    }

//    dataCruncher = new utilities.DataCruncher(smallGroup, smallCategory, users, 0.1f);
//    dataCruncher.crunch();
//    System.out.println(dataCruncher.toString());
//
//    dataCruncher = new utilities.DataCruncher(mediumGroup, smallCategory, users, 0.1f);
//    dataCruncher.crunch();
//    System.out.println(dataCruncher.toString());
//
//    dataCruncher = new utilities.DataCruncher(largeGroup, smallCategory, users, 0.1f);
//    dataCruncher.crunch();
//    System.out.println(dataCruncher.toString());

//    for (Group group : groups) {
//      System.out.println("Group is " + group.getGroupId());
//      for (Category category : categories) {
//        System.out.println("Category is " + category.getCategoryId());
//
//        final LinkedHashMap<String, Object> returnValue = new LinkedHashMap<>();
//
//        //setup is done now - time to algorithm
//        //build an array of user choice rating sums, start with all the current choice ids in the category
//        final Map<String, Integer> choiceRatingsToSums = new HashMap<>();
//        final Map<String, Map<Integer, Integer>> individualChoiceRatingSums = new HashMap<>();
//        for (String choiceId : category.getChoices().keySet()) {
//          choiceRatingsToSums.putIfAbsent(choiceId, 0);
//          individualChoiceRatingSums.putIfAbsent(choiceId, new HashMap<Integer, Integer>() {{
//            put(0, 0);
//            put(1, 0);
//            put(2, 0);
//            put(3, 0);
//            put(4, 0);
//            put(5, 0);
//          }});
//        }
//
//
//        //now we need to go through and add one to 10 percent random votes
//        //sum all of the user ratings
//        for (String username : group.getMembers().keySet()) {
//          try {
//            final User user = users.get(username);
//
//            final Map<String, Integer> categoryChoiceRatings = user.getCategoryRatings()
//                .get(category.getCategoryId());
//
//            for (String choiceId : choiceRatingsToSums.keySet()) {
//              if (categoryChoiceRatings != null && categoryChoiceRatings.containsKey(choiceId)) {
//                choiceRatingsToSums.replace(choiceId,
//                    choiceRatingsToSums.get(choiceId) + categoryChoiceRatings.get(choiceId));
//              } else {
//                choiceRatingsToSums.replace(choiceId, choiceRatingsToSums.get(choiceId) + 3);
//              }
//            }
//          } catch (Exception e) {
//            System.out.println(new ErrorDescriptor<>(username, "blah", e));
//          }
//        }
//
//        List<String> allValuesMapped = choiceRatingsToSums.entrySet().stream()
//            .sorted((e1, e2) -> e1.getValue() > e2.getValue() ? -1 : 1)
//            .map((e) -> e.getKey() + ": " + e.getValue())
//            .collect(Collectors.toList());
//
//        //user ratings have been summed, get the top X now
//        while (returnValue.size() < 3 && choiceRatingsToSums.size() > 0) {
//          String maxKey = null;
//          for (final String key : choiceRatingsToSums.keySet()) {
//            if (maxKey == null || choiceRatingsToSums.get(key) > choiceRatingsToSums.get(maxKey)) {
//              maxKey = key;
//            }
//          }
//
//          //we add to the return map and remove the max from the choice rating map
//          returnValue.putIfAbsent(maxKey, category.getChoices().get(maxKey));
//          choiceRatingsToSums.remove(maxKey);
//        }
//
//        System.out.println("Max choices are: " + returnValue.values());
//        System.out.println("All choices are: " + allValuesMapped.toString());
//        System.out.println();
//      }
//      System.out.println();
//    }

    //Setup the users
//    final User user1 = new User();
//    user1.setUsername("user1");
//    user1.setCategoryRatings(new HashMap<String, Object>(){{
//      put("smallCategory", new HashMap<String, Integer>() {{
//        put("a", 2);
//        put("b", 3);
//        put("c", 5);
//        put("d", 2);
//        put("e", 4);
//        put("f", 4);
//      }});
//      put("mediumCategory", new HashMap<String, Integer>() {{
//        put("a", 1);
//        put("b", 4);
//        put("c", 4);
//        put("d", 2);
//        put("e", 0);
//        put("f", 4);
//        put("g", 3);
//        put("h", 5);
//        put("i", 2);
//        put("j", 4);
//      }});
//      put("largeCategory", new HashMap<String, Integer>() {{
//        put("a", 1);
//        put("b", 0);
//        put("c", 4);
//        put("d", 2);
//        put("e", 4);
//        put("f", 5);
//        put("g", 4);
//        put("h", 3);
//        put("i", 2);
//        put("j", 4);
//        put("k", 4);
//        put("l", 3);
//        put("m", 5);
//        put("n", 0);
//        put("o", 2);
//      }});
//    }});
  }
}

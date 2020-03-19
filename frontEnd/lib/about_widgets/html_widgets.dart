import 'package:flutter_html/flutter_html.dart';

class HtmlWidgets {
  static Html general = Html(data: """
      <h1>Pocket Poll</h1>
      <p>This mobile application expediates group decisions. You can add your friends to a group and create events in those groups using categories that you make. These categories have lists of choices and ratings for each choice. When an event is made, our algorithm will produce optimal choices given everyone else's ratings.</p>
      <h2>General Tips</h2>
      <p>Most pages are pull-to-refresh. That is, you can pull down at the top of the page to get the most up to date changes. Try it out!</p>
    """);

  static Html settings = Html(data: """
      <h2>Location</h2>
      <p>Access user settings by clicking either your personal icon or the settings icon in the left drawer menu accessible from the home page.</p>
      <h2>Features</h2>
      <p>If you prefer brighter screens, try out the light theme!</p>
      <p>If your pocket is getting polled a little too much, you can turn off notifications entirely. You won't receive anymore notifications regarding upcoming events or being added to groups.</p>
      <p>At any time you can change your user icon. Simply click the edit icon in the top right corner of your current one.</p>
      <p>No need to memorize all your friends usernames. Add all your friend's usernames to your Favorites to have their info saved and ready. These favorites can be utilized when adding members to a group.</p>
    """);

  static Html categories = Html(data: """
      <h2>Location</h2>
      <p>Access the category homepage by clicking the category tile in the left drawer menu accessible from the home page.</p>
      <p>Categories can also be edited from inside a group in the settings page.</p>
      <h2>Overview</h2>
      <p>Categories are collections of choices. Each choice has a name and a rating. The lowest rating is 0, and the highest is 5. These ratings allow our algorithm to do its job as it will leverage all the ratings of members that are being considered for an event using the given category.</p>
      <p>Categories can be edited or deleted at any time from the category homepage.</p>
      <p>Categories themselves are independent entities. They must be “attached” to a group in order to be used in events. This is done by going to the group settings page and adding all the categories you wish to add that are yours.</p>    
      <p>When your friends add categories to a group you are in, you can edit your ratings for their choices from the group settings page. E.g. if Bob has Coffee as a choice you can go in and set your rating so your opinion is leveraged anytime that category is used in an event.</p>
    """);
}

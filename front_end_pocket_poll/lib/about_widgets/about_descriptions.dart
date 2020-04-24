import 'package:flutter_html/flutter_html.dart';

class AboutDescriptions {
  static Html general = Html(data: """
      <h1>Pocket Poll</h1>
      <p>This mobile application expedites group decisions. You can add your friends to a group and create events in those groups using categories that you make. These categories have lists of choices and ratings for each choice. When an event is made, our algorithm will produce optimal choices given everyone else's ratings. These choices are voted on and the choice with the highest vote becomes the result of the event.</p>
      <h2>General Tips</h2>
      <p>Many pages are pull-to-refresh. That is, you can pull down at the top of the page to get the most up to date changes.</p>
      <p>Most images in the app allow you to click on them to get a zoomed in image.</p>
      <p>If you ever are unsure of what a button does, try clicking it and holding down to get a description.</p>
    """);

  static Html settings = Html(data: """
      <h2>Location</h2>
      <p>Access settings by clicking either your personal icon or the settings icon in the left drawer menu accessible from the home page.</p>
      <h2>Features</h2>
      <p>If you prefer brighter screens, try out the light theme.</p>
      <p>If your pocket is getting polled a little too much, you can turn off notifications entirely. You won't receive any more notifications regarding upcoming events or being added to groups.</p>
      <p>At any time, you can change your user icon. Simply click the edit icon in the top right corner of your current one.</p>
      <p>No need to memorize all your friends' usernames. Add usernames to your Favorites to have their info saved and ready. These favorites can be utilized when adding members to a group.</p>
      
    """);

  static Html categories = Html(data: """
      <h2>Location</h2>
      <p>Access the category homepage by clicking the category tile in the left drawer menu accessible from the home page.</p>
      <p>Categories can also be edited from inside a group on the group settings page.</p>
      <h2>Overview</h2>
      <p>Categories are collections of choices. Each choice has a name and a rating. The lowest rating is 0, and the highest is 5. These ratings allow our algorithm to do its job as it will leverage the ratings of all members that are being considered for an event.</p>
      <p>Categories can be edited or deleted at any time from the category homepage.</p>
      <p>Categories themselves are independent entities. They must be “attached” to a group in order to be used in events. This is done by going to the group settings page and adding any of the categories that you own.</p>
      <p>When your friends add categories to a group you are in you can edit your ratings for their choices from the group settings page. E.g. If Bob has ‘Coffee Shops’ as a choice you can go in and set your rating so your opinion is leveraged anytime that category is used for an event.</p>
      <p>Categories owned by you or your friends can be copied. When copied you gain ownership of the new category and can add or remove as many choices as you please.</p>
      <p>Categories can change and this is tracked by a category's version. When an event is created it takes a snapshot of the selected category, so check the version number to know if you need to update your ratings.</p>
    """);

  static Html groups = Html(data: """
      <h2>Location</h2>
      <p>When you first open the app and are signed in there will be a list of all the groups you are a part of as well as groups you have left. To access a specific group, click on the row to be taken to that group’s page.</p>
      <h2>Overview</h2>
      <p>Groups are composed of members and categories. If the creator of the group makes it private, then changes to the group are only allowed by the creator. Otherwise, any member can add their categories or other members to the group. To utilize your favorites, click the contacts icon next to the “Add User” button when adding a member.</p>
      <p>To edit a group, navigate to the group settings page by clicking the settings icon in the top right corner of the group page. After making your changes to the group, make sure to hit the save button. Note changes made to the group categories and members are saved automatically when you exit each respective page.</p>
      <p>If you no longer wish to be in a group and are not the group’s creator you can leave at any time from the group settings page. In addition, only you will be able to add yourself back to the group. You can rejoin the group from the “Groups Left” tab on the app’s homepage.</p>
      <p>To toggle muting a group, click the blue mute icon next to the group row in the app’s homepage. You can still see the number of unseen events denoted by the number underneath this blue icon.</p>
    """);

  static Html events = Html(data: """
      <h2>Location</h2>
      <p>Events of a group are listed after clicking on a group from the app homepage.</p>
      <h2>Overview</h2>
      <p>Events are made inside of groups and have a single category attached to them. This category is used by our algorithm to produce a selection of choices to be voted on.</p>
      <p>There are multiple stages of an event:</p>
      <ol>
        <li>Consider stage: If you don’t want your ratings to be leveraged by our algorithm, then you can say “No” to being considered. By default, all members in the group are considered and you must say “No” to opt out.</li>
        <li>Voting stage: The algorithm has produced a handful of choices for all members to vote on (note that if you said “No” to being considered you can still vote). You simply vote no using the thumbs down button and yes with the thumbs up button.</li>
        <li>Ready stage: The highest voted choice is chosen and is displayed along with the start time of the event.</li>
      </ol>
      <p>If you have notifications enabled, you will receive a notification whenever a event goes to a different stage.</p>
      <p>When creating an event, the consider and voting stages can be skipped by either hitting the skip buttons next to the input fields or by putting the number 0 in for the time.</p>
      <p>The default consider and voting durations are set in the group settings page.</p>
      <P>When clicking on the details of an event, you can add that event to your calendar app by clicking the calendar icon button in the top right corner of the page.</P>
    """);
}

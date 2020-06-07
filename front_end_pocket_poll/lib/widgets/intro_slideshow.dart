import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:front_end_pocket_poll/imports/globals.dart';
import 'package:intro_slider/intro_slider.dart';
import 'package:intro_slider/slide_object.dart';

import 'first_login.dart';

class IntroSlideShow extends StatefulWidget {
  @override
  _IntroSlideShowState createState() => _IntroSlideShowState();
}

class _IntroSlideShowState extends State<IntroSlideShow> {
  List<Slide> slides = new List();

  @override
  void initState() {
    super.initState();

    slides.add(
      new Slide(
        title: "Pocket Poll",
        marginTitle: EdgeInsets.fromLTRB(8.0, 32.0, 8.0, 32.0),
        styleTitle: TextStyle(
            color: Color(0xffffffff),
            fontSize: 40.0,
            fontWeight: FontWeight.bold,
            fontFamily: 'RobotoMono'),
        description:
            "Pocket Poll easily allows for group events to be made in an inclusive way.\n\n"
            "Our unique algorithm will leverage your ratings for choices in order to give everyone a voice and ensure that group events are not stale.",
        colorBegin: Globals.pocketPollPrimary,
        colorEnd: Color(0xff7FFFD4),
        directionColorBegin: Alignment.topCenter,
        directionColorEnd: Alignment.bottomCenter,
        styleDescription: TextStyle(
            color: Color(0xffffffff),
            fontSize: 20.0,
            fontStyle: FontStyle.italic,
            fontWeight: FontWeight.bold,
            fontFamily: 'Raleway'),
        pathImage: "assets/images/calendarSplash.png",
      ),
    );
    slides.add(
      new Slide(
        title: "Categories",
        marginTitle: EdgeInsets.fromLTRB(8.0, 32.0, 8.0, 32.0),
        styleTitle: TextStyle(
            color: Color(0xffffffff),
            fontSize: 40.0,
            fontWeight: FontWeight.bold,
            fontFamily: 'RobotoMono'),
        description:
            "Categories are collections of rated choices. They can be whatever you want them to be! "
            "Restaurants, movies, sports, or bars. Your imagination is the limit!",
        colorBegin: Globals.pocketPollPrimary,
        colorEnd: Color(0xff7FFFD4),
        directionColorBegin: Alignment.topCenter,
        directionColorEnd: Alignment.bottomCenter,
        styleDescription: TextStyle(
            color: Color(0xffffffff),
            fontSize: 20.0,
            fontStyle: FontStyle.italic,
            fontWeight: FontWeight.bold,
            fontFamily: 'Raleway'),
        pathImage: "assets/images/categoriesInfoIcon.png",
      ),
    );
    slides.add(
      new Slide(
        title: "Groups",
        marginTitle: EdgeInsets.fromLTRB(8.0, 32.0, 8.0, 32.0),
        styleTitle: TextStyle(
            color: Color(0xffffffff),
            fontSize: 40.0,
            fontWeight: FontWeight.bold,
            fontFamily: 'RobotoMono'),
        colorBegin: Globals.pocketPollPrimary,
        colorEnd: Color(0xff7FFFD4),
        directionColorBegin: Alignment.topCenter,
        directionColorEnd: Alignment.bottomCenter,
        description:
            "Put those categories to use by adding them to a group! You can create a group from the main home page or ask your friends to add you to an existing one.\n\n"
            "If the group is made open, any member can add their categories in the group to be used in events!",
        styleDescription: TextStyle(
            color: Color(0xffffffff),
            fontSize: 18.0,
            fontStyle: FontStyle.italic,
            fontWeight: FontWeight.bold,
            fontFamily: 'Raleway'),
        pathImage: "assets/images/groupsInfoIcon.png",
      ),
    );
    slides.add(
      new Slide(
        title: "Events",
        marginTitle: EdgeInsets.fromLTRB(8.0, 32.0, 8.0, 32.0),
        styleTitle: TextStyle(
            color: Color(0xffffffff),
            fontSize: 40.0,
            fontWeight: FontWeight.bold,
            fontFamily: 'RobotoMono'),
        colorBegin: Globals.pocketPollPrimary,
        colorEnd: Color(0xff7FFFD4),
        directionColorBegin: Alignment.topCenter,
        directionColorEnd: Alignment.bottomCenter,
        description:
            "Inside a group you make events with a specific category. There are three stages of events.\n\n"
            "1.	Consider stage: update your ratings for the category and let our algorithm know if you want your ratings heard.\n\n"
            "2.	Vote stage: vote on the choices that our algorithm produced.\n\n"
            "3.	Ready stage: the choice with the highest vote is selected.",
        styleDescription: TextStyle(
            color: Color(0xffffffff),
            fontSize: 18.0,
            fontStyle: FontStyle.italic,
            fontWeight: FontWeight.bold,
            fontFamily: 'Raleway'),
        pathImage: "assets/images/eventsInfoIcon.png",
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return IntroSlider(
      slides: this.slides,
      // Skip button
      renderSkipBtn: Icon(
        Icons.skip_next,
        color: Color(0xffffffff),
      ),
      colorSkipBtn: Globals.pocketPollDarkBlue,
      highlightColorSkipBtn: Color(0xff11508f),
      // Next button
      renderNextBtn: Icon(
        Icons.navigate_next,
        color: Color(0xffffffff),
        size: 35.0,
      ),
      // Done button
      renderDoneBtn: Icon(
        Icons.done,
        color: Color(0xffffffff),
      ),
      onDonePress: this.onDonePress,
      colorDoneBtn: Globals.pocketPollDarkBlue,
      highlightColorDoneBtn: Globals.pocketPollDarkBlue,
      // Dot indicator
      colorDot: Color(0xff9dccfa),
      colorActiveDot: Globals.pocketPollDarkBlue,
      sizeDot: 15.0,
      shouldHideStatusBar: false,
    );
  }

  void onDonePress() {
    Navigator.pushReplacement(
      this.context,
      MaterialPageRoute(builder: (context) => FirstLogin()),
    );
  }
}

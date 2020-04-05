# decision_maker - Simple Group Event Planner

This mobile application will help users create events in group settings. Users can create groups amongst other users. Users can define categories with many different choices. Users can rate all of the choices for a category. Leveraging saved groups and saved choice preferences, the application can quickly and fairly suggest mutually good choice options for group activities at the click of a button.

The basic architecture of this project consists of Flutter and several AWS services. Flutter is a framework that uses the dart language to create cross platform UI. **We are currently only providing full support for Android plaform**. In the dart code there are api calls to our aws cloud hosted api endpoints. These apis are backed by AWS Lambda cloud functions. Packaged Java code in the form of executable .jar files supply the lambda function with their functionality. Maven is used to package our back end source code and manager dependencies.

## 1 Technologies
This application leverages several technologies, at a high level, it uses flutter for the front end and java for the back end.

### 1.1 Front End Technologies
For the front end we are using Flutter. In order to develop, build, and run our Flutter application, you will need to download several things:
* [Flutter](https://flutter.dev/docs/get-started/install)
   * The application depends on the http package, this is included in the pubspec.yaml file.
* [Android Studio](https://developer.android.com/studio)
   * Flutter Plugin and Dart Plugin will be needed for development. To install these, start android studio, open plugin preferences, select 'Marketplace', select Flutter plugin and click 'Install', click 'Yes' when prompted to install the Dart plugin, and finally click 'Restart' when prompted.
* [Virtual Device (Android)](https://developer.android.com/studio/run/managing-avds)

### 1.2 Back End Technologies
For the back end we are using Java. In order to develop, build, and run our java code, you will need to download several things:
* [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
* [Maven](http://maven.apache.org/download.cgi)
   * You need to make sure you have your JAVA_HOME environment variable pointing to your jdk11 location.
   * You will also need to add the unpacked maven bin to location to you PATH environment variable.

## 2 Running the application
This section is only for building and/or running our front end .apk on an Android device or emulator. The following assumes the back end has been packaged and deployed to the appropriate AWS Lambda function backing the API endpoints the application depends upon.

### 2.1 Running the application from the command line
To run the application from the command line, one must:
1. Have an android device or emulator connected to the computer.
2. cd into the front_end_pocket_poll directory.
3. Run 'flutter doctor' to ensure build environment and system requirements are met. If they are not met, follow the instruction to resolve issues.
4. If no issues are found, using the command 'flutter run' will build and run the application on the aforementioned active virtual device.

### 2.2 Running the application from a release .apk file
To run the application from a .apk file:
1. Ensure there are no prior versions of this application installed on the android device or emulator.
2. If using an emulator: drag and drop the .apk onto the device to install the application.
3. If using a real device, open the download location (presumably github) and download the apk directly on to the device from there.

### 2.3 Notice
**There are known issues with push notifications in Android emulators**. If you are experiencing any issues please try using a real android device.

## 3 Deployment
The backend of our application running in the AWS cloud requires deployment.

### 3.1 Deploying Lambda Functions
1. Develop java code locally and use the command 'mvn package' to package your java code into an executable .jar file.
2. Login to the AWS Management Console and navigate to the Lambda service.
3. Locate or create the Lambda function you want to deploy your code to.
4. Scroll down and upload the function, then save your changes.
5. If needed, change the location of the event handler to match the location in the code.
6. If this is the first deployment, the appropriate AWS Iam roles need to be setup so that the lambda code has authorization to access DynamoDB, Step Functions, SNS, and S3.

### 3.2 Deploying API Gateway Endpoints
1. Login to the AWS Management Console and navigate to the API Gateway service.
2. Create a new API or make necessary changes to give API access to your lambda functions.
3. Click on the 'Actions' drop down and then click the deploy option.
4. Upon deployment, you will be able to view the link to access the api, this can be used within the dart front end code to access the backend.
5. If this is the first deployment, the appropriate AWS Iam roles need to be setup so that the api has authorization to access the lambda function.

## 4 Code Style Guides:
* [Dart](https://dart.dev/guides/language/effective-dart/style)
* [Java](https://google.github.io/styleguide/javaguide.html)

## 5 Testing

### 5.1 Unit Testing on the Back End

##### 5.1.1 Dependencies
* You must have apache [maven](https://maven.apache.org/download.cgi) installed to run the tests.

##### 5.1.2 Running the Tests
1. cd into the backEnd directory.
2. To run the tests: from the backEnd directory run this command 'mvn test'.
3. To run the tests and generate coverage reports: from the backEnd directory run this command: 'mvn clean verify'.
   * To view the coverage reports open the file located at backEnd/target/site/jacoco/index.html in your browser. Then use the name links to navigate to spcific places in code.

##### 5.1.3 Location of These Tests
* Our back end unit tests are located at backEnd/src/test/java.
   * We are currently only testing our code in the imports directory, so from the above path you will have to go into the imports directory to actually see the java files containing our tests.

### 5.2 Behavioral Testing on the Front End

##### 5.2.1 Dependencies
* These tests should be run using flutter version v1.9.1+hotfix.5
* To fetch project specific dependencies, run 'flutter pub get' from the frontEnd directory.

##### 5.2.2 Running the Tests
1. cd into the frontEnd directory.
2. To run the tests: from the frontEnd directory run this command 'flutter drive --target=test_driver/app.dart'

##### 5.1.3 Location of These Tests
* Our front end behavioral tests are located at frontEnd/test_driver/app_test.dart

## 6 Authors
* John Andrews - jha2@email.sc.edu
* Josh Rapoport - rapopoj@email.sc.edu
* Edmond Klaric - eklaric@email.sc.edu
* Jeffrey Arling - jarling@email.sc.edu

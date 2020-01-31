# decision_maker - Simple Group Event Planner

This mobile application will help users in creating events for a group of people. Users can create groups amongst other users. Users can define categories that have related choices. Users can rate their preference of each choice for a category and save their ratings. Leveraging saved groups and saved choice preferences, the application can quickly and fairly suggest mutually good choices for group activities at the click of a button.

The basic architecture of this project consists of Flutter and several AWS services: API Gateway, Lambda, DynamoDB, and S3. Flutter activates the dart code to create the UI and front end. In the dart code there are http calls to api endpoints set up in AWS API Gateway. These apis are backed by AWS Lambda cloud functions. Package Java code in the form of executable .jar files supply the lambda function with their functionality. Maven is used to package source code.

## 1 Technologies
This application leverages several technologies, at the top level, it uses flutter for the front end and java for the back end.

### 1.1 Front End Technologies
For the front end we are using Flutter. In order to develop, build, and run our Flutter application, you will need to download several things:
* [Flutter](https://flutter.dev/docs/get-started/install)
   * The application depends on the http package, this is included in the pubspec.yaml file.
* [Android Studio](https://developer.android.com/studio)
   * Flutter Plugin and Dart Plugin will be needed for development. To install these, start android studio, open plugin preferences, select 'Marketplace', select Flutter plugin and click 'Install', click 'Yes' when prompted to install the Dart plugin, and finally click 'Restart' when prompted.
* [Virtual Device (Android)](https://developer.android.com/studio/run/managing-avds)

### 1.2 Back End Technologies
For the back end we are using Java. In order to develop, build, and run our java code, you will need to download several things:
* [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven](http://maven.apache.org/download.cgi)
   * You need to make sure you have your JAVA_HOME environment variable pointing to your jdk8 location.
   * You will also need to add the unpacked maven bin to location to you PATH environment variable.

## 2 Running
This section assumes you have your .jar files uploaded to the appropriate AWS Lambda function backing the API endpoints the application calls.

To run the application, one must active their virtual android device from Android Studio. Then navigate in a terminal to the root directory of your Flutter project. Run 'flutter doctor' to ensure build environment and system requirements are met. If no issues are found, using the command 'flutter run' will build and run the application on the aforementioned active virtual device.

## 3 Deployment
The backend of our application running in the AWS cloud requires deployment.

### 3.1 Deploying Lambda Functions
1. Develop java code locally and use the command 'mvn package' to package your java code into an executable .jar file.
2. Login to the AWS Management Console and navigate to the Lambda service.
3. Locate or create the Lambda function you want to deploy your code to.
4. Scroll down and upload the function, then save your changes.
5. If needed, change the location of the event handler to match the code that you wrote (safe are updating if needed).
6. If this is the first deployment, the appropriate AWS Iam roles need to be setup so that the lambda code has authorization to access the DynamoDB and S3 bucket.

### 3.2 Deploying API Gateway Endpoints
1. Login to the AWS Management Console and navigate to the API Gateway service.
2. Create a new API or make necessary changes to give API access to your lambda functions.
3. Click on the 'Actions' drop down and then click the deploy option.
4. Upon deployment, you will be able to view the link to access the api, this can be used within the dart front end code to access the backend.
5. If this is the first deployment, the appropriate AWS Iam roles need to be setup so that the api has authorization to access the lambda function.

## 4 Code Style Guides:
[Dart](https://dart.dev/guides/language/effective-dart/style)
[Java](https://google.github.io/styleguide/javaguide.html)

## 5 Testing

### 5.1 Unit Testing on the Back End

##### 5.1.1 Dependencies
* You must have apache maven installed to run the tests.

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

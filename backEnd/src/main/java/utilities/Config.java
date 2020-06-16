package utilities;

import com.amazonaws.regions.Regions;

public class Config {

  public static final String PUSH_SNS_PLATFORM_ARN = "arn:aws:sns:us-east-1:871532548613:app/GCM/PocketPollMessaging";
  public static final String SNS_REPORT_TOPIC_ARN = "arn:aws:sns:us-east-1:871532548613:ReportNotifications";
  public static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-2:871532548613:stateMachine:EventResolver";
  public static final Regions REGION = Regions.US_EAST_2;

  //validation parameters used across more than one handler
  public static final Integer MAX_DURATION = 10000;
  public static final Integer MAX_GROUP_MEMBERS = 300;
  public static final Integer MAX_NUMBER_OF_CHOICES = 500;
}

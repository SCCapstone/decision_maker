package utilities;

import com.amazonaws.regions.Regions;

public class Config {

  public static final String PUSH_SNS_PLATFORM_ARN = "arn:aws:sns:us-east-1:871532548613:app/GCM/PocketPollMessaging";
  public static final String STEP_FUNCTION_ARN = "arn:aws:states:us-east-2:871532548613:stateMachine:EventResolver";
  public static final Regions REGION = Regions.US_EAST_2;
}

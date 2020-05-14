package managers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import java.util.Map;
import utilities.Config;
import utilities.IOStreamsHelper;
import utilities.JsonUtils;

public class StepFunctionManager {

  private final AWSStepFunctions client;

  public StepFunctionManager() {
    this.client = AWSStepFunctionsClientBuilder.standard()
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .withRegion(Regions.US_EAST_2)
        .build();
  }

  public StartExecutionResult startStepMachine(final Map<String, Object> input)
      throws AmazonServiceException {
    final StringBuilder escapedInput = new StringBuilder(JsonUtils.convertMapToJson(input));
    IOStreamsHelper.removeAllInstancesOf(escapedInput,
        '\\'); // step function doesn't like these in the input

    return this.client.startExecution(new StartExecutionRequest()
        .withStateMachineArn(Config.STEP_FUNCTION_ARN)
        .withInput(escapedInput.toString()));
  }
}

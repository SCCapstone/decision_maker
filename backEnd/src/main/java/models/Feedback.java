package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import utilities.RequestFields;

@Data
@AllArgsConstructor
public class Feedback implements Model {

  public static final String FEEDBACK_ID = "FeedbackId";

  private String feedbackId;
  private String reportingUsername;
  private String feedbackMessage;
  private String now;

  public Item asItem() {
    final Item modelAsItem = Item.fromMap(this.asMap());

    //change the feedback id to be the primary key
    modelAsItem.removeAttribute(FEEDBACK_ID);
    modelAsItem.withPrimaryKey(FEEDBACK_ID, this.feedbackId);

    return modelAsItem;
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.put(FEEDBACK_ID, this.feedbackId);
    modelAsMap.put(RequestFields.ACTIVE_USER, this.reportingUsername);
    modelAsMap.put(Report.REPORT_MESSAGE, this.feedbackMessage);
    modelAsMap.put(Report.REPORTED_DATETIME, this.now);
    return modelAsMap;
  }

  public String getEmailSubject() {
    return "New Feedback";
  }

  public String getEmailBody() {
    final StringBuilder emailBody = new StringBuilder();
    emailBody.append("Reporting user: ").append(this.reportingUsername).append("\n\n");
    emailBody.append("Feedback message: ").append(this.feedbackMessage).append("\n\n");
    emailBody.append("Feedback ID: ").append(this.feedbackId);

    return emailBody.toString();
  }
}

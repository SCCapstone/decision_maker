package models;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import utilities.RequestFields;

@Data
@Builder
@AllArgsConstructor
public class Report implements Model {

  public static final String REPORT_ID = "ReportId";
  public static final String REPORTED_USERNAME = "ReportedUsername";
  public static final String REPORTED_GROUP_ID = "ReportedGroupId";
  public static final String REPORT_MESSAGE = "ReportMessage";
  public static final String DATA_SNAPSHOT = "DataSnapshot";
  public static final String REPORTED_DATETIME = "ReportedDatetime";

  private String reportId;
  private String reportingUsername;
  private String reportedUsername;
  private String reportedGroupId;
  private String reportMessage;
  private Map<String, Object> reportedEntitySnapshot;
  private String now;

  public static Report user(final String activeUser, final String reportedUsername,
      final String reportMessage, final Map<String, Object> snapshot, final String now) {
    return Report.builder()
        .reportId(UUID.randomUUID().toString())
        .reportingUsername(activeUser)
        .reportedUsername(reportedUsername)
        .reportedGroupId(null)
        .reportMessage(reportMessage)
        .reportedEntitySnapshot(snapshot)
        .now(now)
        .build();
  }

  public static Report group(final String activeUser, final String reportedGroupId,
      final String reportMessage, final Map<String, Object> snapshot, final String now) {
    return Report.builder()
        .reportId(UUID.randomUUID().toString())
        .reportingUsername(activeUser)
        .reportedUsername(null)
        .reportedGroupId(reportedGroupId)
        .reportMessage(reportMessage)
        .reportedEntitySnapshot(snapshot)
        .now(now)
        .build();
  }

  public Item asItem() {
    final Item modelAsItem = Item.fromMap(this.asMap());

    //change the report id to be the primary key
    modelAsItem.removeAttribute(REPORT_ID);
    modelAsItem.withPrimaryKey(REPORT_ID, this.reportId);

    return modelAsItem;
  }

  @Override
  public Map<String, Object> asMap() {
    final Map<String, Object> modelAsMap = new HashMap<>();
    modelAsMap.put(REPORT_ID, this.reportId);
    modelAsMap.put(RequestFields.ACTIVE_USER, this.reportingUsername);
    modelAsMap.put(REPORTED_USERNAME, this.reportedUsername);
    modelAsMap.put(REPORTED_GROUP_ID, this.reportedGroupId);
    modelAsMap.put(REPORT_MESSAGE, this.reportMessage);
    modelAsMap.put(DATA_SNAPSHOT, this.reportedEntitySnapshot);
    modelAsMap.put(REPORTED_DATETIME, this.now);
    return modelAsMap;
  }

  public String getEmailSubject() {
    String emailSubject;

    if (this.reportedUsername != null) {
      emailSubject = "A user has been reported";
    } else if (this.reportedGroupId != null) {
      emailSubject = "A group has been reported";
    } else {
      emailSubject = "Unknown report";
    }

    return emailSubject;
  }

  public String getEmailBody() {
    final StringBuilder emailBody = new StringBuilder("Reporting user: ")
        .append(this.reportingUsername).append("\n");

    if (this.reportedUsername != null) {
      emailBody.append("Reported user: ").append(this.reportedUsername).append("\n");
    } else if (this.reportedGroupId != null) {
      emailBody.append("Reported group id: ").append(this.reportedGroupId).append("\n");
    } else {
      emailBody.append("Unknown report");
    }

    emailBody.append("\n");
    emailBody.append("Report message: ").append(this.reportMessage).append("\n\n");
    emailBody.append("Report ID: ").append(this.reportId);

    return emailBody.toString();
  }
}

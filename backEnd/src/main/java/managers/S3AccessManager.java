package managers;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import utilities.ErrorDescriptor;
import utilities.Metrics;
import utilities.ResultStatus;

public class S3AccessManager {

  //some common file/mime types
  private final String JPG_TYPE = "jpg";
  private final String JPG_MIME = "image/jpeg";
  private final String PNG_TYPE = "png";
  private final String PNG_MIME = "image/png";

  private AmazonS3 s3Client;
  private static final String S3_IMAGE_BUCKET = "pocketpoll-images";

  public S3AccessManager() {
    this.s3Client = AmazonS3ClientBuilder
        .standard()
        .withRegion(Regions.US_EAST_2)
        .build();
  }

  public S3AccessManager(final AmazonS3 amazonS3) {
    this.s3Client = amazonS3;
  }

  public Optional<String> uploadImage(final List<Integer> fileData, final Metrics metrics) {
    final String classMethod = "S3AccessManager.uploadImage";
    metrics.commonSetup(classMethod);

    String fileName;
    try {
      final UUID uuid = UUID.randomUUID();
      fileName = uuid.toString() + "." + JPG_TYPE;

      int fileLength = fileData.size();
      byte[] rawData = new byte[fileLength];

      for (int i = 0; i < fileLength; i++) {
        rawData[i] = fileData.get(i).byteValue();
      }

      InputStream is = new ByteArrayInputStream(rawData);
      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setContentLength(fileLength);
      objectMetadata.setContentType(JPG_MIME);

      PutObjectRequest putObjectRequest = new PutObjectRequest(S3_IMAGE_BUCKET, fileName, is,
          objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead);

      this.s3Client.putObject(putObjectRequest);

      is.close();
    } catch (Exception e) {
      fileName = null;
      metrics.log(new ErrorDescriptor<>(fileData, classMethod, e));
    }

    metrics.commonClose(fileName != null);
    return Optional.ofNullable(fileName);
  }

  public ResultStatus deleteImage(final String fileName, final Metrics metrics) {
    if (fileName == null) {
      return new ResultStatus(true, "No image to delete");
    }

    final String classMethod = "S3AccessManager.deleteImage";
    metrics.commonSetup(classMethod);

    ResultStatus resultStatus = new ResultStatus();

    try {
      final DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(S3_IMAGE_BUCKET,
          fileName);

      this.s3Client.deleteObject(deleteObjectRequest);

      resultStatus = new ResultStatus(true, "Image deleted successfully.");
    } catch (final Exception e) {
      metrics.log(new ErrorDescriptor<>(fileName, classMethod, e));
      resultStatus.resultMessage = "Error deleting image";
    }

    metrics.commonClose(resultStatus.success);
    return resultStatus;
  }

  public Boolean imageBucketExists() {
    return this.s3Client.doesBucketExistV2(S3_IMAGE_BUCKET);
  }
}

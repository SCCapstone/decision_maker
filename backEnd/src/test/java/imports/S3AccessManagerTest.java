package imports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utilities.Metrics;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
public class S3AccessManagerTest {

  private S3AccessManager s3AccessManager;

  private final List<Integer> imageUploadGoodInput = ImmutableList.of(1, 2, 3);

  @Mock
  private AmazonS3 s3Client;

  @Mock
  private Metrics metrics;

  @BeforeEach
  private void init() {
    this.s3AccessManager = new S3AccessManager(this.s3Client);
  }

  ///////////////////////
  // uploadImage tests //
  ///////////////////////region

  @Test
  public void uploadImage_validInput_successfulResult() {
    Optional<String> result = this.s3AccessManager
        .uploadImage(this.imageUploadGoodInput, this.metrics);

    assertTrue(result.isPresent());
    verify(this.s3Client, times(1)).putObject(any(PutObjectRequest.class));
    verify(this.metrics, times(1)).commonClose(true);
  }

  @Test
  public void uploadImage_validInputS3Fails_failureResult() {
    doThrow(AmazonServiceException.class).when(this.s3Client)
        .putObject(any(PutObjectRequest.class));

    Optional<String> result = this.s3AccessManager
        .uploadImage(this.imageUploadGoodInput, this.metrics);

    assertFalse(result.isPresent());
    verify(this.s3Client, times(1)).putObject(any(PutObjectRequest.class));
    verify(this.metrics, times(1)).commonClose(false);
  }

  //endregion
}

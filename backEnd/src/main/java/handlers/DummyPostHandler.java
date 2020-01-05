package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.security.Key;
import java.util.Map;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import utilities.ExceptionHelper;
import utilities.ResultStatus;

public class DummyPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, ResultStatus> {
  public ResultStatus handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      Key key = new AesKey(ByteUtil.randomBytes(16));
      Map<String, String> headers = request.getHeaders();
      String authorization = headers.get("Authorization");

      String jwt = authorization.substring("Bearer ".length());

      System.out.println("Serialized Encrypted JWE: " + jwt);

      JsonWebEncryption jwe = new JsonWebEncryption();
      jwe.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST,
          KeyManagementAlgorithmIdentifiers.A128KW));
      jwe.setContentEncryptionAlgorithmConstraints(
          new AlgorithmConstraints(ConstraintType.WHITELIST,
              ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
      jwe.setKey(key);
      jwe.setCompactSerialization(jwt);
      System.out.println("Payload: " + jwe.getPayload());
    } catch (Exception e) {
      System.out.println(ExceptionHelper.getStackTrace(e));
    }

    return new ResultStatus();
  }

  public static void main(String[] args) {
    try {
      Key key = new AesKey(ByteUtil.randomBytes(16));
      JsonWebEncryption jwe = new JsonWebEncryption();
      jwe.setPayload("Hello World!");
      jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
      jwe.setEncryptionMethodHeaderParameter(
          ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
      jwe.setKey(key);
      String serializedJwe = jwe.getCompactSerialization();
      System.out.println("Serialized Encrypted JWE: " + serializedJwe);
      jwe = new JsonWebEncryption();
      jwe.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST,
          KeyManagementAlgorithmIdentifiers.A128KW));
      jwe.setContentEncryptionAlgorithmConstraints(
          new AlgorithmConstraints(ConstraintType.WHITELIST,
              ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
      jwe.setKey(key);
      jwe.setCompactSerialization(serializedJwe);
      System.out.println("Payload: " + jwe.getPayload());
    } catch (Exception e) {
      System.out.println("somthing aint right");
    }
  }
}

package handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import utilities.ExceptionHelper;
import utilities.JsonEncoders;
import utilities.ResultStatus;

public class DummyPostHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    ResultStatus resultStatus = new ResultStatus();
    try {
      Map<String, String> headers = request.getHeaders();
      String authorization = headers.get("Authorization");

      String token = authorization.substring("Bearer ".length());

      System.out.println("Token is: " + token);
      String username = this.getUsername(token);
      System.out.println(("Username is: " + username));
      resultStatus = new ResultStatus(true, "Username is: " + username);
    } catch (Exception e) {
      resultStatus.resultMessage = ExceptionHelper.getStackTrace(e);
    }
    APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent().withBody(
        resultStatus.toString());
    return apiGatewayProxyResponseEvent;
  }

  public String getUsername(String token) throws JwkException, MalformedURLException {
    String username = null;

    //String token = "eyJraWQiOiI1RHZhZVE0OCtpSG1uc1wvXC9BOHJxaGpNRTh5VitHRzA4MDdaUjNRUWRoZ3M9IiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoiajdIRVlkeld6bmhDRDYydDdSYWd2ZyIsInN1YiI6ImUxNDUwM2RjLTkzMDYtNDg3Yi1iNWJlLWE5MzQ1Y2ViMjJkMiIsImF1ZCI6IjdlaDRvdG0xcjVwMzUxZDF1OWozaDNyZjFvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNTc4MjAzNjM5LCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0yLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMl9lYmJQUDc2bk8iLCJjb2duaXRvOnVzZXJuYW1lIjoiam9obl9hbmRyZXdzMTIiLCJleHAiOjE1NzgyMDcyMzksImlhdCI6MTU3ODIwMzYzOSwiZW1haWwiOiJqaGFuZHJld3MxQGdtYWlsLmNvbSJ9.BDBI7Bovyq1gZu01UXSQGZ2a51aJRImoOMckKsxiCzG0WlttGi2YSRyv8bqgdhCvuRqAchcbzyIBbfnlN6d5i_aIaaHm6cT6GwgCl-8NMnZUQwV4ySyQL0WL-Wn5zVQbuqcryripKWhVXFf8ksnLQhuwTgbxvhEah-dFnlQpjb7z0ZGM3f5iZARYP1nuS1Olz0fARmhNmvGeB2uvYzvjjW9Ote4ROiLkdooqM5Oz9-KKezcBrxPoVoRjsYxqWUvqk-VMFYX7kXFL4wJcfGtHqQeTJzKzQnF349goKrGugrlBOujI8kx8XiMIEes0WmqvWu-EQOqQZ2KfIR6Ym2HFOA";
    //String token = "eyJraWQiOiI1RHZhZVE0OCtpSG1uc1wvXC9BOHJxaGpNRTh5VitHRzA4MDdaUjNRUWRoZ3M9IiwiYWxnIjoiUlMyNTYifQ.eyJhdF9oYXNoIjoiOHhNWHM1Z0hlUjdERFNxU0dPUmpJUSIsInN1YiI6ImUxNDUwM2RjLTkzMDYtNDg3Yi1iNWJlLWE5MzQ1Y2ViMjJkMiIsImF1ZCI6IjdlaDRvdG0xcjVwMzUxZDF1OWozaDNyZjFvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInRva2VuX3VzZSI6ImlkIiwiYXV0aF90aW1lIjoxNTc4MjYyNTI0LCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtZWFzdC0yLmFtYXpvbmF3cy5jb21cL3VzLWVhc3QtMl9lYmJQUDc2bk8iLCJjb2duaXRvOnVzZXJuYW1lIjoiam9obl9hbmRyZXdzMTIiLCJleHAiOjE1NzgyNjYxMjQsImlhdCI6MTU3ODI2MjUyNCwiZW1haWwiOiJqaGFuZHJld3MxQGdtYWlsLmNvbSJ9.VPqQlsvvh_RgNXq6M7gFeeE50nV-3OPa237ojfWLxehAZODEbwPRl8ij-IVu_w3h-ePsLxOlBmXj5blnJOXRjCtcbk1Ix0aa994WWC3i0isQ8dhEPSRWNY6a1sRhgwvs35Uejdp6shUMebWOZl8enw71zzMcv_PnbtKC8x1jI-IT6F2oMCDM6EtvB7yPFMkz1CVm88vDxl2UVTewhHcJzUqtnFgjPp5YUa34unFQ7XBqIXuyTRV0p4Oto_pyBaJN-0F5jo2xXg7apLVoyQMOMIoQCs7iCX0R4Lj3fsIUC-eIirg_ldCpFYf-ZhS7-XZHeayogtRuJKIC0aSqEXX7KA";
    DecodedJWT jwt = JWT.decode(token);

    String kid = jwt.getKeyId();

    //JwkProvider provider = new UrlJwkProvider(new File("./src/main/java/jwks.json").toURI().toURL());
    JwkProvider provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ebbPP76nO/.well-known/jwks.json"));
    Jwk jwk = provider.get(kid);
    PublicKey publicKey = jwk.getPublicKey();

    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) publicKey, null);

    algorithm.verify(jwt);

    username = jwt.getClaim("cognito:username").asString();

    System.out.println("Username: " + username);
    return username;
  }
}

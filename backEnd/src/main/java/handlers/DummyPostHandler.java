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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import utilities.ExceptionHelper;
import utilities.ResultStatus;

public class DummyPostHandler implements
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    ResultStatus resultStatus = new ResultStatus();
    try {
      Map<String, String> headers = request.getHeaders();
      String authorization = headers.get("Authorization");

      String token = authorization.substring("Bearer ".length());

      String username = this.getUsername(token);
      resultStatus = new ResultStatus(true, "Username is: " + username);
    } catch (Exception e) {
      resultStatus.resultMessage = ExceptionHelper.getStackTrace(e);
    }
    APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent().withBody(
        resultStatus.toString());
    return apiGatewayProxyResponseEvent;
  }

  public String getUsername(String token) throws JwkException, MalformedURLException {
    DecodedJWT jwt = JWT.decode(token);

    JwkProvider provider = new UrlJwkProvider(new URL("https://cognito-idp.us-east-2.amazonaws.com/us-east-2_ebbPP76nO/.well-known/jwks.json"));
    Jwk jwk = provider.get(jwt.getKeyId());

    Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

    algorithm.verify(jwt);

    return jwt.getClaim("cognito:username").asString();
  }
}

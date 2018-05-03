package com.amadeus.client;

import com.amadeus.Amadeus;
import com.amadeus.Params;
import com.amadeus.Request;
import com.amadeus.Response;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.logging.Logger;
import lombok.Getter;

/**
 * The HTTP part of the Amadeus API client. See the Amadeus class for
 * more details on initialization.
 */
public class HTTPClient {
  // A cached copy of the Access Token. It will auto refresh for every bearerToken (if needed)
  private final AccessToken accessToken = new AccessToken(this);

  /**
   * The configuration for this API client.
   */
  private @Getter Configuration configuration;

  /**
   * Constructor.
   * @hide as only used internally
   */
  protected HTTPClient(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * <p>
   *   A helper module for making generic GET requests calls. It is used by
   *   every namespaced API GET method.
   * </p>
   *
   * <pre>
   *   amadeus.referenceData.urls.checkinLinks.get(Params.with("airline", "1X"));
   * </pre>
   *
   * <p>
   *   It can be used to make any generic API call that is automatically
   *   authenticated using your API credentials:
   * </p>
   *
   * <pre>
   *    amadeus.get("/v2/reference-data/urls/checkin-links", Params.with("airline", "1X"));
   * </pre>
   *
   * @param path The full path for the API call
   * @param params The optional GET params to pass to the API
   * @return a Response object containing the status code, body, and parsed data.
   */
  public Response get(String path, Params params) {
    return request("GET", path, params);
  }

  /**
   * <p>
   *   A helper module for making generic POST requests calls. It is used by
   *   every namespaced API POST method.
   * </p>
   *
   * <pre>
   *   amadeus.foo.bar.post(Params.with("airline", "1X"));
   * </pre>
   *
   * <p>
   *   It can be used to make any generic API call that is automatically
   *   authenticated using your API credentials:
   * </p>
   *
   * <pre>
   *    amadeus.post("/v1/foo/bar", Params.with("airline", "1X"));
   * </pre>
   *
   * @param path The full path for the API call
   * @param params The optional POST params to pass to the API
   * @return a Response object containing the status code, body, and parsed data.
   */
  public Response post(String path, Params params) {
    return request("POST", path, params);
  }

  // A generic method for making requests of any verb.
  private Response request(String verb, String path, Params params) {
    return unauthenticatedRequest(verb, path, params, accessToken.getBearerToken());
  }

  /**
   * A generic method for making any authenticated or unauthenticated request,
   * passing in the bearer token explicitly. Used primarily by the
   * AccessToken to get the first AccessToken.
   * @hide as only used internally
   */
  public Response unauthenticatedRequest(String verb, String path,
                                         Params params, String bearerToken) {
    Request request = new Request(verb, path, params, bearerToken, this);
    log(request);
    return execute(request);
  }

  // A simple log that only triggers if we are in debug mode
  private void log(Object object) {
    if (getConfiguration().getLogLevel() == "debug") {
      Logger logger = getConfiguration().getLogger();
      logger.info(object.toString());
    }
  }

  // Executes a request and return a Response
  private Response execute(Request request) {
    Response response = new Response(fetch(request));
    response.parse(this);
    log(response);
    //    response.detectError(this);
    return response;
  }


  // Tries to make an API call. Raises an error if it can't complete the call.
  private Request fetch(Request request) {
    try {
      request.establishConnection();
      write(request);
    } catch (IOException e) {
      // Todo: throw error
      e.printStackTrace();
    }
    return request;
  }

  // Writes the parameters to the request.
  private void write(Request request) throws IOException {
    OutputStream os = request.getConnection().getOutputStream();
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
    writer.write(request.getParams().toQueryString());
    writer.flush();
    writer.close();
    os.close();
  }
}
package io.nard.ircbot.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class APIHandler implements HttpHandler {
  protected String base;

  public APIHandler(String base) {
    this.base = base;
  }

  protected String getBase() {
    return base;
  }

  protected boolean hasParameter(HttpExchange httpExchange) {
    return hasParameter(httpExchange, base);
  }

  protected boolean hasParameter(HttpExchange httpExchange, String base) {
    return getParameter(httpExchange, base).length() > 0;
  }

  protected String getParameter(HttpExchange httpExchange) {
    return getParameter(httpExchange, base);
  }

  protected String getParameter(HttpExchange httpExchange, String base) {
    Pattern regex = Pattern.compile(base);
    Matcher match = regex.matcher(httpExchange.getRequestURI().getPath());
    if (match.find()) {
      String param = match.group(1);
      if (param != null) {
        return param;
      }
    }
    return httpExchange.getRequestURI().getPath().replaceAll(base, "");
  }

  /**
   * get int parameter from the request URI
   * 
   * @param httpExchange
   * @return parameter from URI
   */
  protected Integer getIntParameter(HttpExchange httpExchange, String base) {
    try {
      return Integer.parseInt(getParameter(httpExchange, base));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * get int parameter from the request URI
   * 
   * @param httpExchange
   * @return parameter from URI
   */
  protected Integer getIntParameter(HttpExchange httpExchange) {
    return getIntParameter(httpExchange, base);
  }

  /**
   * send application/json response with status 200
   * 
   * @param httpExchange
   * @param response
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, JSONObject response) throws IOException {
    sendResponse(httpExchange, response.toString());
  }

  /**
   * send application/json response with status 200
   * 
   * @param httpExchange
   * @param response
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, String response) throws IOException {
    sendResponse(httpExchange, response, 200, ContentType.APPLICATION_JSON);
  }

  /**
   * send response with status 200 and given content type
   * 
   * @param httpExchange
   * @param response
   * @param contentType
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, String response, String contentType) throws IOException {
    sendResponse(httpExchange, response, 200, contentType);
  }

  /**
   * send application/json response with given status code
   * 
   * @param httpExchange
   * @param response
   * @param status
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, JSONObject response, int status) throws IOException {
    sendResponse(httpExchange, response.toString(), status);
  }

  /**
   * send application/json response with given status code
   * 
   * @param httpExchange
   * @param response
   * @param status
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, String response, int status) throws IOException {
    sendResponse(httpExchange, response, status, ContentType.APPLICATION_JSON);
  }

  /**
   * send response
   * 
   * @param httpExchange
   * @param response
   * @param status
   * @param contentType
   * @throws IOException
   */
  protected void sendResponse(HttpExchange httpExchange, String response, int status, String contentType)
      throws IOException {
    Headers h = httpExchange.getResponseHeaders();
    h.set("Content-Type", contentType + "; charset=UTF-8");
    httpExchange.sendResponseHeaders(status, response.getBytes().length);
    OutputStream os = httpExchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  @Override
  public void handle(HttpExchange httpExchange) throws IOException {
    if (httpExchange.getRequestURI().getPath().matches(base)) {
      process(httpExchange);
    } else {
      JSONObject error = new JSONObject();
      error.put("error", "you did something wrong");
      sendResponse(httpExchange, error.toString(), 404);
    }
  }

  /**
   * 
   * 
   * @param httpExchange
   * @throws IOException
   */
  protected abstract void process(HttpExchange httpExchange) throws IOException;

  /**
   * some content types
   */
  protected class ContentType {
    public static final String APPLICATION_JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
  }
  /*
   * "application/json" - UTF-8: "application/json; charset=UTF-8";
   */
}

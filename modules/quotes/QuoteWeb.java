package quotes;

import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.nard.ircbot.Bot;
import io.nard.ircbot.web.APIHandler;

public class QuoteWeb {

  QuoteManager quoteManager;

  public QuoteWeb(QuoteManager quoteManager) {
    this.quoteManager = quoteManager;

    HttpServer server = Bot.getHttpServer();
    server.createContext("/quote", new WebQuote("/quote(?:/(\\d+))?/?"));
    server.createContext("/quote/all", new AllQuotes("/quote/all"));
    server.createContext("/quote/last", new LastQuote("/quote/last"));
  }

  private class WebQuote extends APIHandler {

    public WebQuote(String base) {
      super(base);
    }

    public void process(HttpExchange httpExchange) throws IOException {
      boolean hasParam = hasParameter(httpExchange);
      Quote quote = hasParam ? quoteManager.get(getIntParameter(httpExchange)) : quoteManager.get();

      JSONObject response = new JSONObject();
      if (quote == null) {
        response.put("error", hasParam ? "quote not found" : "no quotes found");
        sendResponse(httpExchange, response, 404);
      } else {
        response = jsonFromQuote(quote);
        sendResponse(httpExchange, response);
      }

    }

  }

  private class AllQuotes extends APIHandler {

    public AllQuotes(String base) {
      super(base);
    }

    public void process(HttpExchange httpExchange) throws IOException {
      List<Quote> quotes = quoteManager.getAll();

      if (quotes.size() == 0) {
        JSONObject response = new JSONObject();
        response.put("error", "no quotes found");
        sendResponse(httpExchange, response, 404);
      } else {
        JSONArray response = new JSONArray(quotes);
        // for (Quote quote : quotes) {
        // response.put(String.valueOf(quote.getId()), jsonFromQuote(quote));
        // }
        sendResponse(httpExchange, response.toString());
      }
    }
  }

  private class LastQuote extends APIHandler {

    public LastQuote(String base) {
      super(base);
    }

    public void process(HttpExchange httpExchange) throws IOException {
      Quote quote = quoteManager.getLatest();

      JSONObject response = new JSONObject();
      if (quote == null) {
        response.put("error", "no quotes found");
        sendResponse(httpExchange, response, 404);
      } else {
        response = jsonFromQuote(quote);
        sendResponse(httpExchange, response);
      }
    }
  }

  /**
   * make JSON object from quote
   * 
   * @param quote
   * @return
   */
  private static JSONObject jsonFromQuote(Quote quote) {
    JSONObject response = new JSONObject();

    if (quote != null) {
      response.put("id", quote.getId());
      response.put("added", quote.getAdded());
      response.put("text", quote.getText(true));
      response.put("user", quote.getUser());
      response.put("channel", quote.getChannel());
      response.put("network", quote.getNetwork());
    }

    return response;
  }
}

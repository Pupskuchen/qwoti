

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.json.JSONObject;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;

/**
 * a subreddit grabber grabs the first result from SUBREDDIT, either "new" or "hot"
 * 
 * @author wipeD
 * @params nothing: grabbing newest, "hot": grabbing hottest
 */

public class Reddit extends AbstractCommandModule {


  public Reddit(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl.addCommand(new Command("r", "reddit") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {

        List<String> params = commandParam.getParams();

        if (params.isEmpty()) {
          event.respond(getUsageHint());
          return;
        } else if (params.size() == 1) {
          event.respond(getResult(params, false));
        } else if (!(params.get(1).equals("hot") || params.get(1).equals("new"))) {
          event.respond(getUsageHint());
          return;
        } else {
          event.respond(getResult(params, (params.get(1).equals("hot") ? true : false)));
        }

      }

      @Override
      public String getParams() {
        return "[\"hot\"] <subreddit>";
      }

      @Override
      public String getHelp() {
        return "show most recent post of given subreddit (or hottest when using \"hot\")";
      }
    }.setPrivmsgCapable(true));
  }


  private static String getResult(List<String> params, boolean hot) {
    try {
      JSONObject reddit = new JSONObject(
          readUrl("https://reddit.com/r/" + params.get(0) + "/" + (hot ? "hot" : "new") + "/.json", params.get(0)));
      reddit = (JSONObject) reddit.getJSONObject("data").getJSONArray("children").get(0);
      String imageurl = reddit.getJSONObject("data").getString("url");
      String title = reddit.getJSONObject("data").getString("title");
      String commentlink = reddit.getJSONObject("data").getString("id");

      return (hot ? "Hottest" : "Newest") + " /r/" + params.get(0) + ": " + title + " Direct link: " + imageurl
          + " Comments: https://redd.it/" + commentlink;
    } catch (Exception e) {
      e.printStackTrace();
      return "Something went wrong.";
    }
  }

  private static String readUrl(String urlString, String agent) throws Exception {
    // Got that from stackexchange, modified for it to use a user-agent
    // As reddit demands one (thats not lying!) or else it will return 429
    BufferedReader reader = null;
    try {
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.addRequestProperty("User-Agent", "/r/" + agent + " json grab - Java 1.8");
      conn.connect();
      reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      StringBuffer buffer = new StringBuffer();
      int read;
      char[] chars = new char[1024];
      while ((read = reader.read(chars)) != -1)
        buffer.append(chars, 0, read);

      return buffer.toString();
    } finally {
      if (reader != null) reader.close();
    }
  }


  @Override
  public String getName() {
    return "reddit";
  }


  @Override
  public String getDescription() {
    return "get the latest or hottest post from a reddit subreddit";
  }

}

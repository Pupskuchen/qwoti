

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;

public class Google extends AbstractCommandModule {

  public Google(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl.addCommand((new Command("g", "google") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond("usage: " + commandParam.getCommand() + " <expression>");
          return;
        }
        try {
          GoogleRequest g = new GoogleRequest(commandParam.getParam());
          for (int i = 0; i < 3; i++) {
            String[] result = g.get(i);
            if (result != null) {
              String res = result[0];
              if (result.length > 1) res += " (" + result[1] + ")";

              if (commandParam.isPrivMsg()) event.respond(res);
              else commandParam.getChannel().send().message(res);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public String getParams() {
        return "<expression>";
      }

      @Override
      public String getHelp() {
        return "search google for something";
      }
    }).setPrivmsgCapable(true));
  }

  public class GoogleRequest {

    private List<String[]> results = new ArrayList<String[]>();

    public GoogleRequest(String query) throws Exception {
      if (query == null || query.isEmpty()) {
        throw new Exception("searching requires something to search for");
      }

      String google = "https://www.google.com/search?hl=en&q=";
      String userAgent = Bot.BOTNAME + " " + Bot.VERSION + " (+" + Bot.INFOURL + ")";

      Document res = Jsoup.connect(google + URLEncoder.encode(query, "UTF-8"))//
          .userAgent(userAgent).get();

      Elements links = res.select("h3.r>a");
      Element quick = res.getElementById("_vBb");
      Elements calc = res.select("#res #topstuff h2.r");

      if (calc.size() == 1) {
        results.add(new String[] { calc.get(0).text() });
        return;
      }

      if (quick != null) {
        Elements r = quick.getElementsByClass("_m3b");
        Elements q = quick.getElementsByClass("_eGc");
        if (r.size() > 0 && q.size() > 0) {
          results.add(new String[] { q.get(0).text() + ": " + r.get(0).text() });
          return;
        }
      }

      for (int i = 0; i < links.size(); i++) {
        Element link = links.get(i);
        String title = link.text();
        String url = link.absUrl("href");
        url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");

        if (!url.startsWith("http")) {
          continue;
        }

        results.add(new String[] { title, url });
      }
    }

    public String[] get(int i) {
      if (i < 0 || i > results.size() - 1) {
        return null;
      }
      return results.get(i);
    }

    public List<String[]> get() {
      return results;
    }

  }

  @Override
  public String getName() {
    return "google";
  }

  @Override
  public String getDescription() {
    return "provides a google search command";
  }
}

import org.json.JSONObject;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.GenericChannelUserOutput;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;


public class UrbanDictionary extends AbstractCommandModule {

  public UrbanDictionary(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl.addCommand(new Command("ud") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond(getUsageHint());
          return;
        }

        String term = commandParam.getParam();
        UDSearch ud = new UDSearch(term);

        GenericChannelUserOutput out = commandParam.isPrivMsg() ? event.getUser().send()
            : ((MessageEvent) event).getChannel().send();

        if (ud.error) event.respond("couldn't get a result");
        else if (!ud.hasDef()) event.respond("no result found for " + Colors.BOLD + term);
        else {
          String newline = "(?:\r?\n)+";
          String[] lines = ud.definition.split(newline);
          for (int i = 0; i < lines.length; i++) {
            String message = lines[i];
            if (i == 0) message = term + ": " + message;

            out.message(message);
          }

          if (ud.example != null) {
            lines = ud.example.split(newline);
            for (int i = 0; i < lines.length; i++) {
              String message = lines[i];
              if (i == 0) message = "example: " + message;

              out.message(message);
            }
          }
        }
      }

      @Override
      public String getHelp() {
        return getDescription();
      }

      public String getParams() {
        return "<term>";
      }
    }.setPrivmsgCapable(true));
  }

  @Override
  public String getName() {
    return "urbandictionary";
  }

  @Override
  public String getDescription() {
    return "get a definition for every term (virtually)";
  }

  private class UDSearch {

    private String definition = null;
    private String example = null;
    boolean error = false;

    public UDSearch(String term) {
      try {
        HttpResponse<JsonNode> res = Unirest.get("http://api.urbandictionary.com/v0/define").queryString("term", term)
            .asJson();
        if (res.getStatus() == 200) {
          JSONObject json = res.getBody().getObject();
          String type = json.has("result_type") ? json.getString("result_type") : "no_results";
          if (!type.equals("no_results")) {
            JSONObject match = json.getJSONArray("list").getJSONObject(0);
            definition = match.getString("definition");
            example = match.getString("example");
          }
        } else error = true;
      } catch (UnirestException e) {
        error = true;
      }
    }

    public boolean hasDef() {
      return definition != null;
    }
  }
}

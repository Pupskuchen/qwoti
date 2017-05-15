

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;

public class LastFM extends AbstractCommandModule {

  public LastFM(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    if (Files.notExists(Paths.get("lastfm"))) {
      try {
        Files.createDirectory(Paths.get("lastfm"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    String apiKey = botHelper.getAPIKey("lastfm");

    cl.addCommand(new Command("np") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (apiKey == null || apiKey.isEmpty()) {
          event.respond("api key is not configured");
          return;
        }
        String user = commandParam.hasParam() ? commandParam.getParams().get(0) : event.getUser().getNick();
        String account = botHelper.getAccount(commandParam.getChannel(), user);
        user = account != null ? account : user;

        try {
          BotConfig lastfmConfig = new BotConfig(
              "lastfm/lastfm-" + event.getBot().getServerInfo().getNetwork().toLowerCase() + ".json", true);
          user = lastfmConfig.getString(user, user);
        } catch (JSONException | IOException e) {
          e.printStackTrace();
        }

        String url = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=" + user + "&api_key=" + apiKey
            + "&format=json&limit=1";
        try {
          HttpResponse<JsonNode> request = Unirest.get(url).asJson();
          if (request.getStatus() == 200) {
            JSONObject response = request.getBody().getObject();
            if (response.has("error")) {
              event.respond("error: " + response.getString("message"));
              return;
            }
            JSONArray result = response.getJSONObject("recenttracks").getJSONArray("track");
            if (result.length() == 0) {
              event.respond("user " + user + " hasn't scrobbled any tracks yet");
              return;
            }
            JSONObject track = result.getJSONObject(0);
            String artist = track.has("artist") ? track.getJSONObject("artist").getString("#text") : null;
            String album = track.has("album") ? track.getJSONObject("album").getString("#text") : null;
            String name = track.has("name") ? track.getString("name") : null;
            String mbid = track.has("mbid") ? track.getString("mbid") : null;
            boolean nowPlaying = track.has("@attr") && track.getJSONObject("@attr").has("nowplaying")
                && track.getJSONObject("@attr").getBoolean("nowplaying");
            String res = "";
            res += Colors.BOLD + Colors.OLIVE + artist + Colors.NORMAL + " - ";
            res += album != null && !album.isEmpty() ? Colors.BOLD + Colors.OLIVE + album + Colors.NORMAL + " - " : "";
            res += Colors.BOLD + Colors.OLIVE + name + Colors.NORMAL;
            try {
              url = "http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + apiKey + "&artist="
                  + URLEncoder.encode(artist, "UTF-8") + "&track=" + URLEncoder.encode(name, "UTF-8") + "&username="
                  + user + "&format=json" + (mbid != null && !mbid.isEmpty() ? "&mbid=" + mbid : "");
              request = Unirest.get(url).asJson();
              track = request.getBody().getObject();
              if (!track.has("error")) {
                track = track.getJSONObject("track");
                long duration = track.getLong("duration");
                int playcount = track.has("userplaycount") ? track.getInt("userplaycount") : 0;
                res += " [playcount " + Colors.BOLD + playcount + Colors.NORMAL + "x]";
                if (duration > 0) {
                  res += String.format(" [%s%02d:%02d%s]", //
                      Colors.BOLD + Colors.OLIVE, //
                      TimeUnit.MILLISECONDS.toMinutes(duration), //
                      TimeUnit.MILLISECONDS.toSeconds(duration)
                          - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)), //
                      Colors.NORMAL);
                }
              }
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            String state = nowPlaying ? " is now playing: "
                : " is not listening to anything. The last track played was: ";
            String msg = Colors.BOLD + user + Colors.NORMAL + state + res;
            if (!commandParam.isPrivMsg()) commandParam.getChannel().send().message(msg);
            else event.respond(msg);
            return;
          }
        } catch (UnirestException e) {
          e.printStackTrace();
        }
        event.respond("error getting data");
      }

      @Override
      public String getParams() {
        return "[user]";
      }

      @Override
      public String getHelp() {
        return "show last.fm's now playing information for yourself or given user";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("setuser") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (commandParam.hasParam()) {
          String lastfmUser = commandParam.getParams().get(0);
          String account = botHelper.getAccount(event.getBot(), event.getUser());
          account = account != null ? account : event.getUser().getNick();
          try {
            BotConfig lastfmConfig = new BotConfig(
                "lastfm/lastfm-" + event.getBot().getServerInfo().getNetwork().toLowerCase() + ".json", true);
            lastfmConfig.putString(account, lastfmUser);
            lastfmConfig.save();
            event.respond("you're now associated with " + lastfmUser);
          } catch (JSONException | IOException e) {
            e.printStackTrace();
            event.respond("something went wrong");
          }
        } else {
          event.respond("setuser <lastfm-user>");
        }
      }

      @Override
      public String getParams() {
        return "<user>";
      }

      @Override
      public String getHelp() {
        return "set your last.fm username to be used for now playing information";
      }
    }.setPrivmsgCapable(true));
  }

  @Override
  public String getName() {
    return "lastfm";
  }

  @Override
  public String getDescription() {
    return "show last.fm now playing information (requires last.fm api key to be configured)";
  }
}

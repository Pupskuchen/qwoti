package io.nard.ircbot.simplemods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;

public abstract class LastFM {
  public static Listener module(BotConfig botConfig, BotHelper botHelper) {
    CommandListener commandListener = new CommandListener(botConfig);

    if (Files.notExists(Paths.get("lastfm"))) {
      try {
        Files.createDirectory(Paths.get("lastfm"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    String apiKey = botConfig.getString("lastfm-api", null);

    commandListener.addCommand(new Command("np") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (apiKey == null || apiKey.isEmpty()) {
          event.respond("api key is not configured");
          return;
        }
        String account = commandParam.getUserAccount();
        account = account != null ? account : event.getUser().getNick();
        String user = commandParam.hasParam() ? commandParam.getParam() : account;

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
            JSONArray result = request.getBody().getObject().getJSONObject("recenttracks").getJSONArray("track");
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
            res += artist != null ? artist + " - " : "";
            res += album != null ? album + " - " : "";
            res += name != null ? name : "";
            if (mbid != null) {
              request = Unirest.get("http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + apiKey
                  + "&mbid=" + mbid + "&username=" + user + "&format=json").asJson();
              track = request.getBody().getObject().getJSONObject("track");
              long duration = track.getLong("duration");
              int playcount = track.getInt("userplaycount");
              res += " [playcount " + playcount + "x]";
              if (duration > 0) {
                res += String.format(" | %02d:%02d", //
                    TimeUnit.MILLISECONDS.toMinutes(duration), //
                    TimeUnit.MILLISECONDS.toSeconds(duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
              }
            }
            String state = nowPlaying ? " is now playing: "
                : " is not listening to anything. The last track played was: ";
            event.getChannel().send().message(user + state + res);
            return;
          }
        } catch (UnirestException e) {
          e.printStackTrace();
        }
        event.respond("error getting data");
      }
    }).addCommand(new Command("setuser") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
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
    });

    return commandListener;
  }
}

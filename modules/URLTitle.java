import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputChannel;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.nard.ircbot.AbstractListener;
import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Privilege;


public class URLTitle extends AbstractModule {

  private Listener listener;

  public URLTitle(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    listener = new AbstractListener() {

      @Override
      public void onMessage(MessageEvent event) throws Exception {
        User user = event.getUser();
        PircBotX bot = event.getBot();
        String network = bot.getServerInfo().getNetwork().toLowerCase();
        String userAccount = botHelper.getAccount(bot, user);
        Privilege userPrivileges = userAccount == null ? Privilege.GUEST
            : botHelper.getPrivileges().get(network).get(userAccount.toLowerCase());

        boolean userAuthed = botHelper.isAuthenticated(bot, user);

        if (userPrivileges == null) {
          userPrivileges = userAuthed ? Privilege.PRIVILEGED : Privilege.GUEST;
        }

        if (userPrivileges == Privilege.NONE) return;

        OutputChannel out = event.getChannel().send();

        String msg = event.getMessage();
        TextURLParser p = new TextURLParser(msg);
        for (int i = 0; i < p.size(); i++) {
          String title = p.getTitle(i);
          if (title != null) out.message(String.format("« %s »", title));
        }
      }
    };
  }

  // test urls:
  // ftp://ftp-stud.fht-esslingen.de/
  // https://github.com/Pupskuchen/qwoti

  private static class URLMatch {

    private String scheme, host, path, full;

    public URLMatch(String scheme, String host, String path, String full) {
      this.scheme = scheme;
      this.host = host;
      if (path != null && path.startsWith("/")) path = path.substring(1);
      if (path != null && path.isEmpty()) path = null;
      this.path = path;
      this.full = full;
    }
  }

  private class TextURLParser {

    // (?:\\S+(?::\\S*)?@)? -- after protocol
    public final Pattern PATTERN = Pattern.compile(
        "(?i)\\b(?:(https?|ftp)://)((?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))\\.?)(?::\\d{2,5})?([/?#]\\S*)?\\b");

    private List<URLMatch> matches = new ArrayList<URLMatch>();

    public TextURLParser(String text) {
      Matcher m = PATTERN.matcher(text);
      while (m.find()) {
        try {
          matches.add(new URLMatch(m.group(1), m.group(2), m.groupCount() > 2 ? m.group(3) : "", m.group(0)));
        } catch (Exception e) {
        }
      }
    }

    public int size() {
      return matches.size();
    }

    public URLMatch get(int index) {
      return index >= 0 && index < matches.size() ? matches.get(index) : null;
    }

    public String getTitle(int index) {
      return getTitle(get(index));
    }

    public String getTitle(URLMatch m) {
      if (m == null) return null;
      if (m.host == null || m.host.isEmpty() || m.scheme == null || !m.scheme.startsWith("http")) return null;
      String h = m.host;
      String userAgent = Bot.BOTNAME + " " + Bot.VERSION + " (+" + Bot.INFOURL + ")";

      // api: github, youtube, xkcd, imgur, gfycat, twitter, omdb/imdb
      // parse: vimeo, soundcloud
      if (isHost(h, "youtube.com")) {
        // TODO youtube api
      } else if (isHost(h, "xkcd.com")) {
        String url = m.full;

        if (m.path == null) {
          url = "https://xkcd.com/info.0.json";
        } else if (m.path != null) {
          if (m.path.matches("\\d+/?")) {
            Matcher matcher = Pattern.compile("(\\d+)/?").matcher(m.path);
            if (matcher.find()) {
              String id = matcher.group(1);
              url = "https://xkcd.com/" + id + "/info.0.json";
            }
          }
        }

        try {
          HttpResponse<JsonNode> res = Unirest.get(url).asJson();
          if (res.getStatus() == 200) {
            JSONObject json = res.getBody().getObject();

            return String.format("xkcd #%d: %s (%s-%s-%s)", //
                json.getInt("num"), //
                json.getString("title"), //
                json.getString("year"), //
                json.getString("month"), //
                json.getString("day"));
          }
        } catch (UnirestException e) {
        }
      } else if (isHost(h, "imgur.com") || isHost(h, "i.imgur.com")) {
        if (m.path != null) {
          if (isHost(h, "i.imgur.com")) {
            if (m.path.matches("[\\w\\d]+\\..*")) {
              String image = m.path.substring(0, m.path.indexOf("."));

              try {
                HttpResponse<JsonNode> res = Unirest.get("https://api.imgur.com/3/image/" + image)
                    .header("Authorization", "Client-ID " + botHelper.getAPIKey("imgur")).asJson();
                if (res.getStatus() == 200) {
                  JSONObject json = res.getBody().getObject();
                  System.out.println(json.toString(2));
                  if (json.optBoolean("success")) {
                    json = json.getJSONObject("data");
                    StringJoiner sj = new StringJoiner(" | ");

                    if (json.optBoolean("nsfw")) sj.add("NSFW");
                    if (!json.isNull("title")) sj.add(json.getString("title"));
                    if (!json.isNull("width") && !json.isNull("height"))
                      sj.add(json.getInt("width") + "x" + json.getInt("height") + " px");
                    if (!json.isNull("size")) sj.add(bytesToNice(json.getInt("size")));
                    if (!json.isNull("description")) {
                      String desc = json.getString("description");
                      if (desc.length() > 50) {
                        desc = desc.substring(0, 50) + " …";
                      }
                      sj.add(desc);
                    }

                    return "imgur: " + sj.toString();
                  }
                } else if (res.getStatus() == 403) {
                  System.err.println("imgur client id not configured");
                }
              } catch (UnirestException e) {
              }
            }
          }
        }
      } else if (isHost(h, "gfycat.com")) {
        // TODO gfycat api
      } else if (isHost(h, "twitter.com")) {
        // TODO twitter api
      } else if (isHost(h, "imdb.com")) {
        if (m.path != null && !m.path.isEmpty()) {
          if (m.path.matches("title/tt\\d+")) {
            Matcher matcher = Pattern.compile("title/(tt\\d+)").matcher(m.path);
            if (matcher.find()) {
              String url = "http://www.omdbapi.com/?i=" + matcher.group(1);
            }
          } else if (m.path.matches("name/nm\\d+")) {

          }
        }
      } else if (isHost(h, "vimeo.com")) {

      } else if (isHost(h, "soundcloud.com")) {

      }

      try {
        Elements sel = Jsoup.connect(m.full).userAgent(userAgent).get().getElementsByTag("title");
        String title = sel.size() > 0 ? sel.get(0).text() : null;
        return title;
      } catch (IOException e) {
      }

      return null;
    }

    private boolean isHost(String a, String b) {
      return a.equalsIgnoreCase(b);
    }

    private String bytesToNice(int bytes) {
      String[] types = { "B", "KB", "MB", "GB", "TB", "PB", "EB" };
      int type = 0;
      double result = bytes;
      while (result > 1000) {
        type++;
        result /= 1000.0;
      }
      return String.format("%.3f %s", result, types[type]);
    }
  }

  @Override
  public void startup() {
    listenerManager.addListener(listener);
  }

  @Override
  public void shutdown() {
    listenerManager.removeListener(listener);
  }

  @Override
  public String getName() {
    return "urltitle";
  }

  @Override
  public String getDescription() {
    return "show titles of websites linked here";
  }

}

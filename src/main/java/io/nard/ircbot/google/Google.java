package io.nard.ircbot.google;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.nard.ircbot.Bot;

public class Google {

  private List<String[]> results = new ArrayList<String[]>();

  public Google(String query) throws Exception {
    if (query == null || query.isEmpty()) {
      throw new Exception("searching requires something to search for");
    }

    String google = "https://www.google.com/search?q=";
    String userAgent = Bot.BOTNAME + " " + Bot.VERSION + " (+" + Bot.INFOURL + ")";

    Elements links = Jsoup.connect(google + URLEncoder.encode(query, "UTF-8"))//
        .userAgent(userAgent).get().select(".g>.r>a");

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

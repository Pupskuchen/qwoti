package quotes;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.User;

/**
 * useful utility methods for the QuoteBot module
 */
public abstract class QuoteBotUtils {

  /**
   * prevents users in given channel to be highlighted
   * 
   * @param string
   * @param channel
   * @return unhighlighted string
   */
  public static String unhighlight(String string, Channel channel) {
    if (channel == null) {
      return string;
    }
    for (User user : channel.getUsers()) {
      String nick = user.getNick();
      string = string.replaceAll("(?i)" + nick, nick.substring(0, 1) + "\u200b" + nick.substring(1, nick.length()));
      // note: ^ this replaces all occurences of the nick by the nick itself - unhighlighted, but
      // with the nick's
      // spelling (case sensitive)
    }
    return string;
  }

  /**
   * get numbers given in string<br>
   * e.g.
   * 
   * <code>
   * "1,2,6-10,12"
   * </code>
   * 
   * will result in
   * 
   * <code>
   * [1,2,6,7,8,9,10,12]
   * </code>
   * 
   * @param input
   * @return list of ints
   */
  public static int[] intList(String input) {
    List<Integer> results = new ArrayList<Integer>();
    String[] segments = input.split(",");

    for (String segment : segments) {
      try {
        if (segment.matches("\\d+-\\d+")) {
          String[] range = segment.split("-");
          int a = Integer.parseInt(range[0]);
          int b = Integer.parseInt(range[1]);
          if (a > b) {
            for (int i = b; i <= a; i++) {
              results.add(i);
            }
          } else if (a < b) {
            for (int i = a; i <= b; i++) {
              results.add(i);
            }
          } else {
            results.add(a);
          }
        } else if (segment.matches("\\d+")) {
          results.add(Integer.parseInt(segment));
        }
      } catch (NumberFormatException e) {
      }
    }

    int[] result = new int[results.size()];
    for (int i = 0; i < results.size(); i++) {
      result[i] = results.get(i);
    }
    return result;
  }
}

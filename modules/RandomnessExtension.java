

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;

/**
 * some commands to serve randomness
 * 
 * @author original by wipeD
 *
 */
public class RandomnessExtension extends AbstractCommandModule {

  public RandomnessExtension(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl.addCommand(new Command("random") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        String param = commandParam.getParam();
        Pattern pattern = Pattern.compile("\"([^\\s\"]+[^\"]*[^\\s\"]+)\"|([^\"\\s]+)");
        Matcher matcher = pattern.matcher(param);

        List<String> options = new ArrayList<String>();

        while (matcher.find()) {
          String option = matcher.group(1);
          option = option == null ? matcher.group(2) : option;
          option = option.replaceAll("^\\s+|\\s+$", "");
          if (!option.isEmpty()) {
            options.add(option);
          }
        }
        event.respond(options.get(ThreadLocalRandom.current().nextInt(options.size() - 1)));
      }

      @Override
      public String getParams() {
        return "<option> <option> [<option>...]";
      }

      @Override
      public String getHelp() {
        return "randomly choose one of the given options, use \"quotes\" for options containing whitespaces";
      }

    }.setPrivmsgCapable(true)).addCommand(new Command("roll") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {

        int min = 0;
        int max = 100;
        String param = commandParam.getParam();
        List<String> params = commandParam.getParams();
        if (!param.equals("") && !param.contains("-") && params.size() > 1) {
          try {
            min = Integer.parseInt(params.get(0));
            max = Integer.parseInt(params.get(1));
          } catch (NumberFormatException e) {
          }
        } else if (params.size() == 1) {
          try {
            max = Integer.parseInt(params.get(0));
          } catch (NumberFormatException e) {
          }
        }
        if (param.contains("-") && !param.startsWith("-")) {
          try {
            String[] subString = param.split("-");
            min = Integer.parseInt(subString[0]);
            max = Integer.parseInt(subString[1]);
          } catch (NumberFormatException e) {
          }
        }

        min = Math.abs(min);
        max = Math.abs(max);

        if (max > 999999) max = 999999;
        if (min > 999999) min = 0;

        if (max < min) {
          int t = max;
          max = min;
          min = t;
        } else if (max == min) max++;

        int rollsy = (int) ((Math.random() * (max - min + 1)) + min);

        event.respond("You rolled a " + rollsy + "! (" + min + " - " + max + ")");
      }

      @Override
      public String getParams() {
        return "[min] [max]";
      }

      @Override
      public String getHelp() {
        return "\"roll the dice\" and get a number in the given range; "
            + "if not specified the range will be 0-100; if one value is given, it will be the new maximum";
      }

    }.setPrivmsgCapable(true)).addCommand(new Command("flip", "flop", "flipflop") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        int rollsy = (int) (Math.random() * 2);

        event.respond("You " + commandParam.getCommand() + "ped a coin: " + (rollsy == 1 ? "Tails" : "Heads"));
      }

      @Override
      public String getHelp() {
        return "in need of a decision? flip a coin!";
      }

    }.setPrivmsgCapable(true));
  }

  @Override
  public String getName() {
    return "random";
  }

  @Override
  public String getDescription() {
    return "provides multiple commands that kind of serve randomness";
  }

}

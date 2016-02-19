package io.nard.ircbot.simplemods;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;

/**
 * some commands to serve randomness
 * 
 * @author wipeD
 *
 */
public abstract class RandomnessExtension {

  public static Listener module(BotConfig botConfig) {

    CommandListener commandListener = new CommandListener(botConfig);

    commandListener.addCommand(new Command("random") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        List<String> params = commandParam.getParams();

        if (params.size() == 0) {
          event.respond("Parameters insufficient");
          return;
        }
        List<String> returnList = new ArrayList<String>();

        String temp = "";
        boolean connected = false;
        for (String s : params) {
          if (s.startsWith("\"")) {
            connected = true;
            temp += s + " ";
          } else if (connected && s.endsWith("\"")) {
            connected = false;
            temp += s;
            temp = temp.substring(1, temp.length() - 1);
            returnList.add(temp);
            temp = "";
          } else if (!connected) {
            returnList.add(s);
          } else {
            temp += s + " ";
          }
        }

        int returny = (int) (Math.random() * returnList.size());
        event.respond(returnList.get(returny));
      }

    }).addCommand(new Command("roll") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {

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

        if (max > 999999)
          max = 999999;
        if (min > 999999)
          min = 0;

        if (max < min) {
          int t = max;
          max = min;
          min = t;
        } else if (max == min)
          max++;

        int rollsy = (int) ((Math.random() * (max - min + 1)) + min);

        event.respond("You rolled a " + rollsy + "! (" + min + " - " + max + ")");
      }

    }).addCommand(new Command("flip", "flop", "flipflop") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        int rollsy = (int) (Math.random() * 2);

        event.respond("You " + commandParam.getCommand() + "ped a coin: " + (rollsy == 1 ? "Tails" : "Heads"));
      }

    });

    return commandListener;

  }

}

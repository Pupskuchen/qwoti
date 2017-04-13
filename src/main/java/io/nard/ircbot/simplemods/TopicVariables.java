package io.nard.ircbot.simplemods;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.UserLevel;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.output.OutputChannel;

import com.google.common.collect.Lists;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;
import io.nard.ircbot.Privilege;

public abstract class TopicVariables {

  public static Listener module(BotConfig botConfig) {

    CommandListener commandListener = new CommandListener(botConfig);

    commandListener.addCommand(new Command(Privilege.NONE, "tvar") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        List<UserLevel> uLevels = Lists.newArrayList(event.getUser().getUserLevels(event.getChannel()));

        boolean topicAccess = false;
        for (UserLevel level : uLevels) {
          if (level.compareTo(UserLevel.OP) >= 0) { // pircbotx: OP < HALFOP
            topicAccess = true;
          }
        }
        if (!topicAccess) {
          event.respond("you may not change the topic");
          return;
        }

        List<String> params = commandParam.getParams();

        if (params.size() < 1) {
          event.respond("tvar variable [value]");
        } else {
          Map<String, String> tVars = new HashMap<String, String>();
          String topic = event.getChannel().getTopic();
          Pattern pattern = Pattern.compile("([^:\\s]+): ([^|]*[^\\s|])");
          Matcher matcher = pattern.matcher(topic);

          while (matcher.find()) {
            tVars.put(matcher.group(1), matcher.group(2));
          }

          String variable = params.get(0);
          String param = commandParam.getParam().replaceFirst(variable + "\\s+", "");
          OutputChannel chan = event.getChannel().send();
          boolean exists = tVars.containsKey(variable);

          if (params.size() == 1) {
            if (variable.equalsIgnoreCase("unset")) {
              event.respond("tvar unset [variable]");
            } else if (exists) {
              event.respond(variable + " = " + tVars.get(variable));
            } else {
              event.respond("you don't seem to know what you're doing");
            }
          } else if (params.size() == 2) {
            // numeric stuff
            if (variable.equalsIgnoreCase("unset")) {
              if (tVars.containsKey(param)) {
                updateTopic(chan, topic, tVars, param, null, true);
              } else {
                event.respond("I can't delete what isn't there");
              }
            } else if (exists) {
              try {
                int oldValue = Integer.parseInt(tVars.get(variable));
                String newValue = param;
                if (param.equals("++")) {
                  newValue = String.valueOf(oldValue + 1);
                } else if (param.equals("--")) {
                  newValue = String.valueOf(oldValue - 1);
                }
                updateTopic(chan, topic, tVars, variable, newValue, exists);
              } catch (NumberFormatException e) {
                updateTopic(chan, topic, tVars, variable, param, exists);
              }
            } else {
              updateTopic(chan, topic, tVars, variable, param, exists);
            }
          } else {
            updateTopic(chan, topic, tVars, variable, param, exists);
          }
        }
      }

      private void updateTopic(OutputChannel chan, String topic, Map<String, String> tVars, String variable,
          String newValue, boolean exists) {

        if (newValue == null) {
          String value = tVars.get(variable);
          String current = variable + ": " + value;
          chan.setTopic(topic.replaceAll("(?:\\s+\\|\\s+" + current + "|" + current + "\\s+\\|\\s+)", ""));
          return;
        }

        String change = variable + ": " + newValue;
        if (exists) {
          chan.setTopic(topic.replace(variable + ": " + tVars.get(variable), change));
        } else {
          chan.setTopic(topic + " | " + change);
        }
      }
    });

    return commandListener;

  }

}

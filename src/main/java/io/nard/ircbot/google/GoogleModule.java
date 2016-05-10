package io.nard.ircbot.google;

import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;

public class GoogleModule {

  public static Listener module(BotConfig botConfig) {
    CommandListener commandListener = new CommandListener(botConfig);

    commandListener.addCommand(new Command("g", "google") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond("usage: " + commandParam.getCommand() + " <expression>");
          return;
        }
        try {
          Google g = new Google(commandParam.getParam());
          for (int i = 0; i < 3; i++) {
            String[] result = g.get(i);
            if (result != null) {
              event.getChannel().send().message(result[0] + " (" + result[1] + ")");
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    return commandListener;
  }
}

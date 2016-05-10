package io.nard.ircbot.simplemods;

import org.pircbotx.hooks.Listener;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.CommandListener;

public class WebServices {

  public static Listener module(BotConfig botConfig) {
    CommandListener commandListener = new CommandListener(botConfig);

    return commandListener;
  }
}

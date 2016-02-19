package io.nard.ircbot.simplemods;

import org.pircbotx.hooks.Listener;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.CommandListener;

public abstract class DummyModule {

  public static Listener module(BotConfig botConfig) {
    CommandListener commandListener = new CommandListener(botConfig);

    

    return commandListener;
  }

}

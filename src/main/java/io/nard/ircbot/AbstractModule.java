package io.nard.ircbot;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;

public abstract class AbstractModule {

  protected PircBotX bot;
  protected BotConfig botConfig;
  protected BotHelper botHelper;
  protected ListenerManager listenerManager;

  public AbstractModule(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    this.bot = bot;
    this.botConfig = botConfig;
    this.botHelper = botHelper;
    this.listenerManager = listenerManager;
  }

  public abstract void startup();

  public abstract void shutdown();


  public abstract String getName();

  public abstract String getDescription();


  @Override
  public String toString() {
    return getName() + " - " + bot;
  }
}

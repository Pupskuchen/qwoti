package io.nard.ircbot;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;

public abstract class AbstractCommandModule extends AbstractModule {

  protected CommandListener cl;

  public AbstractCommandModule(PircBotX bot, BotConfig botConfig, BotHelper botHelper,
      ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl = new CommandListener(botConfig);
  }

  @Override
  public void startup() {
    listenerManager.addListener(cl);
  }

  @Override
  public void shutdown() {
    listenerManager.removeListener(cl);
  }

}

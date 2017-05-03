package quotes;

import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;

import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
//import io.nard.ircbot.quotes.Quote;
//import io.nard.ircbot.quotes.QuoteManager;

public class QuoteMove extends AbstractModule {

  public QuoteMove(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);
  }

  private void transport() {
    QuoteManager qm = new QuoteManager((String) null);
    quotes.QuoteManager nm = new quotes.QuoteManager((String) null);

    List<Quote> quotes = qm.getAll();
    qm.close();

    for (Quote q : quotes) {
      try {
        nm.save(new quotes.Quote(q.getUser(), q.getAdded(), q.getChannel(), q.getText(), q.getNetwork())
            .setViewId(q.getId()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void startup() {
    transport();
  }

  @Override
  public void shutdown() {
  }

  @Override
  public String getName() {
    return "quoteTransport";
  }

  @Override
  public String getDescription() {
    return "move quotes";
  }
}

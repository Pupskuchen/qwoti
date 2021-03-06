package quotes;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;

import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
//import io.nard.ircbot.quotes.Quote;
//import io.nard.ircbot.quotes.QuoteManagerLegacy;

public class QuoteMove extends AbstractModule {

  public QuoteMove(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);
  }

  private void transport() {
//    QuoteManagerLegacy qm = new QuoteManagerLegacy((String) null);
//    quotes.QuoteManager nm = new quotes.QuoteManager((String) null);
//
//    List<Quote> quotes = qm.getAll();
//    qm.close();
//
//    for (Quote q : quotes) {
//      try {
//        nm.save(new quotes.Quote(q.getUser(), q.getAdded(), q.getChannel(), q.getText(true), q.getNetwork())
//            .setViewId(q.getId()));
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
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

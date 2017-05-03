package quotes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import com.google.common.base.Joiner;

import io.nard.ircbot.AbstractListener;
import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;
import io.nard.ircbot.MessageBuffer;
import io.nard.ircbot.MessageBuffer.BufferEntry;
import io.nard.ircbot.MessageBuffer.MessageType;
import io.nard.ircbot.Privilege;

/**
 * Listener for IRC bot QuoteBot module
 */
public class QuoteBot extends AbstractModule {

  private List<Listener> listeners = new ArrayList<Listener>();
  private QuoteManager quoteManager;

  public QuoteBot(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    quoteManager = new QuoteManager(botConfig);
    CommandListener commandListener = new CommandListener(botConfig);
    new QuoteWeb(quoteManager);

    MessageBuffer messageBuffer = new MessageBuffer();

    commandListener.addCommand(new Command("add", "new") {

      private static final int MIN_QUOTE_LENGTH = 3;

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        // remove timestamps
        String regNL = "\\s+\\|\\s+([\\[<])"; // new line
        String regTS = "\\h*\\[?\\d+:(?:\\d+:?)+\\]?\\s*"; // timestamp
        String text = commandParam.getParam().trim().replaceAll(regNL, "\n$1").replaceAll(regTS, "");
        boolean privMsg = commandParam.isPrivMsg();
        if (text.length() < MIN_QUOTE_LENGTH) {
          event.respond("quotes have to be at least " + MIN_QUOTE_LENGTH + " characters long");
        } else {
          boolean saved = false;
          try {
            saved = quoteManager.save(new Quote(event.getUser(), privMsg ? null : commandParam.getChannel(), text,
                event.getBot().getServerInfo().getNetwork()));
          } catch (Exception e) {
          }

          if (saved) event.respond("saved quote #" + quoteManager.getLatestId());
          else event.respond("quote couldn't be saved");
        }
      }

      @Override
      public String getParams() {
        return "<quote>";
      }

      @Override
      public String getHelp() {
        return "add a new quote; use | to separate lines";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("q", "quote") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        Quote quote = null;
        boolean searched = false;
        if (commandParam.hasParam()) {
          Integer id = botConfig.toInt(commandParam.getParam());
          if (id != null) {
            quote = quoteManager.get(id);
          } else {
            List<String> params = commandParam.getParams();
            if (params.get(0).startsWith("#")) {
              if (params.size() > 1) {
                String chan = params.remove(0);
                String search = Joiner.on(' ').join(params);
                quote = QuoteManager.random(quoteManager.find(chan, search, false));
              } else {
                quote = quoteManager.get(params.get(0));
              }
            } else {
              quote = QuoteManager.random(quoteManager.find(null, commandParam.getParam(), false));
            }
          }
          searched = true;
        } else {
          quote = quoteManager.get();
        }
        if (quote != null) {
          if (commandParam.isPrivMsg()) event.respond(quote.niceString());
          else commandParam.getChannel().send().message(quote.niceString(commandParam.getChannel()));
        } else if (quote == null && searched) {
          event.respond("nothing found");
        } else if (quote == null) {
          event.respond("there are no quotes");
        }
      }

      @Override
      public String getParams() {
        return "[id]";
      }

      @Override
      public String getHelp() {
        return "show a quote; either with given id or a random one";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("count") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        long count = quoteManager.count();
        if (count == 1) event.respond("there is one quote");
        else event.respond(String.format("there are %d quotes", count));
      }

      @Override
      public String getHelp() {
        return "display amount of quotes saved";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("last") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        Quote quote = quoteManager.getLatest();
        if (quote != null) {
          if (commandParam.isPrivMsg()) event.respond(quote.niceString());
          else commandParam.getChannel().send().message(quote.niceString(commandParam.getChannel()));
        } else {
          event.respond("there are no quotes");
        }
      }

      @Override
      public String getHelp() {
        return "show most recent quote";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("find", "findexact") {

      private static final int MAX_FIND_RESULTS = 3;

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        List<String> params = commandParam.getParams();
        boolean exact = !commandParam.getCommand().equals("find");
        String channel = null, pattern = null;

        if (params.get(0).startsWith("#")) {
          channel = params.get(0);
          params.remove(0);
          pattern = Joiner.on(' ').join(params);
        } else {
          pattern = commandParam.getParam();
        }

        List<Quote> quotes;
        quotes = quoteManager.find(channel, pattern, exact);

        if (quotes == null || quotes.size() < 1) {
          event.respond("no matching quotes found");
        } else {
          int results = quotes.size();
          if (results > MAX_FIND_RESULTS) {
            List<Long> idList = new ArrayList<Long>();
            for (Quote quote : quotes) {
              idList.add(quote.getId());
            }
            event.respond("found " + results + " matching quotes: " + Joiner.on(", ").join(idList));
          } else {
            event.respond("found " + results + " matching quotes");
            for (Quote quote : quotes) {
              if (commandParam.isPrivMsg()) event.respond(quote.shortString());
              else commandParam.getChannel().send().message(quote.shortString(commandParam.getChannel()));
            }
          }
        }
      }

      @Override
      public String getParams() {
        return "<expression>";
      }

      @Override
      public String getHelp() {
        return "use " + Colors.BOLD + "find" + Colors.NORMAL
            + " to search for all quotes containing all of the keywords\n"//
            + "use " + Colors.BOLD + "findexact" + Colors.NORMAL + "to search for the exact expression";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.ADMIN, "del", "delete") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        Long id = null;
        if (commandParam.getParam().length() > 0) {
          try {
            id = (long) botConfig.toInt(commandParam.getParam());
          } catch (Exception e) {
          }
        }
        if (id != null) {
          if (quoteManager.delete(id)) {
            event.respond("quote #" + id + " deleted");
          } else {
            event.respond("couldn't delete that quote");
          }
        } else {
          event.respond("something about that id is wrong...");
        }
      }

      @Override
      public String getParams() {
        return "<id>";
      }

      @Override
      public String getHelp() {
        return "delete a quote";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("snap") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        List<String> params = commandParam.getParams();
        // if (params.size() < 1) {
        // event.respond("snap [user] [n] [offset] [skip]");
        // return;
        // }
        String user = null;
        int n = 1;
        int offset = 0;
        int[] skip = {};

        if (params.size() > 0) {
          try {
            n = Integer.parseInt(params.get(0));
          } catch (NumberFormatException e) {
            user = params.get(0);
            if (!commandParam.getChannel().getUsersNicks().contains(user)) {
              event.respond("who is dis?");
              return;
            }
          }
        }

        if (params.size() > 1) {
          if (user == null) {
            // snap [n]...
            try {
              // snap [n] [offset]
              offset = Integer.parseInt(params.get(1));
              if (params.size() > 2) {
                // snap [n] [offset] [skip]
                skip = QuoteBotUtils.intList(params.get(2));
              }
            } catch (NumberFormatException e) {
            }
          } else {
            // snap [user]
            try {
              // snap [user] [n]
              n = Integer.parseInt(params.get(1));
              if (params.size() > 2) {
                try {
                  // snap [user] [n] [offset]
                  offset = Integer.parseInt(params.get(2));
                  if (params.size() > 3) {
                    // snap [user] [n] [offset] [skip]
                    skip = QuoteBotUtils.intList(params.get(3));
                  }
                } catch (NumberFormatException e) {
                }
              }
            } catch (NumberFormatException e) {
            }
          }
        }

        String network = event.getBot().getServerInfo().getNetwork();
        String channel = commandParam.getChannel().getName();

        List<BufferEntry> messages;
        if (user != null) {
          messages = skip.length > 0 ? messageBuffer.getLast(network, channel, user, n, offset, skip)
              : messageBuffer.getLast(network, channel, user, n, offset);
        } else {
          messages = skip.length > 0 ? messageBuffer.getLast(network, channel, n, offset, skip)
              : messageBuffer.getLast(network, channel, n, offset);
        }

        if (messages.size() > 0) {
          boolean success = false;
          Quote quote = null;
          try {
            quote = new Quote(event.getUser(), commandParam.getChannel(), MessageBuffer.listToString(messages),
                network);
            success = quoteManager.save(quote);
          } catch (Exception e) {
          }
          if (success && quote != null) {
            commandParam.getChannel().send().message("saved " + quote.niceString(commandParam.getChannel()));
          } else {
            event.respond("quote couldn't be saved");
          }
        } else {
          event.respond("this didn't go so well (did nobody say something?)");
        }
      }

      @Override
      public String getParams() {
        return "[user] [lines] [offset] [skip]";
      }

      @Override
      public String getHelp() {
        return "\"snap\" what someone else previously said and save it as quote\n" //
            + Bot.format(Colors.BOLD, "user") + ": who to snap; "//
            + Bot.format(Colors.BOLD, "lines") + ": how many lines to snap; "//
            + Bot.format(Colors.BOLD, "offset") + ": how many lines since your desired line should be ignored\n" //
            + Bot.format(Colors.BOLD, "skip")
            + ": since you can snap multiple lines, you can also leave out some of them (e.g. \"2\" or \"2,4,6\" or \"2,4-6,8\")";
      }
    });

    listeners.add(commandListener);

    listeners.add(new AbstractListener() {

      @Override
      public void onMessage(MessageEvent event) {

        String network = event.getBot().getServerInfo().getNetwork();
        String user = event.getUser().getNick();
        String message = event.getMessage();
        Channel channel = event.getChannel();

        if (!message.startsWith(
            botConfig.getObject("networks").getJSONObject(network.toLowerCase()).getString("commandchar") + "snap")) {
          messageBuffer.add(network, channel.getName(), messageBuffer.new BufferEntry(user, message));
        }

        Pattern pattern = Pattern.compile(".*(?:\\A| |\\()q(\\d+)(?:\\Z| |\\)).*");

        if (message.matches(pattern.toString())) {

          Matcher matcher = pattern.matcher(message);
          if (matcher.find()) {

            long id = Long.parseLong(matcher.group(1));
            Quote quote = quoteManager.get(id);

            if (quote != null) {
              channel.send().message(quote.niceString(channel));
            }
          }
        }
      }

      @Override
      public void onAction(ActionEvent event) throws Exception {
        Channel chan = event.getChannel();
        if (chan != null) {
          String network = event.getBot().getServerInfo().getNetwork();
          String user = event.getUser().getNick();
          String message = event.getMessage();
          messageBuffer.add(network, chan.getName(), messageBuffer.new BufferEntry(user, message, MessageType.ACTION));
        }
      }
    });
  }

  @Override
  public void startup() {
    for (Listener l : listeners) {
      listenerManager.addListener(l);
    }
  }

  @Override
  public void shutdown() {
    for (Listener l : listeners) {
      listenerManager.removeListener(l);
    }
    quoteManager.close();
  }

  @Override
  public String getName() {
    return "quotes";
  }

  @Override
  public String getDescription() {
    return "easily save and manage user quotes";
  }
}

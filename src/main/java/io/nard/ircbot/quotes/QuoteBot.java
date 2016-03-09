package io.nard.ircbot.quotes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.base.Joiner;

import io.nard.ircbot.BotConfig;
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
public abstract class QuoteBot {

  /**
   * create QuoteBot listeners
   * 
   * @param botConfig
   * @return
   * @throws Exception
   */
  public static List<Listener> module(final BotConfig botConfig) {

    List<Listener> listeners = new ArrayList<Listener>();

    final QuoteManager quoteManager = new QuoteManager(botConfig);
    CommandListener commandListener = new CommandListener(botConfig);
    new QuoteWeb(quoteManager);

    MessageBuffer messageBuffer = new MessageBuffer();

    commandListener.addCommand(new Command("add", "new") {

      private static final int MIN_QUOTE_LENGTH = 3;

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        // remove timestamps
        String text = commandParam.getParam().trim().replaceAll("\\s*\\[?\\d+:(?:\\d+:?)+\\]?\\s*", "");
        if (text.length() < MIN_QUOTE_LENGTH) {
          event.respond("quotes have to be at least " + MIN_QUOTE_LENGTH + " characters long");
        } else {
          boolean saved = false;
          try {
            saved = quoteManager.save(
                new Quote(event.getUser(), event.getChannel(), text, event.getBot().getServerInfo().getNetwork()));
          } catch (Exception e) {
          }

          if (saved)
            event.respond("saved quote #" + quoteManager.getLatestId());
          else
            event.respond("quote couldn't be saved");
        }
      }
    }).addCommand(new Command("q", "quote") {
      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
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
          event.getChannel().send().message(quote.niceString(event.getChannel()));
        } else if (quote == null && searched) {
          event.respond("nothing found");
        } else if (quote == null) {
          event.respond("there are no quotes");
        }
      }
    }).addCommand(new Command("count") {
      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        event.respond(String.format("there are %d quotes", quoteManager.count()));
      }
    }).addCommand(new Command("last") {
      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        Quote quote = quoteManager.getLatest();
        if (quote != null) {
          event.getChannel().send().message(quote.niceString(event.getChannel()));
        } else {
          event.respond("there are no quotes");
        }
      }
    }).addCommand(new Command("find", "findexact") {

      private static final int MAX_FIND_RESULTS = 3;

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
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
              event.getChannel().send().message(quote.shortString(event.getChannel()));
            }
          }
        }
      }
    }).addCommand(new Command(Privilege.ADMIN, "del", "delete") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
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
    }).addCommand(new Command("snap") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
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
            if (!event.getChannel().getUsersNicks().contains(user)) {
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
        String channel = event.getChannel().getName();

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
            quote = new Quote(event.getUser(), event.getChannel(), MessageBuffer.listToString(messages), network);
            success = quoteManager.save(quote);
          } catch (Exception e) {
          }
          if (success && quote != null) {
            event.getChannel().send().message("saved " + quote.niceString(event.getChannel()));
          } else {
            event.respond("quote couldn't be saved");
          }
        } else {
          event.respond("this didn't go so well (did nobody say something?)");
        }
      }
    });

    listeners.add(commandListener);

    listeners.add(new ListenerAdapter() {

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

    return listeners;
  }
}

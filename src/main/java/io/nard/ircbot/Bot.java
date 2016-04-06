package io.nard.ircbot;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.SSLSocketFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.Configuration;
import org.pircbotx.MultiBotManager;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.nard.ircbot.quotes.QuoteBot;
import io.nard.ircbot.simplemods.Calculator;
import io.nard.ircbot.simplemods.Networking;
import io.nard.ircbot.simplemods.LastFM;
import io.nard.ircbot.simplemods.RandomnessExtension;
import io.nard.ircbot.simplemods.TopicVariables;
import io.nard.ircbot.web.APIHandler;
import pw.wiped.modules.RedditGrab;

public class Bot extends ListenerAdapter {

  public static final String BOTNAME = "qwoti";
  public static final String VERSION = "0.0.3a-dev";
  public static final String INFOURL = "https://nard.io/qwoti";

  private static HttpServer httpServer;

  public static void main(String[] args) {

    final long startUp = new Date().getTime();

    BotConfig botConfig;
    try {
      botConfig = new BotConfig(BOTNAME + ".json");
    } catch (Exception e) {
      System.out.println("error reading config file");
      e.printStackTrace();
      System.exit(1);
      return;
    }

    try {
      httpServer = HttpServer.create(new InetSocketAddress(botConfig.getInteger("httpPort", 8080)), 0);
      httpServer.createContext("/", new APIHandler("/") {

        @Override
        protected void process(HttpExchange httpExchange) throws IOException {
          sendResponse(httpExchange,
              "<!DOCTYPE html><html><head><title>" + BOTNAME + " " + VERSION + "</title></head><body>"
                  + "<h1>welcome</h1><p>this is " + BOTNAME + ", version " + VERSION + "</p>" //
                  + "<p>there's nothing here, go away</p>"//
                  + "</body></html>",
              ContentType.TEXT_HTML);
        }
      });
    } catch (IOException e) {
      System.out.println("error starting http server");
      e.printStackTrace();
      System.exit(2);
      return;
    }

    BotHelper botHelper = new BotHelper(botConfig);
    final MultiBotManager botManager = new MultiBotManager();

    final Map<String, Map<String, Privilege>> userPrivileges = botHelper.getPrivileges();

    Configuration.Builder globalConfig = new Configuration.Builder()//
        .setVersion(BOTNAME + " " + VERSION + " - " + INFOURL);

    globalConfig//
        .addListeners(QuoteBot.module(botConfig))//
        .addListener(RandomnessExtension.module(botConfig))//
        .addListener(TopicVariables.module(botConfig))//
        .addListener(Calculator.module(botConfig))//
        .addListener(RedditGrab.module(botConfig))//
        .addListener(LastFM.module(botConfig, botHelper))//
        .addListener(Networking.module(botConfig));
    // TODO: statistics module
    // TODO: scheduler/reminder/timer module
    // TODO: network-relay
    // TODO: reload/restart
    // TODO: last <offset> (higher offset -> older quotes)
    // TODO: OPT: global config reload (?)
    // TODO: wa / g / ud
    // TODO: interval: save config / log uptime (precision: seconds)
    // TODO: weather / temp
    // TODO: addlink

    CommandListener commandListener = new CommandListener(botConfig);
    commandListener.addCommand(new Command("ping", "ditti") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (commandParam.getCommand().equals("ditti")) {
          if (event.getUser().getNick().equals("Ditti"))
            event.respond("I <3 U");
          else
            event.respond("i <3 Ditti!");
        } else {
          event.respond("pong!");
        }
      }
    }).addCommand(new Command("version", "info", "about") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        event.respond("I'm " + BOTNAME + " " + VERSION + ", a dynamic IRC bot - " + INFOURL);
      }
    }).addCommand(new Command("time", "date") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date now = new Date();

        event.respond(f.format(now) + " | unix timestamp: " + (now.getTime() / 1000));
      }
    }).addCommand(new Command("uptime") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        long now = new Date().getTime();
        int uptime = (int) ((now - startUp) / 1000);

        int days = uptime / 86400;
        int hours = (uptime / 3600) % 24;
        int minutes = (uptime / 60) % 60;
        int seconds = uptime % 60;

        String result = "";
        if (days > 0)
          result += days + "d ";
        if (hours > 0 || days > 0)
          result += hours + "h ";
        if (hours > 0 || days > 0 || minutes > 0)
          result += minutes + "m ";
        if (hours > 0 || days > 0 || minutes > 0 || seconds > 0)
          result += seconds + "s";

        event.respond("I'm up for " + result);
      }
    }).addCommand(new Command("usage") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        Runtime runtime = Runtime.getRuntime();
        double mem = (((double) runtime.totalMemory()) - runtime.freeMemory()) / (1024L * 1024L); // -> MB

        BigDecimal display = new BigDecimal(mem);
        display = display.setScale(3, RoundingMode.HALF_UP);

        event.respond("memory in use: " + display.doubleValue() + " MB");
      }
    }).addCommand(new Command("whoami") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        String user = botHelper.getAccount(event.getBot(), event.getUser());
        event.respond("you're " + (user == null ? "not identified" : "identified as " + user));
      }
    }).addCommand(new Command("cache") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        botHelper.clearCache(event.getBot(), event.getUser(), commandParam.getParam().equals("refresh"));
      }
    }).addCommand(new Command("help", "commands") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {

        PircBotX bot = event.getBot();
        User user = event.getUser();

        String userAccount = botHelper.getAccount(bot, user);

        Privilege userPrivilege = userAccount == null ? Privilege.GUEST
            : userPrivileges.get(bot.getServerInfo().getNetwork().toLowerCase()).get(userAccount.toLowerCase());

        List<Listener> listeners = Lists.newArrayList(bot.getConfiguration().getListenerManager().getListeners());

        List<String> commands = new ArrayList<String>();

        for (Listener listener : listeners) {

          if (listener instanceof CommandListener) {
            List<Command> commandList = ((CommandListener) listener).getCommands();

            for (Command command : commandList) {
              if (userPrivilege.compareTo(command.getRequiredPrivilege()) >= 0) {
                commands.add(command.getCommands().get(0));
              }
            }

          } else if (listener instanceof Command) {
            Command command = (Command) listener;
            if (userPrivilege.compareTo(command.getRequiredPrivilege()) >= 0) {
              commands.add(command.getCommands().get(0));
            }
          }
        }

        user.send().message("available commands: " + Joiner.on(", ").join(commands));
      }
    }).addCommand(new Command(Privilege.ADMIN, "join") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        PircBotX bot = event.getBot();
        for (String channel : commandParam.getParams()) {
          if (!channel.startsWith("#")) {
            channel = "#" + channel;
          }
          bot.sendIRC().joinChannel(channel);
        }
      }
    }).addCommand(new Command(Privilege.ADMIN, "part") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        PircBotX bot = event.getBot();
        List<String> params = commandParam.getParams();
        for (int i = 0; i < params.size(); i += 2) {
          String channel = params.get(i);
          if (!channel.startsWith("#")) {
            channel = "#" + channel;
          }
          String message = params.size() > i + 1 ? params.get(i + 1) : "";
          bot.sendRaw().rawLine("PART " + channel + " " + message);
        }
      }
    }).addCommand(new Command(Privilege.OWNER, "quitall") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {

        String quitMessage = commandParam.getParam();
        quitMessage = quitMessage.length() > 0 ? quitMessage : "I shall leave you now.";

        for (PircBotX bot : botManager.getBots()) {
          String network = bot.getServerInfo().getNetwork().toLowerCase();
          List<String> currentChannels = new ArrayList<String>();

          for (Channel channel : bot.getUserBot().getChannels()) {
            currentChannels.add(channel.getName());
          }

          botHelper.updateChannelConfig(network, currentChannels);

          // bot.sendIRC().quitServer(quitMessage);

        }

        try {
          botConfig.save();
        } catch (IOException e) {
        }
        botManager.stop(quitMessage);
        System.exit(0); // .stop doesn't seem to stop the bot...
      }
    }).addCommand(new Command(Privilege.OWNER, "quit") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        String quitMessage = commandParam.getParam();
        quitMessage = quitMessage.length() > 0 ? quitMessage : "I shall leave you now.";

        PircBotX bot = event.getBot();

        String network = bot.getServerInfo().getNetwork().toLowerCase();
        List<String> currentChannels = new ArrayList<String>();

        for (Channel channel : bot.getUserBot().getChannels()) {
          currentChannels.add(channel.getName());
        }

        botHelper.updateChannelConfig(network, currentChannels);

        bot.sendIRC().quitServer(quitMessage);

        try {
          botConfig.save();
        } catch (IOException e) {
        }

        // sleep for a while because the bot is still connected afer disconnecting
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
        }
        List<PircBotX> bots = Lists.newArrayList(botManager.getBots());
        int connectedBots = 0;
        for (PircBotX aBot : bots) {
          if (aBot.isConnected()) {
            connectedBots++;
          }
        }
        if (connectedBots < 1) {
          botManager.stop(quitMessage);
          System.exit(0);
        }
      }

    }).addCommand(new Command(Privilege.OWNER, "config") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        List<String> params = commandParam.getParams();
        String network = event.getBot().getServerInfo().getNetwork().toLowerCase();

        if (params.size() == 0) {
          event.respond("config <property> [add|remove] [value]");
          return;
        }

        String property = params.get(0);
        Object configProperty = botHelper.getNetworkConfig(network, property);

        if (configProperty == null) {
          event.respond("that property doesn't exist");
          return;
        }

        if (params.size() == 1) {
          event.respond(configProperty.toString());
        } else {
          String value = params.get(1);
          Object configValue = null;
          if (configProperty instanceof String) {
            configValue = value;
          } else if (configProperty instanceof Boolean) {
            configValue = value.equalsIgnoreCase("true") || value.equals("1");
          } else if (configProperty instanceof Integer) {
            configValue = Integer.parseInt(value);
          } else if (configProperty instanceof Double) {
            configValue = Double.parseDouble(value);
          } else if (configProperty instanceof JSONArray) {
            if (params.size() == 3) {
              JSONArray array = (JSONArray) configProperty;
              String val = params.get(2);
              List<String> values = botConfig.toList(array);
              if (value.equalsIgnoreCase("add")) {
                if (!values.contains(val)) {
                  array.put(val);
                  configValue = array;
                } else {
                  event.respond("array already contains this value");
                }
              } else if (value.equalsIgnoreCase("remove")) {
                if (values.contains(val)) {
                  values.remove(val);
                  configValue = new JSONArray(values);
                } else {
                  event.respond("value not present in array");
                }
              } else {
                event.respond("config " + property + " add|remove <value>");
              }
            } else {
              event.respond("config " + property + " add|remove <value>");
            }
          }
          if (configValue != null) {
            botHelper.setNetworkConfig(network, property, configValue);
            event.respond("saved: " + configValue);
            try {
              botConfig.save();
              botHelper.refreshPrivileges();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }

    });

    globalConfig.addListener(commandListener);

    JSONObject networks = botConfig.getObject("networks");

    if (networks != null) {
      for (String networkName : JSONObject.getNames(networks)) {
        JSONObject network = networks.getJSONObject(networkName);
        JSONArray chanConf = network.getJSONArray("chans");
        List<String> chans = botConfig.toList(chanConf);

        if (chans.size() > 0) {
          Configuration.Builder serverConfiguration = new Configuration.Builder(globalConfig)//
              .setName(network.getString("nick"))//
              .setLogin(network.getString("ident"))//
              .setRealName(network.getString("realname"))//
              .setAutoNickChange(network.getBoolean("changenick"))//
              .setCapEnabled(network.getBoolean("cap"));

          serverConfiguration.addServer(network.getString("host"), network.getInt("port"));

          boolean useSSL = false, trustAllCerts = false;
          try {
            useSSL = network.getBoolean("ssl");
            trustAllCerts = network.getBoolean("trustAllCerts");
          } catch (JSONException e) {
          }
          if (useSSL) {
            if (trustAllCerts) {
              serverConfiguration.setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates());
            } else {
              serverConfiguration.setSocketFactory(SSLSocketFactory.getDefault());
            }
          }

          if (network.has("nickserv")) {
            JSONObject nickserv = network.getJSONObject("nickserv");
            String user = nickserv.optString("user", network.getString("nick"));
            String pass = nickserv.optString("pass", null);
            if (pass != null) {
              serverConfiguration.setNickservNick(user);
              serverConfiguration.setNickservPassword(pass);
            }
          }

          for (String chan : chans) {
            serverConfiguration.addAutoJoinChannel(chan);
          }

          boolean autoReconnect = network.has("autoreconnect") && network.optInt("autoreconnect", 0) >= 0;
          if (autoReconnect) {
            serverConfiguration.setAutoReconnect(true).setAutoReconnectDelay(network.getInt("autoreconnect") * 1000);
          }

          botManager.addBot(serverConfiguration.buildConfiguration());
        }
      }
    }

    botManager.start();
    getHttpServer().start();
  }

  public static HttpServer getHttpServer() {
    return httpServer;
  }

}

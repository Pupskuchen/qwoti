package io.nard.ircbot;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocketFactory;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.Configuration;
import org.pircbotx.MultiBotManager;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MotdEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputUser;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.nard.ircbot.web.APIHandler;

public class Bot {

  public static final String BOTNAME = "qwoti";
  public static final String VERSION = "0.0.5b";
  public static final String INFOURL = "https://github.com/pupskuchen/qwoti";

  private static HttpServer httpServer;
  private static final MultiBotManager botManager = new MultiBotManager();

  public static BotConfig botConfig;
  public static BotHelper botHelper;
  static {
    if (!init()) System.exit(1);
  }
  /**
   * active mods
   */
  private static Map<PircBotX, Map<String, AbstractModule>> modules = new HashMap<PircBotX, Map<String, AbstractModule>>();
  /**
   * inactive mods
   */
  private static Map<PircBotX, Map<String, AbstractModule>> availableMods = new HashMap<PircBotX, Map<String, AbstractModule>>();

  public static void main(String[] args) {

    final long startUp = new Date().getTime();

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


    final Map<String, Map<String, Privilege>> userPrivileges = botHelper.getPrivileges();

    Configuration.Builder globalConfig = new Configuration.Builder()//
        .setVersion(BOTNAME + " " + VERSION + " - " + INFOURL);

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
    commandListener.setPersistent(true);
    commandListener.addCommand(new Command("ping", "ditti") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (commandParam.getCommand().equals("ditti")) {
          if (event.getUser().getNick().equals("Ditti")) event.respond("I <3 U");
          else event.respond("i <3 Ditti!");
        } else {
          event.respond("pong!");
        }
      }

      @Override
      public String getHelp() {
        return "ping/pong to check for possible lag";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("version", "info", "about") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        event.respond("I'm " + BOTNAME + " " + VERSION + ", a dynamic IRC bot - " + INFOURL);
      }

      @Override
      public String getHelp() {
        return "display the bot's version and info URL";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("time", "date") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));

        Date now = new Date();

        event.respond(f.format(now) + " | unix timestamp: " + (now.getTime() / 1000));
      }

      @Override
      public String getHelp() {
        return "show date and time (GMT)";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("uptime") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        long now = new Date().getTime();
        int uptime = (int) ((now - startUp) / 1000);

        int days = uptime / 86400;
        int hours = (uptime / 3600) % 24;
        int minutes = (uptime / 60) % 60;
        int seconds = uptime % 60;

        String result = "";
        if (days > 0) result += days + "d ";
        if (hours > 0 || days > 0) result += hours + "h ";
        if (hours > 0 || days > 0 || minutes > 0) result += minutes + "m ";
        if (hours > 0 || days > 0 || minutes > 0 || seconds > 0) result += seconds + "s";

        event.respond("I'm up for " + result);
      }

      @Override
      public String getHelp() {
        return "show the bot's total uptime";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("usage") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        Runtime runtime = Runtime.getRuntime();
        double mem = (((double) runtime.totalMemory()) - runtime.freeMemory()) / (1024L * 1024L); // -> MB

        BigDecimal display = new BigDecimal(mem);
        display = display.setScale(3, RoundingMode.HALF_UP);

        event.respond("memory in use: " + display.doubleValue() + " MB");
      }

      @Override
      public String getHelp() {
        return "show the bot's current memory usage";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("whoami") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        String user = botHelper.getAccount(event.getBot(), event.getUser());
        event.respond("you're " + (user == null ? "not identified" : "identified as " + user));
      }

      @Override
      public String getHelp() {
        return "show your login name if you're logged in to NickServ (or an equivalent)";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command("cache") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        botHelper.clearCache(event.getBot(), event.getUser(), commandParam.getParam().equalsIgnoreCase("refresh"));
      }

      @Override
      public String getParams() {
        return "[refresh]";
      }

      @Override
      public String getHelp() {
        return "clear whois cache (login status, account information); "
            + "use refresh to automatically obtain and cache the current information";
      }
    }.setPrivmsgCapable(true)).addCommand((new Command("help") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {

        PircBotX bot = event.getBot();
        User user = event.getUser();

        String userAccount = botHelper.getAccount(bot, user);

        Privilege userPrivilege = userAccount == null ? Privilege.GUEST
            : userPrivileges.get(bot.getServerInfo().getNetwork().toLowerCase()).get(userAccount.toLowerCase());

        List<Listener> listeners = Lists.newArrayList(bot.getConfiguration().getListenerManager().getListeners());

        List<String> commands = new ArrayList<String>();
        Map<String, Command> allCommands = new HashMap<String, Command>();

        for (Listener listener : listeners) {

          if (listener instanceof CommandListener) {
            List<Command> commandList = ((CommandListener) listener).getCommands();

            for (Command command : commandList) {
              if (userPrivilege.compareTo(command.getRequiredPrivilege()) >= 0) {
                commands.add(command.getCommands().get(0));

                for (String cmd : command.getCommands()) {
                  allCommands.put(cmd, command);
                }
              }
            }

          } else if (listener instanceof Command) {
            Command command = (Command) listener;
            if (userPrivilege.compareTo(command.getRequiredPrivilege()) >= 0) {
              commands.add(command.getCommands().get(0));

              for (String cmd : command.getCommands()) {
                allCommands.put(cmd, command);
              }
            }
          }
        }

        OutputUser ou = user.send();
        if (commandParam.hasParam()) {
          String cmd = commandParam.getParam().toLowerCase();
          if (allCommands.containsKey(cmd)) {
            Command helpCmd = allCommands.get(cmd);

            String params = helpCmd.getParams();
            String help = helpCmd.getHelp();
            if (help == null || help.isEmpty()) help = "no description available";
            List<String> cmds = helpCmd.getCommands();

            List<String> outlines = Lists.newArrayList();
            StringJoiner sj = new StringJoiner(" | ");

            sj.add(format(Colors.BOLD, "command" + (cmds.size() == 1 ? "" : "s")) + ": " + Joiner.on(", ").join(cmds));
            sj.add(format(Colors.BOLD, "required privileges") + ": " //
                + helpCmd.getRequiredPrivilege().name().toLowerCase());
            sj.add(format(Colors.BOLD, "privmsg support") + ": " + (helpCmd.isPrivmsgCapable() ? "yes" : "no"));
            if (params != null) sj.add(helpCmd.getUsageHint(cmd));

            outlines.add(sj.toString());

            if (help != null) outlines.addAll(Lists.newArrayList(help.split("\n")));

            for (String line : outlines) {
              ou.message(line);
            }
          } else {
            event.respond("no such command");
          }
        } else {
          ou.message("available commands: " + Joiner.on(", ").join(commands));
          ou.message("type " + format(Colors.BOLD, getUsageHint(commandParam.getCommand(), false, false))
              + " to get more detailed information about a command");
        }
      }

      @Override
      public String getParams() {
        return "[command]";
      }

      @Override
      public String getHelp() {
        return "show list of commands or, if a command is given, help for that command";
      }
    }).setPrivmsgCapable(true)).addCommand(new Command(Privilege.ADMIN, "join") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        PircBotX bot = event.getBot();
        for (String channel : commandParam.getParams()) {
          if (!channel.startsWith("#")) {
            channel = "#" + channel;
          }
          bot.sendIRC().joinChannel(channel);
        }
      }

      @Override
      public String getParams() {
        return "<channel> [<channel>...]";
      }

      @Override
      public String getHelp() {
        return "join one or more channel(s)";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.ADMIN, "part") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        PircBotX bot = event.getBot();
        List<String> params = commandParam.getParams();
        if (!commandParam.isPrivMsg() && params.size() == 0) {
          commandParam.getChannel().send().part();
        } else if (params.size() == 0) {
          event.respond(getUsageHint());
        } else {
          for (int i = 0; i < params.size(); i += 2) {
            String channel = params.get(i);
            if (!channel.startsWith("#")) {
              channel = "#" + channel;
            }
            String message = params.size() > i + 1 ? params.get(i + 1) : "";
            bot.sendRaw().rawLine("PART " + channel + " " + message);
          }
        }
      }

      @Override
      public String getParams() {
        return "[channel] [msg] [<channel> <msg>...]";
      }

      @Override
      public String getHelp() {
        return "leave one or more channel(s); when only leaving one channel, part message is optional; "
            + "when leaving multiple channels, give a part message for each channel (no whitespace allowed)";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "quitall") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {

        String quitMessage = commandParam.getParam();
        quitMessage = quitMessage.length() > 0 ? quitMessage : "I shall leave you now.";

        for (PircBotX bot : botManager.getBots()) {
          String network = bot.getServerInfo().getNetwork().toLowerCase();
          List<String> currentChannels = new ArrayList<String>();

          if (modules.containsKey(bot)) {
            List<String> removed = new ArrayList<String>();
            for (AbstractModule mod : modules.get(bot).values()) {
              mod.shutdown();
              removed.add(mod.getName());
            }
            removed.forEach(m -> modules.get(bot).remove(m));
          }

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
        cleanUpModules();
        System.exit(0); // .stop doesn't seem to stop the bot...
      }

      @Override
      public String getParams() {
        return "[quitmsg]";
      }

      @Override
      public String getHelp() {
        return "disconnect the bot from every network and quit completely, optional quit message";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "quit") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        String quitMessage = commandParam.getParam();
        quitMessage = quitMessage.length() > 0 ? quitMessage : "I shall leave you now.";

        PircBotX bot = event.getBot();

        String network = bot.getServerInfo().getNetwork().toLowerCase();
        List<String> currentChannels = new ArrayList<String>();

        if (modules.containsKey(bot)) {
          List<String> removed = new ArrayList<String>();
          for (AbstractModule mod : modules.get(bot).values()) {
            mod.shutdown();
            removed.add(mod.getName());
          }
          removed.forEach(m -> modules.get(bot).remove(m));
        }

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
          cleanUpModules();
          System.exit(0);
        }
      }

      @Override
      public String getParams() {
        return "[quitmsg]";
      }

      @Override
      public String getHelp() {
        return "disconnect the bot from this network (with optional quit message), if no connections remain, quit completely";
      }

    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "config") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        List<String> params = commandParam.getParams();
        String network = event.getBot().getServerInfo().getNetwork().toLowerCase();

        if (params.size() == 0) {
          event.respond("config <property> [add|remove] [value]");
          return;
        }

        String property = params.get(0);
        Object configProperty = botHelper.getNetworkConfig(network, property);

        if (params.size() == 1) {
          event.respond(configProperty == null ? "that property doesn't exist yet" : configProperty.toString());
        } else {
          String value = commandParam.getParam().replaceFirst(params.get(0) + "\\s+", "").trim();
          Object configValue = null;

          if (configProperty == null) {
            String type = params.get(1);
            value = value.replaceFirst(params.get(1) + "\\s+", "").trim();
            if (type.equalsIgnoreCase("string")) configProperty = new String();
            else if (type.equalsIgnoreCase("bool")) configProperty = new Boolean(false);
            else if (type.equalsIgnoreCase("int")) configProperty = new Integer(0);
            else if (type.equalsIgnoreCase("float") || type.equalsIgnoreCase("double")) configProperty = new Double(0d);
            else if (type.equalsIgnoreCase("array") || type.equalsIgnoreCase("list")) configProperty = new JSONArray();
            else {
              event.respond("valid types are: string, bool, int, float, array");
              return;
            }
          }

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
              String val = params.get(1);
              value = value.replaceFirst(val + "\\s*", "");
              List<String> values = BotConfig.toList(array);
              if (val.equalsIgnoreCase("add")) {
                if (!values.contains(value)) {
                  array.put(value);
                  configValue = array;
                } else {
                  event.respond("array already contains this value");
                }
              } else if (val.equalsIgnoreCase("remove")) {
                if (values.contains(value)) {
                  values.remove(value);
                  configValue = new JSONArray(values);
                } else {
                  event.respond("value not present in array");
                }
              } else {
                try {
                  configValue = new JSONArray(value);
                } catch (Exception e) {
                  event.respond("malformed array");
                  return;
                }
              }
            } else {
              event.respond("config " + property + " add|remove <value>");
            }
          }
          if (configValue != null) {
            botHelper.setNetworkConfig(network, property, configValue);
            try {
              botConfig.save();
              botHelper.refreshPrivileges();
              event.respond("saved: " + configValue);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }

      @Override
      public String getParams() {
        return "<property> [type] [add|remove] [value]";
      }

      @Override
      public String getHelp() {
        return "change or add configuration properties - type needs to be specified if you're adding a new property; "
            + "add/remove only works for existing arrays; leave out add/remove when adding a new array\n"
            + "type should be one of: string, bool, int, float, array";
      }

    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "writeconfig") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        try {
          botConfig.save();
          event.respond("config file written");
        } catch (IOException e) {
          event.respond("couldn't write config to file");
        }
      }

      @Override
      public String getHelp() {
        return "write current config to file";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "raw") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (commandParam.hasParam()) event.getBot().sendRaw().rawLine(commandParam.getParam());
        else event.respond(getUsageHint());
      }

      @Override
      public String getParams() {
        return "<command>";
      }

      @Override
      public String getHelp() {
        return "send raw line";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "reload") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        init();
        if (commandParam.hasParam() && commandParam.getParam().equalsIgnoreCase("all")) {
          reloadAll();
        } else {
          reload(event.getBot());
        }
      }

      @Override
      public String getParams() {
        return "[all]";
      }

      @Override
      public String getHelp() {
        return "reload global bot configuration and (un-)load modules for this network; "
            + "when using all, do update modules for every network the bot is connected to";
      }
    }.setPrivmsgCapable(true)).addCommand(new Command(Privilege.OWNER, "module") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond(getUsageHint());
          return;
        }

        PircBotX bot = event.getBot();

        Map<AbstractModule, Boolean> modStates = new HashMap<AbstractModule, Boolean>();
        List<String> active = new ArrayList<String>();
        List<String> inactive = new ArrayList<String>();
        for (AbstractModule mod : modules.get(bot).values()) {
          modStates.put(mod, true);
          active.add(mod.getName());
        }
        for (AbstractModule mod : availableMods.get(bot).values()) {
          modStates.put(mod, false);
          inactive.add(mod.getName());
        }
        Collections.sort(active);
        Collections.sort(inactive);

        String cmd = commandParam.get(0);

        if (cmd.equals("list")) {
          event.respondPrivateMessage(
              format(Colors.BOLD, "active") + ": " + (active.size() > 0 ? Joiner.on(", ").join(active) : "none"));
          event.respondPrivateMessage(
              format(Colors.BOLD, "inactive") + ": " + (inactive.size() > 0 ? Joiner.on(", ").join(inactive) : "none"));
        } else {
          String module = commandParam.get(1);
          if (module == null) {
            event.respond(getUsageHint() + " - module required for this command");
            return;
          }

          AbstractModule mod = moduleByName(bot, module);
          if (mod == null) {
            event.respond("unknown module name");
            return;
          }

          switch (cmd) {
          case "load":
            if (modStates.get(mod)) {
              event.respond("module is already loaded");
            } else {
              try {
                mod.startup();
                availableMods.get(bot).remove(mod.getName());
                modules.get(bot).put(mod.getName(), mod);
                event.respond("module started");
              } catch (Exception e) {
                event.respond("failed to load module");
              }
            }
            break;
          case "unload":
            if (!modStates.get(mod)) {
              event.respond("module has not been loaded");
            } else {
              try {
                mod.shutdown();
                modules.get(bot).remove(mod.getName());
                availableMods.get(bot).put(mod.getName(), mod);
                event.respond("module stopped");
              } catch (Exception e) {
                event.respond("failed to unload module");
              }
            }
            break;
          case "info":
            event.respond(mod.getName() + ": " + mod.getDescription());
            break;
          case "status":
            event.respond(mod.getName() + " is " + (modStates.get(mod) ? "" : "not ") + "active");
            break;
          default:
            event.respond(getUsageHint());
          }
        }
      }

      public String getParams() {
        return "load|unload|info|status|list [module]";
      }

      @Override
      public String getHelp() {
        return "(un-)load a module, show its status or list all modules";
      }

    }.setPrivmsgCapable(true));

    globalConfig.addListener(commandListener)//
        .addListener(new ListenerAdapter() {

          @Override
          public void onMotd(MotdEvent event) throws Exception {
            reload(event.getBot());
          }

        });

    JSONObject networks = botConfig.getObject("networks");

    if (networks != null) {
      for (String networkName : JSONObject.getNames(networks)) {
        JSONObject network = networks.getJSONObject(networkName);
        JSONArray chanConf = network.getJSONArray("chans");
        List<String> chans = BotConfig.toList(chanConf);

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

  private static boolean init() {
    try {
      botConfig = new BotConfig(BOTNAME + ".json");
      botHelper = new BotHelper(botConfig);
    } catch (JSONException | IOException e) {
      System.out.println("error reading config file");
      e.printStackTrace();
    }
    return true;
  }

  private static AbstractModule moduleByName(PircBotX bot, String moduleName) {
    List<Map<PircBotX, Map<String, AbstractModule>>> maps = new ArrayList<Map<PircBotX, Map<String, AbstractModule>>>();
    maps.add(modules);
    maps.add(availableMods);

    for (Map<PircBotX, Map<String, AbstractModule>> map : maps) {
      if (map.containsKey(bot)) {
        for (AbstractModule module : map.get(bot).values()) {
          if (module.getName().equalsIgnoreCase(moduleName)) return module;
        }
      }
    }
    return null;
  }

  private static void getModules(PircBotX bot) {
    cleanUpModules();

    ListenerManager lm = bot.getConfiguration().getListenerManager();
    File modDir = new File("modules");
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    if (!modules.containsKey(bot)) {
      modules.put(bot, new HashMap<String, AbstractModule>());
      availableMods.put(bot, new HashMap<String, AbstractModule>());
    }
    Map<String, AbstractModule> botmods = modules.get(bot);
    Map<String, AbstractModule> available = availableMods.get(bot);
    botmods.clear();
    available.clear();

    String network = bot.getServerInfo().getNetwork().toLowerCase();
    List<String> enabled = BotConfig.toList(botConfig.getNetwork(network).getJSONArray("modules")).stream()
        .map(String::toLowerCase).collect(Collectors.toList());

    if (modDir.isDirectory()) {
      try {
        Files.walk(modDir.toPath())//
            .filter(Files::isRegularFile)//
            .filter(p -> p.toString().endsWith(".java"))//
            .distinct().forEach(path -> {

              String options = "-sourcepath " + modDir + " " + path.toString();
              compiler.run(null, null, null, options.split(" "));

              String className = path.getFileName().toString();
              className = className.substring(0, className.indexOf(".java"));

              String pkg = null;
              if (!path.getParent().equals(modDir.toPath())) {
                pkg = path.getParent().toString().substring(modDir.getPath().length() + 1)
                    .replaceAll("\\" + File.separator, ".");
                className = pkg + "." + className;
              }

              try {
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { modDir.toURI().toURL() });
                Class<?> clazz = Class.forName(className, true, classLoader).asSubclass(AbstractModule.class);
                Constructor<?> cons = null;
                try {
                  cons = clazz.getConstructor(PircBotX.class, BotConfig.class, BotHelper.class, ListenerManager.class);
                } catch (Exception e) {
                  Constructor<?>[] constructors = clazz.getConstructors();
                  if (constructors.length > 0) cons = constructors[0];
                }
                if (cons != null) {
                  try {
                    AbstractModule module = (AbstractModule) cons.newInstance(bot, botConfig, botHelper, lm);
                    String modName = module.getName();
                    if (enabled.contains(modName.toLowerCase())) {
                      module.startup();
                      botmods.put(modName, module);
                    } else {
                      available.put(modName, module);
                    }
                  } catch (Exception e) {
                    System.err.println("failed to load module: " + clazz.getName());
                    e.printStackTrace();
                  }
                }

              } catch (Throwable e) {
                System.err.print("failed to load java file " + path.toString() + " as module: ");
                if (e instanceof ClassCastException) {
                  System.err.println("does not extend " + AbstractModule.class.getName());
                } else if (e instanceof ClassNotFoundException) {
                  System.err.println("class not found");
                } else {
                  System.err.println(e.getMessage());
                  e.printStackTrace();
                }
              }
            });
      } catch (IOException e) {
        System.err.println("error while getting module files");
        e.printStackTrace();
      }
    }
  }

  public static void reloadAll() {
    for (PircBotX bot : botManager.getBots()) {
      reload(bot);
    }
  }

  public static void reload(PircBotX bot) {
    ListenerManager lm = bot.getConfiguration().getListenerManager();

    if (!modules.containsKey(bot)) {
      modules.put(bot, new HashMap<String, AbstractModule>());
      availableMods.put(bot, new HashMap<String, AbstractModule>());
    }
    Map<String, AbstractModule> botmods = modules.get(bot);

    // unload
    for (AbstractModule module : botmods.values()) {
      module.shutdown();
    }
    for (Listener listnr : lm.getListeners()) {
      if (listnr instanceof AbstractListener) {
        AbstractListener listener = (AbstractListener) listnr;
        if (!listener.isPersistent()) {
          lm.removeListener(listnr);
        }
      }
    }

    getModules(bot);
  }

  private static void cleanUpModules() {
    try {
      Files.walk(Paths.get("modules")).filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".class"))
          .distinct().forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
            }
          });
    } catch (IOException e) {
    }
  }

  public static HttpServer getHttpServer() {
    return httpServer;
  }

  public static String format(String format, String text) {
    return format + text + Colors.NORMAL;
  }

}

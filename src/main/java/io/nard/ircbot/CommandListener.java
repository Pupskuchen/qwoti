package io.nard.ircbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import irc.nard.ircbot.config.BotConfig;

public class CommandListener extends ListenerAdapter {
  private BotHelper botHelper;
  private Map<String, String> commandChars = new HashMap<String, String>();
  private List<Command> commands = new ArrayList<Command>();
  private Map<String, Map<String, Privilege>> userPrivileges = new HashMap<String, Map<String, Privilege>>();

  /**
   * create new CommandListener from given config file<br>
   * this listener will hold and handle Command objects
   * 
   * @param botConfig
   */
  public CommandListener(BotConfig botConfig) {
    this.botHelper = new BotHelper(botConfig);
    this.userPrivileges = botHelper.getPrivileges();

    JSONObject networks = botConfig.getObject("networks");
    for (String network : networks.keySet()) {
      commandChars.put(network.toLowerCase(), networks.getJSONObject(network).getString("commandchar"));
    }
  }

  /**
   * add a Command object to the listener
   * 
   * @param command
   * @return commandListener instance
   */
  public CommandListener addCommand(Command command) {
    this.commands.add(command);
    return this;
  }

  /**
   * get all command objects from this listener
   * 
   * @return list of Command objects
   */
  public List<Command> getCommands() {
    return commands;
  }

  @Override
  public void onMessage(MessageEvent event) throws Exception {
    String commandChar = commandChars.get(event.getBot().getServerInfo().getNetwork().toLowerCase());
    String message = event.getMessage();
    if (message.startsWith(commandChar) && message.length() > commandChar.length()) {
      CommandParam commandParam = new CommandParam(message, commandChar);
      String input = commandParam.getCommand();

      String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
      PircBotX bot = event.getBot();
      User user = event.getUser();

      String userAccount = botHelper.getAccount(bot, user);

      Privilege userPrivileges = userAccount == null ? Privilege.GUEST
          : this.userPrivileges.get(network).get(userAccount.toLowerCase());

      if (userPrivileges == null)
        userPrivileges = botHelper.isAuthenticated(bot, user) ? Privilege.PRIVILEGED : Privilege.GUEST;

      for (Command command : commands) {
        if (command.getCommands().contains(input)) {

          if (userPrivileges.compareTo(command.getRequiredPrivilege()) >= 0) {
            command.onCommand(commandParam, event);
          } else {
            event.respond("you do not have permission to use this command");
          }
        }
      }
    }
  }

  /*
   * this makes the bot unresponsive for the first seconds of usage (after joining a channel) // cache account name (is
   * user verified?)
   * 
   * @Override public void onJoin(JoinEvent event) throws Exception { PircBotX bot = event.getBot(); User user =
   * event.getUser(); Channel channel = event.getChannel();
   * 
   * Thread.sleep(500); // we need to wait for the bot to get the user list
   * 
   * if( user.canEqual(bot.getUserBot()) ) { for( User other : channel.getUsers() ) { botHelper.getAccount(bot, other);
   * } } else { botHelper.getAccount(bot, user); } }
   */
}

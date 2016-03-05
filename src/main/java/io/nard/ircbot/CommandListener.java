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

public class CommandListener extends ListenerAdapter {
  private BotHelper botHelper;
  private Map<String, String> commandChars = new HashMap<String, String>();
  private List<Command> commands = new ArrayList<Command>();

  /**
   * create new CommandListener from given config file<br>
   * this listener will hold and handle Command objects
   * 
   * @param botConfig
   */
  public CommandListener(BotConfig botConfig) {
    this.botHelper = new BotHelper(botConfig);

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
      String input = CommandParam.getCommand(message, commandChar);

      String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
      PircBotX bot = event.getBot();
      User user = event.getUser();

      String userAccount = null;
      boolean userAuthed = false;
      Privilege userPrivileges = Privilege.GUEST;

      for (Command command : commands) {
        if (command.getCommands().contains(input)) {

          if (userAccount == null) {
            userAccount = botHelper.getAccount(bot, user);

            userPrivileges = userAccount == null ? Privilege.GUEST
                : botHelper.getPrivileges().get(network).get(userAccount.toLowerCase());

            userAuthed = botHelper.isAuthenticated(bot, user);

            if (userPrivileges == null) {
              userPrivileges = userAuthed ? Privilege.PRIVILEGED : Privilege.GUEST;
            }
          }

          if (userPrivileges.compareTo(command.getRequiredPrivilege()) >= 0) {
            command.onCommand(new CommandParam(message, commandChar, userAccount, userAuthed), event);
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

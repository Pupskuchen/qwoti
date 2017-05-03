package io.nard.ircbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

public class CommandListener extends AbstractListener {

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
    this.botHelper = Bot.botHelper;

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
    handle(event, event.getMessage(), event.getUser());
  }

  @Override
  public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
    handle(event, event.getMessage(), event.getUser());
  }

  private void handle(Event oevent, String message, User user) {
    boolean isPrivMsg = oevent instanceof PrivateMessageEvent;
    GenericMessageEvent event = (GenericMessageEvent) oevent;

    Channel channel = null;
    if (!isPrivMsg) {
      channel = ((MessageEvent) oevent).getChannel();
    }

    String commandChar = isPrivMsg ? "" : commandChars.get(event.getBot().getServerInfo().getNetwork().toLowerCase());
    if (message.startsWith(commandChar) && message.length() > commandChar.length()) {
      String input = CommandParam.getCommand(message, commandChar);

      String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
      PircBotX bot = event.getBot();

      String userAccount = null;
      boolean userAuthed = false;
      Privilege userPrivileges = Privilege.GUEST;

      for (Command command : commands) {
        if (command.getCommands().contains(input) && (!isPrivMsg || command.isPrivmsgCapable())) {

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
            command.onCommand(new CommandParam(message, commandChar, userAccount, userAuthed, channel), event);
          } else {
            event.respond("you do not have permission to use this command");
          }
        } else if (command.getCommands().contains(input) && isPrivMsg && !command.isPrivmsgCapable()) {
          event.respond("this command cannot be used via private message");
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

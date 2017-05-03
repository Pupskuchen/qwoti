package io.nard.ircbot;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Colors;
import org.pircbotx.hooks.types.GenericMessageEvent;

import com.google.common.collect.Lists;

public abstract class Command {

  private List<String> commands;
  private final Privilege requiredPrivilege;
  private boolean privmsgCapable = false;

  /**
   * create new Command object with one or more commands to call it<br>
   * the user needs at least the given privileges to call the command
   * 
   * @param requiredPrivilege
   * @param commands
   * @throws Exception
   */
  public Command(Privilege requiredPrivilege, String... commands) {
    this.requiredPrivilege = requiredPrivilege;
    boolean failed = false;
    if (commands == null) {
      failed = true;
    } else {
      List<String> temp = Lists.newArrayList(commands);
      temp.removeAll(Lists.newArrayList("", null));
      commands = temp.toArray(new String[0]);
      if (commands.length < 1) failed = true;
    }
    if (failed) {
      // throw new Exception("you cannot create a command listener without commands");
    } else {
      this.commands = new ArrayList<String>();
      for (String command : commands) {
        this.commands.add(command.toLowerCase());
      }
    }

  }

  /**
   * create new Command object with one or more commands to call it
   * 
   * @param commands
   * @throws Exception
   */
  public Command(String... commands) {
    this(Privilege.GUEST, commands);
  }

  /**
   * get commands that call this Command
   * 
   * @return commands
   */
  public final List<String> getCommands() {
    return commands;
  };

  /**
   * get required privilege to run this command
   * 
   * @return privilege
   */
  public final Privilege getRequiredPrivilege() {
    return requiredPrivilege;
  }

  /**
   * event listener for messages
   * 
   * @param commandParam
   * @param event
   */
  public abstract void onCommand(CommandParam commandParam, GenericMessageEvent event);

  /**
   * get required and/or optional parameters for this command to be used in help
   * 
   * @return params
   */
  public String getParams() {
    return null;
  }

  /**
   * get help text for this command
   * 
   * @return help text
   */
  public abstract String getHelp();

  /**
   * get a hint on how to use this command by displaying it's params
   * 
   * @return usage hint
   */
  public String getUsageHint() {
    return getUsageHint(null);
  }

  /**
   * get a hint on how to use this command by displaying it's params
   * 
   * @param withHelp
   *          whether or not to show help text
   * 
   * @return usage hint
   */
  public String getUsageHint(boolean withHelp, boolean prefix) {
    return getUsageHint(null, withHelp, prefix);
  }

  /**
   * get a hint on how to use this command by displaying it's params
   * 
   * @param called
   *          which command to show usaeg with
   * 
   * @return usage hint
   */
  public String getUsageHint(String called) {
    return getUsageHint(called, false, true);
  }

  /**
   * get a hint on how to use this command, displays params and optionally help text
   * 
   * @param called
   *          which command to show usaeg with
   * @param withHelp
   *          whether or not to show help text
   * @param prefix
   *          prefix hint with "usage: "
   * @return usage hint
   */
  public String getUsageHint(String called, boolean withHelp, boolean prefix) {
    String text = prefix ? Bot.format(Colors.BOLD, "usage") + ": " : "";
    if (getParams() != null) {
      text += (called != null ? called + " " : "") + getParams();
      if (withHelp && getHelp() != null) {
        text += " - " + getHelp();
      }
    }
    return text;
  }

  /**
   * control whether this command can be called from both channels and private messages
   * 
   * @param privmsgCapable
   *          true = channel + privmsg; false = channel
   */
  public Command setPrivmsgCapable(boolean privmsgCapable) {
    this.privmsgCapable = privmsgCapable;
    return this;
  }


  /**
   * whether this command can be called from both channels and private messages
   * 
   * @return true = channel + privmsg; false = channel
   */
  public boolean isPrivmsgCapable() {
    return privmsgCapable;
  }

  @Override
  public String toString() {
    return commands.toString();
  }
}

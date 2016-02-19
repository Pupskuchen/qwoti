package io.nard.ircbot;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.collect.Lists;

public abstract class Command {

  private List<String> commands;
  private final Privilege requiredPrivilege;

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
    setUp(commands);
  }

  /**
   * create new Command object with one or more commands to call it
   * 
   * @param commands
   * @throws Exception
   */
  public Command(String... commands) {
    this.requiredPrivilege = Privilege.GUEST;
    setUp(commands);
  }

  /**
   * set up the command
   * 
   * @param commands
   * @throws Exception
   */
  private void setUp(String... commands) {
    boolean failed = false;
    if( commands == null ) {
      failed = true;
    } else {
      List<String> temp = Lists.newArrayList(commands);
      temp.removeAll(Lists.newArrayList("", null));
      commands = temp.toArray(new String[0]);
      if( commands.length < 1 )
        failed = true;
    }
    if( failed ) {
//        throw new Exception("you cannot create a command listener without commands");
    } else {
      this.commands = new ArrayList<String>();
      for( String command : commands ) {
        this.commands.add(command.toLowerCase());
      }
    }
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
  public abstract void onCommand(CommandParam commandParam, MessageEvent event);
}

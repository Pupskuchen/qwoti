package io.nard.ircbot;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Channel;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class CommandParam {

  private String command;
  private String param;
  private List<String> params;
  private String userAccount;
  private boolean userAuthed;
  private Channel channel;

  /**
   * get the command
   * 
   * @return command
   */
  public String getCommand() {
    return command;
  }

  /**
   * get everything passed with the command
   * 
   * @return parameter(s) as string
   */
  public String getParam() {
    return param;
  }

  /**
   * get channel the command was called in
   * 
   * @return channel or null if privmsg
   */
  public Channel getChannel() {
    return channel;
  }

  /**
   * check whether this command was called from a channel or privmsg
   * 
   * @return true if command was called in private message
   */
  public boolean isPrivMsg() {
    return channel == null;
  }

  /**
   * get the first parameter or null if there is none
   * 
   * @return first parameter
   */
  public String getFirst() {
    return hasParam() ? getParams().get(0) : null;
  }

  /**
   * convenience method to get command parameters
   * 
   * @param index
   * @return parameter at given index or null
   */
  public String get(int index) {
    if (index >= 0 && index < params.size()) {
      return params.get(index);
    }
    return null;
  }

  /**
   * check wheter there is a parameter
   * 
   * @return true if there's at least one parameter
   */
  public boolean hasParam() {
    return getParams().size() > 0;
  }

  /**
   * get list of all parameters
   * 
   * @return list of parameters
   */
  public List<String> getParams() {
    return params;
  }

  public String getUserAccount() {
    return userAccount;
  }

  public boolean isUserAuthed() {
    return userAuthed;
  }

  /**
   * create CommandParam
   * 
   * @param input
   * @param userAccount
   * @param userAuthed
   * @param channel
   */
  public CommandParam(String input, String userAccount, boolean userAuthed, Channel channel) {
    List<String> temp = Lists.newArrayList(Splitter.on(' ').trimResults().omitEmptyStrings().split(input));

    this.command = temp.remove(0).toLowerCase();
    this.params = new ArrayList<String>(temp);
    this.param = Joiner.on(' ').join(temp);

    this.userAccount = userAccount;
    this.userAuthed = userAuthed;

    this.channel = channel;
  }

  /**
   * create CommandParam, remove command char from input
   * 
   * @param input
   * @param commandChar
   * @param userAccount
   * @param userAuthed
   * @param channel
   */
  public CommandParam(String input, String commandChar, String userAccount, boolean userAuthed, Channel channel) {
    this(input.substring(commandChar.length()), userAccount, userAuthed, channel);
  }

  /**
   * get the command the user called
   * 
   * @param message
   *          raw input message
   * @param commandChar
   *          the call character
   * @return command
   */
  public static String getCommand(String message, String commandChar) {
    return Lists.newArrayList(Splitter.on(' ').trimResults().omitEmptyStrings()//
        .split(message.substring(commandChar.length()))).remove(0).toLowerCase();
  }
}

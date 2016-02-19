package io.nard.ircbot;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class CommandParam {
  private String command;
  private String param;
  private List<String> params;

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

  private void setUp(String input) {
    List<String> temp = Lists.newArrayList(Splitter.on(' ').trimResults().omitEmptyStrings().split(input));

    this.command = temp.remove(0).toLowerCase();
    this.params = new ArrayList<String>(temp);
    this.param = Joiner.on(' ').join(temp);
  }

  /**
   * create CommandParam
   * 
   * @param input
   */
  public CommandParam(String input) {
    setUp(input);
  }

  /**
   * create CommandParam, remove command char from input
   * 
   * @param input
   * @param commandChar
   */
  public CommandParam(String input, String commandChar) {
    setUp(input.substring(commandChar.length()));
  }
}

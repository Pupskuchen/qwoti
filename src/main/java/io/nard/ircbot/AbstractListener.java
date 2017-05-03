package io.nard.ircbot;

import org.pircbotx.hooks.ListenerAdapter;


public abstract class AbstractListener extends ListenerAdapter {

  private boolean persistent = false;

  /**
   * set persistence of this listener (decides if listener will be removed when modules are reloaded)
   * 
   * @param persistent
   */
  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  /**
   * whether or not this listener should persist when modules are reloaded
   * 
   * @return persistent
   */
  public boolean isPersistent() {
    return persistent;
  }
}

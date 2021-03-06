package io.nard.ircbot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.WhoisEvent;

public class BotHelper {

  /**
   * max time to cache (in minutes)
   */
  private int CACHE_MAX_AGE = 360;

  /**
   * cache stuff because whois is slow
   */
  private static Map<UUID, Map<Long, String>> userAccounts = new HashMap<UUID, Map<Long, String>>();
  private static Map<UUID, Map<Long, Boolean>> userAuthed = new HashMap<UUID, Map<Long, Boolean>>();
  private static Map<String, Map<String, Privilege>> userPrivileges = new HashMap<String, Map<String, Privilege>>();

  private BotConfig botConfig = null;

  public BotHelper(BotConfig botConfig) {
    this.botConfig = botConfig;

    CACHE_MAX_AGE = botConfig.getInteger("whoiscache", CACHE_MAX_AGE);

    constructPrivileges(botConfig);
  }

  /**
   * clear cache of given user if data was cached
   * 
   * @param bot
   * @param user
   * @param refresh
   */
  public void clearCache(PircBotX bot, User user, boolean refresh) {
    UUID uid = user.getUserId();
    userAccounts.remove(uid);
    userAuthed.remove(uid);
    if (refresh) {
      getAccount(bot, user);
    }
  }

  /**
   * get the name the given user is registered with
   * 
   * @param bot
   * @param user
   * @return name of the user's account (or null)
   */
  public String getAccount(PircBotX bot, User user) {
    UUID userId = user.getUserId();
    if (userAccounts.containsKey(userId)) {
      Map.Entry<Long, String> cachedName = userAccounts.get(userId).entrySet().iterator().next();
      if (new Date().getTime() - CACHE_MAX_AGE * 60000 < cachedName.getKey()) {
        return cachedName.getValue();
      }
    }
    try {
      bot.sendRaw().rawLine("WHOIS " + user.getNick() + " " + user.getNick());
      WaitForQueue waitForQueue = new WaitForQueue(bot);
      while (true) {
        WhoisEvent event = waitForQueue.waitFor(WhoisEvent.class);
        if (!event.getNick().equals(user.getNick())) continue;

        waitForQueue.close();
        boolean identified = event.isRegistered();

        String registered = event.getRegisteredAs();
        String account = identified ? (registered.isEmpty() ? event.getNick() : registered) : null;

        Map<Long, String> newEntry = new HashMap<Long, String>();
        newEntry.put(new Date().getTime(), account);
        userAccounts.put(userId, newEntry);

        Map<Long, Boolean> newAuthEntry = new HashMap<Long, Boolean>();
        newAuthEntry.put(new Date().getTime(), identified);
        userAuthed.put(userId, newAuthEntry);

        return account;
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException("Error while getting the account name of " + user.getNick(), ex);
    }
  }

  /**
   * get the account of someone in a channel by their nickname
   * 
   * @param channel
   * @param user
   * @return user account or null
   */
  public String getAccount(Channel channel, String user) {
    if (channel == null) return null;
    List<User> users = channel.getUsers().asList();
    for (User u : users) {
      if (u.getNick().equals(user)) {
        return getAccount(channel.getBot(), u);
      }
    }
    return null;
  }

  /**
   * find out if user is logged in or not
   * 
   * @param bot
   * @param user
   * @return true if authenticaed, false if not
   */
  public boolean isAuthenticated(PircBotX bot, User user) {
    UUID userId = user.getUserId();
    if (userAuthed.containsKey(userId)) {
      Map.Entry<Long, Boolean> cachedState = userAuthed.get(userId).entrySet().iterator().next();
      if (new Date().getTime() - CACHE_MAX_AGE * 60000 < cachedState.getKey()) {
        return cachedState.getValue();
      }
    }

    getAccount(bot, user);

    return userAuthed.get(userId).entrySet().iterator().next().getValue();
  }

  /**
   * get user privileges from config
   * 
   * @return map of privileges
   */
  public Map<String, Map<String, Privilege>> getPrivileges() {
    return userPrivileges;
  }

  /**
   * construct internal privileges map
   * 
   * @param botConfig
   * @param privilege
   * @param configValue
   */
  private void constructPrivileges(BotConfig botConfig) {
    if (botConfig == null) return;

    Map<String, Privilege> privileges = new HashMap<String, Privilege>();
    privileges.put("blacklisted", Privilege.NONE);
    privileges.put("unprivileged", Privilege.GUEST);
    privileges.put("admins", Privilege.ADMIN);
    privileges.put("owners", Privilege.OWNER);

    JSONObject networks = botConfig.getObject("networks");

    if (networks != null) {
      for (String network : JSONObject.getNames(networks)) {
        network = network.toLowerCase();

        if (userPrivileges.get(network) == null) {
          userPrivileges.put(network, new HashMap<String, Privilege>());
        }

        for (String priv : privileges.keySet()) {
          List<String> accounts = BotConfig.toList(networks.getJSONObject(network).getJSONArray(priv));
          for (String account : accounts) {
            userPrivileges.get(network).put(account.toLowerCase(), privileges.get(priv));
          }
        }
      }
    }
  }

  /**
   * refresh user privileges (re-read from config)
   */
  public void refreshPrivileges() {
    userPrivileges.clear();
    constructPrivileges(botConfig);
  }

  public void updateChannelConfig(String network, List<String> currentChannels) {
    setNetworkConfig(network, "chans", currentChannels);
  }

  public JSONObject getNetworkConfig(String network) {
    return botConfig.getObject("networks").getJSONObject(network);
  }

  public Object getNetworkConfig(String network, String key) {
    try {
      return getNetworkConfig(network).get(key);
    } catch (JSONException e) {
      return null;
    }
  }

  public void setNetworkConfig(String network, String key, Object value) {
    JSONObject networks = botConfig.getObject("networks");
    JSONObject networkC = networks.getJSONObject(network);
    networkC.put(key, value);
    networks.put(network, networkC);
    botConfig.putObject("networks", networks);
  }

  public String getAPIKey(String api) {
    if (botConfig.has("api_keys")) {
      JSONObject o = botConfig.getObject("api_keys");
      if (o.has(api)) return o.getString(api);
    }
    return null;
  }
}

package io.nard.ircbot.quotes;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.jdo.annotations.Unique;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.pircbotx.Channel;
import org.pircbotx.User;

@Entity
public class Quote implements Serializable {

  private static final long serialVersionUID = 1L;

  private long added;
  private String user;
  private String channel;
  private String network;
  private String text;
  @Unique
  private Long viewId;

  @Id
  @GeneratedValue
  private long id;

  /**
   * get current time in milliseconds
   * 
   * @return
   */
  private static long getCurrentTime() {
    Date now = new Date();
    return now.getTime();
  }

  private void setUp(User user, long added, Channel channel, String text, String network) {
    this.user = user.getNick();
    this.added = added;
    this.channel = channel.getName();
    this.text = text;
    this.network = network;
  }

  /**
   * create new quote (with current time)
   * 
   * @param user
   * @param channel
   * @param text
   * @param network
   * @throws Exception
   */
  public Quote(User user, Channel channel, String text, String network) throws Exception {

    if (user == null || text == null || channel == null || network == null)
      throw new Exception("missing mandatory quote properties");

    setUp(user, getCurrentTime(), channel, text, network);
  }

  /**
   * create new quote
   * 
   * @param user
   * @param added
   * @param channel
   * @param text
   * @param network
   * @throws Exception
   */
  public Quote(User user, Long added, Channel channel, String text, String network) throws Exception {

    if (user == null || text == null || channel == null || network == null)
      throw new Exception("missing mandatory quote properties");

    if (added == null)
      added = getCurrentTime();

    setUp(user, added, channel, text, network);
  }

  public String getChannel() {
    return channel;
  }

  public String getNetwork() {
    return network;
  }

  public String getUser() {
    return user;
  }

  /**
   * get time when quote was added as readable string
   * 
   * @return readable time string
   */
  public String readableTime() {
    SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    f.setTimeZone(TimeZone.getTimeZone("GMT"));
    return f.format(new Date(added));
  }

  public long getAdded() {
    return added;
  }

  public String getText() {
    return text.replaceAll("\\n", " | ");
  }

  public String getText(boolean newLines) {
    return newLines ? text : getText();
  }

  public long getInternalId() {
    return id;
  }

  public Long getId() {
    return viewId;
  }

  /**
   * set displayed ID of quote
   * 
   * @param viewId
   * @return true if ID was set, false if it wasn't
   */
  public boolean setViewId(Long viewId) {
    if (this.viewId != null) {
      return false;
    }
    this.viewId = viewId;
    return true;
  }

  /**
   * get quote as nice, readable string<br>
   * if <code>currentChannel != null</code>, all strings will be escaped so they won't highlight
   * users in the desired channel
   * 
   * @param currentChannel
   * @return quote as nicely formatted string
   */
  public String niceString(Channel currentChannel) {
    return niceString(currentChannel, false);
  }

  /**
   * get quote as nice, readable string<br>
   * this will highlight users if the quote contains their nick
   * 
   * @return quote as nicely formatted string
   */
  public String niceString() {
    return niceString(null, false);
  }

  private String niceString(Channel currentChannel, boolean shortString) {
    String text = getText();
    String user = getUser();
    String network = getNetwork();
    String channel = getChannel();
    Long id = getId();
    String quote;
    if (shortString) {
      quote = String.format("(#%d by %s, %s/%s): « %s »", id, user, network, channel, text);
    } else {
      quote = String.format("quote #%d: « %s » (%s by %s in %s/%s)", id, text, readableTime(), user, network, channel);
    }
    return QuoteBotUtils.unhighlight(quote, currentChannel);
  }

  /**
   * get quote as nice, readable and short string<br>
   * parameters will be unhighlighted if channel is given (not null)
   * 
   * @param currentChannel
   * @return readable, short quote
   */
  public String shortString(Channel currentChannel) {
    return niceString(currentChannel, true);
  }

  /**
   * get quote as nice, readable and short string<br>
   * this will highlight users if the quote contains their nick
   * 
   * @return quote as nicely formatted string
   */
  public String shortString() {
    return shortString(null);
  }

  /**
   * makes quote object readable
   */
  @Override
  public String toString() {
    return String.format("(#%d by %s, %s, %s/%s): « %s »", getId(), getUser(), readableTime(), getNetwork(),
        getChannel(), getText());
  }
}

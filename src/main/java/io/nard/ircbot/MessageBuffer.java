package io.nard.ircbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageBuffer {

  public static final int MAX_BUFFER = 30;

  private Map<String, Map<String, List<BufferEntry>>> bufferMap = new HashMap<String, Map<String, List<BufferEntry>>>();

  // private List<BufferEntry> buffer = new ArrayList<BufferEntry>();

  public void add(String network, String channel, BufferEntry bufferEntry) {
    if (!bufferMap.containsKey(network)) {
      bufferMap.put(network, new HashMap<String, List<BufferEntry>>());
    }
    if (!bufferMap.get(network).containsKey(channel)) {
      bufferMap.get(network).put(channel, new ArrayList<BufferEntry>());
    }

    List<BufferEntry> buffer = bufferMap.get(network).get(channel);
    buffer.add(bufferEntry);
    if (buffer.size() > MAX_BUFFER) {
      buffer.remove(0);
    }
  }

  /**
   * get all buffer entries
   * 
   * @return list of buffer entries
   */
  public Map<String, Map<String, List<BufferEntry>>> getAll() {
    return bufferMap;
  }

  /**
   * get all buffer entries for given network
   * 
   * @param network
   * @return list of buffer entries
   */
  public Map<String, List<BufferEntry>> getAll(String network) {
    return bufferMap.get(network);
  }

  /**
   * get all buffer entries for given network and channel
   * 
   * @param network
   * @param channel
   * @return list of buffer entries
   */
  public List<BufferEntry> getAll(String network, String channel) {
    return bufferMap.get(network).get(channel);
  }

  /**
   * get maximum buffer size
   * 
   * @return buffer size
   */
  public static int getMaxBuffer() {
    return MAX_BUFFER;
  }

  /**
   * get last line
   * 
   * @return last line as buffer entry
   */
  public BufferEntry getLast(String network, String channel) {
    List<BufferEntry> buffer = bufferMap.get(network).get(channel);
    return buffer.size() > 0 ? buffer.get(buffer.size() - 1) : null;
  }

  /**
   * get last n lines
   * 
   * @param n
   * @return list of buffer entries
   */
  public List<BufferEntry> getLast(String network, String channel, int n) {
    return getLast(network, channel, n, 0);
  }

  /**
   * get last n lines with given offset
   * 
   * @param n
   * @param offset
   * @return list of buffer entries
   */
  public List<BufferEntry> getLast(String network, String channel, int n, int offset) {
    List<BufferEntry> buffer = bufferMap.get(network).get(channel);
    List<BufferEntry> list = new ArrayList<BufferEntry>();

    for (int i = buffer.size() - 1 - offset; i >= 0; i--) {
      list.add(0, buffer.get(i));
      if (list.size() >= n) {
        return list;
      }
    }

    return list;
  }

  /**
   * get buffer lines but skip given lines
   * 
   * @param network
   * @param channel
   * @param n
   * @param offset
   * @param skip
   * @return list of buffer entries
   */
  public List<BufferEntry> getLast(String network, String channel, int n, int offset, int... skip) {
    List<BufferEntry> buffer = getLast(network, channel, n, offset);
    return getLast(buffer, skip);
  }

  /**
   * remove skipped lines from buffer list
   * 
   * @param bufferEntries
   * @param skip
   * @return list of buffer entries without skipped lines
   */
  private List<BufferEntry> getLast(List<BufferEntry> bufferEntries, int... skip) {
    Map<Integer, BufferEntry> bufferMap = new HashMap<Integer, BufferEntry>();

    for (int i = 0; i < bufferEntries.size(); i++) {
      bufferMap.put(i, bufferEntries.get(i));
    }

    for (int i = 0; i < skip.length; i++) {
      if (bufferMap.containsKey(skip[i] - 1)) {
        bufferMap.remove(skip[i] - 1);
      }
    }
    return new ArrayList<BufferEntry>(bufferMap.values());
  }

  /**
   * get last n lines of user
   * 
   * @param user
   * @param n
   * @return list of buffer entries
   */
  public List<BufferEntry> getLast(String network, String channel, String user, int n) {
    return getLast(network, channel, user, n, 0);
  }

  /**
   * get last line of user
   * 
   * @param user
   * @return buffer entry
   */
  public BufferEntry getLast(String network, String channel, String user) {
    List<BufferEntry> buffer = getLast(network, channel, user, 1);
    if (buffer.size() < 1) {
      return null;
    } else {
      return buffer.get(0);
    }
  }

  /**
   * get last n lines of user with offset
   * 
   * @param user
   * @param n
   * @param offset
   * @return list of buffer entries
   */
  public List<BufferEntry> getLast(String network, String channel, String user, int n, int offset) {
    List<BufferEntry> buffer = bufferMap.get(network).get(channel);
    List<BufferEntry> list = new ArrayList<BufferEntry>();
    List<BufferEntry> userBuffer = new ArrayList<BufferEntry>();

    for (BufferEntry entry : buffer) {
      if (entry.getUser().equals(user)) {
        userBuffer.add(entry);
      }
    }

    for (int i = userBuffer.size() - 1 - offset; i >= 0; i--) {
      BufferEntry entry = userBuffer.get(i);
      if (entry.getUser().equals(user)) {
        list.add(0, entry);
        if (list.size() >= n) {
          return list;
        }
      }
    }

    return list;
  }

  public List<BufferEntry> getLast(String network, String channel, String user, int n, int offset, int... skip) {
    List<BufferEntry> buffer = getLast(network, channel, user, n, offset);
    return getLast(buffer, skip);
  }

  /**
   * get a string from buffer entries
   * 
   * @param bufferEntries
   * @return buffer entries as string
   */
  public static String listToString(List<BufferEntry> bufferEntries) {
    String result = "";

    for (int i = 0; i < bufferEntries.size(); i++) {
      result += bufferEntries.get(i);
      if (i < bufferEntries.size() - 1) {
        result += "\n";
      }
    }

    return result;
  }

  /**
   * a buffer entry stores user and message
   */
  public class BufferEntry {
    private String user;
    private String message;
    private MessageType type;

    public BufferEntry(String user, String message) {
      this(user, message, MessageType.MESSAGE);
    }

    public BufferEntry(String user, String message, MessageType type) {
      this.user = user;
      this.message = message;
      this.type = type;
    }

    public String getMessage() {
      return message;
    }

    public String getUser() {
      return user;
    }

    public MessageType getType() {
      return type;
    }

    @Override
    public String toString() {
      if (type == MessageType.ACTION) {
        return "* " + getUser() + " " + getMessage();
      } else {
        return "<" + getUser() + "> " + getMessage();
      }
    }
  }

  /**
   * what kind of message we're looking at
   */
  public enum MessageType {
    MESSAGE, ACTION
  }
}

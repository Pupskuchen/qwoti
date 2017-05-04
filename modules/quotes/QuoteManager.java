package quotes;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;

/**
 * Handle database interactions for quotes
 */
public class QuoteManager {

  private Connection con;
  private Statement st;

  /**
   * create QuoteManager instance to handle database interactions
   * 
   * @param botConfig
   */
  public QuoteManager(BotConfig botConfig) {
    this(botConfig.getString("dbfile"));
  }

  /**
   * create QuoteManager instance to handle database interactions
   * 
   * @param dbFile
   */
  public QuoteManager(String dbFile) {
    dbFile = dbFile == null ? Bot.BOTNAME + ".db" : dbFile;
    String dbPath = "db" + File.separator + dbFile;

    try {
      con = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
      st = con.createStatement();
      st.setQueryTimeout(3);

      st.execute("CREATE TABLE IF NOT EXISTS quotes ("//
          + "viewId BIGINT UNIQUE,"//
          + "added BIGINT NOT NULL,"//
          + "user VARCHAR(32) NOT NULL,"//
          + "channel VARCHAR(32),"//
          + "network VARCHAR(32) NOT NULL,"//
          + "text TEXT NOT NULL"//
          + ")");
    } catch (SQLException e) {
      System.err.println("can't access database");
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      if (!con.isClosed()) con.close();
    } catch (SQLException e) {
      System.err.println("failed to close database connection");
      e.printStackTrace();
    }
  }

  /**
   * get number of quotes in database
   * 
   * @return amount of quotes
   */
  public long count() {
    try {
      ResultSet rs = st.executeQuery("SELECT COUNT(*) as total FROM quotes");
      return rs.getLong("total");
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * save given quote to database
   * 
   * @param quote
   * @return true if saved successfully
   */
  public boolean save(Quote quote) {
    long before = count();

    Long newId = before + 1;
    if (before != 0) {
      newId = getLatestId() + 1;
    }
    quote.setViewId(newId);

    try {
      PreparedStatement pst = con
          .prepareStatement("INSERT INTO quotes(viewId, added, user, channel, network, text) VALUES(?,?,?,?,?,?)");
      pst.setLong(1, newId);
      pst.setLong(2, quote.getAdded());
      pst.setString(3, quote.getUser());
      pst.setString(4, quote.getChannel());
      pst.setString(5, quote.getNetwork());
      pst.setString(6, quote.getText(true));

      pst.executeUpdate();

    } catch (SQLException e) {
      System.err.println("couldn't save quote");
      e.printStackTrace();
      return false;
    }

    return count() > before;
  }

  /**
   * get latest quote in database (highest id)
   * 
   * @return quote
   */
  public Quote getLatest() {
    try {
      ResultSet rs = st.executeQuery("SELECT *, max(rowid) as id FROM quotes LIMIT 1");
      return rs.next() ? rsToQuote(rs) : null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * get the id of the latest quote
   * 
   * @return id
   */
  public Long getLatestId() {
    Quote quote = getLatest();
    return quote != null ? quote.getId() : null;
  }

  private Quote rsToQuote(ResultSet rs) {
    try {
      return new Quote(rs.getString("user"), //
          rs.getLong("added"), //
          rs.getString("channel"), //
          rs.getString("text"), //
          rs.getString("network")).setViewId(rs.getLong("viewId"));
    } catch (Exception e) {
      System.err.println("can't create quote from resultset");
    }
    return null;
  }

  /**
   * get quote (with given id) from database if id is null, this returns a random quote
   * 
   * @param id
   * @return quote
   */
  public Quote get(Long id) {
    String query = null;
    if (id == null) query = "SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1";
    else query = "SELECT * FROM quotes WHERE viewId = " + id;

    try {
      ResultSet rs = st.executeQuery(query);
      if (rs.next()) return rsToQuote(rs);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Quote get(String chan) {
    if (chan == null || chan.isEmpty()) {
      return get((Long) null);
    }
    try {
      PreparedStatement pst = con.prepareStatement("SELECT * FROM quotes WHERE channel = ? ORDER BY RANDOM() LIMIT 1");
      pst.setString(1, chan);

      ResultSet rs = pst.executeQuery();
      if (rs.next()) return rsToQuote(rs);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * get quote (with given id) from database if id is null, this returns a random quote
   * 
   * @return quote
   */
  public Quote get(Integer id) {
    return id == null ? get() : get((long) id);
  }

  /**
   * get random quote from database
   * 
   * @return quote
   */
  public Quote get() {
    return get((Long) null);
  }

  /**
   * get all quotes
   * 
   * @return quotes
   */
  public List<Quote> getAll() {
    List<Quote> quotes = new ArrayList<Quote>();
    try {
      ResultSet rs = st.executeQuery("SELECT * FROM quotes");
      while (rs.next()) {
        quotes.add(rsToQuote(rs));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return quotes;
  }

  /**
   * find quotes which contain the given pattern (exactly)<br>
   * case insensitive
   * 
   * @param pattern
   * @return found quotes
   */
  private List<Quote> findExact(String channel, String pattern) {
    pattern = "%" + pattern.toLowerCase() + "%";
    List<Quote> quotes = new ArrayList<Quote>();

    String query = "SELECT * FROM quotes WHERE (text LIKE ? OR user LIKE ?)";
    if (channel != null) query += " AND (channel LIKE ? )";
    query += " COLLATE NOCASE";

    try {
      PreparedStatement pst = con.prepareStatement(query);
      pst.setString(1, pattern);
      pst.setString(2, pattern);
      if (channel != null) pst.setString(3, pattern);

      ResultSet rs = pst.executeQuery();
      while (rs.next()) {
        quotes.add(rsToQuote(rs));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return quotes;
  }

  /**
   * find quotes matching the pattern<br>
   * if <code>exact</code> is <code>true</code>, text or user have to match or contain the exact pattern<br>
   * if it's <code>false</code>, whitespace in the pattern can be anything in matched quotes
   * 
   * @param channel
   * @param pattern
   * @param exact
   * @return matching quotes
   */
  public List<Quote> find(String channel, String pattern, boolean exact) {
    if (exact) {
      return findExact(channel, pattern);
    } else {
      return find(channel, pattern);
    }
  }

  /**
   * find quotes which contain everything in the pattern (but not exactly)
   * 
   * @param pattern
   * @return found quotes
   */
  private List<Quote> find(String channel, String pattern) {
    return findExact(channel, pattern.replaceAll("\\s+", "%"));
  }

  /**
   * delete quote with given id from database
   * 
   * @param id
   * @return true if delete was successful
   */
  public boolean delete(Long id) {

    Quote quote = get(id);
    if (quote != null) {
      return delete(quote);
    }
    return false;
  }

  /**
   * delete given quote from database
   * 
   * @param quote
   * @return true if successful
   */
  public boolean delete(Quote quote) {
    if (quote == null) {
      return false;
    }
    long before = count();

    try {
      PreparedStatement pst = con.prepareStatement("DELETE FROM quotes WHERE viewId = ?");
      pst.setLong(1, quote.getId());
      pst.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return count() < before;
  }

  /**
   * get random quote from given list of quotes
   * 
   * @param <T>
   * 
   * @param list
   * @return random quote from list
   */

  public static <T> T random(List<T> list) {
    return list.size() == 0 ? null : list.get(ThreadLocalRandom.current().nextInt(list.size()));
  }
}

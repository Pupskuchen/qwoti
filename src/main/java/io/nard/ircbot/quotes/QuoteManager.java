package io.nard.ircbot.quotes;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;

/**
 * Handle database interactions for quotes
 */
public class QuoteManager {
  private EntityManagerFactory emf;
  private EntityManager em;
  // private BotConfig bc;

  /**
   * create QuoteManager instance to handle database interactions
   * 
   * @param botConfig
   */
  public QuoteManager(BotConfig botConfig) {
    // bc = botConfig;
    // this("db/" + botConfig.getStringValue("dbfile"));
    this(botConfig.getString("dbfile"));
  }

  /**
   * create QuoteManager instance to handle database interactions
   * 
   * @param dbFile
   */
  public QuoteManager(String dbFile) {
    dbFile = dbFile == null ? Bot.BOTNAME + ".odb" : dbFile;
    String dbPath = "db/" + dbFile;
    emf = Persistence.createEntityManagerFactory(dbPath);
    em = emf.createEntityManager();

    em.getTransaction().begin();
  }

  private void begin() {
    if (!em.getTransaction().isActive())
      em.getTransaction().begin();
  }

  private void commit() {
    em.getTransaction().commit();
  }

  public void close() {
    em.close();
    emf.close();
  }

  /**
   * get number of quotes in database
   * 
   * @return amount of quotes
   */
  public long count() {
    try {
      Query query = em.createQuery("SELECT COUNT(quote) FROM Quote quote");
      if (query.getResultList().size() < 1)
        return 0;
      return (long) query.getSingleResult();
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

    begin();
    em.persist(quote);
    commit();

    return count() > before;
  }

  /**
   * get latest quote in database (highest id)
   * 
   * @return quote
   */
  public Quote getLatest() {
    Query query = em.createQuery("SELECT quote FROM Quote quote ORDER BY quote.id DESC").setMaxResults(1);
    if (query.getResultList().size() < 1)
      return null;
    return (Quote) query.getSingleResult();
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

  /**
   * get quote (with given id) from database if id is null, this returns a random quote
   * 
   * @param id
   * @return quote
   */
  public Quote get(Long id) {
    if (id == null) {
      Query query = em.createQuery("SELECT COUNT(quote) FROM Quote quote");
      long total = 0;
      try {
        total = (long) query.getSingleResult();
      } catch (Exception e) {
        return null;
      }
      if (total < 1) {
        return null;
      }
      query = em.createQuery("SELECT quote FROM Quote quote").setMaxResults(1)
          .setFirstResult((int) (new Random().nextDouble() * total));
      if (query.getResultList().size() < 1)
        return null;
      return (Quote) query.getSingleResult();
    } else {
      CriteriaBuilder cb = em.getCriteriaBuilder();
      CriteriaQuery<Quote> q = cb.createQuery(Quote.class);
      Root<Quote> c = q.from(Quote.class);
      q.select(c);
      ParameterExpression<Long> p = cb.parameter(Long.class);
      q.where(cb.equal(c.get("viewId"), p));
      TypedQuery<Quote> query = em.createQuery(q);
      query.setParameter(p, id);
      try {
        return query.getSingleResult();
      } catch (Exception e) {
        return null;
      }
    }
    // return em.find(Quote.class, id);
  }

  public Quote get(String chan) {
    if (chan == null) {
      return get((Long) null);
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Quote> q = cb.createQuery(Quote.class);
    Root<Quote> c = q.from(Quote.class);
    q.select(c);
    ParameterExpression<String> p = cb.parameter(String.class);
    q.where(cb.equal(c.get("channel"), p));
    TypedQuery<Quote> query = em.createQuery(q);
    query.setParameter(p, chan);
    try {
      return query.getSingleResult();
    } catch (Exception e) {
      return null;
    }
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
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Quote> q = cb.createQuery(Quote.class);
    Root<Quote> c = q.from(Quote.class);
    q.select(c);
    q.orderBy(cb.asc(c.get("viewId")));

    TypedQuery<Quote> query = em.createQuery(q);
    return query.getResultList();
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

    CriteriaBuilder cb = em.getCriteriaBuilder();

    CriteriaQuery<Quote> q = cb.createQuery(Quote.class);
    Root<Quote> c = q.from(Quote.class);

    Expression<String> text = c.get("text");
    Expression<String> user = c.get("user");

    if (channel != null) {
      q.select(c).where(cb.and(cb.like(cb.lower(c.get("channel")), "%" + channel.toLowerCase() + "%"),
          cb.or(cb.like(cb.lower(text), pattern), cb.like(cb.lower(user), pattern)))).distinct(true);
    } else {
      q.select(c).where(cb.or(cb.like(cb.lower(text), pattern), cb.like(cb.lower(user), pattern))).distinct(true);
    }
    q.orderBy(cb.asc(c.get("viewId")));

    TypedQuery<Quote> query = em.createQuery(q);

    return query.getResultList();
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

    begin();
    em.remove(quote);
    commit();

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

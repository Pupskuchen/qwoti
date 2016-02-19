package irc.nard.ircbot.config;

import java.util.List;
import java.util.Map;

public abstract class BotConfigSuper {

  protected BotConfigSuper() {
  }

  /**
   * write config to file
   */
  public abstract void save() throws Exception;

  /**
   * update/insert string
   * 
   * @param key
   * @param value
   */
  public abstract void putString(String key, String value);

  /**
   * get string from config
   * 
   * @param key
   * @return value or null
   */
  public abstract String getString(String key);

  /**
   * get string from config
   * 
   * @param key
   * @param defaultValue
   * @return value or, if unset, given default value
   */
  public String getString(String key, String defaultValue) {
    String val = getString(key);
    return val != null ? val : defaultValue;
  }

  /**
   * update/insert int
   * 
   * @param key
   * @param value
   */
  public abstract void putInteger(String key, Integer value);

  /**
   * get int from config
   * 
   * @param key
   * @return value or null
   */
  public abstract Integer getInteger(String key);

  /**
   * get int from config
   * 
   * @param key
   * @param defaultValue
   * @return value or, if unset, given default value
   */
  public Integer getInteger(String key, Integer defaultValue) {
    Integer val = getInteger(key);
    return val != null ? val : defaultValue;
  }

  /**
   * update/insert double
   * 
   * @param key
   * @param value
   */
  public abstract void putDouble(String key, Double value);

  /**
   * get double from config
   * 
   * @param key
   * @return value or null
   */
  public abstract Double getDouble(String key);

  /**
   * get double from config
   * 
   * @param key
   * @param defaultValue
   * @return value or, if unset, given default value
   */
  public Double getDouble(String key, Double defaultValue) {
    Double val = getDouble(key);
    return val != null ? val : defaultValue;
  }

  /**
   * update/insert boolean
   * 
   * @param key
   * @param value
   */
  public abstract void putBoolean(String key, Boolean value);

  /**
   * get boolean from config
   * 
   * @param key
   * @return value or null
   */
  public abstract Boolean getBoolean(String key);

  /**
   * get boolean from config
   * 
   * @param key
   * @param defaultValue
   * @return value or, if unset, given default value
   */
  public Boolean getBoolean(String key, Boolean defaultValue) {
    Boolean val = getBoolean(key);
    return val != null ? val : defaultValue;
  }

  /**
   * update/insert list
   * 
   * @param key
   * @param value
   */
  public abstract void putList(String key, List<?> value);

  /**
   * get list from config
   * 
   * @param key
   * @return value or null
   */
  public abstract List<?> getList(String key);

  /**
   * update/insert map
   * 
   * @param key
   * @param value
   */
  public abstract void putMap(String key, Map<?, ?> value);

  /**
   * get map from config
   * 
   * @param key
   * @return value or null
   */
  public abstract Map<?, ?> getMap(String key);

  /**
   * parse {@link String} to {@link Integer}
   * 
   * @param value
   * @return {@link Integer} or null
   */
  public Integer toInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * parse {@link String} to {@link Double}
   * 
   * @param value
   * @return {@link Double} or null
   */
  public Double toDouble(String value) {
    try {
      return Double.parseDouble(value);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * parse {@link String} to {@link Boolean}
   * 
   * @param value
   * @return {@link Boolean} or null
   */
  public Boolean toBoolean(String value) {
    try {
      return Boolean.parseBoolean(value);
    } catch (Exception e) {
      return null;
    }
  }
}

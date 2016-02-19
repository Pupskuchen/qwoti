package io.nard.ircbot;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * manage JSON configuration
 */
public class BotConfig {

  private JSONObject config;
  private String configPath;

  /**
   * create new JSON configuration
   * 
   * @param configurationFile
   * @throws Exception
   */
  public BotConfig(String configurationFile) throws Exception {
    this.configPath = configurationFile;
    config = new JSONObject(new String(Files.readAllBytes(Paths.get(configurationFile))));
  }

  /**
   * get configuration json object<br>
   * be careful!
   * 
   * @return json config
   */
  private JSONObject getConfig() {
    return config;
  }

  /**
   * write config to file
   */
  public void save() throws IOException {
    try (FileWriter file = new FileWriter(configPath)) {
      file.write(config.toString(4));
    }
  }

  /**
   * update/insert string
   * 
   * @param key
   * @param value
   */
  public void putString(String key, String value) {
    getConfig().put(key, value);
  }

  /**
   * get string from config
   * 
   * @param key
   * @return value or null
   */
  public String getString(String key) {
    try {
      return getConfig().getString(key);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * get string from config
   * 
   * @param key
   * @param defaultValue
   * @return value or, if unset, given default value
   */
  public String getString(String key, String defaultValue) {
    return getConfig().optString(key, defaultValue);
  }

  /**
   * update/insert int
   * 
   * @param key
   * @param value
   */
  public void putInteger(String key, Integer value) {
    getConfig().put(key, value);
  }

  /**
   * get int from config
   * 
   * @param key
   * @return value or null
   */
  public Integer getInteger(String key) {
    try {
      return getConfig().getInt(key);
    } catch (JSONException e) {
      return null;
    }
  }

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
  public void putDouble(String key, Double value) {
    getConfig().put(key, value);
  }

  /**
   * get double from config
   * 
   * @param key
   * @return value or null
   */
  public Double getDouble(String key) {
    try {
      return getConfig().getDouble(key);
    } catch (JSONException e) {
      return null;
    }
  }

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
  public void putBoolean(String key, Boolean value) {
    getConfig().put(key, value);
  }

  /**
   * get boolean from config
   * 
   * @param key
   * @return value or null
   */
  public Boolean getBoolean(String key) {
    try {
      return getConfig().getBoolean(key);
    } catch (JSONException e) {
      return null;
    }
  }

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
  public void putList(String key, List<?> value) {
    getConfig().put(key, value);
  }

  /**
   * get list from config
   * 
   * @param key
   * @return value or null
   */
  public List<?> getList(String key) {
    return toList(getConfig().getJSONArray(key));
  }

  /**
   * convert json array to list
   * 
   * @param arr
   * @return list
   */
  public List<String> toList(JSONArray arr) {
    List<String> list = new ArrayList<String>();
    for (Object item : arr) {
      list.add((String) item);
    }
    return list;
  }

  /**
   * update/insert map
   * 
   * @param key
   * @param value
   */
  public void putMap(String key, Map<?, ?> value) {
    getConfig().put(key, value);
  }

  /**
   * get object from config
   * 
   * @param key
   * @return json object
   */
  public JSONObject getObject(String key) {
    try {
      return getConfig().getJSONObject(key);
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * update/insert object
   * 
   * @param key
   * @param value
   */
  public void putObject(String key, Object value) {
    getConfig().put(key, value);
  }

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

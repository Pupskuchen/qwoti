package irc.nard.ircbot.config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Object to handle configuration files
 */
public class BotConfigProp {
  private Properties prop;
  private String configurationFile;

  /**
   * create new configuration instance
   * 
   * @param configurationFile
   * @throws FileNotFoundException
   */
  public BotConfigProp(String configurationFile) throws FileNotFoundException {
    prop = new Properties();
    this.configurationFile = configurationFile;

    try {
      prop.load(new FileReader(configurationFile));
    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("configuration file '" + configurationFile + "' not found");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * save configuration file
   * 
   * @throws FileNotFoundException
   */
  public void save() throws FileNotFoundException {
    try {
      prop.store(new FileWriter(configurationFile), "qwoti configuration");
    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("error saving configuration to '" + configurationFile + "'");
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void putString(String key, String value) {
    prop.setProperty(key, value);
  }

  public String getStringValue(String key, String defaultValue) {
    String value = getStringValue(key);
    return value == null ? defaultValue : value;
  }

  public String getStringValue(String key) {
    return prop.getProperty(key);
  }

  public void putInt(String key, int value) {
    prop.setProperty(key, Integer.toString(value));
  }

  public Integer getIntValue(String key, int defaultValue) {
    Integer value = getIntValue(key);
    return value == null ? defaultValue : value;
  }

  public Integer getIntValue(String key) {
    String value = prop.getProperty(key);
    return value == null ? null : Integer.parseInt(value);
  }

  public int intValue(String value) {
    return Integer.parseInt(value);
  }

  public void putDouble(String key, double value) {
    prop.setProperty(key, Double.toString(value));
  }

  public Double getDoubleValue(String key, double defaultValue) {
    Double value = getDoubleValue(key);
    return value == null ? defaultValue : value;
  }

  public Double getDoubleValue(String key) {
    return Double.parseDouble(prop.getProperty(key));
  }

  public double doubleValue(String value) {
    return Double.parseDouble(value);
  }

  public void putBool(String key, boolean value) {
    prop.setProperty(key, Boolean.toString(value));
  }

  public Boolean getBoolValue(String key, boolean defaultValue) {
    Boolean value = getBoolValue(key);
    return value == null ? defaultValue : value;
  }

  public Boolean getBoolValue(String key) {
    return Boolean.parseBoolean(prop.getProperty(key));
  }

  public boolean boolValue(String value) {
    return Boolean.parseBoolean(value);
  }

  public void putList(String key, List<String> list) {
    prop.setProperty(key, formatList(list));
  }

  /**
   * parse string to a list of strings<br>
   * values are seperated by ,
   * 
   * @param string
   * @return parsed list
   */
  public List<String> getListValue(String key) {
    return parseList(prop.getProperty(key));
  }

  /**
   * parse string to a list of strings<br>
   * values are seperated by ,
   * 
   * @param string
   * @return parsed list
   */
  public List<String> listValue(String value) {
    return parseList(value);
  }

  public void putMap(String key, Map<String, String> map) {
    prop.setProperty(key, formatMap(map));
  }

  /**
   * parse string to a map<br>
   * entries are seperated by :<br>
   * key and value are seperated by =
   * 
   * @param string
   * @return parsed map
   */
  public Map<String, String> getMapValue(String key) {
    return parseMap(prop.getProperty(key));
  }

  /**
   * parse string to a map<br>
   * entries are seperated by :<br>
   * key and value are seperated by =
   * 
   * @param string
   * @return parsed map
   */
  public Map<String, String> mapValue(String value) {
    return parseMap(value);
  }

  public String formatMap(Map<String, String> map) {
    return Joiner.on(':').withKeyValueSeparator("=").join(map);
  }

  /**
   * parse string to a map<br>
   * entries are seperated by :<br>
   * key and value are seperated by =
   * 
   * @param string
   * @return parsed map
   */
  private Map<String, String> parseMap(String string) {
    if (string == null || string.equals(""))
      return null;
    return Splitter.on(':').trimResults().withKeyValueSeparator('=').split(string);
  }

  public String formatList(List<String> list) {
    return Joiner.on(',').join(list);
  }

  /**
   * parse string to a list of strings<br>
   * values are seperated by ,
   * 
   * @param string
   * @return parsed list
   */
  private List<String> parseList(String string) {
    if (string == null)
      return null;
    return Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(string));
  }
}

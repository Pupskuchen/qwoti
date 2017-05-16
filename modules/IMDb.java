import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;

import com.google.common.collect.Lists;

public abstract class IMDb {

  private class IMDbImage {

    String link, url;
    int width, height;


    public IMDbImage(int width, int height, String url) {
      this(width, height, url, null);
    }

    public IMDbImage(int width, int height, String url, String link) {
      this.width = width;
      this.height = height;
      this.url = url;
      this.link = link;
    }
  }

  public abstract class IMDbEntry {

    String id;
    String name;

    private boolean exact;

    IMDbImage mainImage;
    List<IMDbImage> images = new ArrayList<>();

    public IMDbEntry(String id, String name, boolean exact) {
      this.id = id;
      this.name = name;
      this.exact = exact;
    }

    public boolean isExact() {
      return exact;
    }

    public IMDbEntry setMainImage(IMDbImage image) {
      this.mainImage = image;
      return this;
    }

    public IMDbEntry addImage(IMDbImage image) {
      if (!images.contains(image)) images.add(image);
      return this;
    }

    public IMDbImage getImage(int index) {
      if (index >= 0 && index < images.size()) return images.get(index);
      return null;
    }
  }

  public class Person extends IMDbEntry {


    public Person(String id, String name, boolean exact) {
      super(id, name, exact);
    }

    List<String> knownFor = new ArrayList<>();
    String attr;
  }

  public abstract class Title extends IMDbEntry {

    public Title(String id, String name, boolean exact) {
      super(id, name, exact);
    }

    double rating;
    long num_votes;
    List<String> genres = new ArrayList<>();
    List<Person> writers = new ArrayList<>();
    List<Person> directors = new ArrayList<>();
    List<Person> castSummary = new ArrayList<>();
    String plot;
    Date releaseDate;
    int runtime, year;

  }

  public class Movie extends Title {

    public Movie(String id, String name, boolean exact) {
      super(id, name, exact);
    }

  }

  public class Series extends Title {

    public Series(String id, String name, boolean exact) {
      super(id, name, exact);
    }

    int yearEnd;
  }

  public enum Type {
    Movie("feature"), Series("tv_series"), Person();

    private List<String> types = null;

    private Type(String... imdb_types) {
      if (imdb_types != null && imdb_types.length > 0) types = Lists.newArrayList(imdb_types);
    }
  }

  // https://app.imdb.com/title/maindetails?tconst=tt0944947
  public static Title getTitle(String id) {
    return null;
  }

  public static Title searchTitle(String search) {
    List<Title> r = searchTitles(search);
    return (r != null && r.size() > 0) ? r.get(0) : null;
  }

  public static List<Title> searchTitles(String search) {
    return null;
  }

  // https://app.imdb.com/name/maindetails?nconst=nm0227759
  public static Person getPerson(String id) {
    return null;
  }

  // https://app.imdb.com/find?q=
  public static Person searchPerson(String search) {
    List<Person> r = searchPersons(search);
    return (r != null && r.size() > 0) ? r.get(0) : null;
  }

  public static List<Person> searchPersons(String search) {
    return null;
  }

  private static JSONObject search() {
    return null;
  }
}

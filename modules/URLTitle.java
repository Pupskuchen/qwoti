import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.output.OutputChannel;

import io.nard.ircbot.AbstractListener;
import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.Bot;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Privilege;


public class URLTitle extends AbstractModule {

  private Listener listener;

  public URLTitle( PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager ) {
    super( bot, botConfig, botHelper, listenerManager );

    listener = new AbstractListener() {

      @Override
      public void onMessage( MessageEvent event ) throws Exception {
        User user = event.getUser();
        PircBotX bot = event.getBot();
        String network = bot.getServerInfo().getNetwork().toLowerCase();
        String userAccount = botHelper.getAccount( bot, user );
        Privilege userPrivileges = userAccount == null ? Privilege.GUEST
          : botHelper.getPrivileges().get( network ).get( userAccount.toLowerCase() );

        boolean userAuthed = botHelper.isAuthenticated( bot, user );

        if( userPrivileges == null ) {
          userPrivileges = userAuthed ? Privilege.PRIVILEGED : Privilege.GUEST;
        }

        if( userPrivileges == Privilege.NONE ) return;

        OutputChannel out = event.getChannel().send();

        String msg = event.getMessage();
        TextURLParser p = new TextURLParser( msg );
        for( int i = 0; i < p.size(); i++ ) {
          String title = p.getTitle( i );
          if( title != null ) out.message( String.format( "« %s »", title ) );
        }
      }
    };
  }

  // test urls:
  // ftp://ftp-stud.fht-esslingen.de/
  // https://github.com/Pupskuchen/qwoti

  private static class URLMatch {

    private String scheme, host, full;

    public URLMatch( String scheme, String host, String full ) {
      this.scheme = scheme;
      this.host = host;
      this.full = full;
    }
  }

  private class TextURLParser {

    // (?:\\S+(?::\\S*)?@)? -- after protocol
    public final Pattern PATTERN = Pattern.compile(
      "(?i)\\b(?:(https?|ftp)://)((?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))\\.?)(?::\\d{2,5})?(?:[/?#]\\S*)?\\b" );

    private List<URLMatch> matches = new ArrayList<URLMatch>();

    public TextURLParser( String text ) {
      Matcher m = PATTERN.matcher( text );
      while( m.find() ) {
        try {
          matches.add( new URLMatch( m.group( 1 ), m.group( 2 ), m.group( 0 ) ) );
        }
        catch( Exception e ) {
        }
      }
    }

    public int size() {
      return matches.size();
    }

    public URLMatch get( int index ) {
      return index >= 0 && index < matches.size() ? matches.get( index ) : null;
    }

    public String getTitle( int index ) {
      return getTitle( get( index ) );
    }

    public String getTitle( URLMatch m ) {
      if( m == null ) return null;
      if( m.host == null || m.host.isEmpty() || m.scheme == null || !m.scheme.startsWith( "http" ) ) return null;
      String h = m.host;
      String userAgent = Bot.BOTNAME + " " + Bot.VERSION + " (+" + Bot.INFOURL + ")";

      if( isHost( h, "github.com" ) ) {

      }
      else {
        try {
          Elements sel = Jsoup.connect( m.full ).userAgent( userAgent ).get().select( "title" );
          String title = sel.size() > 0 ? sel.get( 0 ).text() : null;
          return title;
        }
        catch( IOException e ) {
        }
      }

      return null;
    }

    private boolean isHost( String a, String b ) {
      return a.equalsIgnoreCase( b );
    }
  }

  @Override
  public void startup() {
    listenerManager.addListener( listener );
  }

  @Override
  public void shutdown() {
    listenerManager.removeListener( listener );
  }

  @Override
  public String getName() {
    return "urltitle";
  }

  @Override
  public String getDescription() {
    return "show titles of websites linked here";
  }

}

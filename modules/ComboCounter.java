

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import io.nard.ircbot.AbstractListener;
import io.nard.ircbot.AbstractModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;
import io.nard.ircbot.Privilege;

public class ComboCounter extends AbstractModule {

  private List<Listener> listeners = new ArrayList<Listener>();

  public ComboCounter(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    listeners.add(new AbstractListener() {

      class Combo {

        private String current;
        private int combo = 1;
        private User last;
      }

      private Map<String, Combo> combos = new HashMap<String, Combo>();

      @Override
      public void onMessage(MessageEvent event) throws Exception {
        String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
        if (!combos.containsKey(network)) {
          combos.put(network, new Combo());
        }

        User user = event.getUser();
        PircBotX bot = event.getBot();
        String userAccount = botHelper.getAccount(bot, user);

        Privilege userPrivileges = userAccount == null ? Privilege.GUEST
            : botHelper.getPrivileges().get(network).get(userAccount.toLowerCase());

        boolean userAuthed = botHelper.isAuthenticated(bot, user);

        if (userPrivileges == null) {
          userPrivileges = userAuthed ? Privilege.PRIVILEGED : Privilege.GUEST;
        }

        if (userPrivileges == Privilege.NONE) return;

        Combo combo = combos.get(network);

        boolean sameUser = combo.last != null && combo.last.equals(user);
        combo.last = user;

        String msg = event.getMessage();
        boolean sameMsg = combo.current != null && combo.current.equals(msg);
        if (!sameUser && sameMsg) {
          combo.combo++;
        } else if (!sameMsg) {
          if (combo.combo >= botConfig.getInteger("min_combo", 3)) {
            event.respond("C-C-C-COMBO BREAKER!!!");
            event.getChannel().send().message("you destroyed a combo of " + combo.combo);

            BotConfig miscStore = new BotConfig("db/misc_" + network + ".json", true);
            int record = miscStore.getInteger("combo_record", 0);
            if (combo.combo > record) {
              event.getChannel().send().message("new combo record! (old: " + record + "; new: " + combo.combo + ")");
              miscStore.putInteger("combo_record", combo.combo);
              miscStore.save();
            }
          }
          combo.current = event.getMessage();
          combo.combo = 1;
        }
      }
    });
    listeners.add(new CommandListener(botConfig).addCommand(new Command("combo") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
        try {
          BotConfig miscStore = new BotConfig("db/misc_" + network + ".json", true);
          int record = miscStore.getInteger("combo_record", 0);
          event.respond("current combo record is " + record);
        } catch (Exception e) {
          event.respond("No idea about any records");
        }
      }

      @Override
      public String getHelp() {
        return "display current combo record";
      }
    }.setPrivmsgCapable(true)));
  }

  @Override
  public void startup() {
    for (Listener l : listeners) {
      listenerManager.addListener(l);
    }
  }

  @Override
  public void shutdown() {
    for (Listener l : listeners) {
      listenerManager.removeListener(l);
    }
  }

  @Override
  public String getName() {
    return "combocounter";
  }

  @Override
  public String getDescription() {
    return "count combos and keep a record";
  }

}

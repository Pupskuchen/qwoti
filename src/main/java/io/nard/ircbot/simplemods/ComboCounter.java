package io.nard.ircbot.simplemods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;
import io.nard.ircbot.Privilege;

public abstract class ComboCounter {

  public static List<Listener> module(BotConfig botConfig) {

    BotHelper botHelper = new BotHelper(botConfig);
    List<Listener> listeners = new ArrayList<Listener>();

    listeners.add(new ListenerAdapter() {

      class Combo {

        private String current;
        private int combo;
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

        if (combo.last.equals(user)) return;
        combo.last = user;

        String msg = event.getMessage();
        if (combo.current != null && combo.current.equals(msg)) {
          combo.combo++;
        } else {
          if (combo.combo > botConfig.getInteger("min_combo", 3)) {
            event.respond("C-C-C-COMBO BREAKER!!!");

            BotConfig miscStore = new BotConfig("db/misc_" + network + ".json", true);
            int record = miscStore.getInteger("combo_record", 0);
            if (combo.combo > record) {
              event.getChannel().send().message("new combo record! (old: " + record + "; new: " + combo.combo + ")");
              miscStore.putInteger("combo_record", combo.combo);
              miscStore.save();
            }
          }
          combo.current = event.getMessage();
          combo.combo = 0;
        }
      }
    });

    listeners.add(new CommandListener(botConfig).addCommand(new Command("combo") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        String network = event.getBot().getServerInfo().getNetwork().toLowerCase();
        try {
          BotConfig miscStore = new BotConfig("db/misc_" + network + ".json", true);
          int record = miscStore.getInteger("combo_record", 0);
          event.respond("current combo record is " + record);
        } catch (Exception e) {
          event.respond("No idea about any records");
        }
      }
    }));

    return listeners;
  }

}

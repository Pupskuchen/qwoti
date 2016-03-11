package io.nard.ircbot.simplemods;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.base.Joiner;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;

public abstract class DNSResolve {

  public static Listener module(BotConfig botConfig) {
    CommandListener commandListener = new CommandListener(botConfig);

    commandListener.addCommand(new Command("resolve", "dns") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond(commandParam.getCommand() + " <hostname>");
          return;
        }
        try {
          InetAddress[] addresses = InetAddress.getAllByName(commandParam.getFirst());
          List<String> ips = new ArrayList<String>();
          for (InetAddress address : addresses) {
            ips.add(address.getHostAddress());
          }
          event.respond(Joiner.on(", ").join(ips));
        } catch (UnknownHostException e) {
          event.respond("could not resolve " + commandParam.getFirst());
        }
      }
    });

    return commandListener;
  }

}

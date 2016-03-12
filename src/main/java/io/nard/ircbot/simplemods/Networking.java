package io.nard.ircbot.simplemods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;

import com.google.common.base.Joiner;

import io.nard.ircbot.BotConfig;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandListener;
import io.nard.ircbot.CommandParam;

public abstract class Networking {

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
    }).addCommand(new Command("isup") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (!commandParam.hasParam()) {
          event.respond(commandParam.getCommand() + " <address>");
          return;
        }
        String input = commandParam.getFirst();
        try {
          InetAddress address = InetAddress.getByName(input);
          double ping = ping(address);
          if (ping > -1) {
            double http = isReachable(address, 80, 3000);
            event.respond(String.format("%s is up - ping: %.2f ms, http: ", input, ping)
                + (http == -1 ? "unreachable" : String.format("%.2f ms", http)));
          } else {
            event.respond(input + " is unreachable for me");
          }
        } catch (UnknownHostException e) {
          event.respond("can't resolve that host");
        }
      }
    });

    return commandListener;
  }

  /**
   * ping given address
   * 
   * @param address
   * @return ping in ms
   */
  public static double ping(InetAddress address) {
    boolean windoof = System.getProperty("os.name").startsWith("Windows");
    String cmd = String.format("ping -%s 1 %s", windoof ? "n" : "c", address.getHostAddress());
    try {
      Process ping = Runtime.getRuntime().exec(cmd);
      BufferedReader in = new BufferedReader(new InputStreamReader(ping.getInputStream()));
      String pong = "";
      Pattern time = Pattern.compile("time=([\\d.]+)");
      while ((pong = in.readLine()) != null) {
        Matcher matcher = time.matcher(pong);
        if (matcher.find()) {
          pong = matcher.group(1);
          break;
        }
      }
      ping.waitFor();
      if (ping.exitValue() == 0) {
        return Double.parseDouble(pong);
      }
    } catch (Exception e) {
    }
    return -1;
  }

  /**
   * measure time elapsed to connect to given address (and port)
   * 
   * @param address
   * @param port
   * @param timeout
   * @return elapsed time to connect in ms
   */
  public static double isReachable(InetAddress address, int port, int timeout) {
    try (Socket socket = new Socket()) {
      InetSocketAddress addr = new InetSocketAddress(address, port);
      long now = System.nanoTime();
      socket.connect(addr, timeout);
      long ping = System.nanoTime() - now;
      return (ping / 1000d / 1000);
    } catch (IOException e) {
      return -1;
    }
  }
}

package io.nard.ircbot.simplemods;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpRequest;
import org.apache.http.ProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.events.MessageEvent;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.google.common.base.Joiner;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;

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

        if (commandParam.getParams().size() == 1) {
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
        } else {
          String hostname = commandParam.getParams().remove(0);
          try {
            Map<String, StringJoiner> ips = new HashMap<String, StringJoiner>();
            for (String req : commandParam.getParams()) {
              int type = Type.ANY;
              type = Type.value(req);
              Record[] records = type == -1 ? null : new Lookup(hostname, type).run();
              if (records != null) {
                for (Record record : records) {
                  String rtype = Type.string(record.getType());
                  if (!ips.containsKey(rtype)) {
                    ips.put(rtype, new StringJoiner(", "));
                  }
                  ips.get(rtype).add(record.rdataToString());
                }
              } else {
                ips.put(req, new StringJoiner(", ").add("no result"));
              }
            }
            event.respond(Joiner.on(" | ").withKeyValueSeparator(": ").join(ips));
          } catch (TextParseException e) {
            event.respond("that's not a valid DNS name");
          }
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
            int[][] http = httpCheck(input);
            event.respond(//
                String.format("%s is up - ping: %.2f ms, http: %s, https: %s", input, ping,
                    (http[0][0] == -1 ? "unreachable" : String.format("%d (%d ms)", http[0][0], http[0][1])),
                    (http[1][0] == -1 ? "unreachable" : String.format("%d (%d ms)", http[1][0], http[1][1]))));
          } else {
            event.respond(input + " is unreachable for me");
          }
        } catch (UnknownHostException e) {
          event.respond("can't resolve that host");
        }
      }
    }).addCommand(new Command("connectable") {

      @Override
      public void onCommand(CommandParam commandParam, MessageEvent event) {
        if (commandParam.getParams().size() < 2) {
          event.respond(commandParam.getCommand() + " <address> <ports...>");
          return;
        }
        String input = commandParam.getParams().remove(0);
        try {
          int successful = 0;
          InetAddress address = InetAddress.getByName(input);
          StringJoiner joiner = new StringJoiner(" | ");
          for (String arg : commandParam.getParams()) {
            try {
              int port = Integer.parseInt(arg);
              if (port > 0) {
                double elapsed = isConnectable(address, port, 500);
                joiner.add(port + ": " //
                    + (elapsed == -1 ? "not connectable" : String.format("connectable (%.2f ms)", elapsed)));
                successful++;
              }
            } catch (NumberFormatException e) {
            }
          }
          if (successful > 0) {
            event.respond(input + ": " + joiner);
          } else {
            event.respond("please provide valid ports");
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
   * get http status codes of given address
   * 
   * @param address
   * @return status codes [http, https]
   */
  public static int[][] httpCheck(String address) {
    String[] addresses = { "http://" + address, "https://" + address };
    int[][] result = new int[addresses.length][2];
    Unirest.setHttpClient(trustingClient());
    for (int i = 0; i < addresses.length; i++) {
      try {
        long before = System.nanoTime();
        HttpResponse<String> req = Unirest.get(addresses[i]).asString();
        long elapsed = System.nanoTime() - before;
        result[i][0] = req.getStatus();
        result[i][1] = (int) (elapsed / 1000 / 1000);
      } catch (UnirestException e) {
        result[i][0] = -1;
      }
    }
    Options.refresh();
    return result;
  }

  /**
   * measure time elapsed to connect to given address (and port)<br>
   * returns -1 if not connectable
   * 
   * @param address
   * @param port
   * @param timeout
   * @return elapsed time to connect in ms
   */
  public static double isConnectable(InetAddress address, int port, int timeout) {
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

  private static HttpClient trustingClient() {
    try {
      SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
          return true;
        }
      }).build();
      CloseableHttpClient httpclient = HttpClients.custom()
          .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)//
          .setRedirectStrategy(new RedirectStrategy() {
            @Override
            public boolean isRedirected(HttpRequest request, org.apache.http.HttpResponse response, HttpContext context)
                throws ProtocolException {
              return false;
            }

            @Override
            public HttpUriRequest getRedirect(HttpRequest request, org.apache.http.HttpResponse response,
                HttpContext context) throws ProtocolException {
              return null;
            }
          }).setSslcontext(sslcontext).build();
      return httpclient;
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
      e.printStackTrace();
    }
    return null;
  }
}

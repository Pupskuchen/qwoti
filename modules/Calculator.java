

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.types.GenericMessageEvent;

import io.nard.ircbot.AbstractCommandModule;
import io.nard.ircbot.BotConfig;
import io.nard.ircbot.BotHelper;
import io.nard.ircbot.Command;
import io.nard.ircbot.CommandParam;

public class Calculator extends AbstractCommandModule {

  public Calculator(PircBotX bot, BotConfig botConfig, BotHelper botHelper, ListenerManager listenerManager) {
    super(bot, botConfig, botHelper, listenerManager);

    cl.addCommand(new Command("calc", "c") {

      @Override
      public void onCommand(CommandParam commandParam, GenericMessageEvent event) {
        if (commandParam.hasParam()) {
          event.respond(String.valueOf(eval(commandParam.getParam())));
        } else {
          event.respond("calc <expression>");
        }
      }

      @Override
      public String getParams() {
        return "<expression>";
      }

      @Override
      public String getHelp() {
        return "do some (rather basic) maths";
      }
    }.setPrivmsgCapable(true));
  }

  // http://stackoverflow.com/questions/3422673/evaluating-a-math-expression-given-in-string-form/26227947#26227947
  public static double eval(final String str) {
    class Parser {

      int pos = -1, c;

      void eatChar() {
        c = (++pos < str.length()) ? str.charAt(pos) : -1;
      }

      void eatSpace() {
        while (Character.isWhitespace(c))
          eatChar();
      }

      double parse() {
        eatChar();
        double v = parseExpression();
        if (c != -1) throw new RuntimeException("Unexpected: " + (char) c);
        return v;
      }

      // Grammar:
      // expression = term | expression `+` term | expression `-` term
      // term = factor | term `*` factor | term `/` factor | term brackets
      // factor = brackets | number | factor `^` factor
      // brackets = `(` expression `)`

      double parseExpression() {
        double v = parseTerm();
        for (;;) {
          eatSpace();
          if (c == '+') { // addition
            eatChar();
            v += parseTerm();
          } else if (c == '-') { // subtraction
            eatChar();
            v -= parseTerm();
          } else {
            return v;
          }
        }
      }

      double parseTerm() {
        double v = parseFactor();
        for (;;) {
          eatSpace();
          if (c == '/') { // division
            eatChar();
            v /= parseFactor();
          } else if (c == '*' || c == '(') { // multiplication
            if (c == '*') eatChar();
            v *= parseFactor();
          } else {
            return v;
          }
        }
      }

      double parseFactor() {
        double v;
        boolean negate = false;
        eatSpace();
        if (c == '+' || c == '-') { // unary plus & minus
          negate = c == '-';
          eatChar();
          eatSpace();
        }
        if (c == '(') { // brackets
          eatChar();
          v = parseExpression();
          if (c == ')') eatChar();
        } else { // numbers
          StringBuilder sb = new StringBuilder();
          while ((c >= '0' && c <= '9') || c == '.') {
            sb.append((char) c);
            eatChar();
          }
          if (sb.length() == 0) throw new RuntimeException("Unexpected: " + (char) c);
          v = Double.parseDouble(sb.toString());
        }
        eatSpace();
        if (c == '^') { // exponentiation
          eatChar();
          v = Math.pow(v, parseFactor());
        }
        if (negate) v = -v; // unary minus is applied after exponentiation; e.g. -3^2=-9
        return v;
      }
    }
    return new Parser().parse();
  }

  @Override
  public String getName() {
    return "calc";
  }

  @Override
  public String getDescription() {
    return "provides a command for basic calculations";
  }

}

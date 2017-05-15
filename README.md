# qwoti

This is an irc bot written in Java using the [PircBotX IRC library](https://github.com/TheLQ/pircbotx). It comes with some basic features (and commands) and can easily be extended with modules. Configuration (per network - yeah, qwoti supports multinetworking) is done in a simple JSON file. Commands can be accepted from both channel and private messages.

## modules
Loading and unloading modules happens at runtime using commands on any network the bot is connected to, no need to restart the bot and no interruption in service - only [some `.java` files lying around in the /modules directory](https://github.com/Pupskuchen/qwoti/tree/master/modules) and extending the modules super class ([`AbstractModule`](https://github.com/Pupskuchen/qwoti/blob/master/src/main/java/io/nard/ircbot/AbstractModule.java)).

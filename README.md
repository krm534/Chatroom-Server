# Chatroom Server
Java application that runs the server portion of a chatroom.

## Prerequisites
- The chatroom client located [here](https://github.com/krm534/Chatroom-Client) must be used to communicate with the Chatroom Server. **The Chatroom Server must be started before the Chatroom Client connects.**
To run this application, you must use Java JDK 11 at least, as some of the Gradle dependencies depend on this version.

## How to use?
- Run the Gradle Shadow plugin's built .jar file on an accessible machine/server.

## Notes
- The server was only tested on a Linux-based OS so this could need tweaking if trying to run on Windows OS.
- The server was only tested as stable against 2 clients. So, using more clients than this could lead to issues. However, this depends on the memory capacity or processing speed of the machine the server is using.

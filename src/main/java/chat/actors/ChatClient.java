package chat.actors;

//class ChatClient(val name: String) {
//val chat = RemoteClient.actorFor("chat:service", "localhost", 2552)
//
//def login = chat ! Login(name)
//def logout = chat ! Logout(name)
//def post(message: String) = chat ! ChatMessage(name, name + ": " + message)
//def chatLog = (chat !! GetChatLog(name)).as[ChatLog].getOrElse(throw new
//Exception("Couldn't get the chat log from ChatServer"))
//}

import chat.events.ChatLog;
import chat.events.ChatMessage;
import chat.events.GetChatLog;
import chat.events.Login;
import chat.events.Logout;
import se.scalablesolutions.akka.actor.ActorRef;
import se.scalablesolutions.akka.remote.RemoteClient;

/**
 * Chat client.
 */
public class ChatClient {

	private String name = null;
	private ActorRef chat = null;

	public ChatClient(String name) {
		this.name = name;

		// starts and connects the client to the remote server
		this.chat = RemoteClient.actorFor("chat:service", "localhost", 2552);
		// this.chat =
		// UntypedActorRef.wrap(RemoteClient.actorFor("chat:service",
		// "localhost", 2552));
	}

	public void login() {
		chat.sendOneWay(new Login(name));
	}

	public void logout() {
		chat.sendOneWay(new Logout(name));
	}

	public void post(String message) {
		chat.sendOneWay(new ChatMessage(name, name + ": " + message));
	}

	public ChatLog getChatLog() {
		return (ChatLog) chat.sendRequestReply(new GetChatLog(name));
	}
}
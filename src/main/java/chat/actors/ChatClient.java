package chat.actors;

import akka.actor.ActorRef;
import akka.actor.Actors;
import chat.events.ChatLog;
import chat.events.ChatMessage;
import chat.events.GetChatLog;
import chat.events.Login;
import chat.events.Logout;

/**
 * Chat client.
 */
public class ChatClient {

	private String name = null;
	private ActorRef chat = null;

	public ChatClient(String name) {
		this.name = name;

		// starts and connects the client to the remote server
        this.chat = Actors.remote().actorFor("chat:service", "localhost", 2552);
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
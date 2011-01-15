package chat.actors;

import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import chat.events.ChatMessage;
import chat.events.GetChatLog;

/**
 * Internal chat client session.
 */
public class Session extends UntypedActor {
	private ActorRef storage = null;
	private final long loginTime = System.currentTimeMillis();
	private List<String> userLog = new ArrayList<String>();

	public Session(String user, ActorRef storage) {
		this.storage = storage;
		log().logger().info(
				"New session for user [%s] has been created at [%s]", user,
				loginTime);
	}

	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof ChatMessage) {
			userLog.add(((ChatMessage) msg).getMessage());
			storage.sendOneWay(msg);
		} else if (msg instanceof GetChatLog) {
			storage.forward(msg, getContext());
		}

	}
}

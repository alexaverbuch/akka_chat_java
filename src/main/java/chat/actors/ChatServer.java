package chat.actors;

import java.util.HashMap;
import java.util.Map;

import chat.events.ChatMessage;
import chat.events.GetChatLog;
import chat.events.Login;
import chat.events.Logout;

import se.scalablesolutions.akka.actor.ActorRef;
import se.scalablesolutions.akka.actor.UntypedActor;
import se.scalablesolutions.akka.actor.UntypedActorFactory;
import se.scalablesolutions.akka.config.Supervision.FaultHandlingStrategy;
import se.scalablesolutions.akka.config.Supervision.OneForOneStrategy;

/**
 * Chat server. Manages sessions and redirects all other messages to the Session
 * for the client.
 */
public class ChatServer extends UntypedActor {
	private ActorRef storage = null;
	private SessionManagement sessionManagement = null;
	private ChatManagement chatManagement = null;

	public ChatServer() {
		// Creates and links a RedisChatStorage
		storage = getContext().spawnLink(RedisChatStorage.class);

		FaultHandlingStrategy faultHandler = new OneForOneStrategy(
				new Class[] { Exception.class }, // exceptions to handle
				3, // max restart retries
				5000); // within time in ms
		getContext().setFaultHandler(faultHandler);

		sessionManagement = new SessionManagement(getContext(), storage);
		chatManagement = new ChatManagement(getContext(), sessionManagement
				.getSessions());

		log().logger().info("Chat server is starting up...");
	}

	public void onReceive(final Object msg) throws Exception {
		sessionManagement.handleReceive(msg);
		chatManagement.handleReceive(msg);
	}

	public void postStop() {
		log().logger().info("Chat server is shutting down...");
		sessionManagement.shutdownSessions();
		getContext().unlink(storage);
		storage.stop();
	}

	/**
	 * Implements user session management.
	 */
	private class SessionManagement {
		private ActorRef self = null;
		private ActorRef storage = null;
		private Map<String, ActorRef> sessions = new HashMap<String, ActorRef>();

		public SessionManagement(ActorRef self, ActorRef storage) {
			this.self = self;
			this.storage = storage;
		}

		public Map<String, ActorRef> getSessions() {
			return sessions;
		}

		public void handleReceive(final Object msg) {
			if (msg instanceof Login) {
				final String username = ((Login) msg).getUser();
				ActorRef session = UntypedActor
						.actorOf(new UntypedActorFactory() {
							public UntypedActor create() {
								return new Session(username, storage);
							}
						});
				session.start();
				sessions.put(((Login) msg).getUser(), session);
				self.log().logger().info("User [%s] has logged in", username);

			} else if (msg instanceof Logout) {
				String username = ((Logout) msg).getUser();
				ActorRef session = sessions.get(username);
				session.stop();
				sessions.remove(username);
				self.log().logger().info("User [%s] has logged out", username);
			}
		}

		public void shutdownSessions() {
			for (ActorRef session : sessions.values())
				session.stop();
		}
	}

	/**
	 * Implements chat management, e.g. chat message dispatch.
	 */
	private class ChatManagement {
		private ActorRef self = null;
		private Map<String, ActorRef> sessions = null;

		public ChatManagement(ActorRef self, Map<String, ActorRef> sessions) {
			this.self = self;
			this.sessions = sessions;
		}

		public void handleReceive(final Object msg) {
			if (msg instanceof ChatMessage)
				sessions.get(((ChatMessage) msg).getFrom()).sendOneWay(msg);
			else if (msg instanceof GetChatLog)
				sessions.get(((GetChatLog) msg).getFrom()).forward(msg, self);
		}

	}
}
package chat.actors;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Actors;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.config.Supervision;
import chat.events.ChatMessage;
import chat.events.GetChatLog;
import chat.events.Login;
import chat.events.Logout;

/**
 * Chat server. Manages sessions and redirects all other messages to the Session
 * for the client.
 */
public class ChatServer extends UntypedActor {
    private ActorRef storage = null;
    private SessionManagement sessionMgr = null;
    private ChatManagement chatMgr = null;

    public ChatServer() {
        // Creates and links a RedisChatStorage
        storage = getContext().spawnLink(RedisChatStorage.class);

        Supervision.FaultHandlingStrategy faultHandler = new Supervision.OneForOneStrategy(
                null, // exceptions to handle
                null, // max restart retries
                null); // within time in ms
        getContext().setFaultHandler(faultHandler);

        sessionMgr = new SessionManagement(getContext(), storage);
        chatMgr = new ChatManagement(getContext(), sessionMgr);

        logger().info("Chat server is starting up...");
    }

    public void onReceive(final Object msg) {
        sessionMgr.handleReceive(msg);
        chatMgr.handleReceive(msg);
    }

    public void postStop() {
        logger().info("Chat server is shutting down...");
        sessionMgr.shutdownSessions();
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

        public ActorRef getSession(String username) {
            return sessions.get(username);
        }

        public void handleReceive(final Object msg) {
            if (msg instanceof Login) {
                final String username = ((Login) msg).getUser();
                ActorRef session = Actors.actorOf(new UntypedActorFactory() {
                    public UntypedActor create() {
                        return new Session(username, storage);
                    }
                });
                session.start();
                sessions.put(((Login) msg).getUser(), session);
                logger().info("User [%s] has logged in", username);

            } else if (msg instanceof Logout) {
                String username = ((Logout) msg).getUser();
                ActorRef session = sessions.get(username);
                session.stop();
                sessions.remove(username);
                logger().info("User [%s] has logged out", username);
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
        private SessionManagement sessionMgr = null;

        public ChatManagement(ActorRef self, SessionManagement sessionMgr) {
            this.self = self;
            this.sessionMgr = sessionMgr;
        }

        public void handleReceive(final Object msg) {
            if (msg instanceof ChatMessage)
                sessionMgr.getSession(((ChatMessage) msg).getFrom())
                        .sendOneWay(msg);
            else if (msg instanceof GetChatLog)
                sessionMgr.getSession(((GetChatLog) msg).getFrom()).forward(
                        msg, self);
        }

    }
}
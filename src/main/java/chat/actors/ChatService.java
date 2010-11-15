package chat.actors;

import se.scalablesolutions.akka.remote.RemoteNode;

/**
 * Class encapsulating the full Chat Service. Start service by invoking:
 * 
 * <pre>
 * val chatService = Actor.actorOf[ChatService].start
 * </pre>
 */
public class ChatService extends ChatServer {
	public void preStart() {
		RemoteNode.start("localhost", 2552);
		RemoteNode.register("chat:service", getContext());
	}
}

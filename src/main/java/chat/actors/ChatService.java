package chat.actors;


import akka.actor.Actors;

/**
 * Class encapsulating the full Chat Service. Start service by invoking:
 * 
 * <pre>
 * val chatService = Actor.actorOf[ChatService].start
 * </pre>
 */
public class ChatService extends ChatServer {
	public void preStart() {
		Actors.remote().start("localhost", 2552);
		Actors.remote().register("chat:service", getContext());
	}
}

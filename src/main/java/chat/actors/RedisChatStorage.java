package chat.actors;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import chat.events.ChatLog;
import chat.events.ChatMessage;
import chat.events.GetChatLog;

import se.scalablesolutions.akka.actor.UntypedActor;
import se.scalablesolutions.akka.persistence.common.PersistentVector;
import se.scalablesolutions.akka.persistence.redis.RedisStorage;
import se.scalablesolutions.akka.stm.global.*;

/**
 * Redis-backed chat storage implementation.
 */
public class RedisChatStorage extends UntypedActor {

	private final String CHAT_LOG = "akka.chat.log";
	private List<byte[]> chatLog = null;

	// private PersistentVector<byte[]> chatLog = null;

	public RedisChatStorage() {
		// FIXME
		// self.lifeCycle = Permanent

		chatLog = new ArrayList<byte[]>();
		// chatLog = RedisStorage.newVector(CHAT_LOG).asJava();
		// chatLog = RedisStorage.newVector(CHAT_LOG);

		log().logger().info("Redis-based chat storage is starting up...");
	}

	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof ChatMessage) {
			log().logger().debug("New chat message [%s]",
					((ChatMessage) msg).getMessage());

			new Atomic() {
				public Object atomically() {
					try {
						return chatLog.add(((ChatMessage) msg).getMessage()
								.getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					return null;
				}
			}.execute();

		} else if (msg instanceof GetChatLog) {
			List<String> messageList = new Atomic<List<String>>() {
				public List<String> atomically() {
					List<String> messages = new ArrayList<String>();

					for (byte[] messageBytes : chatLog)
						try {
							messages.add(new String(messageBytes, "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					return messages;
				}
			}.execute();
			getContext().replyUnsafe(new ChatLog(messageList));
		}

	}

	public void postRestart(Throwable reason) {
		chatLog = new ArrayList<byte[]>();
		// chatLog = RedisStorage.getVector(CHAT_LOG).asJava();
		// chatLog = RedisStorage.getVector(CHAT_LOG);
	}
}

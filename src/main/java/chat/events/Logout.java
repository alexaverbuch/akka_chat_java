package chat.events;

public class Logout extends Event {

	private static final long serialVersionUID = -1251115846831134471L;
	private String user = null;

	public Logout(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}
}
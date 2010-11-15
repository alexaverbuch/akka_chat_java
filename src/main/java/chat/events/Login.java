package chat.events;

public class Login extends Event {

	private static final long serialVersionUID = 1535536798818479222L;
	private String user = null;

	public Login(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

}
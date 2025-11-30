package iialib.games.model;

/**
 * class used to describe the score corresponding to each player role when the game is over
 */
public class Score<Role extends IRole> {
	
	/**
	 * 
	 */
	public enum Status {WIN,LOOSE,TIE};
	
	// ----------- Attributes ------------

	/**
	 * 
	 */
	private Role role;
	
	/**
	 * 
	 */
	private Status status;
	
	/**
	 * score can be just 1/0 or a real score depending on the game
	 */
	private int score;
	
	// ----------- Constructors ------------


	public Score(Role role, Status status, int score) {
		super();
		this.role = role;
		this.status = status;
		this.score = score;
	}

	// ----------- Getter / Setters  ------------

	public Role getRole() {
		return role;
	}

	public Status getStatus() {
		return status;
	}

	public int getScore() {
		return score;
	}
	
	// ----------- Other public methods  ------------

	public String toString() {
		return "Score <" + role + "," + status + "," + score + ">";
	}
	
}
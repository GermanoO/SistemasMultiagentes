package ball_follower_team;

import java.net.UnknownHostException;


public class MainBall {

	public static void main(String[] args) throws UnknownHostException {
		BallFollowerTeam team1 = new BallFollowerTeam("a");
		
		team1.launchTeamAndServer();
		//team2.launchTeam();
	}
	
}


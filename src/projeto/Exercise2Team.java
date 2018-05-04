package projeto;


import simple_soccer_lib.AbstractTeam;
import simple_soccer_lib.PlayerCommander;


public class Exercise2Team extends AbstractTeam {

	public Exercise2Team(String suffix) {
		super(suffix, 6, true);
	}

	@Override
	protected void launchPlayer(int ag, PlayerCommander commander) {
		criaTimeEsquerda(ag, commander);
	}
	
	private void criaTimeEsquerda(int ag, PlayerCommander commander) {
		double targetX, targetY;
		targetX = 0.0;
		targetY	= 0.0;
		
		//posição goleiro esquerda
		if (ag == 0) {
			targetX = 50.0;
			targetY	= 0.0;	
		}
		
		//lateral esquerdo
		else if(ag == 1) {
			targetX = 30.0;
			targetY	= -12.0;	
		}
		
		//Lateral Direito
		else if(ag == 2) {
			targetX = 30.0;
			targetY	= 12.0;
		}
		
		//meia
		else if(ag == 3) {
			targetX = 18.0;
			targetY	= 0.0;
		}
		
		//ponta esquerda
		else if(ag == 4) {
			targetX = 4.0;
			targetY	= -6.0;
		}
		
		//ponta direita
		else if(ag == 5) {
			targetX = 3.0;
			targetY	= 6.0;
		}
		
		Exercise2Player pl = new Exercise2Player(commander, targetX, targetY);
		pl.start();
	}

}

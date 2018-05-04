package projeto;

import java.util.ArrayList;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.EMatchState;
import simple_soccer_lib.utils.Vector2D;


public class Exercise2Player extends Thread {

	private enum State { ATTACKING, RETURN_TO_HOME, GOLEIRO_OLHANDO_BOLA, GOLEIRO_ALINHADO_BOLA, GOLEIRO_PEGA_BOLA_TOCA, SEM_A_BOLA, COM_A_BOLA, CORRER_PARA_BOLA, TOCAR_BOLA, IR_PARA_GOL};

	private PlayerCommander commander;
	private State state;

	private PlayerPerception selfInfo;
	private FieldPerception  fieldInfo;
	private MatchPerception  matchInfo;
	private Vector2D lastBallPosition;

	private Vector2D homebase; //posição base do jogador

	public Exercise2Player(PlayerCommander player, double x, double y) {
		commander = player;
		homebase = new Vector2D(x, y);
	}

	@Override
	public void run() {
		_printf("Waiting initial perceptions...");
		selfInfo  = commander.perceiveSelfBlocking();
		fieldInfo = commander.perceiveFieldBlocking();
		matchInfo = commander.perceiveMatchBlocking();


		state = State.RETURN_TO_HOME; //todos começam neste estado

		_printf("Starting in a random position...");

		//Desenha os jogadores do time esquerdo
		if(EFieldSide.LEFT.equals(selfInfo.getSide())){
			homebase.setX(- homebase.getX());
			commander.doMoveBlocking(homebase.getX(), homebase.getY());
			
		}
		if(EFieldSide.RIGHT.equals(selfInfo.getSide())){
			
			commander.doMoveBlocking(- homebase.getX(), homebase.getY());
			
		}

		try {
			Thread.sleep(3000); // espera, para dar tempo de ver as mensagens iniciais
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		while (commander.isActive()) {
			updatePerceptions();  //deixar aqui, no começo do loop, para ler o resultado do 'doMove'

			if(EMatchState.AFTER_GOAL_LEFT.equals(matchInfo.getState()) || EMatchState.AFTER_GOAL_RIGHT.equals(matchInfo.getState())) {
				if(EFieldSide.LEFT.equals(selfInfo.getSide())){
					commander.doMoveBlocking(homebase.getX(), homebase.getY());
				}
				if(EFieldSide.RIGHT.equals(selfInfo.getSide())){
					commander.doMoveBlocking(- homebase.getX(), homebase.getY());
				}
				
			}

			if(falta() || Escanteio()) {
				_printf("BATE FALTA" + matchInfo.getState());
				if(isCloserToTheBall()){
					baterFalta();
				}
			}

			if(MeioCampo()) {
				if(selfInfo.getUniformNumber() == 5) {
					cobraMeioCampo();
				}
			}

			if (matchInfo.getState() == EMatchState.PLAY_ON) {

				if(selfInfo.isGoalie()) {
					switch (state) {
					case RETURN_TO_HOME:
						stateReturnToHomeBase();
						break;
					case GOLEIRO_OLHANDO_BOLA:
						stateGoleiroOlhandoBola();
						break;
					case GOLEIRO_ALINHADO_BOLA:
						stateGoleiroAlinhadoBola();
						break;
					case GOLEIRO_PEGA_BOLA_TOCA:
						stateGoleiroPegaBolaToca();
						break;
					default:
						_printf("Goleiro", state);
						break;	
					}
				}
				//TRANSIÇÕES PARA DEFENSORES
				else if(selfInfo.getUniformNumber() == 2 || selfInfo.getUniformNumber() == 3){
					switch (state) {
					case RETURN_TO_HOME:
						stateReturnToHomeBase();
						break;
					case SEM_A_BOLA:
						stateSemABola();
						break;
					case COM_A_BOLA:
						stateComABola();
						break;
					default:
						_printf("Defensores", state);
						break;	
					}
				}
				else if(selfInfo.getUniformNumber() == 4 || selfInfo.getUniformNumber() == 5 || selfInfo.getUniformNumber() == 6){
					switch (state) {
					case RETURN_TO_HOME:
						stateReturnToHomeBase();
						break;
					case CORRER_PARA_BOLA:
						stateCorrerParaBola();
						break;
					case TOCAR_BOLA:
						stateTocarBola();
						break;
					case IR_PARA_GOL:
						stateIrParaGol();
						break;
					default:
						_printf("Atacantes: %s", state);
						break;	
					}
				}
			}
		}

	}

	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelf();
		FieldPerception newField = commander.perceiveField();
		MatchPerception newMatch = commander.perceiveMatch();

		// só atualiza os atributos se tiver nova percepção (senão, mantém as percepções antigas)
		if (newSelf != null) {
			this.selfInfo = newSelf;
		}
		if (newField != null) {
			this.fieldInfo = newField;
		}
		if (newMatch != null) {
			this.matchInfo = newMatch;
		}
	}

	//ESTADO 2 GOLEIRO OBSERVA A BOLA
	private void stateGoleiroOlhandoBola() {
		_printf_once("GE2");
		turnToBall();
		//TRANSIÇÂO PARA O ESTADO 3
		if (EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0) {
			state = State.GOLEIRO_ALINHADO_BOLA;
			return;
		} else if (EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0) {
			state = State.GOLEIRO_ALINHADO_BOLA;
			return;
		}
		//GOLEIRO OBSERVA A BOLA
		if(EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0) {
			Vector2D ballPosition = fieldInfo.getBall().getPosition();
			runY(homebase.getX(),ballPosition.getY()/4, 3);
		} else if(EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0) {
			Vector2D ballPosition = fieldInfo.getBall().getPosition();
			runY(homebase.getX(),ballPosition.getY()/4, 3);
		}
	}

	//ESTADO 3 GOLEIRO ALINHA COM TRAJETORIA DA BOLA -- INCOMPLETO
	private void stateGoleiroAlinhadoBola() {
		_printf_once("GE3");
		if(lastBallPosition == null) {
			lastBallPosition = new Vector2D(0,0);
		}
		turnToBall();
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		if(ballPosition.getX() != lastBallPosition.getX() &&  ballPosition.getY() != lastBallPosition.getY()) {

			_printf_once("Ultima posicao da Bola" + lastBallPosition);
			_printf_once("posicao da bola" + ballPosition);
			Vector2D direcao = ballPosition.sub(lastBallPosition);

			runY(homebase.getX(),direcao.getY()/4, 3);
			_printf_once("ALINHA COM A BOLA");

			//TRANSIÇÃO PARA O ESTADO 4
			if((Math.abs(selfInfo.getPosition().getX() - fieldInfo.getBall().getPosition().getX()) < 12)) {
				state = State.GOLEIRO_PEGA_BOLA_TOCA;
				return;
			}

			lastBallPosition = fieldInfo.getBall().getPosition();;
		}
	}

	//ESTADO 4 GOLEIRO CORRE PARA A BOLA -- IMCOMPLETO -- FALTA TOCAR PARA COMPANHEIRO DESMARCADO
	private void stateGoleiroPegaBolaToca() {
		_printf_once("GE4");
		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		if (arrivedAt(fieldInfo.getBall().getPosition())) {
			commander.doCatchBlocking(0);
			turnToPoint(0, 0);
			Vector2D v;
			if(EFieldSide.LEFT.equals(selfInfo.getSide())){
				v = new Vector2D(0,0);
			}
			else {
				v = new Vector2D(-10,-10);
			}
			commander.doKickToDirectionBlocking(100d,v);
		}else if (isAlignedTo(ballPosition)) {
			_printf_once("ATK: Running to the ball...");
			commander.doDashBlocking(100.0d);
		} else {
			_printf("ATK: Turning...");
			commander.doTurnToPointBlocking(ballPosition);
		}

		if(EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0 ) {
			state = State.GOLEIRO_OLHANDO_BOLA;
			return;
		}else if(EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > -40) {
			state = State.GOLEIRO_ALINHADO_BOLA;
			return;
		}
		
		if(EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0 ) {
			state = State.GOLEIRO_OLHANDO_BOLA;
			return;
		}else if(EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 40) {
			state = State.GOLEIRO_ALINHADO_BOLA;
			return;
		}
	}

	// ESTADO RETURN_TO_HOME_BASE
	private void stateReturnToHomeBase() {

		//TRANSIÇÕES PARA GOLEIRO
		if(selfInfo.isGoalie()) {
			_printf_once("GE1");
			//TRANSIÇÃO PARA O ESTADO 2
			if(EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0) {
				state = State.GOLEIRO_OLHANDO_BOLA;
				return;
			} else if(EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0) {
				state = State.GOLEIRO_OLHANDO_BOLA;
				return;
			}

			//TRANSIÇÃO PARA O ESTADO 3
			if(EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0) {
				state = State.GOLEIRO_ALINHADO_BOLA;
				return;
			} else if(EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0) {
				state = State.GOLEIRO_ALINHADO_BOLA;
				return;
			}
			return;
		}

		//TRANSIÇÕES PARA DEFENSORES
		if(selfInfo.getUniformNumber() == 2 || selfInfo.getUniformNumber() == 3) {
			state = State.SEM_A_BOLA;
			return;
		}

		if(selfInfo.getUniformNumber() == 4 || selfInfo.getUniformNumber() == 5 || selfInfo.getUniformNumber() == 6) {
			state = State.CORRER_PARA_BOLA;
			return;
		}

		if (! arrivedAt(homebase)) {
			if (isAlignedTo(homebase)) {
				_printf_once("RTHB: Running to the base...");
				commander.doDashBlocking(100.0d);			
			} else {
				_printf("RTHB: Turning...");
				commander.doTurnToPointBlocking(homebase);
			}
		}		
	}

	private boolean isCloserToTheBall() {
		ArrayList<PlayerPerception> jogadores  = fieldInfo.getTeamPlayers(selfInfo.getSide());
		int num = 0;
		double dist = 100;

		for (PlayerPerception jogador : jogadores) {
			if(Vector2D.distance(jogador.getPosition(), fieldInfo.getBall().getPosition()) < dist) {
				dist = Vector2D.distance(jogador.getPosition(), fieldInfo.getBall().getPosition());
				num = jogador.getUniformNumber();
			}
		}
		_printf_once("Jogador N: " + num);
		_printf_once("Distancia pra bola: " + dist);
		return selfInfo.getUniformNumber() == num;  
	}

	private boolean arrivedAt(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		return Vector2D.distance(myPos, targetPosition) <= 1.5;
	}

	private boolean isAlignedTo(Vector2D targetPosition) {
		Vector2D myPos = selfInfo.getPosition();
		double angle = selfInfo.getDirection().angleFrom(targetPosition.sub(myPos));
		return angle < 15.0d && angle > -15.0d;
	}

	//for debugging
	public void _printf_once(String format, Object...objects) {
		if (! format.equals(lastformat)) {  //dependendo, pode usar ==
			_printf(format, objects);
		}
	}
	private String lastformat = ""; 
	public void _printf(String format, Object...objects) {
		String playerInfo = "";
		if (selfInfo != null) {
			playerInfo += "[" + selfInfo.getTeam() + "/" + selfInfo.getUniformNumber() + "] ";
		}
		System.out.printf(playerInfo + format + "%n", objects);
		lastformat = format;
	}

	private void turnToBall() {
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		Vector2D myPos = selfInfo.getPosition();

		Vector2D newDirection = ballPosition.sub(myPos);
		_printf_once("TURN TO BALL");
		commander.doTurnToDirectionBlocking(newDirection);		
	}

	private void runY(double x, double y, double erro) {
		if(matchInfo != null){
			Vector2D point = new Vector2D(x, y);
			while ((Math.abs(selfInfo.getPosition().getX() - point.getX()) > erro ||
					Math.abs(selfInfo.getPosition().getY() - point.getY()) > erro)) {
				turnToPoint(x, y);
				commander.doDashBlocking(100.0d);
				updatePerceptions();
			}
		}
	}

	private void turnToPoint(double x, double y){
		Vector2D myPos = selfInfo.getPosition();
		Vector2D point = new Vector2D(x, y);
		Vector2D newDirection = point.sub(myPos);

		commander.doTurnToDirectionBlocking(newDirection);		
	}

	//ESTADO SEM A BOLA
	public void stateSemABola() {
		_printf_once("DE1");

		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		if((EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0) || (EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0)) {
			if(Vector2D.distance(selfInfo.getPosition(), ballPosition) <= 25) {
				if (isAlignedTo(ballPosition)) {
					_printf_once("ATK: Running to the ball...");
					commander.doDashBlocking(100.0d);
				} else {
					_printf("ATK: Turning...");
					commander.doTurnToPointBlocking(ballPosition);
				}
				if (arrivedAt(fieldInfo.getBall().getPosition())) {
					_printf_once("CHEGUEI NA BOLA");
					state = State.COM_A_BOLA;
					return;
				}	
			}
			else {
				if (! arrivedAt(homebase)) {
					if (isAlignedTo(homebase)) {
						_printf_once("RTHB: Running to the base...");
						commander.doDashBlocking(100.0d);			
					} else {
						_printf("RTHB: Turning...");
						commander.doTurnToPointBlocking(homebase);
					}
				}
				else {
					turnToBall();
				}
			}


		}
		//SE A BOLA ESTA NO CAMPO DE ATAQUE ELE VOLTA PRA SUA POSIÇÃO INICIAL
		else if((EFieldSide.LEFT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() > 0) || (EFieldSide.RIGHT.equals(selfInfo.getSide()) && fieldInfo.getBall().getPosition().getX() < 0)) {
			if (! arrivedAt(homebase)) {
				if (isAlignedTo(homebase)) {
					_printf_once("RTHB: Running to the base...");
					commander.doDashBlocking(100.0d);			
				} else {
					_printf("RTHB: Turning...");
					commander.doTurnToPointBlocking(homebase);
				}
			}
			else {
				turnToBall();
			}
		}

	}

	public void stateComABola() {
		_printf_once("DE2");
		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		if (arrivedAt(ballPosition)) {
			Vector2D v;
			if(EFieldSide.LEFT.equals(selfInfo.getSide())){
				v = new Vector2D(0,0);
			}
			else {
				v = new Vector2D(-1,-1);
			}
			//Vector2D v = new Vector2D(0,0);
			commander.doKickToDirectionBlocking(50d,v);
		} else {
			if (isAlignedTo(ballPosition)) {
				_printf_once("ATK: Running to the ball...");
				commander.doDashBlocking(100.0d);
				state = State.SEM_A_BOLA;
				return;
			} else {
				_printf("ATK: Turning...");
				commander.doTurnToPointBlocking(ballPosition);
			}
		}
	}

	private void stateCorrerParaBola() {
		_printf_once("AE2");

		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		if (isAlignedTo(ballPosition)) {
			_printf_once("ATK: Running to the ball...");
			commander.doDashBlocking(100.0d);
		} else {
			_printf("ATK: Turning...");
			commander.doTurnToPointBlocking(ballPosition);
		}

		if(isCloserToTheBall()) {
			state = State.IR_PARA_GOL;
			return;
		}

		else {
			if((EFieldSide.LEFT.equals(selfInfo.getSide()) && ballPosition.getX() > 0) || (EFieldSide.RIGHT.equals(selfInfo.getSide()) && ballPosition.getX() < 0) ) {
				_printf("AJUDANDO O ATACANTE");
				Vector2D ponto = new Vector2D(ballPosition.getX(), homebase.getY());

				if (! arrivedAt(ponto)) {
					if (isAlignedTo(ponto)) {
						_printf_once("INDO AJUDAR");
						commander.doDashBlocking(100.0d);			
					} else {
						_printf("GIRANDO PARA AJUDAR");
						commander.doTurnToPointBlocking(ponto);
					}
				}

			}else {
				_printf("VOLTANDO PARA POSICAO");
				if (! arrivedAt(homebase)) {
					if (isAlignedTo(homebase)) {
						_printf_once("RTHB: Running to the base...");
						commander.doDashBlocking(100.0d);			
					} else {
						_printf("RTHB: Turning...");
						commander.doTurnToPointBlocking(homebase);
					}
				}
				else {
					turnToBall();
				}
			}
		}
	}

	private void stateTocarBola() {
		_printf_once("AE3");
		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		if (arrivedAt(ballPosition)) {
			_printf_once("Jogador numero: " + selfInfo.getUniformNumber());
			Vector2D jogador = jogadorMaisPertoDoGol(selfInfo.getUniformNumber());
			Vector2D v = jogador.sub(ballPosition);
			commander.doKickToDirectionBlocking(100d,v);
			_printf_once("TOCANDO BOLA");
			state = State.CORRER_PARA_BOLA;
			return;
		}
		else {
			state = State.CORRER_PARA_BOLA;
			return;
		}

	}

	private void stateIrParaGol() {
		_printf_once("AE4");
		Vector2D ballPosition = fieldInfo.getBall().getPosition();

		_printf_once("Estado da partida" + matchInfo.getState());

		if (arrivedAt(ballPosition)) {
			Vector2D gol;
			if(EFieldSide.LEFT.equals(selfInfo.getSide())) {
				gol = new Vector2D(50,0);
			}else {
				gol = new Vector2D(-50,0);
			}
			Vector2D v = gol.sub(ballPosition);
			if(EFieldSide.LEFT.equals(selfInfo.getSide())  && ballPosition.getX() > 35 && (ballPosition.getY() < 20 || ballPosition.getY() > -20)) {
				_printf_once("CHUTAR FORTE PARA O GOL");
				commander.doKickToDirectionBlocking(100d,v);
			}
			if(EFieldSide.RIGHT.equals(selfInfo.getSide())  && ballPosition.getX() < -35 && (ballPosition.getY() < 20 || ballPosition.getY() > -20)) {
				_printf_once("CHUTAR FORTE PARA O GOL");
				commander.doKickToDirectionBlocking(100d,v);
			}
			if(ballPosition.getY() > 20 || ballPosition.getY() < -20) {
				state = State.TOCAR_BOLA;
				return;
			}
			else {
				_printf_once("CONDUZIR BOLA PARA O GOL");
				commander.doKickToDirectionBlocking(50d,v);
			}
		} 
		else {
			state = State.CORRER_PARA_BOLA;
			return;
		}
	}

	public Vector2D jogadorMaisPertoDoGol(int num) {
		ArrayList<PlayerPerception> jogadores  = fieldInfo.getTeamPlayers(selfInfo.getSide());
		Vector2D posicaoGol = new Vector2D(50,0);
		Vector2D jogadorMaisProximo = new Vector2D(0,0);
		double dist = 100;

		for (PlayerPerception jogador : jogadores) {
			if(Vector2D.distance(jogador.getPosition(), posicaoGol) < dist) {
				if(jogador.getUniformNumber() != num) {
					dist = Vector2D.distance(jogador.getPosition(), fieldInfo.getBall().getPosition());
					jogadorMaisProximo = jogador.getPosition();
					num = jogador.getUniformNumber();		
				}
			}
		}
		_printf_once("Jogador N: " + num);
		_printf_once("Distancia para o gol: " + dist);
		return jogadorMaisProximo;  
	}

	public boolean Escanteio() {
		boolean retorno = false;
		if(EMatchState.CORNER_KICK_LEFT.equals(matchInfo.getState()) || EMatchState.CORNER_KICK_RIGHT.equals(matchInfo.getState())) {
			retorno = true;
		}
		return retorno;
	}

	public boolean falta() {
		boolean retorno = false;
		if(EMatchState.FREE_KICK_LEFT.equals(matchInfo.getState()) || EMatchState.FREE_KICK_RIGHT.equals(matchInfo.getState())) {
			retorno = true;
		}
		return retorno;
	}

	public boolean MeioCampo() {
		boolean retorno = false;
		if(EMatchState.KICK_OFF_LEFT.equals(matchInfo.getState()) || EMatchState.KICK_OFF_RIGHT.equals(matchInfo.getState())) {
			retorno = true;
		}
		return retorno;
	}

	private void baterFalta() {
		if(matchInfo!= null){
			Vector2D ballPosition = fieldInfo.getBall().getPosition();
			while ((Math.abs(selfInfo.getPosition().getX() - ballPosition.getX()) > 1 ||
					Math.abs(selfInfo.getPosition().getY() - ballPosition.getY()) > 1)) {
				turnToBall();
				commander.doDashBlocking(100.0d);
				updatePerceptions();
			}
			if (arrivedAt(ballPosition)) {
				Vector2D gol = new Vector2D(50,0);
				if(ballPosition.getX() > 30 && (ballPosition.getY() < 15 || ballPosition.getY() > -15)) {
					_printf_once("COBRAR FALTA PRO GOL");
					Vector2D v = gol.sub(ballPosition);
					commander.doKickToDirectionBlocking(100d,v);
				}else {
					_printf_once("TOCAR PARA AMIGO");
					Vector2D jogador = jogadorMaisPertoDoGol(selfInfo.getUniformNumber());
					Vector2D v = jogador.sub(ballPosition);
					commander.doKickToDirectionBlocking(50d,v);
				}
			}

		}
	}

	public void cobraMeioCampo() {
		_printf("COBRANDO MEIO CAMPO");
		Vector2D ballPosition = fieldInfo.getBall().getPosition();
		if (arrivedAt(ballPosition)) {
			Vector2D v;
			if(EFieldSide.LEFT.equals(selfInfo.getSide())) {
				v = new Vector2D(-3,6);
			}
			else {
				v = new Vector2D(3,-6);
			}
			 
			commander.doKickToDirectionBlocking(15d,v);
		} else if (isAlignedTo(ballPosition)) {
			commander.doDashBlocking(100.0d);
		} else {
			commander.doTurnToPointBlocking(ballPosition);
		}
	}
}


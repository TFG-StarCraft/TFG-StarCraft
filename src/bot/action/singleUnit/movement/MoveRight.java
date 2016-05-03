package bot.action.singleUnit.movement;

import com.Com;

import bwapi.Unit;
import newAgent.GenericAgent;

/**
 * Movement. Move right.
 * @author Alberto Casas Ortiz
 * @author Raúl Martín Guadaño
 * @author Miguel Ascanio Gómez
 */
public class MoveRight extends MoveAction {	
	
	/***************/
	/* CONSTRUCTOR */
	/***************/
	
	/**
	 * Constructor of the class MoveRight.
	 * @param com Comunication.
	 * @param unit Unit to move.
	 * @param agentEpoch 
	 */
	public MoveRight(GenericAgent agent, Com com, Unit unit) {
		super(agent, com, unit);
	}	
	
	/*******************/
	/* OVERRIDE METHOD */
	/*******************/
	
	/**
	 * Do the move up movement.
	 */
	@Override
	protected void setUpMove() {
		this.endX = unit.getX() + bot.Const.STEP;
		this.endY = unit.getY();

		this.testX = unit.getX() + bot.Const.STEP + bot.Const.TEST;
		this.testY = unit.getY();
	}

}
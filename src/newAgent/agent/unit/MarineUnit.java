package newAgent.agent.unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.Com;

import bot.Bot;
import bot.action.GenericAction;
import bot.commonFunctions.CheckAround;
import bot.commonFunctions.HP;
import bwapi.Unit;
import newAgent.Const;
import newAgent.decisionMaker.DM_LambdaQE;
import newAgent.decisionMaker.Shared_LambdaQE;
import newAgent.event.AbstractEvent;
import newAgent.event.factories.AEFDestruirUnidad;
import newAgent.master.GenericMaster;
import newAgent.state.DataMarine;
import newAgent.state.State;

public class MarineUnit extends UnitAgent {

	private static final int TIMEOUT = 1500;
	
	private HashMap<Integer, Unit> map;
	private double iniMyHP, iniEnemyHP, endMyHP, endEnemyHP;

	private int frameCount;
	
	private boolean endCondition;
	private double nextReward;
	
	public MarineUnit(GenericMaster master, Unit unit, Com com, Bot bot, Shared_LambdaQE shared) {
		super(master, unit, com, bot);

		this.map = new HashMap<>();
		
		this.frameCount = 0;
		this.decisionMaker = new DM_LambdaQE(this, shared);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// ENVIRONMENT ////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	@Override
	protected void setUpFactory() {
		this.factory = new AEFDestruirUnidad(com);
	}

	@Override
	public State getInitState() {		
		this.waitForBotEndsInit();
		
		return new State(this, new DataMarine(com, unit));
	}

	@Override
	public int getNumDims() {
		return DataMarine.getNumDims();
	}

	@Override
	public ArrayList<Integer> getNumValuesPerDims() {
		return DataMarine.getNumValuesPerDims();
	}

	///////////////////////////////////////////////////////////////////////////
	// BWAPI //////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onFrame() {
		if (frameCount >= TIMEOUT) {
			master.onTimeOut();
			return;
		}
		
		frameCount++;
		ArrayList<GenericAction> actionsToRegister = this.actionsToDispatch.getQueueAndFlush();
		
		if (actionsToRegister.size() > 1) {
			System.err.println("More than 1 action to register");
		}

		for (GenericAction action : actionsToRegister) {
			onNewAction(action);
		}
		
		if (this.currentAction != null)
			this.currentAction.onUnit(this.unit);
		
		for (Unit u : CheckAround.getEnemyUnitsAround(unit)) {
			if (!this.map.containsKey(u.getID()))
				this.map.put(u.getID(), u);
		}
	}
	
	@Override
	public void onUnitDestroy(Unit u) {
		if (this.unit.getID() == u.getID()) {
			addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_KILLED));
		} else {
			if (this.map.containsKey(u.getID())) {
				this.map.remove(u.getID());
				if (this.map.size() == 0)
					addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_KILL_ALL));
				else
					addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_KILL));
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// ACTIONS ////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void onNewAction() {
		iniMyHP = unit.getHitPoints();
		iniEnemyHP = HP.getHPOfEnemiesAround(unit);
	}

	@Override
	public void onEndAction(GenericAction genericAction, boolean correct) {
		// TODO ASSERT PRE DEBUG
		if (genericAction != currentAction)
			com.onError("end action != current action", true);

		addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_DEFAULT_ACTION, genericAction, correct));
	}

	///////////////////////////////////////////////////////////////////////////
	// EVENTS /////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean solveEventsAndCheckEnd() {
		// Three possible scenarios:
		// 1 - No events (and therefore no end)
		// 2 - One event, kill event or actionEnd event, endEvent is determined
		// by the event itself
		// 3 - Two events, caused when the unit ends moving and kills the target
		// in the same frame. This causes an end.

		boolean isFinal = false;
		// Descending order (attend first more prio. events)
		java.util.Collections.sort(events, AbstractEvent.getPrioCompDescend());

		for (AbstractEvent event : events) {
			isFinal = isFinal | event.isFinalEvent();
			if (event.returnsControlToAgent()) {
				// Calculate reward for current ending action
				if (event.isFinalEvent()) {
					nextReward = event.isGoalState() ? Const.REWARD_SUCCESS : Const.REWARD_FAIL;
				} else {
					// Calculate end hp's
					endMyHP = unit.getHitPoints();
					endEnemyHP = HP.getHPOfEnemiesAround(unit);
					if (endEnemyHP != -1 && iniEnemyHP == -1) {
						// Killed enemies that initially unit didn't see
						List<Unit> l = CheckAround.getEnemiesAround(unit);
						if (l.isEmpty())
							iniEnemyHP = -1;

						iniEnemyHP = 0.0;
						for (int i = 0; i < l.size(); i++) {
							iniEnemyHP += l.get(i).getHitPoints();
						}
					}

					if (iniEnemyHP == -1 && endEnemyHP == -1) {
						nextReward = 0;
					}

					double r = (iniEnemyHP - endEnemyHP) / (double) iniEnemyHP - (iniMyHP - endMyHP) / (double) iniMyHP;
					nextReward = r * newAgent.Const.REWARD_MULT_FACTOR;
				}

				this.endCondition = event.isFinalEvent();

				event.notifyEvent();			
				// TODO only agents observe unit
				// this.currentAction.unRegisterOnUnitObserver();
				this.currentAction = null;

				// Signal AFTER onEnd and reward are set
				this.signalActionEnded();
				
				break;
			}
		}

		this.events.clear();
		return isFinal;
	}
	
	@Override
	public Boolean getOnFinalUpdated() {
		return endCondition;
	}
	
	@Override
	public double getRewardUpdated() {
		return nextReward;
	}
}

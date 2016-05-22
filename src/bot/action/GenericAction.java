package bot.action;

import com.Com;

import bwapi.Order;
import newAgent.agent.OnEndActionObserver;
import utils.DebugEnum;

public abstract class GenericAction {

	protected Com com;

	// Unit that is executing the action
	protected final OnEndActionObserver agent;

	// Frame num were the action should have finished, reaching this frame
	// before the action has ended (reaching endPos in this case), the action
	// is forced to end (maybe the unit is stuck)
	protected long frameEnd;
	// Max frames that this action can be executing
	protected long maxFramesOfExecuting;

	protected boolean actionStarted;
	
	// Current BWAPI order
	protected Order order;
	
	final protected boolean specialStart;

	/**
	 * This method executes the action, this means that it checks the current
	 * state of the environment (usually related with this.unit) and acts in
	 * consequence: 
	 * 	- if the action hasn't started yet, it starts. 
	 * 	- if the action isn't finished yet, but no termination condition is
	 *	satisfied (i.e. the unit hasn't arrived to the target position, but
	 * 	keeps moving), the action keeps going without problem. 
	 * 	- if the action has reached its goal, onEndAction is called and the 
	 * 	agent is usually notified that an action has ended correctly. 
	 * 	- if the action has't reached its goal, but it has been interrupted 
	 * 	(i.e. too many frames executing without ending), onEndAction(false) 
	 * 	is called and the agent is notified than an action ended unsuccessfully.
	 */
	public abstract void executeAction();

	/**
	 * 
	 * @return true if the action is possible to start/it should start.
	 */
	public abstract boolean isPossible();

	/**
	 * 
	 * @param correct
	 */
	public void onEndAction(boolean correct) {
		com.onDebugMessage("Correct: " + correct + ", " + this.getClass().getName(), DebugEnum.ACTION_OK);
		agent.onEndAction(this, correct);
	}

	/**
	 * 
	 * @param com
	 *            com object
	 * @param unit
	 *            unit that is executing this action
	 * @param maxFramesOfExecuting
	 *            number of frames that this action can be executing
	 * @param specialStart
	 *            set to FALSE if the startAction method should be called only
	 *            once in onUnit before any call of executeAction, set to TRUE
	 *            if the startAction is called inside of execute action, and
	 *            therefore it wont be called in onUnit
	 */
	public GenericAction(OnEndActionObserver agent, Com com, Long maxFramesOfExecuting, boolean specialStart) {
		this.agent = agent;
		this.com = com;

		this.maxFramesOfExecuting = maxFramesOfExecuting;
		this.actionStarted = false;
		this.specialStart = specialStart;
	}

	public abstract void onFrame();
	
	/**
	 * Starts this action. This means that the action can be executing
	 * maxFramesOfExecuting frames from the first call of this method. This
	 * method can be called many times, the framesCount wont be affected,
	 * meaning that the action will keep executing only maxFramesOfExecuting
	 * frames.
	 */
	protected void startAction() {
		if (!actionStarted) {
			this.frameEnd = com.bot.frames + this.maxFramesOfExecuting;
			this.actionStarted = true;
		}
	}

	/**
	 * 
	 * @return true if this action has been executing more than
	 *         maxFramesOfExecuting frames.
	 */
	protected boolean isFramesLimitsReached() {
		return com.bot.frames >= this.frameEnd;
	}

}

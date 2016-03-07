package bot;

import com.Com;

import bot.action.GenericAction;
import bot.event.AbstractEvent;
import bot.event.factories.AbstractEventsFactory;
import bot.event.factories.AEFDestruirUnidad;
import bwapi.Unit;
import utils.DebugEnum;

public class BotDestruirUnidad extends Bot {

	public BotDestruirUnidad(Com com) {
		super(com);
	}

	@Override
	public AbstractEventsFactory getNewFactory() {
		return new AEFDestruirUnidad(com);
	}

	@Override
	public void onEndAction(GenericAction genericAction, Object... args) {
		addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_DEFAULT_ACTION, genericAction, args[0]));
	}

	@Override
	public void onUnitDestroy(Unit unit) {
		try {
			com.onDebugMessage("DESTROY " + frames, DebugEnum.ON_UNIT_DESTROY);
			
			if (unit.getID() == (com.ComData.unit.getID())) {
				addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_KILLED));
			} else {
				addEvent(factory.newAbstractEvent(AEFDestruirUnidad.CODE_KILL));
			}
	
			super.onUnitDestroy(unit);
		} catch (Throwable e) {
			com.onError(e.getLocalizedMessage(), true);
		}
	}

	@Override
	public boolean solveEventsAndCheckEnd() {
		// TODO DEBUG assert pre
		if (com.ComData.getOnFinalUpdated())
			com.onError("Called solveEventsAndCheck while onFinal", true);
		
		/* Three possible scenarios:
		 * 1 - No events (and therefore no end)
		 * 2 - One event, kill event or actionEnd event, endEvent is determined by the event itself
		 * 3 - Two events, caused when the unit ends moving and kills the target in the same frame. This causes an end.
		 */
		boolean isFinal = false;
		// Descending order (attend first more prio. events)
		java.util.Collections.sort(events, AbstractEvent.getPrioCompDescend());	
		
//		if (!events.isEmpty()) {
//			AbstractEvent event = events.get(0);
//			
//			isFinal = isFinal | event.isFinalEvent();
//			// on final is set *** BEFORE *** calling solveEvent, therefore is
//			// set BEFORE returning the control to Agent
//			com.ComData.setOnFinal(isFinal);
//			event.solveEvent();
//		}
		for (AbstractEvent event : events) {
			isFinal = isFinal | event.isFinalEvent();
			// on final is set *** BEFORE *** calling solveEvent, therefore is
			// set BEFORE returning the control to Agent

			com.ComData.setOnFinal(isFinal);
			event.solveEvent();

			if (event.returnsControlToAgent()) {
				break;
			}
		}

		return isFinal;
	}

}

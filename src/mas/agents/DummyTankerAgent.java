package mas.agents;

import env.EntityType;
import env.Environment;
import jade.core.behaviours.TickerBehaviour;
import mas.abstractAgent;

public class DummyTankerAgent extends abstractAgent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1784844593772918359L;

	/** This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time.
	 * 1) set the agent attributes
	 * 2) add the behaviours */
	@Override
	protected void setup() {

		super.setup();

		// get the parameters given into the object[]. In the current case, the environment where the agent will evolve
		final Object[] args = getArguments();
		if (args != null && args[0] != null && args[1] != null) {
			deployAgent((Environment) args[0], (EntityType) args[1]);

		} else {
			System.err.println("Malfunction during parameter's loading of agent" + this.getClass().getName());
			System.exit(-1);
		}

		// Add the behaviours
		addBehaviour(new RandomTankerBehaviour(this));

		System.out.println("the agent " + this.getLocalName() + " is started");

	}

	/** This method is automatically called after doDelete() */
	@Override
	protected void takeDown() {

	}
}

/************************************** BEHAVIOUR **************************************/

class RandomTankerBehaviour extends TickerBehaviour {
	/** When an agent choose to move */
	private static final long serialVersionUID = 9088209402507795289L;

	public RandomTankerBehaviour(final mas.abstractAgent myagent) {
		super(myagent, 10000);
		// super(myagent);
	}

	@Override
	public void onTick() {
		// Example to retrieve the current position
		String myPosition = ((mas.abstractAgent) this.myAgent).getCurrentPosition();

		if (myPosition != "") {
			// List of observable from the agent's current position
			// List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition

			// DO NOTHING. But it could be a good idea to change that
		}

	}

}
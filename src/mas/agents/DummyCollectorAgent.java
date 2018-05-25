package mas.agents;

import java.util.List;
import java.util.Random;

import env.Attribute;
import env.Couple;
import env.EntityType;
import env.Environment;
import jade.core.behaviours.TickerBehaviour;
import mas.abstractAgent;

public class DummyCollectorAgent extends abstractAgent {

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
		addBehaviour(new RandomWalkExchangeBehaviour(this));

		System.out.println("the agent " + this.getLocalName() + " is started");

	}

	/** This method is automatically called after doDelete() */
	@Override
	protected void takeDown() {

	}

	/************************************** BEHAVIOUR **************************************/

	class RandomWalkExchangeBehaviour extends TickerBehaviour {
		/** When an agent choose to move */
		private static final long serialVersionUID = 9088209402507795289L;

		public RandomWalkExchangeBehaviour(final mas.abstractAgent myagent) {
			super(myagent, 600);
			// super(myagent);
		}

		@Override
		public void onTick() {
			// Example to retrieve the current position
			String myPosition = ((mas.abstractAgent) this.myAgent).getCurrentPosition();

			if (myPosition != "") {
				// List of observable from the agent's current position
				List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition
				System.out.println(this.myAgent.getLocalName() + " -- list of observables: " + lobs);

				// list of attribute associated to the currentPosition
				List<Attribute> lattribute = lobs.get(0).getRight();

				// example related to the use of the backpack for the treasure hunt
				Boolean b = false;
				for (Attribute a : lattribute) {
					switch (a) {
					case TREASURE:
					case DIAMONDS:
						System.out.println("My treasure type is :" + ((mas.abstractAgent) this.myAgent).getMyTreasureType());
						System.out.println("My current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
						System.out.println("Value of the treasure on the current position: " + a.getName() + ": " + a.getValue());
						System.out.println("The agent grabbed :" + ((mas.abstractAgent) this.myAgent).pick());
						System.out.println("the remaining backpack capacity is: " + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
						// System.out.println("The value of treasure on the current position: (unchanged before a new call to observe()): "+a.getValue());
						b = true;
						break;

					default:
						break;
					}
				}

				// If the agent picked (part of) the treasure
				if (b) {
					List<Couple<String, List<Attribute>>> lobs2 = ((mas.abstractAgent) this.myAgent).observe();// myPosition
					System.out.println("lobs after picking " + lobs2);
				}
				System.out.println(
						this.myAgent.getLocalName() + " - My current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
				System.out.println(this.myAgent.getLocalName() + " - The agent tries to transfer is load into the Silo (if reachable); succes ? : "
						+ ((mas.abstractAgent) this.myAgent).emptyMyBackPack("Agent5"));
				System.out.println("My current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());

				// Random move from the current position
				Random r = new Random();
				int moveId = 1 + r.nextInt(lobs.size() - 1);

				// The move action (if any) should be the last action of your behaviour
				((mas.abstractAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());
			}

		}

	}
}

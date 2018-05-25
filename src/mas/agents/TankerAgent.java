package mas.agents;

import env.EntityType;
import env.Environment;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import mas.behaviours.ReceiveMapBehavior;
import mas.behaviours.SendMapBehaviour;
import mas.behaviours.TankerBehaviour;

public class TankerAgent extends CustomAgent {

	public TankerAgent() {
		super(EntityType.AGENT_TANKER);
	}

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
			// deployAgent((Environment) args[0]);
			deployAgent((Environment) args[0], (EntityType) args[1]);
		} else {
			System.err.println("Malfunction during parameter's loading of agent" + this.getClass().getName());
			System.exit(-1);
		}

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID()); /* getAID est l’AID de l’agent qui veut s’enregistrer*/
		ServiceDescription sd = new ServiceDescription();
		sd.setType("tanker"); /* il faut donner des noms aux services qu’on propose (ici explorer)*/
		sd.setName(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Add the behaviours
		addBehaviour(new TankerBehaviour(this));
		// addBehaviour(new UnstuckBehaviour(this));
		addBehaviour(new SendMapBehaviour(this));
		addBehaviour(new ReceiveMapBehavior(this));

		System.out.println("the agent " + this.getLocalName() + "of type" + ((EntityType) args[1]).toString() + " is started");

	}

	/** This method is automatically called after doDelete() */
	@Override
	protected void takeDown() {

	}

}

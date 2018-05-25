package mas.behaviours;

import java.io.IOException;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.lang.acl.ACLMessage;
import mas.abstractAgent;
import mas.agents.CustomAgent;
import mas.utils.AgentInfo;
import mas.utils.MessageInformation;
import mas.utils.NodeInfo;
import mas.utils.Serialiser;


/** Share map with other agents */
public class SendMapBehaviour extends TickerBehaviour{

	private static final long serialVersionUID = 9088209402507795289L;

	/**The previous map to test for newly visited nodes*/
	public HashMap<String, NodeInfo> previousMap;

	private int ticks = 0;
	private static final int maxTicks = 3;


	/** @param myagent the Agent this behaviour is linked to */
	public SendMapBehaviour(final Agent myagent) {
		super(myagent, ((CustomAgent)(myagent)).agentBlockingTime);
		this.previousMap = new HashMap<>();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onTick() {

		ticks++;
		HashMap<String, NodeInfo> map = ((CustomAgent)this.myAgent).map;
		HashMap<String, AgentInfo> agents = ((CustomAgent)this.myAgent).agents;

		//get nearby agents
		DFAgentDescription dfd = new DFAgentDescription();
		// ServiceDescription sd = new ServiceDescription();
		// sd.setType( "explorer" );
		// dfd.addServices(sd);
		DFAgentDescription[] result;
		try {
			result = DFService.search(this.myAgent, dfd);
			//			System.out.println(result.length + " results" );
			if (result.length>0) {
				//System.out.println(" " + result[0].getName() );
				if (map.size() > previousMap.size() || ticks > maxTicks || agents.get(myAgent.getLocalName()).stuckCounter > 0) {

					ticks = 0;
					//map size increased, sending new map to the other agents

					//Create the message
					final ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setSender(this.myAgent.getAID());
					for (DFAgentDescription n:result) {
						if(! n.getName().getName().equals(this.myAgent.getName())) {
							//TODO test if agent has not recently received message instead of random periodic send
							msg.addReceiver(new AID(n.getName().getLocalName(), AID.ISLOCALNAME));
							//							System.out.println(this.myAgent.getLocalName()+" : sending map to : " + n.getName().getLocalName() + " (map size : " + map.size() + ")");
						}
					}

					//serialize map and send it

					try {
						String content = Serialiser.convertToString(new MessageInformation(map, agents));
						//						System.out.println(content);
						msg.setContent(content);
						((abstractAgent)(this.myAgent)).sendMessage(msg);
						previousMap = (HashMap<String, NodeInfo>) map.clone();

					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}

	}

}

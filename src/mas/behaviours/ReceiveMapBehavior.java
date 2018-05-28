package mas.behaviours;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.agents.CustomAgent;
import mas.utils.AgentInfo;
import mas.utils.MessageInformation;
import mas.utils.NodeInfo;
import mas.utils.Serialiser;
import mas.utils.TreasureInfo;

public class ReceiveMapBehavior extends SimpleBehaviour {

	private static final long serialVersionUID = -3548666428797430411L;

	public ReceiveMapBehavior(final mas.abstractAgent myagent) {
		super(myagent);
	}

	@Override
	public void action() {

		//1) create the reception template (inform + name of the sender)
		MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		//					MessageTemplate.MatchSender(new AID(senderName, AID.ISLOCALNAME)));

		//2) get the message
		ACLMessage msg = this.myAgent.receive(msgTemplate);


		if (msg != null) {		
			try {

				// get sent map
				MessageInformation inf = (MessageInformation) Serialiser.convertFromString(msg.getContent());
				HashMap<String, NodeInfo> newMap = inf.map;
				HashMap<String, NodeInfo> myMap = ((CustomAgent)(this.myAgent)).map;
				HashMap<String, AgentInfo> agents = ((CustomAgent)this.myAgent).agents;
				HashMap<String, TreasureInfo> treasures = ((CustomAgent) this.myAgent).treasures;

				// browse treasures
				for (Entry<String, TreasureInfo> entry : inf.treasures.entrySet()) {
					String key = entry.getKey();
					TreasureInfo value = entry.getValue();
					if (treasures.containsKey(key)) {
						treasures.get(key).merge(value);
					} else {
						treasures.put(key, value);
					}
				}
				// TODO ? remove empty treasures

				// browse agents
				for (Entry<String, AgentInfo> entry : inf.agents.entrySet()) {
					String key = entry.getKey();
					AgentInfo value = entry.getValue();
					if(agents.containsKey(key)) {
						if(agents.get(key).lastUpdate < value.lastUpdate) {
							agents.put(key, value);
						}
					}
					else {
						agents.put(key, value);
					}
				}

				// update reference node
				if (agents.get(msg.getSender().getLocalName()).reference != null && (agents.get(myAgent.getLocalName()).reference == null
						|| agents.get(msg.getSender().getLocalName()).reference.compareTo(agents.get(myAgent.getLocalName()).reference) < 0))
				{
					agents.get(myAgent.getLocalName()).reference = agents.get(msg.getSender().getLocalName()).reference;
				}

				// browse map
				for (Map.Entry<String, NodeInfo> entry : newMap.entrySet()) {
					String key = entry.getKey();
					NodeInfo value = entry.getValue();

					//System.out.println(myMap.containsKey(key) + " " + (!myMap.containsKey(key) ? "-1" : myMap.get(key).lastUpdate) + " " + value.lastUpdate);
					// refresh our map
					if( !myMap.containsKey(key) || myMap.get(key).lastUpdate < value.lastUpdate) {
						myMap.put(key, value);
						//updated.add(key);
					}
				}
				//				if(!updated.isEmpty()) System.out.println(this.myAgent.getLocalName()+" : received update on : " + updated);


			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}else{
			//block the behaviour until the next message
			//			System.out.println("No message received, the behaviour "+this.getBehaviourName()+ " goes to sleep");
			block();
		}
	}

	@Override
	public boolean done() {
		return false;
	}

}

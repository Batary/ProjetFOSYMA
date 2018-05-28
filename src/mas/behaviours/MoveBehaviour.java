package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import mas.agents.CustomAgent;
import mas.utils.AgentInfo;
import mas.utils.NodeInfo;
import mas.utils.TreasureInfo;


/** This behaviour is used to make basic movement with simple monitoring from a parent behaviour. */
public class MoveBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private String myAgentName;
	private AgentInfo agInfo;

	// once destination is reached, we go back to callerBehaviour
	private Behaviour callerBehaviour;
	public String destination;

	// attribute for parent class to test for new treasures
	public boolean newTreasure = false;

	public MoveBehaviour(final mas.abstractAgent myagent, final Behaviour caller, String destination) {
		super(myagent, ((CustomAgent) (myagent)).agentBlockingTime);
		callerBehaviour = caller;
		this.destination = destination;
	}

	@Override
	public void onTick() {
		// Example to retrieve the current position
		String myPosition = ((mas.abstractAgent) this.myAgent).getCurrentPosition();
		HashMap<String, AgentInfo> agents = ((CustomAgent) this.myAgent).agents;
		HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;
		HashMap<String, TreasureInfo> treasures = ((CustomAgent) this.myAgent).treasures;

		if (myPosition != "") {

			myAgentName = this.myAgent.getLocalName();
			if (!agents.containsKey(myAgentName)) {
				agInfo = new AgentInfo(myPosition, ((CustomAgent) (this.myAgent)).type, myAgentName);
				agents.put(myAgentName, agInfo);
			} else {
				agInfo = agents.get(myAgentName);
				agInfo.position = myPosition;
				agInfo.update();
			}

			if (agInfo.isStuck()) {
				this.myAgent.removeBehaviour(callerBehaviour);
				String dest = (agInfo.path.isEmpty() ? "" : agInfo.path.get(agInfo.path.size() - 1));
				agInfo.stuckCounter = 3;
				agents.put(myAgentName, agInfo);
				myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, dest));
				this.stop();
				return;
			}

			// List of observable from the agent's current position
			List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition

			// list of attribute associated to the currentPosition
			List<Attribute> lattribute = lobs.get(0).getRight();

			List<String> connected = new ArrayList<>(lobs.size() - 1);

			// create nodes and add them to the map
			for (Couple<String, List<Attribute>> a : lobs) {
				connected.add(a.getLeft());
				if (!map.containsKey(a.getLeft())) {
					map.put(a.getLeft(), new NodeInfo(a.getLeft(), lobs.get(0).getLeft()));
				} else if (map.get(a.getLeft()).lastUpdate == 0) {
					if (!map.get(a.getLeft()).connectedNodes.contains(myPosition)) {
						map.get(a.getLeft()).connectedNodes.add(myPosition);
					}

				}
			}
			connected.remove(0);

			map.put(lobs.get(0).getLeft(), new NodeInfo(lobs.get(0).getRight(), lobs.get(0).getLeft(), connected));

			// example related to the use of the backpack for the treasure hunt
			Attribute b = null;

			for (Attribute a : lattribute) {
				switch (a) {
				case TREASURE:
					System.out.println(myAgentName + " : the value of treasure on the current position is : " + a.getValue());
					b = a;
					break;
				case DIAMONDS:
					System.out.println(myAgentName + " : the value of diamonds on the current position is : " + a.getValue());
					b = a;
				default:
					break;
				}
			}

			// If the agent picked (part of) the treasure
			if (((mas.abstractAgent) this.myAgent).getBackPackFreeSpace() > 0 && b != null
					&& ((mas.abstractAgent) this.myAgent).getMyTreasureType().equals(b.getName())) {
				int g = ((mas.abstractAgent) this.myAgent).pick();
				if (g > 0) {
					List<Couple<String, List<Attribute>>> lobs2 = ((mas.abstractAgent) this.myAgent).observe();// myPosition
					System.out.println(myAgentName + " : list of observables after picking " + lobs2);
					System.out.println(myAgentName + " : the agent grabbed :" + g);
					System.out.println(myAgentName + " : my current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
				}
			}

			if (((mas.abstractAgent) this.myAgent).emptyMyBackPack("AgentTanker1")) {
				System.out.println(myAgentName + " : given backpack content to tanker.");
				System.out.println(myAgentName + " : my current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
				awakeParent();
			}

			// TODO check if a treasure was moved or removed

			// wake up behaviour if new treasure was found
			if (!treasures.containsKey(myPosition) && b != null) {
				newTreasure = true;
				treasures.put(myPosition, new TreasureInfo(b, (int) b.getValue(), myPosition));
				System.out.println(myAgentName + " : new treasure found on node " + myPosition + " : " + b);
				awakeParent();
			}
			else {
				newTreasure = false;
			}

			String nextNode = "";

			if (agInfo.path != null && agInfo.path.size() > 0) {
				nextNode = agInfo.path.get(0);
			}
			else {
				// wake up parent behaviour
				awakeParent();
				return;
			}

			if (!nextNode.equals(agInfo.position)) {
				if (!((mas.abstractAgent) this.myAgent).moveTo(nextNode)) {
					// could not move to node
					System.out.println(myAgentName + " : could not make movement : " + lobs.get(0).getLeft() + " --> " + nextNode);
					agInfo.stuckCounter++;
				} else {
					agInfo.stuckCounter = 0;
					if (!agInfo.path.isEmpty()) {
						agInfo.path.remove(0);
						agInfo.position = nextNode;
						agInfo.update();
					}
				}
			} else {
				System.out.println(myAgentName + " : standing on node " + nextNode);
				if (!agInfo.path.isEmpty()) {
					agInfo.path.remove(0);
					agInfo.update();
				}
			}

			agents.put(myAgentName, agInfo);

		}

	}

	private void awakeParent() {
		callerBehaviour.restart();
	}

}
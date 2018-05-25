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


/** This behaviour is used to make basic movement with simple monitoring from a parent behaviour. */
public class MoveBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private String myAgentName;
	private AgentInfo agInfo;

	// once destination is reached, we go back to callerBehaviour
	private Behaviour callerBehaviour;
	public String destination;

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
				myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, dest));
				this.stop();
			}

			// List of observable from the agent's current position
			List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition

			// System.out.println(myAgentName+" -- list of observables: "+lobs);

			// //Little pause to allow you to follow what is going on
			// try {
			// System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
			// System.in.read();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			// list of attribute associated to the currentPosition
			List<Attribute> lattribute = lobs.get(0).getRight();

			HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;
			List<String> connected = new ArrayList<>(lobs.size() - 1);
			// create nodes and add them to the map
			lobs.forEach(a -> {
				connected.add(a.getLeft());
				if (!map.containsKey(a.getLeft())) {
					map.put(a.getLeft(), new NodeInfo(a.getLeft(), lobs.get(0).getLeft()));
				} else if (map.get(a.getLeft()).lastUpdate == 0 && !map.get(a.getLeft()).connectedNodes.contains(a.getLeft())) {
					map.get(a.getLeft()).connectedNodes.add(a.getLeft());
					// TODO check treasure on side nodes ?
				}
			});
			connected.remove(0);

			map.put(lobs.get(0).getLeft(), new NodeInfo(lobs.get(0).getRight(), lobs.get(0).getLeft(), connected));

			// System.out.println(map);

			// example related to the use of the backpack for the treasure hunt
			Boolean b = false;

			for (Attribute a : lattribute) {
				switch (a) {
				case TREASURE:

					// System.out.println("My type is : "+((mas.abstractAgent)this.myAgent).getMyTreasureType());
					// System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					// System.out.println("Value of the treasure on the current position: "+a.getValue());
					// System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					// System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());

					System.out.println(myAgentName + " : the value of treasure on the current position is : " + a.getValue());
					b = true;
					// Little pause to allow you to follow what is going on
					// try {
					// System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
					// System.in.read();
					// } catch (IOException e) {
					// e.printStackTrace();
					// }
					break;
				case DIAMONDS:
					// System.out.println("My type is : "+((mas.abstractAgent)this.myAgent).getMyTreasureType());
					// System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					// System.out.println("Value of the diamonds on the current position: "+a.getValue());
					// System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					// System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());

					System.out.println(myAgentName + " : the value of diamonds on the current position is : " + a.getValue());
					b = true;
					// Little pause to allow you to follow what is going on
					// try {
					// System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
					// System.in.read();
					// } catch (IOException e) {
					// e.printStackTrace();
					// }
				default:
					break;
				}
			}

			// If the agent picked (part of) the treasure
			if (((mas.abstractAgent) this.myAgent).getBackPackFreeSpace() > 0 && b) {
				int g = ((mas.abstractAgent) this.myAgent).pick();
				if (g > 0) {
					List<Couple<String, List<Attribute>>> lobs2 = ((mas.abstractAgent) this.myAgent).observe();// myPosition
					System.out.println("list of observables after picking " + lobs2);

					System.out.println("The agent grabbed :" + g);

					System.out.println("My current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
					System.out.println("The agent tries to transfer is load into the Silo (if reachable); succes ? : "
							+ ((mas.abstractAgent) this.myAgent).emptyMyBackPack("AgentTanker1"));
					System.out.println("My current backpack capacity is:" + ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace());
				}

				// TODO wake up behaviour if new treasure was found

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
				}
			}

			agents.put(myAgentName, agInfo);

		}

	}

	private void awakeParent() {
		callerBehaviour.restart();
	}

}
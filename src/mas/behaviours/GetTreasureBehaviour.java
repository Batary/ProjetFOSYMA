package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import env.Attribute;
import env.Couple;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import mas.abstractAgent;
import mas.agents.CustomAgent;
import mas.exceptions.PathBlockedException;
import mas.utils.AgentInfo;
import mas.utils.GoalType;
import mas.utils.MapUtils;
import mas.utils.NodeInfo;
import mas.utils.TreasureInfo;

public class GetTreasureBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -8312041662098834690L;
	private String myAgentName;
	private AgentInfo agInfo;

	private boolean done = false;
	private MoveBehaviour move;
	private String myTreasure;

	private long lastMessage = System.currentTimeMillis();

	public GetTreasureBehaviour(final mas.abstractAgent myagent) {
		super(myagent);
		move = new MoveBehaviour(myagent, this, "");
		myagent.addBehaviour(move);
	}

	@Override
	public void action() {
		// Example to retrieve the current position
		String myPosition = ((mas.abstractAgent) this.myAgent).getCurrentPosition();
		HashMap<String, AgentInfo> agents = ((CustomAgent) this.myAgent).agents;
		HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;
		HashMap<String, TreasureInfo> treasures = ((CustomAgent) this.myAgent).treasures;
		AgentInfo tanker = agents.get(CustomAgent.tankerAgent);

		if (myPosition != "") {

			myAgentName = this.myAgent.getLocalName();
			if (!agents.containsKey(myAgentName)) {
				agInfo = new AgentInfo(myPosition, ((CustomAgent) (this.myAgent)).type, myAgentName);
				agents.put(myAgentName, agInfo);
			} else {
				agInfo = agents.get(myAgentName);
				agInfo.position = myPosition;
				agInfo.goal = GoalType.getTreasure;
				agInfo.freeSpace = ((mas.abstractAgent) this.myAgent).getBackPackFreeSpace();
				agInfo.maxSpace = (agInfo.maxSpace > agInfo.freeSpace ? agInfo.maxSpace : agInfo.freeSpace);
				agInfo.update();
			}

			// List of observable from the agent's current position
			List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition

			List<String> connected = new ArrayList<>(lobs.size() - 1);
			// create nodes and add them to the map
			lobs.forEach(a -> {
				connected.add(a.getLeft());
				if (!map.containsKey(a.getLeft())) {
					map.put(a.getLeft(), new NodeInfo(a.getLeft(), lobs.get(0).getLeft()));
				} else if (map.get(a.getLeft()).lastUpdate == 0 && !map.get(a.getLeft()).connectedNodes.contains(a.getLeft())) {
					map.get(a.getLeft()).connectedNodes.add(a.getLeft());
				}
			});
			connected.remove(0);

			map.put(lobs.get(0).getLeft(), new NodeInfo(lobs.get(0).getRight(), lobs.get(0).getLeft(), connected));

			myTreasure = agInfo.currentTreasure;
			// check if treasure is already assigned
			if (myTreasure == null) {
				for (Map.Entry<String, TreasureInfo> entry : treasures.entrySet()) {
					String key = entry.getKey();
					TreasureInfo value = entry.getValue();
					if (value.collectorAgent != null && value.collectorAgent.equals(myAgentName) && value.amount > 0) {
						// treasure is already assigned to this agent
						myTreasure = key;
						break;
					}
				}
			}

			if (agInfo.currentTreasure != null && treasures.get(agInfo.currentTreasure).amount == 0) {
				agInfo.currentTreasure = null;
			}

			if (myTreasure == null) {
				for (Map.Entry<String, TreasureInfo> entry : treasures.entrySet()) {
					String key = entry.getKey();
					TreasureInfo value = entry.getValue();
					// treasure is not assigned and has the good type
					if (value.amount > 0 && value.collectorAgent == null && ((abstractAgent) myAgent).getMyTreasureType().equals(value.type.getName())) {
						myTreasure = key;
						// System.out.println(myAgentName + " : my treasure : " + myTreasure + " reference : " + agInfo.reference);
						break;
					}
				}
			}

			if (myTreasure != null) {

				if (agInfo.freeSpace == agInfo.maxSpace) {

					// call tanker if treasure is more than two trips remaining
					if (myTreasure != null && tanker != null && (tanker.currentTreasure == null || !tanker.currentTreasure.equals(myTreasure))
							&& System.currentTimeMillis() - lastMessage > ((CustomAgent) myAgent).agentBlockingTime
							&& (treasures.get(myTreasure).amount - agInfo.maxSpace) * 0.66 > 0) {
						lastMessage = System.currentTimeMillis();
						final ACLMessage msg1 = new ACLMessage(ACLMessage.PROPOSE);
						msg1.setContent(myTreasure);
						msg1.setSender(this.myAgent.getAID());
						msg1.addReceiver(new AID(tanker.name, AID.ISLOCALNAME));
						((abstractAgent) (this.myAgent)).sendMessage(msg1);
					}

					// empty backpack -- go to treasure
					agInfo.goal = GoalType.getTreasure;
					if (!myTreasure.equals(myPosition)) {
						try {
							agInfo.path = MapUtils.getPath(myPosition, myTreasure, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
									myAgentName);
						} catch (PathBlockedException e) {
							System.out.println(myAgentName + " : path is blocked");
							agInfo.stuckCounter = 3;
							agInfo.update();
							agents.put(myAgentName, agInfo);
							move.stop();
							myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, myTreasure));
							this.stop();
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			if (agInfo.freeSpace < agInfo.maxSpace) {
				agInfo.goal = GoalType.giveTreasure;
				if (tanker != null && myTreasure != null && tanker.currentTreasure != null && tanker.currentTreasure.equals(myTreasure)) {
					if (myTreasure.equals(myPosition)) {
						// wait
						block();
					}
					else {
						// go to treasure
						try {
							agInfo.path = MapUtils.getPath(myPosition, myTreasure, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
									myAgentName);
						} catch (PathBlockedException e) {
							System.out.println(myAgentName + " : path is blocked");
							agInfo.stuckCounter = 3;
							agInfo.update();
							agents.put(myAgentName, agInfo);
							move.stop();
							myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, myTreasure));
							this.stop();
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					// go to tanker to empty backpack
					// TODO go to real tanker position if known
					if (agInfo.reference != null) {

						try {
							agInfo.path = MapUtils.getPath(myPosition, agInfo.reference, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
									myAgentName);
						} catch (PathBlockedException e) {
							System.out.println(myAgentName + " : path is blocked");
							agInfo.stuckCounter = 3;
							agInfo.update();
							agents.put(myAgentName, agInfo);
							move.stop();
							myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, agInfo.reference));
							this.stop();
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					else {
						// no reference to tanker, keep exploring
						agInfo.goal = GoalType.explore;
						agInfo.update();
						agents.put(myAgentName, agInfo);
						move.stop();
						myAgent.addBehaviour(new ExploreBehaviour((abstractAgent) myAgent));
						this.stop();
						return;
					}
				}

				// pick treasure

				// get tanker active treasure
				// --> check with currentTreasure

				// call tanker if treasure is more than two trips remaining

				// unload backpack

				// System.out.println("Assigned node " + key + " " + value.type + " to " + myAgentName);
				// treasures.get(key).collectorAgent = myAgentName;

				// final ACLMessage msg1 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				// msg1.setSender(this.myAgent.getAID());
				// msg1.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
				// ((abstractAgent) (this.myAgent)).sendMessage(msg1);

			}
			if (myTreasure == null && agInfo.freeSpace == agInfo.maxSpace) {

				// get back to exploreBehaviour
				System.out.println(myAgentName + " : no more treasure to get, continue exploration.");
				agInfo.goal = GoalType.explore;
				agInfo.update();
				agents.put(myAgentName, agInfo);
				move.stop();
				myAgent.addBehaviour(new ExploreBehaviour((abstractAgent) myAgent));
				this.stop();
				return;
			}

			// String dest = "";

			if (!MapUtils.checkPath(agInfo.path, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName)) {

			}
			if (agInfo.path != null && agInfo.path.size() > 0) {
				move.destination = agInfo.path.get(agInfo.path.size() - 1);
			}

			agents.put(myAgentName, agInfo);

			block();
		}
	}

	private void stop() {
		this.done = true;

	}

	@Override
	public boolean done() {
		return done;
	}

}
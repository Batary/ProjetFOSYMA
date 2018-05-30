package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import env.Attribute;
import env.Couple;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import mas.abstractAgent;
import mas.agents.CustomAgent;
import mas.exceptions.NoUnvisitedNodeException;
import mas.exceptions.PathBlockedException;
import mas.utils.AgentInfo;
import mas.utils.GoalType;
import mas.utils.MapUtils;
import mas.utils.NodeInfo;
import mas.utils.TreasureInfo;

//wumpus : attribut stench (sur case)

public class TankerBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private String myAgentName;
	private AgentInfo agInfo;
	private boolean done = false;
	private MoveBehaviour move;

	public TankerBehaviour (final mas.abstractAgent myagent) {
		super(myagent);
	}

	@Override
	public void action() {
		//Example to retrieve the current position
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
		HashMap<String, AgentInfo> agents = ((CustomAgent)this.myAgent).agents;
		HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;
		HashMap<String, TreasureInfo> treasures = ((CustomAgent) this.myAgent).treasures;

		if (myPosition != null && !myPosition.isEmpty()) {

			myAgentName = this.myAgent.getLocalName();
			if(!agents.containsKey(myAgentName)){
				agInfo = new AgentInfo(myPosition, ((CustomAgent) (this.myAgent)).type, myAgentName);
				agents.put(myAgentName, agInfo);
			} else {
				agInfo = agents.get(myAgentName);
				agInfo.position = myPosition;
				agInfo.update();
				if (agInfo.goal == null) {
					agInfo.goal = GoalType.waitForInput;
				}
			}

			// end simulation
			try {
				MapUtils.getUnvisitedNode(myPosition, map, agents, ((CustomAgent) myAgent).agentBlockingTime, myAgentName);
			} catch (NoUnvisitedNodeException e1) {
				boolean finished = true;
				for (Map.Entry<String, TreasureInfo> entry : treasures.entrySet()) {
					if (entry.getValue().amount > 0) {
						finished = false;
						break;
					}
				}
				for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
					if (entry.getValue().freeSpace < entry.getValue().maxSpace) {
						finished = false;
						break;
					}
				}
				// assumes wumpus has not moved a treasure and we do not know about it
				if (finished) {
					System.err.println(
							"Map is completely explored and all treasures have been gathered !\n(see TankerBehaviour to disable this)\n\nStopping platform...\n");

					Codec codec = new SLCodec();
					Ontology jmo = JADEManagementOntology.getInstance();
					myAgent.getContentManager().registerLanguage(codec);
					myAgent.getContentManager().registerOntology(jmo);
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(myAgent.getAMS());
					msg.setLanguage(codec.getName());
					msg.setOntology(jmo.getName());
					try {
						myAgent.getContentManager().fillContent(msg, new Action(myAgent.getAID(), new ShutdownPlatform()));
						myAgent.send(msg);
					} catch (Exception e) {
					}
					this.stop();
					return;
				}
			} catch (Exception e1) {
				// e1.printStackTrace();
			}

			// set movement behaviour
			if (move == null || move.done()) {
				move = new MoveBehaviour((abstractAgent) myAgent, this, null);
				myAgent.addBehaviour(move);
			}

			// if (agInfo.isStuck()) {
			// myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, agInfo.path.get(agInfo.path.size() - 1)));
			// this.stop();
			// return;
			// }

			// List of observable from the agent's current position
			List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition

			// list of attribute associated to the currentPosition
			// List<Attribute> lattribute= lobs.get(0).getRight();

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

			if(agInfo.reference == null) {

				agInfo.path = MapUtils.getFreeNodePath(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);
				if(agInfo.path == null) {
					agInfo.path = MapUtils.getUnusedNodePath(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);
					if(agInfo.path == null) {
						agInfo.path = new ArrayList<>();
					}
					else {
						System.out.println(myAgentName + " : my position : " + myPosition + " ; path to unused node : " + agInfo.path);
					}
				}
				else {

					// // test
					// // if test fails, need to check current position of agents ( A->B and B->A -->[BOOM]<-- )
					// try {
					// if (agInfo.path.size() > 0) {
					// agInfo.path = MapUtils.getPath(myPosition, agInfo.path.get(agInfo.path.size() - 1), map, agents,
					// ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);
					// }
					// } catch (Exception e) {
					// e.printStackTrace();
					// }

					if (MapUtils.isFreeNode(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName)) {
						agInfo.reference = myPosition;
						System.out.println(myAgentName + " : reached reference node " + agInfo.reference);
					}
					else {
						// agInfo.reference = agInfo.path.get(agInfo.path.size() - 1);
						System.out.println(myAgentName + " : my position : " + myPosition + " ; path to reference point : " + agInfo.path);

						move.destination = agInfo.path.get(agInfo.path.size() - 1);
					}
				}
				block();
			}
			else {

				// check messageBox for collector that wants to get treasure
				MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				// MessageTemplate.MatchSender(new AID(senderName, AID.ISLOCALNAME)));

				// 2) get the message
				ACLMessage msg = this.myAgent.receive(msgTemplate);

				if (agInfo.currentTreasure == null) {
					agInfo.goal = GoalType.waitForInput;
					if (msg != null) {
						String dest = msg.getContent();
						if (map.containsKey(dest) && agents.containsKey(msg.getSender().getLocalName()) && treasures.containsKey(dest)) {

							// accept and send reply
							// final ACLMessage msg1 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							// msg1.setSender(this.myAgent.getAID());
							// msg1.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
							// ((abstractAgent) (this.myAgent)).sendMessage(msg1);

							agInfo.currentTreasure = dest;
							System.out.println("Assigned treasure node " + dest + " to " + msg.getSender().getLocalName());
							treasures.get(dest).collectorAgent = msg.getSender().getLocalName();

							// switch goal
							agInfo.goal = GoalType.getTreasure;
							agInfo.stuckCounter++;
							agInfo.update();
							agents.put(myAgentName, agInfo);
							block();
						}
						else {
							// send reply : node is not in map
							// final ACLMessage msg1 = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
							// msg1.setSender(this.myAgent.getAID());
							// msg1.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
							// ((abstractAgent) (this.myAgent)).sendMessage(msg1);
						}
					}
				} else {
					// send reply : agent is already busy
					// final ACLMessage msg1 = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
					// msg1.setSender(this.myAgent.getAID());
					// msg1.addReceiver(new AID(msg.getSender().getLocalName(), AID.ISLOCALNAME));
					// ((abstractAgent) (this.myAgent)).sendMessage(msg1);
					block();
				}

				if (agInfo.reference == myPosition && agInfo.currentTreasure == null) {
					block();
				}
				else {
					if (agInfo.currentTreasure == null || treasures.get(agInfo.currentTreasure).amount == 0) {
						// go back to reference
						agInfo.currentTreasure = null;
						try {
							agInfo.path = MapUtils.getPath(myPosition, agInfo.reference, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
									myAgentName);
							// if (!agInfo.path.isEmpty()) {
							// agInfo.path.remove(agInfo.path.size() - 1);
							// }
							block();
						} catch (PathBlockedException e) {
							System.out.println(myAgentName + " : path is blocked");
							agInfo.stuckCounter = 3;
							agInfo.update();
							move.stop();
							agents.put(myAgentName, agInfo);
							myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.waitForInput, agInfo.reference));
							this.stop();
							return;
						} catch (Exception e) {
							e.printStackTrace();
						}

					} else {
						// get to the treasure
						// test if we are on a nearby node
						if (!map.get(agInfo.currentTreasure).connectedNodes.contains(myPosition)) {
							try {
								agInfo.path = MapUtils.getPath(myPosition, agInfo.currentTreasure, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
										myAgentName);
								if (!agInfo.path.isEmpty()) {
									agInfo.path.remove(agInfo.path.size()-1);
								}
								else {
									agInfo.path = MapUtils.getUnusedNodePath(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
											myAgentName);
									if (agInfo.path == null) {
										agInfo.stuckCounter = 3;
										agInfo.update();
										agents.put(myAgentName, agInfo);
										move.stop();
										myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.getTreasure, null));
										this.stop();
										return;
									}
								}
								block();
							} catch (PathBlockedException e) {
								// System.out.println(myAgentName + " : path is blocked by " + e.agent);
								agInfo.stuckCounter = 3;
								agInfo.update();
								move.stop();
								agents.put(myAgentName, agInfo);
								myAgent.addBehaviour(
										new UnstuckBehaviour(myAgent, GoalType.getTreasure, map.get(agInfo.currentTreasure).connectedNodes.get(0)));
								this.stop();
								return;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						else {
							// we are next to the treasure, test if treasure is finished
							if (treasures.get(agInfo.currentTreasure) == null || treasures.get(agInfo.currentTreasure).amount == 0) {
								agInfo.currentTreasure = null;
								return;
							} else {
								String treasure = agInfo.currentTreasure;
								// TreasureInfo ti = ((CustomAgent) myAgent).treasures.get(treasure);
								String posCollector = agents.get(treasures.get(treasure).collectorAgent).position;
								if (!treasure.equals(posCollector)) {
									agInfo.path = MapUtils.getUnusedNodePath(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime,
											myAgentName);
									if (agInfo.path == null) {
										agInfo.stuckCounter = 3;
										agInfo.update();
										agents.put(myAgentName, agInfo);
										move.stop();
										myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.getTreasure, null));
										this.stop();
										return;
									}
								}
								block();
							}
						}
					}
				}
			}
			agents.put(myAgentName, agInfo);
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
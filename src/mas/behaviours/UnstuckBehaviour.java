package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import env.Attribute;
import env.Couple;
import env.EntityType;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import mas.abstractAgent;
import mas.agents.CustomAgent;
import mas.exceptions.PathBlockedException;
import mas.utils.AgentInfo;
import mas.utils.GoalType;
import mas.utils.MapUtils;
import mas.utils.NodeInfo;
import mas.utils.TreasureInfo;

/**This behaviour is used for any agent which is stuck. <br>
 * This will not be used in simple situations, as a priority order should handle it. <br><br>
 * Stategy :<br> - send broadcast messages to warn other agents that this one is stuck <br>
 * - try again once or twice reaching goal <br>
 * - try to send a MOVEOUT signal to nearby agents and wait, they will relay the MOVEOUT signal and activate this behaviour <br>
 * - if it did not solve the problem, try to reach a highly-connected, low-crowded node <br>
 * - if this fails : try to move the other way, if there is a highly-connected node on opposite direction <br> 
 * - if agent is still blocked, move randomly until situation gets better (last resort) <br>
 * - make sure agent is no longer stuck (check at every step) <br>
 * - once agent is not stuck anymore, release the caller behaviour
 * */
public class UnstuckBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = -3029763588541389794L;

	// when the initial target node has been reached, switch back to previous behaviour
	private GoalType previousGoal;
	private String target;

	private int tick = 0;
	private int checkCounter = 10;

	public UnstuckBehaviour(Agent a, GoalType goal, String targetNode) {
		super(a,((CustomAgent)a).agentBlockingTime);
		this.previousGoal = goal;
		this.target = targetNode;
		System.out.println(myAgent.getLocalName() + " is stuck !");
	}

	@Override
	public void onTick() {
		tick++;

		HashMap<String, NodeInfo> map = ((CustomAgent) this.myAgent).map;
		HashMap<String, AgentInfo> agents = ((CustomAgent) this.myAgent).agents;

		String myPosition = ((mas.abstractAgent) this.myAgent).getCurrentPosition();

		String myAgentName = this.myAgent.getLocalName();
		AgentInfo agInfo = agents.get(myAgentName);
		agInfo.position = myPosition;
		agInfo.update();

		agents.put(myAgentName, agInfo);

		List<Couple<String, List<Attribute>>> lobs = ((mas.abstractAgent) this.myAgent).observe();// myPosition
		// List<Attribute> lattribute = lobs.get(0).getRight();

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

		if (agInfo.type == EntityType.AGENT_COLLECTOR && ((mas.abstractAgent) this.myAgent).emptyMyBackPack("AgentTanker1")) {
			if (((mas.abstractAgent) this.myAgent).getBackPackFreeSpace() < agInfo.maxSpace)
			{
				System.out.println(myAgentName + " : given backpack content to tanker.");
			}
		}

		int time = ((CustomAgent) this.myAgent).agentBlockingTime;
		if (tick > 3 && ((target != null && map.containsKey(target) && target.equals(myPosition)) || checkGoal(map, agents, time))) {
			// not stuck anymore !
			agInfo.position = myPosition;
			agInfo.update();

			System.out.println(this.myAgent.getLocalName() + " is no longer stuck.");
			agents.put(myAgentName, agInfo);
			switchBehaviour();
			return;
		}

		// (last resort) : move randomly
		Random r = new Random();
		int moveId = r.nextInt(lobs.size() - 1) + 1;
		String dest = lobs.get(moveId).getLeft();

		if(agInfo.path != null && agInfo.path.size() > 0) {
			dest = agInfo.path.get(0);
		}
		else {

			agInfo.path = new ArrayList<>();
			agInfo.path.add(dest);

			// try to reach unused node
			if ((agInfo.stuckCounter <= 5 && tick < 10) || tick % checkCounter / 1.5 == 0) {

				agInfo.path = MapUtils.getFreeNodePath(myPosition, map, agents, time, myAgentName);
				if (agInfo.path == null) {
					agInfo.path = MapUtils.getUnusedNodePath(myPosition, map, agents, time, myAgentName);
				}
				if (agInfo.path != null && !agInfo.path.isEmpty()) {
					dest = agInfo.path.get(0);
				}
				else {
					// TODO test free node ?
					agInfo.path = new ArrayList<>();
					agInfo.path.add(dest);
				}
			}

			// try to go back to destination
			if (((agInfo.stuckCounter > 5 && agInfo.stuckCounter < 7) || tick % checkCounter == 0) && target != null && target != "") {
				checkCounter += 1 + checkCounter / 10;
				try {
					agInfo.path = MapUtils.getPath(myPosition, target, map, agents, time, myAgentName);
					dest = agInfo.path.get(0);
				} catch (PathBlockedException e) {
					System.out.println(this.myAgent.getLocalName() + " : path to node " + target + " is still blocked.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}


		if (!((mas.abstractAgent) this.myAgent).moveTo(dest)) {
			agInfo.stuckCounter++;
			agInfo.path.clear();
			//could not move to node
			System.out.println(this.myAgent.getLocalName() + "[stuck:" + agInfo.stuckCounter + "] : could not make movement : " + myPosition + " --> "
					+ dest);
		}
		else {
			agInfo.path.remove(0);
		}
		agInfo.update();
		((CustomAgent) myAgent).agents.put(myAgentName, agInfo);
	}
	// TODO add MOVEOUT behaviour
	// TODO try to get path to free node ?

	private void switchBehaviour() {
		switch (previousGoal) {
		case explore:
			myAgent.addBehaviour(new ExploreBehaviour((abstractAgent) myAgent));
			break;
		case shareInformation:
			myAgent.addBehaviour(new ExploreBehaviour((abstractAgent) myAgent));
			break;
		case waitForInput:
			myAgent.addBehaviour(new TankerBehaviour((abstractAgent) myAgent));
			break;
		case getTreasure:
			myAgent.addBehaviour((((CustomAgent) myAgent).type == EntityType.AGENT_TANKER ? new TankerBehaviour((abstractAgent) myAgent)
					: new GetTreasureBehaviour((abstractAgent) myAgent)));
			break;
		case giveTreasure:
			myAgent.addBehaviour(new GetTreasureBehaviour((abstractAgent) myAgent));
			break;
		default:
			System.err.println("Error : unimplemented behaviour switch : " + previousGoal.toString());
			break;
		}
		(((CustomAgent) this.myAgent).agents).get(myAgent.getLocalName()).stuckCounter = 0;
		this.stop();
	}

	private boolean checkGoal(HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time) {
		boolean reached = tick > 100;
		AgentInfo tanker = agents.get("AgentTanker1");
		switch (previousGoal) {
		case explore:
			if (target != null && map.containsKey(target) && map.get(target).lastUpdate > 0) {
				reached = true;
			}
			break;
		case shareInformation:

			if (tanker != null && tanker.lastUpdate > System.currentTimeMillis() - 2 * ((CustomAgent) myAgent).agentBlockingTime) {
				reached = true;
			}

			break;
		case waitForInput:
			if (MapUtils.isFreeNode(((mas.abstractAgent) this.myAgent).getCurrentPosition(), map, agents, time, myAgent.getLocalName())
					|| MapUtils.checkPath(
							MapUtils.getFreeNodePath(((mas.abstractAgent) this.myAgent).getCurrentPosition(), map, agents, time, myAgent.getLocalName()),
							map, agents, time, myAgent.getLocalName())) {
				reached = true;
			}
			break;

		case getTreasure:
			if (((CustomAgent) myAgent).type == EntityType.AGENT_TANKER) {
				if (((abstractAgent) myAgent).getBackPackFreeSpace() == 0) {
					reached = true;
				}
			}
			else {
				String treasure = agents.get(myAgent.getLocalName()).currentTreasure;
				TreasureInfo ti = ((CustomAgent) myAgent).treasures.get(treasure);
				if (ti == null) {
					reached = true;
				} else {
					String posCollector = agents.get(ti.collectorAgent).position;
					if (treasure.equals(posCollector) && map.get(treasure).connectedNodes.contains(((mas.abstractAgent) this.myAgent).getCurrentPosition())) {
						reached = true;
					}
				}
			}
			break;

		case giveTreasure:
			if (tanker != null && tanker.lastUpdate > System.currentTimeMillis() - 2 * ((CustomAgent) myAgent).agentBlockingTime
			&& ((abstractAgent) myAgent).getBackPackFreeSpace() > 0) {
				reached = true;
			}
			break;

		default:
			System.err.println("Error : unimplemented behaviour goal check : " + previousGoal.toString());
			break;
		}
		return reached;

	}


}

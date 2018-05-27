package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.SimpleBehaviour;
import mas.agents.CustomAgent;
import mas.exceptions.NoUnvisitedNodeException;
import mas.exceptions.PathBlockedException;
import mas.utils.AgentInfo;
import mas.utils.GoalType;
import mas.utils.MapUtils;
import mas.utils.NodeInfo;

//wumpus : attribut stench (sur case)

public class ExploreBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 9088209402507795289L;
	private String myAgentName;
	private AgentInfo agInfo;

	private boolean done = false;
	private MoveBehaviour move;


	public ExploreBehaviour (final mas.abstractAgent myagent) {
		super(myagent);
		move = new MoveBehaviour(myagent, this, "");
		myagent.addBehaviour(move);
	}

	@Override
	public void action() {
		//Example to retrieve the current position
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
		HashMap<String, AgentInfo> agents = ((CustomAgent)this.myAgent).agents;

		if (myPosition!=""){

			myAgentName = this.myAgent.getLocalName();
			if(!agents.containsKey(myAgentName)){
				agInfo = new AgentInfo(myPosition, ((CustomAgent) (this.myAgent)).type, myAgentName);
				agents.put(myAgentName, agInfo);
			} else {
				agInfo = agents.get(myAgentName);
				agInfo.position = myPosition;
				agInfo.update();
			}
			HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;

			// if (agInfo.isStuck()) {
			// myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.explore, agInfo.path.get(agInfo.path.size() - 1)));
			// this.stop();
			// }

			//List of observable from the agent's current position
			List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();//myPosition

			System.out.println(myAgentName + " -- list of observables: " + lobs);

			//list of attribute associated to the currentPosition
			// List<Attribute> lattribute= lobs.get(0).getRight();

			List<String> connected = new ArrayList<>(lobs.size() - 1);
			//create nodes and add them to the map
			lobs.forEach( a -> {
				connected.add(a.getLeft());
				if(!map.containsKey(a.getLeft())) {
					map.put(a.getLeft(),new NodeInfo(a.getLeft(), lobs.get(0).getLeft()) );
				}
				else if(map.get(a.getLeft()).lastUpdate == 0 && !map.get(a.getLeft()).connectedNodes.contains(a.getLeft())) {
					map.get(a.getLeft()).connectedNodes.add(a.getLeft());
				}
			});
			connected.remove(0);

			map.put(lobs.get(0).getLeft(),new NodeInfo(lobs.get(0).getRight(), lobs.get(0).getLeft(), connected) );

			//System.out.println(map);


			agInfo.goal = GoalType.explore;

			String dest = "";

			if (!MapUtils.checkPath(agInfo.path, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName)) {
				try {
					//try to move to an unvisited location
					dest = MapUtils.getUnvisitedNode(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);
					agInfo.path = MapUtils.getPath(myPosition, dest, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);

					System.out.println(myAgentName + " path : " + agInfo.path);

				} 
				catch (NoUnvisitedNodeException e) {
					System.out.println(myAgentName + " : map exploration is over (" + map.size() + " nodes). Switching to another behaviour.");
					//this.myAgent.addBehaviour(new RandomWalkBehaviour((abstractAgent) this.myAgent));

					//TODO change behaviour here --> go back to transmit data
					// agents.get(myAgent.getLocalName()).goal = GoalType.shareInformation;

					move.stop();

					// test
					// myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.explore, agInfo.path.get(agInfo.path.size() - 1)));
					this.stop();
					return;
				}
				catch (PathBlockedException e) {

					// TODO wait a little before stuck behaviour ?

					System.out.println(myAgentName + " : path is blocked.");
					agInfo.stuckCounter = 3;
					agInfo.update();
					agents.put(myAgentName, agInfo);
					move.stop();
					myAgent.addBehaviour(new UnstuckBehaviour(myAgent, agInfo.goal, dest));
					this.stop();
					return;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (agInfo.path != null && agInfo.path.size() > 0) {
				move.destination = agInfo.path.get(agInfo.path.size() - 1);
			}

			//TODO try to secure treasure --> behaviour

			agents.put(myAgentName, agInfo);
		}
		block();
	}

	private void stop() {
		this.done = true;

	}

	@Override
	public boolean done() {
		return done;
	}

}
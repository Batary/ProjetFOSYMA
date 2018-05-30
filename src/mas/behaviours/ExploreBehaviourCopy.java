package mas.behaviours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import env.Attribute;
import env.Couple;
import jade.core.behaviours.TickerBehaviour;
import mas.agents.CustomAgent;
import mas.exceptions.NoUnvisitedNodeException;
import mas.exceptions.PathBlockedException;
import mas.utils.AgentInfo;
import mas.utils.GoalType;
import mas.utils.MapUtils;
import mas.utils.NodeInfo;

//wumpus : attribut stench (sur case)

public class ExploreBehaviourCopy extends TickerBehaviour{

	private static final long serialVersionUID = 9088209402507795289L;
	private String myAgentName;
	private AgentInfo agInfo;

	@Deprecated
	public ExploreBehaviourCopy (final mas.abstractAgent myagent) {
		super(myagent, ((CustomAgent)(myagent)).agentBlockingTime);
	}

	@Override
	public void onTick() {
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

			if (agInfo.isStuck()) {
				myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.explore, agInfo.path.get(agInfo.path.size() - 1)));
				this.stop();
			}

			//List of observable from the agent's current position
			List<Couple<String,List<Attribute>>> lobs=((mas.abstractAgent)this.myAgent).observe();//myPosition

			//System.out.println(myAgentName+" -- list of observables: "+lobs);

			//			//Little pause to allow you to follow what is going on
			//			try {
			//				System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
			//				System.in.read();
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//			}


			//list of attribute associated to the currentPosition
			List<Attribute> lattribute= lobs.get(0).getRight();

			HashMap<String, NodeInfo> map = ((CustomAgent) (this.myAgent)).map;
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


			//example related to the use of the backpack for the treasure hunt
			Boolean b=false;

			for(Attribute a:lattribute){
				switch (a) {
				case TREASURE:

					//					System.out.println("My type is : "+((mas.abstractAgent)this.myAgent).getMyTreasureType());
					//					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					//					System.out.println("Value of the treasure on the current position: "+a.getValue());
					//					System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					//					System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());

					System.out.println(myAgentName + " : the value of treasure on the current position is : " + a.getValue());
					b=true;
					//Little pause to allow you to follow what is going on
					//					try {
					//						System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
					//						System.in.read();
					//					} catch (IOException e) {
					//						e.printStackTrace();
					//					}
					break;
				case DIAMONDS:
					//				System.out.println("My type is : "+((mas.abstractAgent)this.myAgent).getMyTreasureType());
					//				System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					//				System.out.println("Value of the diamonds on the current position: "+a.getValue());
					//				System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
					//				System.out.println("the remaining backpack capacity is: "+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());

					System.out.println(myAgentName + " : the value of diamonds on the current position is : " + a.getValue());
					b=true;
					//Little pause to allow you to follow what is going on
					//				try {
					//					System.out.println("Press Enter in the console to allow the agent "+myAgentName +" to execute its next move");
					//					System.in.read();
					//				} catch (IOException e) {
					//					e.printStackTrace();
					//				}
				default:
					break;
				}
			}

			//If the agent picked (part of) the treasure
			if (((mas.abstractAgent) this.myAgent).getBackPackFreeSpace() > 0 && b) {
				int g = ((mas.abstractAgent) this.myAgent).pick();
				if (g > 0) {
					List<Couple<String,List<Attribute>>> lobs2=((mas.abstractAgent)this.myAgent).observe();//myPosition
					System.out.println("list of observables after picking "+lobs2);

					System.out.println("The agent grabbed :" + g);

					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
					System.out.println("The agent tries to transfer is load into the Silo (if reachable); succes ? : "
							+ ((mas.abstractAgent) this.myAgent).emptyMyBackPack("AgentTanker1"));
					System.out.println("My current backpack capacity is:"+ ((mas.abstractAgent)this.myAgent).getBackPackFreeSpace());
				}
			}

			agInfo.goal = GoalType.explore;

			String destination = "", dest = "";

			if (!MapUtils.checkPath(agInfo.path, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName)) {
				try {
					//try to move to an unvisited location
					dest = MapUtils.getUnvisitedNode(myPosition, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);
					agInfo.path = MapUtils.getPath(myPosition, dest, map, agents, ((CustomAgent) this.myAgent).agentBlockingTime, myAgentName);

					// destination = agInfo.path.get(0);

					System.out.println(myAgentName + " path : " + agInfo.path);

				} 
				catch (NoUnvisitedNodeException e) {
					System.out.println(myAgentName + " : map exploration is over (" + map.size() + " nodes). Switching to another behaviour.");

					// go back to transmit data
					agents.get(myAgent.getLocalName()).goal = GoalType.shareInformation;
					this.stop();
					return;
				}
				catch (PathBlockedException e) {
					System.out.println(myAgentName + " : path is blocked by " + e.agent);
					agInfo.stuckCounter = 3;
					agInfo.update();
					agents.put(myAgentName, agInfo);
					myAgent.addBehaviour(new UnstuckBehaviour(myAgent, GoalType.explore, dest));
					this.stop();
					return;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (agInfo.path.size() > 0) {
				destination = agInfo.path.get(0);
			}

			/*			
			//Random move from the current position
			Random r= new Random();

			List<String> l2 = new ArrayList<>();

			for (Couple<String, List<Attribute>> c : lobs) {
				if(!map.containsKey(c.getLeft())) {
					l2.add(c.getLeft());
				}
			}

			//move to oldest updated (or unvisited) node
			if(l2.size() == 0) {
				NodeInfo n = map.get(lobs.get(0).getLeft());
				for (Couple<String, List<Attribute>> c : lobs) {
					if (map.get(c.getLeft()).lastUpdate <= n.lastUpdate) {
			//						System.out.println(myAgentName + " : " + c.getLeft() + " : " + map.get(c.getLeft()).lastUpdate + " " + n.position + " : " + n.lastUpdate);
						n = map.get(c.getLeft());
			//						System.out.println(myAgentName + " : " + "n = " + n.position);
					}
				}
				l2.add(n.position);
			}
			//			System.out.println(myAgentName + " : " + "l2 = " + Arrays.toString(l2.toArray()));

			//test if not following another agent
			for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
			    String key = entry.getKey();
			    AgentInfo value = entry.getValue();
			    if(key.equals(myAgentName))continue;
			    if(value.position.equals(myPosition))
			    	System.out.println(myAgentName + " following " + key +  " : " + (value.lastUpdate - System.currentTimeMillis()) + " " + l2 + " " + value.position);

				if( value.lastUpdate > System.currentTimeMillis() - 2500 && (value.position.equals(myPosition) || l2.contains(value.position))) {
			//					l2.remove(value.position);
					l2.clear();
					for (Couple<String, List<Attribute>> c : lobs) {
						if(!value.position.equals(c)) {
							l2.add(c.getLeft());
						}
					}
					break;
				}
			}

			int moveId = 0;
			//1) get a couple <Node ID,list of percepts> from the list of observables
			if(l2.size() > 1) {
				moveId=r.nextInt(l2.size() - 1) + 1;
			}
			 */			

			//TODO try not to move toward another agent

			//TODO try to secure treasure --> behaviour

			//			System.out.println( myAgentName + " : " + lobs.get(0).getLeft() + " --> " + l2.get(moveId));

			//2) Move to the picked location. The move action (if any) MUST be the last action of your behaviour
			if (!destination.equals(agInfo.position)) {
				if(!((mas.abstractAgent)this.myAgent).moveTo(destination)) {
					//could not move to node
					System.out.println( myAgentName + " : could not make movement : " + lobs.get(0).getLeft() + " --> " + destination);
					agInfo.stuckCounter++;
				}
				else {
					agInfo.stuckCounter = 0;
					if (!agInfo.path.isEmpty()) {
						agInfo.path.remove(0);
						agInfo.position = destination;
						agInfo.update();
					}
				}
			} else {
				System.out.println(myAgentName + " : standing on node " + destination);
				if (!agInfo.path.isEmpty()) {
					agInfo.path.remove(0);
				}
			}

			agents.put(myAgentName, agInfo);

		}

	}

}
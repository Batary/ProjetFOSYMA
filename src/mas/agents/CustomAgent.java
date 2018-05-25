package mas.agents;

import java.util.HashMap;

import env.EntityType;
import mas.abstractAgent;
import mas.utils.AgentInfo;
import mas.utils.NodeInfo;

public abstract class CustomAgent extends abstractAgent {


	private static final long serialVersionUID = 5792560475078466977L;

	public int agentBlockingTime = 700;
	public EntityType type;

	public HashMap<String, NodeInfo> map;
	public HashMap<String, AgentInfo> agents;
	//public List<String> path;


	public CustomAgent(EntityType type) {
		super();
		this.type = type;
		map = new HashMap<>();
		agents = new HashMap<>();
		//path = new ArrayList<>();
	}

	public CustomAgent(EntityType type, int time) {
		this(type);
		this.agentBlockingTime = time;
	}

}

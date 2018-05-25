package mas.utils;

import java.io.Serializable;
import java.util.List;

import env.EntityType;

public class AgentInfo implements Serializable {

	private static final long serialVersionUID = 7846848645754140261L;

	public long lastUpdate = 0;
	public String position = null;
	public EntityType type = null;
	public String name;

	// TODO reference to the position of a tanker agent
	public String reference = null;

	// triggers priority and stuck mechanisms
	public int stuckCounter = 0;

	//priority increases as long as the agent is stuck
	public int priority = 0;

	//the path this agent planned to follow
	public List<String> path;

	public GoalType goal;

	public AgentInfo(String position, EntityType type, String name) {
		this.position = position;
		this.type = type;
		this.name = name;
		lastUpdate = System.currentTimeMillis();
		//hard coded priority in case 2 agents block each other in an easy context
		this.priority = (type == EntityType.AGENT_EXPLORER ? 0 : 2);
	}

	public void update() {
		this.lastUpdate = System.currentTimeMillis();
	}

	public boolean isStuck() {
		return stuckCounter > 2;
	}

}

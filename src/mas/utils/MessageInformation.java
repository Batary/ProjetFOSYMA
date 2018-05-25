package mas.utils;

import java.io.Serializable;
import java.util.HashMap;

public class MessageInformation implements Serializable {

	private static final long serialVersionUID = 2093441856406345505L;
	public HashMap<String, NodeInfo> map;
	public HashMap<String, AgentInfo> agents;
	
	public MessageInformation(HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents) {
		super();
		this.map = map;
		this.agents = agents;
	}

	
}

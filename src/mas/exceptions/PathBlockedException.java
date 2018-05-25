package mas.exceptions;

import mas.utils.AgentInfo;

public class PathBlockedException extends Exception {

	private static final long serialVersionUID = -8857439242906030414L;

	public AgentInfo agent;
	
	public PathBlockedException(AgentInfo a) {
		this.agent = a;
	}


}

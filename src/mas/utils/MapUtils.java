package mas.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import env.Attribute;
import mas.exceptions.NoUnvisitedNodeException;
import mas.exceptions.PathBlockedException;

public class MapUtils {

	private static Map<String, List<Integer>> getNodesToAvoid(String start, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time,
			String caller) {
		Map<String, List<Integer>> nodesToAvoid = new HashMap<>();

		// get nodes where other agents are going
		for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
			String key = entry.getKey();
			AgentInfo value = entry.getValue();
			if (!key.equals(caller)) {

				if (value.lastUpdate >= System.currentTimeMillis() - 10 * time
						&& ((agents.get(caller).priority < value.priority || caller.compareTo(key) > 0) /*static priority*/)) {
					int step = (int) ((System.currentTimeMillis() - value.lastUpdate) / time); // rounds since last update
					for (int i = step; i < value.path.size(); i++) {
						if (!nodesToAvoid.containsKey(value.path.get(i))) {
							nodesToAvoid.put(value.path.get(i), new ArrayList<Integer>());
						}
						// here we assume path of this agent will not be blocked and has the same time step as caller
						nodesToAvoid.get(value.path.get(i)).add(i - step);
						nodesToAvoid.get(value.path.get(i)).add(i - step + 1);
						// System.out.println(caller + " : avoid node " + value.path.get(i) + " on step " + nodesToAvoid.get(value.path.get(i)));
					}
				}

				// prevent path from returning current position of another agent trying to go to our location
				if (value.lastUpdate >= System.currentTimeMillis() - 2 * time) {
					if (value.stuckCounter > 0 || value.path.isEmpty() || value.path.get(0).equals(start)) {
						if (!nodesToAvoid.containsKey(value.position)) {
							nodesToAvoid.put(value.position, new ArrayList<Integer>());
						}
						nodesToAvoid.get(value.position).add(0);
						nodesToAvoid.get(value.position).add(1);
					}
				}

			}
		}

		return nodesToAvoid;
	}

	/** Gets a valid path to target, trying to include nearby agents.
	 * 
	 * @param start
	 *            the current node of the agent
	 * @param map
	 *            the current map of this agent
	 * @return path to target, and null if path is blocked
	 * @throws Exception
	 *             if start is not in map or path is completely blocked --> unstuck behaviour */
	public static List<String> getPath(String start, String dest, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time,
			String caller) throws Exception {

		// basic search : get nearest nodes, then get nodes connected to them, and so on
		// possible improvement : implement "BA*" (start from both start and destination nodes)
		// --> better complexity, more rubust to blocked path ?
		NodeInfo startNode = map.get(start), destNode = map.get(dest);
		if (startNode == null || destNode == null) {
			throw new Exception("String node " + start + " or " + dest + " is not in map.");
		}
		PathNode currentNode = new PathNode(startNode, null, 0, false, null);
		Map<String, PathNode> visitedNodes = new HashMap<>();
		Map<String, PathNode> nodesToVisit = new HashMap<>();
		List<String> nodesToVisitNames = new ArrayList<>();

		// defines a list of steps where this agent should avoid some nodes
		Map<String, List<Integer>> nodesToAvoid = getNodesToAvoid(start, map, agents, time, caller);
		List<String> blockedNodes = new ArrayList<>();

		// TODO save blocking agent's names to throw ?

		nodesToVisit.put(start, currentNode);
		nodesToVisitNames.add(start);

		System.out.println("MapUtils.getPath() dest : " + dest + " nodestovisit : " + nodesToVisit);

		while (!currentNode.position.equals(dest) && !nodesToVisit.isEmpty()) {
			visitedNodes.put(currentNode.position, currentNode);
			nodesToVisit.remove(currentNode.position);
			nodesToVisitNames.remove(currentNode.position);

			// browse neighbours
			boolean blocked = false;
			for (String n : currentNode.connectedNodes) {
				if (visitedNodes.get(n) == null && nodesToVisit.get(n) == null) {
					// add neighbours if they have not been met yet and they are not blocked
					if (!nodesToAvoid.containsKey(n) || !nodesToAvoid.get(n).contains(currentNode.distance + 1)) {
						nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
						// System.out.println(nodesToVisit.get(n).connectedNodes.toString());
						nodesToVisitNames.add(n);
					} else {
						if (!blockedNodes.contains(n)) {
							blockedNodes.add(n);

							visitedNodes.remove(currentNode.position);
							nodesToVisit.put(currentNode.position, currentNode);
							nodesToVisitNames.add(currentNode.position);
							blocked = true;

							// nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
							// // System.out.println(nodesToVisit.get(n).connectedNodes.toString());
							// nodesToVisitNames.add(n);
						}
					}
				}
			}
			if (blocked) {
				// try to get by, by waiting a little
				nodesToVisit.get(currentNode.position).distance += 1;
			}

			if (nodesToVisit.isEmpty()) {
				break;
			} else {
				currentNode = nodesToVisit.get(nodesToVisitNames.get(0));
			}
		}
		// System.out.println("end");
		// System.out.println(currentNode.position);

		// if path is blocked, get a free node from the nearest reachable node
		// if no path is found, send a PathNotFoundException(agentBlocking)
		if (!currentNode.position.equals(dest)) {

			System.out.println(caller + " start = " + start + ", dest = " + dest);
			System.out.println(caller + " : path blocked on nodes " + blockedNodes.toString());
			List<String> l = getUnusedNodePath(start, map, agents, time, caller);
			if (l == null) {
				throw new PathBlockedException(null);
			} else {
				return l;
			}
		} else {
			List<String> path = new ArrayList<>(currentNode.distance + 1);
			// System.out.println();
			while (currentNode.previousNode != null) {
				// System.out.print(currentNode.position + " <-- ");
				path.add(0, currentNode.position);
				currentNode = currentNode.previousNode;
			}
			// System.out.println();
			return path;
		}
	}

	/** @param path the path to check
	 * @return true if path is clean, false if there are potential issues */
	public static boolean checkPath(List<String> path, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time, String caller) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		// get nodes where other agents are going
		for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
			String key = entry.getKey();
			AgentInfo value = entry.getValue();
			if (!key.equals(caller)) {

				if (value.lastUpdate >= System.currentTimeMillis() - 3 * time) {
					int step = (int) ((System.currentTimeMillis() - value.lastUpdate) / time); // rounds since last update
					if (path.contains(value.position)) {
						return false;
					}
					for (int i = step; i < value.path.size(); i++) {
						if (path.contains(value.path.get(i))) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/** Get nearest unvisited node from start, trying to balance exploration between all the exploring agents */
	public static String getUnvisitedNode(String start, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time, String caller)
			throws Exception {
		NodeInfo startNode = map.get(start);
		if (startNode == null) {
			throw new Exception("String node " + start + " is not in map.");
		}
		PathNode currentNode = new PathNode(startNode, null, 0, false, null);
		PathNode destNode = null;
		Map<String, PathNode> visitedNodes = new HashMap<>();
		Map<String, PathNode> nodesToVisit = new HashMap<>();
		List<String> nodesToVisitNames = new ArrayList<>();
		List<String> unexploredNodes = new ArrayList<>();

		nodesToVisit.put(start, currentNode);
		nodesToVisitNames.add(start);
		// TODO skip if distance = 1 ?
		while (/*(destNode == null || destNode.distance > currentNode.distance) &&*/ !nodesToVisit.isEmpty()) {
			visitedNodes.put(currentNode.position, currentNode);
			nodesToVisit.remove(currentNode.position);
			nodesToVisitNames.remove(currentNode.position);
			// System.out.print(currentNode.position + " -> ");

			// System.out.println(currentNode.connectedNodes.toString());

			// add neighbours if they have not been met yet
			for (String n : currentNode.connectedNodes) {
				if (visitedNodes.get(n) == null && nodesToVisit.get(n) == null) {
					nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
					// System.out.println(nodesToVisit.get(n).connectedNodes.toString());
					nodesToVisitNames.add(n);
				}
			}
			if (nodesToVisit.isEmpty()) {
				break;
			} else {
				currentNode = nodesToVisit.get(nodesToVisitNames.get(0));
				if (currentNode.lastUpdate == 0 /*&& (destNode == null || currentNode.distance < destNode.distance)*/) {
					// destNode = currentNode;
					unexploredNodes.add(currentNode.position);
				}

			}
		}

		if (unexploredNodes.isEmpty()) {
			throw new NoUnvisitedNodeException();
		}

		// check for other exploring agents to assess which node has the best potential
		destNode = visitedNodes.get(unexploredNodes.get(0));

		if (unexploredNodes.size() > 1) {
			for (String node : unexploredNodes) {
				currentNode = visitedNodes.get(node);

				// add to distance of unvisited nodes the positive-only value of myDist(node) - agentDist(node)
				for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
					String key = entry.getKey();
					AgentInfo value = entry.getValue();
					if (!key.equals(caller) && value.goal == GoalType.explore) {

						// get current position of agent
						int step = (int) (System.currentTimeMillis() - value.lastUpdate) / time + currentNode.distance; // rounds since last update
						if (step < value.path.size() + currentNode.distance + 4) {
							if (step >= value.path.size()) {
								step = value.path.size() - 1;
							}
							int distance = currentNode.distance + 1;
							try {
								if (value.path.get(step).equals(node)) {
									distance = 0;
								} else {
									distance = MapUtils.getPath(value.path.get(step), node, map, agents, time, key).size();
								}
							} catch (Exception e) {
							}
							if (currentNode.distance >= distance) {
								currentNode.distance += currentNode.distance - distance + 1;
							}
						}
					}
				}
				if (destNode.distance > currentNode.distance) {
					destNode = currentNode;
				}
			}
		}
		return destNode.position;
	}

	/** This function is used to find a non-blocking, free spot (node arity = 1 or another (short) path exists between all connected nodes)
	 * It should be able to take into account that other agents may want to go to this node as well.
	 * 
	 * @param start
	 *            the current node of the agent
	 * @param map
	 *            the current map of this agent
	 * @return the nearest free node path, and null if no such node exists
	 * @throws Exception
	 *             if start is not in map */
	public static List<String> getFreeNodePath(String start, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time, String caller) {

		NodeInfo startNode = map.get(start);
		if (startNode == null) {
			System.err.println("getFreeNodePath : start should not be null !");
			return null;
		}
		PathNode currentNode = new PathNode(startNode, null, 0, false, null);
		Map<String, PathNode> visitedNodes = new HashMap<>();
		Map<String, PathNode> nodesToVisit = new HashMap<>();
		List<String> nodesToVisitNames = new ArrayList<>();

		// defines a list of steps where this agent should avoid some nodes
		Map<String, List<Integer>> nodesToAvoid = getNodesToAvoid(start, map, agents, time, caller);
		List<String> blockedNodes = new ArrayList<>();

		nodesToVisit.put(start, currentNode);
		nodesToVisitNames.add(start);

		while (!isFreeNode(currentNode.position, map, agents, time, caller) && !nodesToVisit.isEmpty()) {
			visitedNodes.put(currentNode.position, currentNode);
			nodesToVisit.remove(currentNode.position);
			nodesToVisitNames.remove(currentNode.position);
			// System.out.print(currentNode.position + " -> ");

			// System.out.println(currentNode.connectedNodes.toString());

			// browse neighbours
			for (String n : currentNode.connectedNodes) {
				if (visitedNodes.get(n) == null && nodesToVisit.get(n) == null) {
					// add neighbours if they have not been met yet and they are not blocked
					if (!nodesToAvoid.containsKey(n) || !nodesToAvoid.get(n).contains(currentNode.distance + 1)) {
						nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
						// System.out.println(nodesToVisit.get(n).connectedNodes.toString());
						nodesToVisitNames.add(n);
					} else {
						if (!blockedNodes.contains(n)) {
							blockedNodes.add(n);
						}
					}
				}
			}
			if (nodesToVisit.isEmpty()) {
				break;
			} else {
				currentNode = nodesToVisit.get(nodesToVisitNames.get(0));
			}
		}

		if (!isFreeNode(currentNode.position, map, agents, time, caller)) {

			System.out.println(caller + " (at node " + start + ") : no free node found !");
			return null;
		} else {
			List<String> path = new ArrayList<>(currentNode.distance + 1);
			// System.out.println();
			while (currentNode.previousNode != null) {
				// System.out.print(currentNode.position + " <-- ");
				path.add(0, currentNode.position);
				currentNode = currentNode.previousNode;
			}
			// System.out.println();
			return path;
		}
	}

	/** @return true if node does not block path */
	public static boolean isFreeNode(String node, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time, String caller) {

		if (isTreasure(node, map)) {
			return false;
		}

		// get nodes where other agents are going
		for (Map.Entry<String, AgentInfo> entry : agents.entrySet()) {
			String key = entry.getKey();
			AgentInfo value = entry.getValue();
			if (!key.equals(caller)) {
				// System.out.println(caller + " : path of " + key + " " +(int)(System.currentTimeMillis() - value.lastUpdate) / time
				// + " round(s) ago : " + value.path);

				if (value.lastUpdate >= System.currentTimeMillis() - 10 * time
						&& ((agents.get(caller).priority < value.priority || caller.compareTo(key) > 0) /*static priority*/)) {
					int step = (int) ((System.currentTimeMillis() - value.lastUpdate) / time); // rounds since last update
					for (int i = step; i < value.path.size(); i++) {

						if (value.path.contains(node)) {
							return false;
						}
					}
				}
			}
		}

		// check that path is not blocked between any neighbour of this node
		PathNode myNode = new PathNode(map.get(node), null, 0, false, null);
		for (int i = 0; i < myNode.connectedNodes.size() - 1; i++) {
			// If node 1 is connected to node 2 and node 2 is connected to node 3, and so on, all is connected so we are fine.
			String node1 = myNode.connectedNodes.get(i);
			String node2 = myNode.connectedNodes.get(i + 1);

			// if(visitedNodes.containsKey(node2)) {
			// continue;
			// }
			PathNode currentNode = new PathNode(map.get(node1), null, 0, false, null);
			Map<String, PathNode> visitedNodes = new HashMap<>();
			Map<String, PathNode> nodesToVisit = new HashMap<>();
			List<String> nodesToVisitNames = new ArrayList<>();
			nodesToVisit.put(node1, currentNode);
			nodesToVisitNames.add(node1);
			visitedNodes.put(node, myNode);

			while (!currentNode.position.equals(node2) && !nodesToVisit.isEmpty() && currentNode.distance < 5) {
				visitedNodes.put(currentNode.position, currentNode);
				nodesToVisit.remove(currentNode.position);
				nodesToVisitNames.remove(currentNode.position);

				// browse neighbours
				for (String n : currentNode.connectedNodes) {
					if (visitedNodes.get(n) == null && nodesToVisit.get(n) == null) {
						nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
						// System.out.println(nodesToVisit.get(n).connectedNodes.toString());
						nodesToVisitNames.add(n);
					}
				}
				if (nodesToVisit.isEmpty()) {
					break;
				} else {
					currentNode = nodesToVisit.get(nodesToVisitNames.get(0));
				}
			}
			if (!currentNode.position.equals(node2)) {
				return false;
			}
			while (currentNode.previousNode != null) {
				currentNode = currentNode.previousNode;
			}
		}
		return true;
	}

	/** Gets a path to a node where no one wants to go. */
	public static List<String> getUnusedNodePath(String start, HashMap<String, NodeInfo> map, HashMap<String, AgentInfo> agents, int time, String caller) {

		NodeInfo startNode = map.get(start);
		if (startNode == null) {
			System.err.println("getFreeNodePath : start should not be null !");
			return null;
		}
		PathNode currentNode = new PathNode(startNode, null, 0, false, null);
		Map<String, PathNode> visitedNodes = new HashMap<>();
		Map<String, PathNode> nodesToVisit = new HashMap<>();
		List<String> nodesToVisitNames = new ArrayList<>();

		// defines a list of steps where this agent should avoid some nodes
		Map<String, List<Integer>> nodesToAvoid = getNodesToAvoid(start, map, agents, time, caller);
		List<String> blockedNodes = new ArrayList<>();

		nodesToVisit.put(start, currentNode);
		nodesToVisitNames.add(start);

		// TODO select node with shortest path to destination if not null ?

		boolean found = false;
		while (!nodesToVisit.isEmpty() && !found) {
			visitedNodes.put(currentNode.position, currentNode);
			nodesToVisit.remove(currentNode.position);
			nodesToVisitNames.remove(currentNode.position);

			// browse neighbours
			for (String n : currentNode.connectedNodes) {
				if (visitedNodes.get(n) == null && nodesToVisit.get(n) == null) {
					// add neighbours if they have not been met yet and they are not blocked
					if (!nodesToAvoid.containsKey(n) || !nodesToAvoid.get(n).contains(currentNode.distance + 1)) {
						nodesToVisit.put(n, new PathNode(map.get(n), currentNode, currentNode.distance + 1, false, null));
						// System.out.println(nodesToVisit.get(n).connectedNodes.toString());
						nodesToVisitNames.add(n);
						// if (!nodesToAvoid.containsKey(n)) {
						// found free node
						currentNode = nodesToVisit.get(n);
						found = true;
						break;
						// }
					} else {
						if (!blockedNodes.contains(n)) {
							blockedNodes.add(n);
						}
					}
				}
			}
			if (nodesToVisit.isEmpty() || found) {
				break;
			} else {
				currentNode = nodesToVisit.get(nodesToVisitNames.get(0));
			}
		}

		if (!found /*nodesToAvoid.containsKey(currentNode.position)*/) {
			System.out.println(caller + " (at node " + start + ") : no unused node found ! Nodes " + blockedNodes.toString() + " blocked !");
			return null;
		} else {
			List<String> path = new ArrayList<>(currentNode.distance + 1);
			// System.out.println();
			while (currentNode.previousNode != null) {
				// System.out.print(currentNode.position + " <-- ");
				path.add(0, currentNode.position);
				currentNode = currentNode.previousNode;
			}
			// System.out.println();
			return path;
		}
	}

	public static boolean isTreasure(String node, HashMap<String, NodeInfo> map) {
		if (map.get(node).nodeContent != null) {
			for (Attribute a : map.get(node).nodeContent) {
				if (a == Attribute.TREASURE) {
					return true;
				}
			}
		}

		return false;
	}

	/** Try to find the wumpus possible nodes.
	 * 
	 * @param map
	 *            the current map of this agent
	 * @return the nodes where the wumpus could be, an empty list if unknown */
	public static List<NodeInfo> getWumpusNodes(HashMap<String, NodeInfo> map) {
		// TODO
		return null;
	}

}

class PathNode extends NodeInfo {
	private static final long serialVersionUID = 1L;
	public PathNode previousNode;
	public int distance;
	public boolean isBlocked;
	public List<String> blockingAgents;

	public PathNode(NodeInfo node, PathNode prev, int dist, boolean isBlocked, List<String> blockingAgents) {
		super(node.lastUpdate, node.position, node.nodeContent, node.connectedNodes);
		previousNode = prev;
		distance = dist;
		this.isBlocked = isBlocked;
		this.blockingAgents = blockingAgents;
	}
}

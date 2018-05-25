package mas.utils;

import java.util.List;

import env.Attribute;

import java.io.Serializable;
import java.util.ArrayList;

public class NodeInfo implements Serializable {

	private static final long serialVersionUID = -4814633158273687134L;
	
	//The last update of this node. If it is 0, node is not explored
	public long lastUpdate = 0;
	public String position = null;
	public List<Attribute> nodeContent = null;
	public List<String> connectedNodes = new ArrayList<>();
	
	/**Constructor when created from adjacent node*/
	public NodeInfo(String pos, String from){
		this.position = pos;
		this.connectedNodes.add(from);
	}
	
	/**Constructor when created from current node*/
	public NodeInfo(List<Attribute> newNodeContent, String pos, List<String> connectedNodes){
		this.position = pos;
		this.lastUpdate = System.currentTimeMillis();
		this.nodeContent = newNodeContent;
		this.connectedNodes = connectedNodes;
	}
	
	public NodeInfo(long lastUpdate, String position, List<Attribute> nodeContent, List<String> connectedNodes) {
		this.lastUpdate = lastUpdate;
		this.position = position;
		this.nodeContent = nodeContent;
		this.connectedNodes = connectedNodes;
	}

	@Deprecated
	public void setNew(List<Attribute> newNodeContent) {
		lastUpdate = System.currentTimeMillis();
		nodeContent = newNodeContent;
	}
	
}

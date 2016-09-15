/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

import java.util.ArrayDeque;
import java.util.HashSet;

/*
 * This class represents a node in the Groum
 */
public class GroumNode {

	private HashSet<GroumNode> nextNodes; // (This node, nextNode) is a directed edge
	
	private HashSet<GroumNode> prevNodes; // (prevNode, this node) is a directed edge
		
	private MethodNode methodNode;	// The method node that this Groum node represents
	
	private HashSet<String> data;	// The data linked to this Groum node
	
	/*
	 * Start of method implementation.
	 */
	public GroumNode(MethodNode methodNode, HashSet<String> data) {
		this.nextNodes 	= new HashSet<GroumNode>();
		this.prevNodes 	= new HashSet<GroumNode>();
		this.methodNode = methodNode;
		this.data 		= new HashSet<String>(data);
	}
	
	public HashSet<GroumNode> getNextNodes() {
		return new HashSet<GroumNode>(nextNodes);
	}
	
	public HashSet<GroumNode> getPrevNodes() {
		return new HashSet<GroumNode>(prevNodes);
	}
	
	public MethodNode getMethodNode() {
		return methodNode;
	}
	
	public HashSet<String> getData() {
		return new HashSet<String>(data);
	}
	
	public void addData(String newData) {
		data.add(newData);
	}
	
	// Returns true if two groumNodes have the same Method ID and data
	public boolean isIdenticalTo(GroumNode groumNode) {
		return (this.methodNode ==  groumNode.methodNode && this.data.equals(groumNode.data));
	}
	
	// Returns true if two groumNodes have data dependency
	public boolean hasDataDependencyWith(GroumNode groumNode) {
		HashSet<String> commonData = new HashSet<String>(this.data);
		commonData.retainAll(groumNode.data);
		return commonData.size() > 0;
	}
	
	// Add an edge connecting this Groum node to another node
	public void addEdgeTo(GroumNode groumNode) {
		this.nextNodes.add(groumNode);
		groumNode.prevNodes.add(this);
	}
	
	// Remove the edge connecting this Groum node to another node
	public void removeEdgeTo(GroumNode groumNode) {
		this.nextNodes.remove(groumNode);
		groumNode.prevNodes.remove(this);
	}

	// Add edges connecting other Groum nodes to this node
	public void addEdgesFrom(HashSet<GroumNode> groumNodes) {		
		this.prevNodes.addAll(groumNodes);
		for (GroumNode prevNode : groumNodes) {
			prevNode.nextNodes.add(this);
		}
	}
	
	// Remove edges connecting other Groum nodes to this node
	public void removeEdgesFrom(HashSet<GroumNode> groumNodes) {		
		this.prevNodes.removeAll(groumNodes);
		for (GroumNode prevNode : groumNodes) {
			prevNode.nextNodes.remove(this);
		}
	}
	
	// Add edges connecting this Groum node to other nodes
	public void addEdgesTo(HashSet<GroumNode> groumNodes) {		
		this.nextNodes.addAll(groumNodes);
		for (GroumNode nextNode : groumNodes) {
			nextNode.prevNodes.add(this);
		}
	}
	
	// Remove edges connecting this Groum node to other nodes
	public void removeEdgesTo(HashSet<GroumNode> groumNodes) {
		this.nextNodes.removeAll(groumNodes);
		for (GroumNode nextNode : groumNodes) {
			nextNode.prevNodes.remove(this);
		}
	}
	
	// Returns all the previous nodes (not necessarily immediate previous nodes)
	public HashSet<GroumNode> getAllPreviousNodes() {
		HashSet<GroumNode> groumNodes = new HashSet<GroumNode>();
		
		ArrayDeque<GroumNode> waitingNodes = new ArrayDeque<GroumNode>();
		waitingNodes.addAll(this.prevNodes);
		while (!waitingNodes.isEmpty()) {
			GroumNode groumNode = waitingNodes.poll();
			groumNodes.add(groumNode);
			for (GroumNode prevNode : groumNode.prevNodes) {
				if (!waitingNodes.contains(prevNode) && !groumNodes.contains(prevNode))
					waitingNodes.add(prevNode);
			}
		}
		
		return groumNodes;
	}
	
	// Returns all the next nodes (not necessarily immediate next nodes)
	public HashSet<GroumNode> getAllNextNodes() {
		HashSet<GroumNode> groumNodes = new HashSet<GroumNode>();
		
		ArrayDeque<GroumNode> waitingNodes = new ArrayDeque<GroumNode>();
		waitingNodes.addAll(this.nextNodes);
		while (!waitingNodes.isEmpty()) {
			GroumNode groumNode = waitingNodes.poll();
			groumNodes.add(groumNode);
			for (GroumNode nextNode : groumNode.nextNodes) {
				if (!waitingNodes.contains(nextNode) && !groumNodes.contains(nextNode))
					waitingNodes.add(nextNode);
			}
		}
		
		return groumNodes;
	}
	
}

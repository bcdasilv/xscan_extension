/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/*
 * Data structure for a group of methods
 */
public class MethodGroup {
	
	protected String header = ""; // The header of a method group
	
	protected HashSet<MethodNode> methodNodes = new HashSet<MethodNode>(); // The set of methods in this group
	
	/*
	 * Start of code implementation.
	 */
	
	public void setHeader(String header) {
		this.header = header;
	}
	
	public String getHeader() {
		return header;
	}
	
	public void add(MethodNode methodNode) {
		methodNodes.add(methodNode);
	}
	
	public void remove(MethodNode methodNode) {
		methodNodes.remove(methodNode);
	}
	
	public HashSet<MethodNode> getMethodNodes() {
		return new HashSet<MethodNode>(methodNodes);
	}
	
	/*
	 * Returns the method nodes with their names sorted.
	 */
	public ArrayList<MethodNode> getSortedMethodNodes() {
		ArrayList<MethodNode> sortedMethodNodes = new ArrayList<MethodNode>(methodNodes);
		Collections.sort(sortedMethodNodes, new Comparator<MethodNode>() {
			@Override
			public int compare(MethodNode methodNode1, MethodNode methodNode2) {
				return methodNode1.getMethodID().compareTo(methodNode2.getMethodID());
			}
		});
		return sortedMethodNodes;
	}
	
	public int size() {
		return methodNodes.size();
	}
	
	/*
	 * Returns true if one of the methods in the group contains the string.
	 */
	public boolean containsMethodString(String methodString) {
		for (MethodNode methodNode : methodNodes)
			if (methodNode.getMethodID().contains(methodString))
				return true;
		return false;
	}
	
	/*
	 * Returns the intersection between this method group and another method group
	 */
	public HashSet<MethodNode> intersectionWith(MethodGroup methodGroup) {
		HashSet<MethodNode> commonMethodNodes = new HashSet<MethodNode>(this.methodNodes);
		commonMethodNodes.retainAll(methodGroup.methodNodes);
		return commonMethodNodes;
	}
	
	/*
	 * Returns the fscore between this method group and another method group
	 */
	public double fscoreBy(MethodGroup methodGroup) {
		double coverage  = this.coverageBy(methodGroup);
		double precision = this.precisionBy(methodGroup);
		if (coverage == 0 && precision == 0)
			return 0;
		else
			return 2 * coverage * precision / (coverage + precision);
	}
	
	/*
	 * Returns the fscore based on a given coverage and precision
	 */
	public static double fscoreBy(double coverage, double precision) {;
		if (coverage == 0 && precision == 0)
			return 0;
		else
			return 2 * coverage * precision / (coverage + precision);
	}
	
	/*
	 * Returns the coverage between this method group and another method group
	 */
	public double coverageBy(MethodGroup methodGroup) {
		if (methodNodes.size() == 0)
			return 0;
		else
			return (double) this.intersectionWith(methodGroup).size() / this.methodNodes.size();
	}
	
	/*
	 * Returns the precision between this method group and another method group
	 */
	public double precisionBy(MethodGroup methodGroup) {
		if (methodGroup.methodNodes.size() == 0)
			return 0;
		else
			return (double) this.intersectionWith(methodGroup).size() / methodGroup.methodNodes.size();
	}
	
}

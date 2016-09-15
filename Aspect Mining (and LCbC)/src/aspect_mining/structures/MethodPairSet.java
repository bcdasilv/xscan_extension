/**
 * 
 * @author HUNG
 *
 */

package aspect_mining.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/*
 * This class implements a set of method pairs such that no duplicates exist.
 */
public class MethodPairSet {
	
	// An entry <x, <set of y's>> indicates method x is paired with a set of method y's.
	private HashMap<MethodNode, HashSet<MethodNode>> pairedMethods = new HashMap<MethodNode, HashSet<MethodNode>>();
	
	/*
	 * Returns true if this set already contains the method pair
	 */
	public boolean containsPair(MethodNode method1, MethodNode method2) {
		if (pairedMethods.containsKey(method1))
			if (pairedMethods.get(method1).contains(method2))
				return true;
		
		return false;
	}
	
	/*
	 * Add a method pair to this set (only if there won't be any duplicates)
	 */
	public void addPair(MethodNode method1, MethodNode method2) {
		if (method1 == null || method2 == null || this.containsPair(method1, method2))
			return;
	
		if (!pairedMethods.containsKey(method1))
			pairedMethods.put(method1, new HashSet<MethodNode>());
		HashSet<MethodNode> methodSet1 = pairedMethods.get(method1);
		methodSet1.add(method2);
		
		if (!pairedMethods.containsKey(method2))
			pairedMethods.put(method2, new HashSet<MethodNode>());
		HashSet<MethodNode> methodSet2 = pairedMethods.get(method2);
		methodSet2.add(method1);
	}
	
	/*
	 * Remove a method pair from this set (only if the pair exists)
	 */
	public void removePair(MethodNode method1, MethodNode method2) {
		if (method1 == null || method2 == null || !this.containsPair(method1, method2))
			return;
	
		HashSet<MethodNode> methodSet1 = pairedMethods.get(method1);
		methodSet1.remove(method2);
		if (methodSet1.isEmpty())
			pairedMethods.remove(method1);
		
		HashSet<MethodNode> methodSet2 = pairedMethods.get(method2);
		methodSet2.remove(method1);
		if (methodSet2.isEmpty())
			pairedMethods.remove(method2);
	}
	
	/*
	 * Add all method pairs to this set
	 */
	public void addAllPairs(ArrayList<MethodPair> methodPairs) {
		for (MethodPair methodPair : methodPairs) {
			this.addPair(methodPair.getMethod1(), methodPair.getMethod2());
		}
	}
	
	/*
	 * Add all pairs in the method group to this set
	 */
	public void addAllPairsInGroup(MethodGroup methodGroup) {
		ArrayList<MethodNode> methodList = new ArrayList<MethodNode>(methodGroup.getMethodNodes());
		for (int i = 0; i < methodList.size() - 1; i++)
		for (int j = i + 1; j < methodList.size(); j++) {
			this.addPair(methodList.get(i), methodList.get(j));
		}
	}
	
	/*
	 * Returns the list of method pairs
	 */
	public ArrayList<MethodPair> getMethodPairList() {
		ArrayList<MethodPair> methodPairs = new ArrayList<MethodPair>();
		for (MethodNode method1 : pairedMethods.keySet())
		for (MethodNode method2 : pairedMethods.get(method1))
			if (method1.hashCode() < method2.hashCode()) // Avoid the case where one pair is counted twice
				methodPairs.add(new MethodPair(method1, method2));
		
		return methodPairs;
	}
	
	/*
	 * Clear this set
	 */
	public void clear() {
		pairedMethods.clear();
	}
	
}

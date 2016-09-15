/**
 * 
 */
package user.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import aspect_mining.structures.MethodGroup;
import aspect_mining.structures.MethodNode;


/**
 * @author hoan
 *
 */
public class FinerGroup {
	//private HashSet<MethodNode> rawGroup;
	private HashMap<Long, HashSet<MethodNode>> groups = new HashMap<Long, HashSet<MethodNode>>();
	private HashMap<MethodGroup, HashSet<MethodNode>> singles = new HashMap<MethodGroup, HashSet<MethodNode>>();
	private ArrayList<MethodGroup> callees = new ArrayList<MethodGroup>();
	private ArrayList<HashSet<MethodNode>> sortedGroups = new ArrayList<HashSet<MethodNode>>();
	private ArrayList<Integer> weights = new ArrayList<Integer>();
	
	public FinerGroup(HashSet<MethodNode> rawGroup)
	{
		//this.rawGroup = rawGroup;
		SetDescendingBySize comparator = new SetDescendingBySize();
		for(MethodNode m : rawGroup)
		{
			HashSet<MethodNode> callees = m.getCalledMethodNodes();
			for(MethodNode callee : callees)
			{
				MethodGroup p = callee.getPeerGroup();
				HashSet<MethodNode> s = singles.get(p);
				if(s == null)
					s = new HashSet<MethodNode>();
				s.add(m);
				singles.put(p, s);
			}
			
		}
		for(MethodGroup p : singles.keySet())
		{
			if(singles.get(p).size() > 1)
			{
				int index = Collections.binarySearch(this.callees, p, comparator);
				if(index < 0)
					this.callees.add(-1-index, p);
				else
					this.callees.add(index, p);
			}
		}
		
		extend(rawGroup, 0, 0);
		
		NumberDescendingOrder comp = new NumberDescendingOrder();
		for(long ids : groups.keySet())
		{
			HashSet<MethodNode> g = groups.get(ids);
			int n = 0;
			for(int i = 0; i < callees.size(); i++)
				if((ids & (1l << i)) > 0)
					n++;
			int w = n * g.size();
			int index = Collections.binarySearch(weights, w, comp);
			if(index < 0)
			{
				weights.add(-1-index, w);
				sortedGroups.add(-1-index, g);
			}
			else
			{
				weights.add(index, w);
				sortedGroups.add(index, g);
			}
		}
	}
	private void extend(HashSet<MethodNode> methods, long calleeIds, int calleeId)
	{
		if(calleeId == callees.size())
			return;
		if(calleeId == 64)
			System.err.println("Too big");
		System.out.println(methods.size() + " " + calleeIds + " " + calleeId);
		HashSet<MethodNode> remain = new HashSet<MethodNode>(methods);
		methods.retainAll(singles.get(callees.get(calleeId)));
		remain.removeAll(methods);
		if(methods.size() > 1)
		{
			long ids = calleeIds | (1l << calleeId);
			groups.put(ids, new HashSet<MethodNode>(methods));
			extend(methods, ids, calleeId+1);
		}
		methods.addAll(remain);
		if(!remain.isEmpty())
		{
			remain = null;
			extend(methods, calleeIds, calleeId+1);
		}
		else
			remain = null;
	}
	
	public ArrayList<HashSet<MethodNode>> getSortedGroups() {
		return sortedGroups;
	}

	public ArrayList<Integer> getWeights() {
		return weights;
	}

	private class SetDescendingBySize implements Comparator<MethodGroup>
	{
		/**
		 * Sort in reverse natural order.
		 * Defines an alternate sort order for Pair.
		 * Compare two Pair Objects.
		 * Compares descending.
		 *
		 * @param p1 first String to compare
		 * @param p2 second String to compare
		 *
		 * @return +1 if p1<p2, 0 if p1==p2, -1 if p1>p2
		 */
		public final int compare(MethodGroup g1, MethodGroup g2)
		{
			int d = singles.get(g2).size() - singles.get(g1).size();
			//return d/Math.abs(d);
			return d;
		}
	}
	private class NumberDescendingOrder implements Comparator<Integer>
	{
		/**
		 * Sort in reverse natural order.
		 * Defines an alternate sort order for Pair.
		 * Compare two Pair Objects.
		 * Compares descending.
		 *
		 * @param p1 first String to compare
		 * @param p2 second String to compare
		 *
		 * @return +1 if p1<p2, 0 if p1==p2, -1 if p1>p2
		 */
		public final int compare(Integer i1, Integer i2)
		{
			int d = i2 - i1;
			//return d/Math.abs(d);
			return d;
		}
	}
}

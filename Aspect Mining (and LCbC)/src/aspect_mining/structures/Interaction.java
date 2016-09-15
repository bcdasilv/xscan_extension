/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import aspect_mining.main.AspectWizImpl;

/*
 * This class describes an interaction (either internal or external to a method).
 * For example, if method x calls method y, Before is the set of methods called before y, 
 * and After is the set of methods called after y, then 
 * 		p = (y, Before, After) is an internal interaction of x, 
 *   	q = (x, Before, After) is an external interaction of y.
 */
public class Interaction {
	
	public static final int UNDEFINED_SIMILARITY = -1; 
	
	private MethodNode methodNode;
	private GroumNode groumNode;
	private ArrayList<MethodGroup> beforeMethodGroups;
	private ArrayList<MethodGroup> afterMethodGroups;

	public Interaction(MethodNode methodNode, GroumNode groumNode) {
		this.methodNode 		= methodNode;
		this.groumNode			= groumNode;
		this.beforeMethodGroups = new ArrayList<MethodGroup>();
		this.afterMethodGroups 	= new ArrayList<MethodGroup>();
		
		for (GroumNode prevNode : groumNode.getPrevNodes())
			this.beforeMethodGroups.add(prevNode.getMethodNode().getPeerGroup());
		for (GroumNode nextNode : groumNode.getNextNodes())
			this.afterMethodGroups.add(nextNode.getMethodNode().getPeerGroup());
	}
	
	/*
	 * Returns true if the two interactions are similar
	 */
	public static boolean similarInteractions(Interaction interaction1, Interaction interaction2) {
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\t\t\tComparing interactions at " + interaction1.methodNode.getMethodID() + " and " + interaction2.methodNode.getMethodID());
		/* =============== END DEBUG =============== */
		
		if (interaction1.groumNode == interaction2.groumNode) {
			/* ============== BEGIN DEBUG ============== */
			if (AspectWizImpl.debugMode)
				System.out.println("\t\t\t\tSame Groum node");
			if (AspectWizImpl.debugMode)
				System.out.println("\t\t\t\t=> Similar interactions: true");
			/* =============== END DEBUG =============== */
			return true;
		}
		
		// Get the common method nodes in each interaction
		int commonBeforeMethodGroups = 0;
		ArrayList<MethodGroup> interaction2Before = new ArrayList<MethodGroup>(interaction2.beforeMethodGroups);
		for (MethodGroup methodGroup1 : interaction1.beforeMethodGroups)
		for (MethodGroup methodGroup2 : interaction2Before) {
			if (methodGroup1 == methodGroup2) {
				commonBeforeMethodGroups++;
				interaction2Before.remove(methodGroup2);
				break;
			}
		}
		
		int commonAfterMethodGroups = 0;
		ArrayList<MethodGroup> interaction2After = new ArrayList<MethodGroup>(interaction2.afterMethodGroups);
		for (MethodGroup methodGroup1 : interaction1.afterMethodGroups)
		for (MethodGroup methodGroup2 : interaction2After) {
			if (methodGroup1 == methodGroup2) {
				commonAfterMethodGroups++;
				interaction2After.remove(methodGroup2);
				break;
			}
		}	
			
		// TODO: Policy 5.1 - Compare the components of the interactions
		boolean similarMethod = (interaction1.methodNode.getPeerGroup() == interaction2.methodNode.getPeerGroup());
		
		double similarBefore;
		if (interaction1.beforeMethodGroups.size() == 0 || interaction2.beforeMethodGroups.size() == 0)
			similarBefore = 1;
		else {
			similarBefore = (interaction1.beforeMethodGroups.size() < interaction2.beforeMethodGroups.size()) ? (double) commonBeforeMethodGroups / interaction1.beforeMethodGroups.size() : (double) commonBeforeMethodGroups / interaction2.beforeMethodGroups.size();
		}
		
		double similarAfter;
		if (interaction1.afterMethodGroups.size() == 0 || interaction2.afterMethodGroups.size() == 0)
			similarAfter = 1;
		else {
			similarAfter = (interaction1.afterMethodGroups.size() < interaction2.afterMethodGroups.size()) ? (double) commonAfterMethodGroups / interaction1.afterMethodGroups.size() : (double) commonAfterMethodGroups / interaction2.afterMethodGroups.size();
		}
		
		// TODO: Policy 5.2 - Decide if two interactions are similar based on the components above
		boolean isSimilar = (similarMethod && similarBefore >= 0.5 && similarAfter >= 0.5);
		
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\t\t\tSimilar method: " + similarMethod + "  Similar before: " + similarBefore + "  Similar after: " + similarAfter);
		
		if (AspectWizImpl.debugMode)
			System.out.println("\t\t\t\t=> Similar interactions: " + isSimilar);
		/* =============== END DEBUG =============== */
		
		return isSimilar;
	}

	/*
	 * Compute the similarity of two sets of interactions.
	 * Algorithm: do bipartite mapping between two sets. Then, the similarity can be computed as follows.
	 * 	 Similarity = number of mapped elements / the minimum number of elements in each set
	 */
	public static double computeInteractionSetSimilarity(ArrayList<Interaction> interactions1, ArrayList<Interaction> interactions2) {	
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\t\tComparing 2 interaction sets of size " + interactions1.size() + " and " + interactions2.size());
		/* =============== END DEBUG =============== */
		
		/*
		 * Implement bipartite matching
		 */
				
		// Initialize nodes and edges
		HashMap<Interaction, HashSet<Interaction>> edgesSet1ToSet2 = new HashMap<Interaction, HashSet<Interaction>>();
		for (Interaction interaction1 : interactions1) {
			edgesSet1ToSet2.put(interaction1, new HashSet<Interaction>());
			for (Interaction interaction2 : interactions2)
				if (Interaction.similarInteractions(interaction1, interaction2))
					edgesSet1ToSet2.get(interaction1).add(interaction2);
		}
		
		// Initialize mapping
		HashMap<Interaction, Interaction> mapSet1ToSet2 = new HashMap<Interaction, Interaction>();
		HashMap<Interaction, Interaction> mapSet2ToSet1 = new HashMap<Interaction, Interaction>();
		for (Interaction interaction1 : interactions1)
			mapSet1ToSet2.put(interaction1, null);
		for (Interaction interaction2 : interactions2)
			mapSet2ToSet1.put(interaction2, null);
		
		// For each interaction in set 1, map it to one interaction in set 2
		for (Interaction interaction1 : interactions1) {
			ArrayDeque<Interaction> waitingList = new ArrayDeque<Interaction>();
			HashMap<Interaction, Interaction> traceMap = new HashMap<Interaction, Interaction>();
			waitingList.add(interaction1);
			Interaction matchedInteraction = null;
			
			// Traverse back and forth between the two sets
			while (!waitingList.isEmpty()) {
				Interaction interaction = waitingList.poll();
				for (Interaction interaction2 : edgesSet1ToSet2.get(interaction)) {		
					if (traceMap.get(interaction2) != null) // Implies interaction 2 has been visited
						continue;
					
					traceMap.put(interaction2, interaction);
					Interaction nextInteraction = mapSet2ToSet1.get(interaction2);
					if (nextInteraction == null) {
						matchedInteraction = interaction2;
						break;
					}
					else
						waitingList.add(nextInteraction);
				}
				if (matchedInteraction != null)
					break;
			}
			
			// Re-map the elements in the two sets
			while (matchedInteraction != null) {
				Interaction prevInteraction = traceMap.get(matchedInteraction);
				Interaction prevPrevInteraction = mapSet1ToSet2.get(prevInteraction);
				
				mapSet1ToSet2.put(prevInteraction, matchedInteraction);
				mapSet2ToSet1.put(matchedInteraction, prevInteraction);
				
				matchedInteraction = prevPrevInteraction;
			}				
		}
		
		// Count the number of mapped interactions
		int numMappedInteractions = 0;
		for (Interaction interaction1 : interactions1) {
			if (mapSet1ToSet2.get(interaction1) != null)
				numMappedInteractions++;
		}

		// TODO: Policy 4 - Compute the similarity of two interaction sets
		double similarity;
		if (interactions1.size() == 0 || interactions2.size() == 0)
			similarity = Interaction.UNDEFINED_SIMILARITY;
		else
			similarity = (interactions1.size() < interactions2.size()) ? (double) numMappedInteractions / interactions1.size() : (double) numMappedInteractions / interactions2.size(); 
		
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\t\tMapped interactions: " + numMappedInteractions + "\tSimilarity: " + similarity);
		/* =============== END DEBUG =============== */
	
		return similarity;
	}
		
}

/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

import java.util.ArrayList;

import aspect_mining.main.AspectWizImpl;

/*
 * Data structure for a pair of methods
 */
public class MethodPair {

	private MethodNode method1;	
	private MethodNode method2;
	private double similarity = 0; // The similarity between method 1 and 2
	
	public MethodPair(MethodNode method1, MethodNode method2) {
		this.method1 = method1;
		this.method2 = method2;
	}
	
	public MethodNode getMethod1() {
		return method1;
	}
	
	public MethodNode getMethod2() {
		return method2;
	}
	
	/*
	 * MethodPair.computeSimilarity should be called before this method.
	 */
	public double getSimilarity() {
		return similarity;
	}
	
	/*
	 * Returns true if the two methods are peers
	 */
	public boolean isPeers() {
		/* ============== BEGIN DEBUG ============== */
		String debugMethod1 = "!";
		String debugMethod2 = "!";
		if (method1.getMethodID().contains(debugMethod1) && method2.getMethodID().contains(debugMethod2) ||
			method1.getMethodID().contains(debugMethod2) && method2.getMethodID().contains(debugMethod1))
			AspectWizImpl.debugMode = true;
		
		if (AspectWizImpl.debugMode)
			System.out.println("\tComparing " + method1.getMethodID() + " and " + method2.getMethodID());
		/* =============== END DEBUG =============== */
		
		// TODO: Policy 2 - Decide if two methods are peers based on a threshold
		this.similarity = computeSimilarity();
		boolean isPeers = (this.similarity >= 0.5);
		
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\tOverall similarity: " + similarity + " => Peers? = " + isPeers);
		
		AspectWizImpl.debugMode = false;
		/* =============== END DEBUG =============== */
		
		return isPeers;
	}
	
	/*
	 * Compute the similarity (internal structures and external usages) of the two methods
	 */
	public double computeSimilarity() {
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tComputing internal similarity:");
		/* =============== END DEBUG =============== */
		
		double internalSimilarity = getInternalSimilarity();
		
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tInternal similarity: " + internalSimilarity);
		
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tComputing external similarity:");
		/* =============== END DEBUG =============== */
		
		double externalSimilarity = getExternalSimilarity();
				
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tExternal similarity: " + externalSimilarity);
		
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tComputing parent external similarity:");
		/* =============== END DEBUG =============== */
		
		double parentExternalSimilarity = getParentExternalSimilarity();
		
		/* ============== BEGIN DEBUG ============== */
		if (AspectWizImpl.debugMode)
			System.out.println("\t\tParent external similarity: " + parentExternalSimilarity);
		/* =============== END DEBUG =============== */
		
		// TODO: Policy 3.1 - Compute the similarity of two methods based on their internal and external similarities
		double similarity = Interaction.UNDEFINED_SIMILARITY;
		int computationType = 1; // Select type 1 or 2 depending on the project
		
		switch (computationType) {
			// Computation type 1: put weights on different kinds of interactions
			case 1: 
				double numerator = 0;
				double denominator = 0;
				if (internalSimilarity != Interaction.UNDEFINED_SIMILARITY) {
					numerator 	+= 0.3 * internalSimilarity; 
					denominator += 0.3;
				}
				if (externalSimilarity != Interaction.UNDEFINED_SIMILARITY) {
					numerator	+= 0.3 * externalSimilarity;
					denominator += 0.3;
				}
				if (parentExternalSimilarity != Interaction.UNDEFINED_SIMILARITY) {
					numerator	+= 0.4 * parentExternalSimilarity;
					denominator += 0.4;
				}
				
				if (denominator > 0)
					similarity = numerator / denominator;
				break;
			
			// Computation type 2: similarity = maximum similarity of all interactions
			case 2: 
				double maxSimilarity = -1;
				if (internalSimilarity != Interaction.UNDEFINED_SIMILARITY && internalSimilarity > maxSimilarity)
					maxSimilarity = internalSimilarity;
				if (externalSimilarity != Interaction.UNDEFINED_SIMILARITY && externalSimilarity > maxSimilarity)
					maxSimilarity = externalSimilarity;
				if (parentExternalSimilarity != Interaction.UNDEFINED_SIMILARITY && parentExternalSimilarity > maxSimilarity)
					maxSimilarity = parentExternalSimilarity;
				
				if (maxSimilarity != -1)
					similarity = maxSimilarity;
				break;
		}
		
		// TODO: Policy 3.2 - If there are no interactions to compare, then use clone similarity
		if (similarity == Interaction.UNDEFINED_SIMILARITY) {
			similarity = 0;
			if (method1.getSimpleName().equals(method2.getSimpleName())
				&& method1.getClonedMethodNodes().containsKey(method2))
				if (method1.getClonedMethodNodes().get(method2) >= 0.9) 
					similarity = 1;
		}
		
		return similarity;
	}
	
	/*
	 * Returns the internal structure similarity of the two methods
	 */
	private double getInternalSimilarity() {
		// Get the interactions in the Groum of each method
		ArrayList<Interaction> interactions1 = new ArrayList<Interaction>();
		for (GroumNode groumNode : method1.getGroumNodes())
			interactions1.add(new Interaction(groumNode.getMethodNode(), groumNode));
		
		ArrayList<Interaction> interactions2 = new ArrayList<Interaction>();
		for (GroumNode groumNode : method2.getGroumNodes())
			interactions2.add(new Interaction(groumNode.getMethodNode(), groumNode));
		
		// Compare two interaction sets
		return Interaction.computeInteractionSetSimilarity(interactions1, interactions2);
	}	
		
	/*
	 * Returns the external usage similarity of the two methods
	 */
	private double getExternalSimilarity() {		
		// Get the interactions at the call site of each method
		ArrayList<Interaction> interactions1 = new ArrayList<Interaction>();
		for (MethodNode callingMethodNode : method1.getCallingMethodNodes())
		for (GroumNode groumNode : callingMethodNode.getGroumNodes()) {
			if (groumNode.getMethodNode() == method1)
				interactions1.add(new Interaction(callingMethodNode, groumNode));
		}

		ArrayList<Interaction> interactions2 = new ArrayList<Interaction>();
		for (MethodNode callingMethodNode : method2.getCallingMethodNodes())
		for (GroumNode groumNode : callingMethodNode.getGroumNodes()) {
			if (groumNode.getMethodNode() == method2)
				interactions2.add(new Interaction(callingMethodNode, groumNode));
		}
		
		// Compare two interaction sets		
		return Interaction.computeInteractionSetSimilarity(interactions1, interactions2);
	}
	
	/*
	 * Returns the external usage similarity of the parents of the two methods
	 */
	private double getParentExternalSimilarity() {
		/* DO NOT USE THE CODE BELOW. It would create very large peer groups.
		// If one method overrides the other, then their parent external similarity is 1.
		if (method1.getOverriddenMethods().contains(method2) || method2.getOverriddenMethods().contains(method1))
			return 1;
		*/
		
		// Get the interactions at the call site of the parents of each method
		ArrayList<Interaction> interactions1 = new ArrayList<Interaction>();
		for (MethodNode parentMethod1 : method1.getOverriddenMethods()) {
			for (MethodNode callingMethodNode : parentMethod1.getCallingMethodNodes())
			for (GroumNode groumNode : callingMethodNode.getGroumNodes()) {
				if (groumNode.getMethodNode() == parentMethod1)
					interactions1.add(new Interaction(callingMethodNode, groumNode));
			}
		}

		ArrayList<Interaction> interactions2 = new ArrayList<Interaction>();
		for (MethodNode parentMethod2 : method2.getOverriddenMethods()) {
			for (MethodNode callingMethodNode : parentMethod2.getCallingMethodNodes())
			for (GroumNode groumNode : callingMethodNode.getGroumNodes()) {
				if (groumNode.getMethodNode() == parentMethod2)
					interactions2.add(new Interaction(callingMethodNode, groumNode));
			}
		}
		
		// Compare two interaction sets		
		return Interaction.computeInteractionSetSimilarity(interactions1, interactions2);
	}
	
}

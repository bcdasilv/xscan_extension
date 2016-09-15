/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import aspect_mining.structures.CBFAGroup;
import aspect_mining.structures.MethodGroup;
import aspect_mining.structures.MethodNode;

import user.util.FileAccess;

/*
 * CBFAImpl.findMethodGroup() implements the CBFA algorithm.
 */
public class CBFAImpl extends AbstractImpl {
	
	/*
	 * Find CBFA methods
	 */
	protected void findMethodGroups() {
		methodGroups = new ArrayList<MethodGroup>();
		HashMap<MethodNode, CBFAGroup> matchedCBFAGroup = new HashMap<MethodNode, CBFAGroup>();
		
		// Get the list of methods excluding get/set, constructors, etc.
		ArrayList<MethodNode> methodNodes = new ArrayList<MethodNode>();
		for (MethodNode methodNode : methodMap.values()) {
			if (!methodNode.getClassNode().isLibraryClass() && !methodNode.isConstructor() && !methodNode.isGetSetMethod() && !methodNode.isPseudoMethod() && !methodNode.isTestMethod() && !methodNode.isUtilityMethod(inputProject))
				methodNodes.add(methodNode);
		}
		
		// For each method, find a previous method that has the maximum similarity with it.
		for (int i = 0; i < methodNodes.size(); i++) {
			double maxSimilarity = 0.3;	// This is the threshold chosen by the CBFA paper.
			MethodNode maxSimilarityMethodNode = null;
			for (int j = 0; j < i; j++) {
				double similarity = computeSimilarity(methodNodes.get(i), methodNodes.get(j));
				if (similarity > maxSimilarity) {
					maxSimilarity = similarity;
					maxSimilarityMethodNode = methodNodes.get(j);
				}
			}
			// Add methodNodes[i] to an existing CBFAGroup or a new CBFAGroup.
			CBFAGroup cbfaGroup;
			if (maxSimilarityMethodNode != null)
				cbfaGroup = matchedCBFAGroup.get(maxSimilarityMethodNode);
			else {
				cbfaGroup = new CBFAGroup();
				methodGroups.add(cbfaGroup);
			}
			cbfaGroup.add(methodNodes.get(i));
			matchedCBFAGroup.put(methodNodes.get(i), cbfaGroup);
		} 
	}

	/*
	 * Compute the similarity between two methods using Jaccard Coefficient.
	 */
	private double computeSimilarity(MethodNode methodNode1, MethodNode methodNode2) {
		HashSet<String> tokens1 = convertMethodNameToTokens(methodNode1.getSimpleName());
		HashSet<String> tokens2 = convertMethodNameToTokens(methodNode2.getSimpleName());
		
		HashSet<String> intersection = new HashSet<String>(tokens1);
		intersection.retainAll(tokens2);
		
		if (tokens1.size() + tokens2.size()== 0)
			return 0;
		else
			return (double) intersection.size() / (tokens1.size() + tokens2.size() - intersection.size());
	}
	
	/*
	 * Convert a method name to tokens, e.g. rankCBFAGroups => rank, CBFA, Groups
	 */
	private HashSet<String> convertMethodNameToTokens(String methodName) {
		HashSet<String> tokens = new HashSet<String>();		
		
		int startIndex = 0;
		while (true) {
			if (startIndex >= methodName.length())
				break;
			int endIndex = startIndex + 1;
			if (endIndex < methodName.length() && Character.isUpperCase(methodName.charAt(endIndex))) {
				while (endIndex < methodName.length() && Character.isUpperCase(methodName.charAt(endIndex)))
					endIndex++;
				if (endIndex < methodName.length())
					endIndex--;
			} 
			else if (endIndex < methodName.length() && Character.isLowerCase(methodName.charAt(endIndex))) {
				while (endIndex < methodName.length() && Character.isLowerCase(methodName.charAt(endIndex)))
					endIndex++;
			}
			tokens.add(methodName.substring(startIndex, endIndex));
			startIndex = endIndex;
		}
		
		return tokens;
	}
	
	/*
	 * Rank CBFA groups
	 */
	protected void rankMethodGroups() {
		// Compute the rank of each peer group
		for (MethodGroup methodGroup : methodGroups) {
			((CBFAGroup)methodGroup).computeRank();		
		}
		
		// Sort the peer groups based on the ranks of the groups
		Collections.sort(methodGroups, new Comparator<MethodGroup>() {
			@Override
			public int compare(MethodGroup methodGroup1, MethodGroup methodGroup2) {
				if (((CBFAGroup)methodGroup1).getRank() > ((CBFAGroup)methodGroup2).getRank())
					return -1;
				else if (((CBFAGroup)methodGroup1).getRank() < ((CBFAGroup)methodGroup2).getRank())
					return 1;
				// If the ranks are equal, then compare their sizes.
				else if (methodGroup1.size() < methodGroup2.size())
					return -1;
				else if (methodGroup1.size() > methodGroup2.size())
					return 1;
				else
					return 0;
			}
		});
	}

	/*
	 * Output results
	 */
	protected void outputResults() {		
		// Print the list of CBFA groups
		printCBFAGroups("CBFA - CBFA Groups.txt");
		
		// Print mapping results between the Oracle method groups and CBFA groups
		printOracleCoverage("CBFA - Oracle Coverage.txt");
	}

	/*
	 * Print the list of CBFA groups
	 */
	private void printCBFAGroups(String fileName) {
	    StringBuilder results = new StringBuilder();
	    results.append("*** LIST OF CBFA GROUPS ***\r\n");
	    results.append("Number of CBFA groups: " + methodGroups.size() + "\r\n\r\n");
		for (MethodGroup methodGroup : methodGroups) {
			((CBFAGroup)methodGroup).makeHeader(methodGroups.indexOf(methodGroup));
			results.append(methodGroup.getHeader() + "\r\n");
			for (MethodNode methodNode : methodGroup.getSortedMethodNodes()) 
				results.append(methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
			results.append("\r\n");
		}		
		FileAccess.writeFileContent(outputFolder + "\\" + fileName, results.toString());
	}
	
}
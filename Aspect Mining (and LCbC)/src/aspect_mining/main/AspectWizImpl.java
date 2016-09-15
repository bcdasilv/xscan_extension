/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import aspect_mining.structures.MethodGroup;
import aspect_mining.structures.MethodNode;
import aspect_mining.structures.MethodPair;
import aspect_mining.structures.MethodPairSet;
import aspect_mining.structures.PeerGroup;

import cleman.Detector;

import user.util.FileAccess;
import user.util.FinerGroup;
import user.util.Util;

/*
 * AspectWizImpl.findMethodGroup() implements the AspectWiz algorithm.
 */
public class AspectWizImpl extends AbstractImpl {
	
	// Print some information if debug mode is true
	public static boolean debugMode = false;
	
	// Peer candidates from the hierarchy
	private ArrayList<MethodGroup> candidatesFromHierarchy;
	
	// Peer candidates from clones
	private MethodPairSet candidatesFromClones;
	
	// Peer candidates from interfaces
	private MethodPairSet candidatesFromInterfaces;
	
	/*
	 * Find peer methods
	 */
	protected void findMethodGroups() {
		// Initialize peer groups, each group contains only one method
		methodGroups = new ArrayList<MethodGroup>();
		for (String key : methodMap.keySet()) {
			MethodNode methodNode = methodMap.get(key);
			PeerGroup peerGroup = new PeerGroup();
			
			peerGroup.add(methodNode);
			methodNode.setPeerGroup(peerGroup);
			
			methodGroups.add(peerGroup);
		}
		
		// Get candidates from 3 sources: hierarchy, clones, and interfaces
		candidatesFromHierarchy 	= getCandidatesFromHierarchy();
		candidatesFromClones 		= getCandidatesFromClones();
		candidatesFromInterfaces 	= getCandidatesFromInterfaces();
		
		// Remove utility methods from candidates
		removeUtilityMethods(candidatesFromHierarchy);
		removeUtilityMethods(candidatesFromClones);
		removeUtilityMethods(candidatesFromInterfaces);
					
		// For the candidates from hierarchy and clones, add them to the list of candidate pairs
		MethodPairSet candidatePairSet = new MethodPairSet();
		for (MethodGroup methodGroup : candidatesFromHierarchy)
			candidatePairSet.addAllPairsInGroup(methodGroup);
		candidatePairSet.addAllPairs(candidatesFromClones.getMethodPairList());
		
		/* ============== BEGIN DEBUG ============== */
		// Write all pairs of peer methods to a file for debugging purposes
		StringBuilder peerPairs = new StringBuilder(); 
		/* =============== END DEBUG =============== */
		
		// TODO: [*] For the candidates obtained from interfaces, specify them as peers.
		// This option produces better results for several projects such as:
		//		+ jhotdraw60b1
		// For other projects, this code can be used instead:
		// candidatePairSet.addAllPairs(candidatesFromInterfaces.getMethodPairList());
		for (MethodPair candidatePair : candidatesFromInterfaces.getMethodPairList()) {
			PeerGroup peerGroup1 = candidatePair.getMethod1().getPeerGroup();
			PeerGroup peerGroup2 = candidatePair.getMethod2().getPeerGroup();
			if (peerGroup1 != peerGroup2) {
				/* ============== BEGIN DEBUG ============== */
				peerPairs.append("Candidates from interfaces\r\n");
				peerPairs.append(candidatePair.getMethod1().getMethodID() + "\r\n");
				peerPairs.append(candidatePair.getMethod2().getMethodID() + "\r\n\r\n");
				/* =============== END DEBUG =============== */				
				for (MethodNode methodNode : peerGroup2.getMethodNodes()) {
					peerGroup1.add(methodNode);
					methodNode.setPeerGroup(peerGroup1);
				} 
				methodGroups.remove(peerGroup2);
			}
		}
		
		// Iteratively find new peers
		ArrayList<MethodPair> reservedCandidates = new ArrayList<MethodPair>(1000);
		int iterCount = 0;
		while (true) {
			iterCount++;
			int numNewPeers = 0;
			int numNewCandidates = 0;
			System.out.println("\tIteration " + iterCount + ":");
			
			// Consider every candidate pair
			ArrayList<MethodPair> candidatePairList = candidatePairSet.getMethodPairList();
			System.out.println("\t\tNumber of candidates: " + candidatePairList.size());			
			for (MethodPair candidatePair : candidatePairList) {
				PeerGroup peerGroup1 = candidatePair.getMethod1().getPeerGroup();
				PeerGroup peerGroup2 = candidatePair.getMethod2().getPeerGroup();
				
				if (peerGroup1 == peerGroup2) // Move on if method 1 and 2 are already in a peer group
					continue;
				
				if (candidatePair.isPeers()) { // If the candidates are peers, then update peer groups
					numNewPeers++;
					
					/* ============== BEGIN DEBUG ============== */
					peerPairs.append("Similarity: " + candidatePair.getSimilarity() + "\r\n");
					peerPairs.append(candidatePair.getMethod1().getMethodID() + "\r\n");
					peerPairs.append(candidatePair.getMethod2().getMethodID() + "\r\n\r\n");
					/* =============== END DEBUG =============== */
					
					// Update the lowest similarity among the methods within the combined peer group.
					// The similarity of the candidate pair has been computed in the candidatePair.isPeers() statement above.
					double lowestSimilarity = candidatePair.getSimilarity();
					if (peerGroup1.getLowestSimilarity() < lowestSimilarity) 
						lowestSimilarity = peerGroup1.getLowestSimilarity();
					if (peerGroup2.getLowestSimilarity() < lowestSimilarity) 
						lowestSimilarity = peerGroup2.getLowestSimilarity();
					
					// Merge two groups together
					for (MethodNode methodNode : peerGroup2.getMethodNodes()) {
						peerGroup1.add(methodNode);
						methodNode.setPeerGroup(peerGroup1);
					}
					peerGroup1.setLowestSimilarity(lowestSimilarity);
					methodGroups.remove(peerGroup2);
					
					// New candidates may appear after we have found the peers, so try getting some more candidates
					ArrayList<MethodPair> newCandidates = getCandidatesFromPeerMethods(candidatePair.getMethod1(), candidatePair.getMethod2()).getMethodPairList();
					numNewCandidates += newCandidates.size();
					reservedCandidates.addAll(newCandidates);					
				}
				else { // Save the candidates for the next iteration (when they might become peers)
					reservedCandidates.add(candidatePair);
				}
			}
			System.out.println("\t\tNew candidates suggested: " + numNewCandidates);
			System.out.println("\t\t" + numNewPeers + " new peer(s) found.");			
			
			if (numNewPeers > 0) { // Use reservedCandidates as the new candidates for the next iteration
				candidatePairSet.clear();
				candidatePairSet.addAllPairs(reservedCandidates);
				reservedCandidates.clear();
			} 
			else break;
		}
		/* ============== BEGIN DEBUG ============== */
		// Write all pairs of peer methods to a file for debugging purposes
		new File(outputFolder).mkdir(); // Create the output folder
		FileAccess.writeFileContent(outputFolder + "\\Peer Pairs.txt", peerPairs.toString());
		/* =============== END DEBUG =============== */
		
		// Remove "test" methods, "get-set" methods, and "utility" methods.
		// Don't remove "get-set" methods for HealthWatcher project.
		// Also remove interface methods (to display better results).
		for (MethodGroup methodGroup : methodGroups) {
			for (MethodNode methodNode : methodGroup.getMethodNodes())
				if (methodNode.getClassNode().isLibraryClass() || methodNode.isTestMethod() || methodNode.isUtilityMethod(inputProject)
						|| (methodNode.isGetSetMethod() && !inputProject.contains("HealthWatcher"))
						|| methodNode.getClassNode().isInterface())
					methodGroup.remove(methodNode);
		}
		
		// Split big peer groups into smaller groups
		splitBigPeerGroups();
		
		// Remove peer groups that contain only 1 or 0 element
		for (MethodGroup methodGroup : new ArrayList<MethodGroup>(methodGroups)) {
			if (methodGroup.size() <= 1)
				methodGroups.remove(methodGroup);
		}
	}

	/*
	 * Returns all peer candidates based on hierarchical information
	 * (i.e. methods overriding the same interface) 
	 */
	private ArrayList<MethodGroup> getCandidatesFromHierarchy() {		
		ArrayList<MethodGroup> candidates = new ArrayList<MethodGroup>();

		// Traverse all methods to group them based on the hierarchy
		for (MethodNode methodNode : methodMap.values()) {
			// For each method m(), see if there are override methods in its subclasses.
			// If yes, they can be considered as peer candidates.
			// Note: To avoid duplication in the overall counting, m() itself must not be an override method.
			MethodGroup methodGroup = new MethodGroup();
			methodGroup.add(methodNode);
			
			// If methodNode is a constructor, then add all the constructors of its subclasses to the group
			if (methodNode.isConstructor()){
				if (methodNode.getParentConstructors().size() == 0)
					for (MethodNode childConstructor : methodNode.getChildrenConstructors())
						methodGroup.add(childConstructor);
			}
			else { // Otherwise, add all the overriding methods to the group (first, check m() itself is not an override method)
				if (methodNode.getOverriddenMethods().size() == 0) {
					for (MethodNode overridingMethod : methodNode.getOverridingMethods())
						methodGroup.add(overridingMethod);
				}
			}
			
			// Only count method groups that have more than one element
			if (methodGroup.size() > 1) {
				candidates.add(methodGroup);
			}
		}
		
		return candidates;
	}
	
	/*
	 * Returns all peer candidates based on clone detection
	 */
	@SuppressWarnings("unchecked")
	private MethodPairSet getCandidatesFromClones() {
		MethodPairSet candidates = new MethodPairSet();
		HashMap<String, HashMap<String, Double>> cloneMap;

		if (new File(clonesDataFile).exists()) {
			//cloneMap = new cleman.Detector().detect(inputProject, dataFolder);
			cloneMap = (HashMap<String, HashMap<String, Double>>) (new Detector()).loadObjectFromFile(clonesDataFile);
		} else {
			System.out.println("\tClones file does not exist: " + clonesDataFile);
			return candidates;
		}

		for(String methodKey1 : cloneMap.keySet())
		for(String methodKey2 : cloneMap.get(methodKey1).keySet()) {
			// Change the format of the methods
			String method1 = MethodNode.getMethodIDFromMethodKey(methodKey1);
			String method2 = MethodNode.getMethodIDFromMethodKey(methodKey2);
				
			MethodNode methodNode1 = methodMap.get(method1);
			MethodNode methodNode2 = methodMap.get(method2);				
			
			if (methodNode1 != null && methodNode2 != null) {
				candidates.addPair(methodNode1, methodNode2);
				double similarity = cloneMap.get(methodKey1).get(methodKey2);
				methodNode1.addClonedMethodNode(methodNode2, similarity);
				methodNode2.addClonedMethodNode(methodNode1, similarity);				
			}
			else {
				if (methodNode1 == null)
					System.out.println("\tClone method not recognized: " + method1);
				if (methodNode2 == null)
					System.out.println("\tClone method not recognized: " + method2);
			}
		}

		return candidates;
	}

	/*
	 * Returns all peer candidates based on similar method names within similar interface names
	 * @author: Anh Hoan
	 */
	private MethodPairSet getCandidatesFromInterfaces() {
		MethodPairSet candidates = new MethodPairSet();

		HashMap<String, HashSet<MethodNode>> methodNames = new HashMap<String, HashSet<MethodNode>>();
		for(MethodNode mNode : methodMap.values())
		{		
			if (mNode.getClassNode().isLibraryClass())
				continue;
			if (mNode.isConstructor() || mNode.isPseudoMethod())
				continue;
			if (mNode.getClassNode().isInterface() 
					|| (inputProject.contains("HealthWatcher") && mNode.hasEmptyBody())) {			
				String simpleName = mNode.getSimpleName();
				ArrayList<String> tokens = Util.serialize(simpleName);
				for(String token : tokens)
				{
					HashSet<MethodNode> methods = methodNames.get(token);
					if(methods == null)
						methods = new HashSet<MethodNode>();
					methods.add(mNode);
					methodNames.put(token, methods);
				}
			}
		}
		for(String token : methodNames.keySet())
		{
			ArrayList<MethodNode> methods = new ArrayList<MethodNode>(methodNames.get(token));
			for(int i = 0; i < methods.size()-1; i++)
				for(int j = i+1; j < methods.size(); j++)
				{
					MethodNode m1 = methods.get(i), m2 = methods.get(j);
					String className1 = m1.getClassNode().getSimpleName();
					String className2 = m2.getClassNode().getSimpleName();
					String methodName1 = m1.getSimpleName(), methodName2 = m2.getSimpleName();
					if(Util.isRelative(Util.serialize(className1), Util.serialize(className2))
							&& Util.isRelative(Util.serialize(methodName1), Util.serialize(methodName2)))
					{
						candidates.addPair(m1, m2);
					}
				}
		}
		
		return candidates;
	}
	
	/*
	 * Returns all peer candidates based on the two newly detected peer methods.
	 * TODO: WARNING - Indeterministic Output
	 *     This technique may lead to the instability of the output. For example, suppose
	 * 	 at some point during execution, we have 2 detected peer methods m1 and m2. Now, m3 is
	 * 	 going to be put in the same group because m3 are peer to both m1 and m2. The
	 *   algorithm only compares m3 with either m1 and m2. If m3 is compared with m1,
	 *   then after the successful detection of the peer pair (m3, m1), a new set of peer 
	 *   candidates will be created based on this pair. Meanwhile, in a different execution,
	 *   a different set of peer candidates may be created based on the pair (m3, m2).
	 *      Each execution may have different set of peer candidates, therefore producing
	 *   different results. This error is observed in ASPECT-SimpleNode+.eval in JEdit-4.3.1.
	 */
	private MethodPairSet getCandidatesFromPeerMethods(MethodNode method1, MethodNode method2) {
		MethodPairSet candidates = new MethodPairSet();
			
		for (MethodNode methodNode1 : method1.getClassNode().getMethodNodes())
		for (MethodNode methodNode2 : method2.getClassNode().getMethodNodes()) {
			if (methodNode1.getPeerGroup() != methodNode2.getPeerGroup() 
				&& !methodNode1.isConstructor() 
				&& !methodNode1.isPseudoMethod()
				&& methodNode1.getSimpleName().equals(methodNode2.getSimpleName())) {
					candidates.addPair(methodNode1, methodNode2);
				}
		}
			
		return candidates;
	}
	
	/*
	 * Remove utility methods from a method group
	 */
	private void removeUtilityMethods(ArrayList<MethodGroup> methodGroups) {
		for (MethodGroup methodGroup : new ArrayList<MethodGroup>(methodGroups)) {
			for (MethodNode methodNode : methodGroup.getMethodNodes())
				if (methodNode.isUtilityMethod(inputProject))
					methodGroup.remove(methodNode);
			if (methodGroup.size() < 2)
				methodGroups.remove(methodGroup);
		}
	}
	
	/*
	 * Remove utility methods from a method pair set
	 */
	private void removeUtilityMethods(MethodPairSet methodPairSet) {
		for (MethodPair methodPair : methodPairSet.getMethodPairList()) {
			if (methodPair.getMethod1().isUtilityMethod(inputProject) || methodPair.getMethod2().isUtilityMethod(inputProject))
				methodPairSet.removePair(methodPair.getMethod1(), methodPair.getMethod2());
		}
	}
	
	/*
	 * Split big peer groups into smaller groups
	 */
	private void splitBigPeerGroups() {
		for (MethodGroup peerGroup : new ArrayList<MethodGroup>(methodGroups)) {
			// TODO: Policy 6 - If the peer group is bigger than a certain threshold, then split them.
			// Currently the threshold is set to 1000000, meaning this function is not used.
			if (peerGroup.size() > 1000000) {
				// Get the list of smaller groups after splitting (written by anh Hoan)
				FinerGroup finerGroup = new FinerGroup(peerGroup.getMethodNodes());
				ArrayList<HashSet<MethodNode>> smallGroups = finerGroup.getSortedGroups();
				
				// Update the list of peer groups
				for (HashSet<MethodNode> smallGroup : smallGroups) {
					PeerGroup newPeerGroup = new PeerGroup();
					for (MethodNode methodNode : smallGroup) {
						newPeerGroup.add(methodNode);
						methodNode.setPeerGroup(newPeerGroup);
					}
					methodGroups.add(newPeerGroup);
				}
				methodGroups.remove(peerGroup);
			}
		}
	}

	/*
	 * Rank peer groups
	 */
	protected void rankMethodGroups() {
		// Compute the rank of each peer group
		for (MethodGroup peerGroup : methodGroups) {
			((PeerGroup)peerGroup).computeRank();		
		}
		
		// Sort the peer groups based on the ranks of the groups
		Collections.sort(methodGroups, new Comparator<MethodGroup>() {
			@Override
			public int compare(MethodGroup peerGroup1, MethodGroup peerGroup2) {
				if (((PeerGroup)peerGroup1).getRank() > ((PeerGroup)peerGroup2).getRank())
					return -1;
				else if (((PeerGroup)peerGroup1).getRank() < ((PeerGroup)peerGroup2).getRank())
					return 1;
				// If the ranks are equal, then compare their sizes.
				else if (peerGroup1.size() < peerGroup2.size())
					return -1;
				else if (peerGroup1.size() > peerGroup2.size())
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
		// Print general information
		super.outputResults();
		
		// Print the list of peer candidates
		printPeerCandidates();
		
		// Print the list of peer groups
		printPeerGroups("AspectWiz - Peer Groups.txt");
		
		// Print mapping results between the Oracle method groups and peer groups
		printOracleCoverage("AspectWiz - Oracle Coverage.txt");
		
		FileAccess.saveCopyAs(outputFolder + "\\AspectWiz - Oracle Coverage.txt", outputFolder + "\\Oracle Coverage - " + inputProject + ".txt");
		
		//Added by Bruno (brunocs@dcc.ufba.br)
		printAndCountingLCbC("LCbCcounting_"+super.defaultInputProject);
	}

	/*
	 * Print the list of peer candidates
	 */
	private void printPeerCandidates() {
		StringBuilder results;		
			
		// Print the candidates from hierarchy (override method groups)
	    results = new StringBuilder();
	    results.append("*** LIST OF OVERRIDE METHODS ***\r\n");
	    results.append("Number of override method groups: " + candidatesFromHierarchy.size() + "\r\n\r\n");
		for (MethodGroup methodGroup : candidatesFromHierarchy) {
			for (MethodNode methodNode : methodGroup.getMethodNodes()) {
				if (methodNode.getClassNode().isInterface())
					results.append("[I]");
				if (methodNode.getOverriddenMethods().isEmpty() && methodNode.getParentConstructors().isEmpty())
					results.append("[*]" + methodNode.getMethodID() + "\r\n");
				else
					results.append(methodNode.getMethodID() + "\r\n");
			}
			results.append("\r\n");
		}		
		FileAccess.writeFileContent(outputFolder + "\\Candidates from Hierarchy.txt", results.toString());
		
		// Print the candidates from clones
		results = new StringBuilder();
	    results.append("*** LIST OF CLONES ***\r\n");
	    results.append("Number of clone pairs: " + candidatesFromClones.getMethodPairList().size() + "\r\n\r\n");
	    for (MethodPair clonePair : candidatesFromClones.getMethodPairList()) {
			results.append(clonePair.getMethod1().getMethodID() + "\r\n" + clonePair.getMethod2().getMethodID() + "\r\n" + "Similarity: " + clonePair.getMethod1().getClonedMethodNodes().get(clonePair.getMethod2()) + "\r\n\r\n");
		}
		FileAccess.writeFileContent(outputFolder + "\\Candidates from Clones.txt", results.toString());
		
		// Print the candidates from interfaces
		results = new StringBuilder();
	    results.append("*** LIST OF CANDIDATES FROM INTERFACES ***\r\n");
	    results.append("Number of candidate pairs: " + candidatesFromInterfaces.getMethodPairList().size() + "\r\n\r\n");
	    for (MethodPair candidatePair : candidatesFromInterfaces.getMethodPairList()) {
			results.append(candidatePair.getMethod1().getMethodID() + "\r\n" + candidatePair.getMethod2().getMethodID() + "\r\n\r\n");
		}
		FileAccess.writeFileContent(outputFolder + "\\Candidates from Interfaces.txt", results.toString());
	}

	/*
	 * Print the list of peer groups
	 */
	private void printPeerGroups(String fileName) {
	    StringBuilder results = new StringBuilder();
	    results.append("*** LIST OF PEER GROUPS ***\r\n");
	    results.append("Number of peer groups: " + methodGroups.size() + "\r\n\r\n");
		for (MethodGroup peerGroup : methodGroups) {
			((PeerGroup)peerGroup).makeHeader(methodGroups.indexOf(peerGroup));
			results.append(peerGroup.getHeader() + "\r\n");
			for (MethodNode methodNode : peerGroup.getSortedMethodNodes()) 
				results.append(methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
			results.append("\r\n");
		}		
		FileAccess.writeFileContent(outputFolder + "\\" + fileName, results.toString());
	}
	
	/*
	 * Added by Bruno (brunocs@dcc.ufba.br)
	 * */
	private void printAndCountingLCbC(String fileName){
	    
	    HashMap<String, Integer> components = new HashMap<String, Integer>();
	    ArrayList<String> compsTemp = new ArrayList<String>();
		
	    for (MethodGroup peerGroup : methodGroups) {
			for (MethodNode methodNode : peerGroup.getSortedMethodNodes()){
				String filePath = methodNode.getMethodID();
				//eliminating the first char, which is an 'L'.
				//Then getting the string until the first ';'.
				filePath = filePath.substring(1, filePath.indexOf(';'));
				if(filePath.contains("$"))
					filePath = filePath.substring(0, filePath.indexOf("$"));
				else if (filePath.contains("~"))
					filePath = filePath.substring(0, filePath.indexOf("~"));
				filePath = filePath.replace('/', '.');
				if (!compsTemp.contains(filePath))
					compsTemp.add(filePath);
			}
			//Now updating the LCbC counting
			for (String comp : compsTemp) {
				int lcbc = 0;
				if(components.containsKey(comp))
            		lcbc = (Integer) components.get(comp);
				lcbc++;
				components.put(comp, lcbc);
			}
			//after the internal for, it's time to jump to another group
			compsTemp.clear();
		}
	    //Now output the components mapping and also include the other components that don't
	    //have any concern assigned
	    includeZeroLCbCFiles(components);
	    StringBuilder lcbcCounting = new StringBuilder();
	    StringBuilder lcbcCountingNoZeroValues = new StringBuilder();
	    lcbcCounting.append("Component, LCbC\n");
	    lcbcCountingNoZeroValues.append("Component, LCbC\n");
		Set<String> comps = components.keySet();
		for (String c : comps) {
			if (components.containsKey(c)){
				lcbcCounting.append(c+","+components.get(c)+"\r\n");
				//now feeding the stringbuffer with components having at least one concern
				if(components.get(c) > 0)
					lcbcCountingNoZeroValues.append(c+","+components.get(c)+"\r\n");
			}
		}
	    
		FileAccess.writeFileContent(outputFolder + "\\" + fileName+".csv", lcbcCounting.toString());
		FileAccess.writeFileContent(outputFolder + "\\" + fileName+"_noZeroLCbC.csv", lcbcCountingNoZeroValues.toString());
	}
	
	private void includeZeroLCbCFiles(HashMap<String, Integer> comps) {
		//Get the project 
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(super.defaultInputProject);
		try {
			if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
				IJavaProject javaProject = JavaCore.create(project);
				IPackageFragment[] packages = javaProject.getPackageFragments();
				for (IPackageFragment mypackage : packages) {
					// Package fragments include all packages in the
					// classpath
					// We will only look at the package from the source
					// folder
					// K_BINARY would include also included JARS, e.g.
					// rt.jar
					if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
						//System.out.println("Package " + mypackage.getElementName());
						for (ICompilationUnit unit : mypackage.getCompilationUnits()){
							//System.out.println("Source file " + unit.getElementName());
							String unitName = unit.getElementName();
							String unitWithoutDotJava = unitName.substring(0,unitName.indexOf(".java"));
							if(!comps.containsKey(mypackage.getElementName()+'.'+unitWithoutDotJava))
								comps.put(mypackage.getElementName()+'.'+unitWithoutDotJava, 0);
						}
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}
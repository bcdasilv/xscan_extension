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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

import user.util.FileAccess;
import user.util.StringUtils;

import aspect_mining.structures.ClassNode;
import aspect_mining.structures.GroumNode;
import aspect_mining.structures.MethodGroup;
import aspect_mining.structures.MethodNode;
import aspect_mining.structures.OracleGroup;
import aspect_mining.structures.PeerGroup;

/*
 * This class serves as the superclass of any concrete implementations (peer detection, CBFA, etc.)
 */
public abstract class AbstractImpl {
	
	// Input and output settings
	
	private static String workspace = "C:\\Temp";
	
	//protected final String defaultInputProject = "JFreeChart_1.0.6";
	//protected final String defaultInputProject = "FreeMind_integration";
	//protected final String defaultInputProject = "jEdit_4.2";
	//protected final String defaultInputProject = "jEdit_4.3.1";
	//protected final String defaultInputProject = "findbugs";
	//protected final String defaultInputProject = "Rhino_1_6R5";
	//protected final String defaultInputProject = "Rhino";
	//protected final String defaultInputProject = "tomcat6";
	//protected final String defaultInputProject = "DiffJ";
	//protected final String defaultInputProject = "voldemort";
	//protected final String defaultInputProject = "Freecol_0.8.4";
	//protected final String defaultInputProject = "hsqldb";
	//protected final String defaultInputProject = "jbpm";
//	protected final String defaultInputProject = "HealthWatcherOO_09_Evolution";
//	protected final String defaultInputProject = "jedit-4.3.2";
	protected final String defaultInputProject = "ibatis";
	
	//protected final String defaultInputProject = "test-project";
	//protected final String defaultInputProject = "jhotdraw60b1";
	//protected final String defaultInputProject = "columba-1.4-src";
	//protected final String defaultInputProject = "apache-tomcat-6.0.26-src";
	//protected final String defaultInputProject = "jEdit-4.3.1";
	//protected final String defaultInputProject = "jfreechart-1.0.13";
	//protected final String defaultInputProject = "jarp-source-1.0.1";
	
	//protected final String defaultInputProject = "HealthWatcherAO_01_Base";
	//protected final String defaultInputProject = "HealthWatcherAO_02_Command";
	//protected final String defaultInputProject = "HealthWatcherAO_03_State";
	//protected final String defaultInputProject = "HealthWatcherAO_04_Observer";
	//protected final String defaultInputProject = "HealthWatcherAO_09_Evolution";
	//protected final String defaultInputProject = "HealthWatcherAO_10_ExceptionHandling";
	
	protected String inputProject;
	protected String oracleDataFile;
	protected String clonesDataFile;
	protected String outputFolder;
	
	public void setInputProject(String inputProject) {
		this.inputProject 	= inputProject;
		this.oracleDataFile = workspace + "\\Input\\ORACLE-" + inputProject + ".txt";
		this.clonesDataFile = workspace + "\\Input\\Clones-" + inputProject + ".dat";
		this.outputFolder 	= workspace + "\\Output\\Output for " + inputProject;
		new File(this.outputFolder).mkdirs();
	}
	
	// Measure the running time
	private long detectionTime = 0;	// The running time for peer/CBFA group detection
	private long runningTime = 0;	// The total running time (including parsing)
	
	// List of ICompilationUnit's
	protected ArrayList<ICompilationUnit> iCompilationUnits; 
	
	// List of classes and methods
	protected HashMap<String, ClassNode> classMap;
	protected HashMap<String, MethodNode> methodMap;
	
	// List of Oracle groups
	protected ArrayList<OracleGroup> oracleGroups;
	
	// List of peer/CBFA groups
	protected ArrayList<MethodGroup> methodGroups;
	
	// Mapping Oracle method groups to peer/CBFA groups
	protected HashMap<OracleGroup, MethodGroup> matchedMethodGroups;

	/*
	 * Start of implementation.
	 */
	private void run() {
		System.out.println("Started.");
		long startTime1 = new Date().getTime();
		
		// Parse the project
		System.out.println("1. Gathering project information...");
		initialize();
		
		// Find peer groups
		System.out.println("2. Running method groups detection...");
		long startTime2 = new Date().getTime();
		findMethodGroups();
		rankMethodGroups();
		detectionTime 	= new Date().getTime() - startTime2;
		runningTime 	= new Date().getTime() - startTime1;
		
		// Output results
		System.out.println("3. Outputting results...");
		compareToOracle();
		outputResults();
		
		long totalRunningTime = new Date().getTime() - startTime1;
		System.out.println("Done [" + totalRunningTime/1000 + " seconds].");		
	}
	
	/*
	 * Run one project
	 */
	public void runOneProject() {
		setInputProject(defaultInputProject);
		run();
	}
	
	/*
	 * Run all the projects available
	 */
	public void runAllProjects() {
		String[] inputProjects = new String[] {
			"jhotdraw60b1",
			"columba-1.4-src",
			"apache-tomcat-6.0.26-src",
			"jEdit-4.3.1",
			"jfreechart-1.0.13",
			"jarp-source-1.0.1",
			"HealthWatcherAO_01_Base",
			"HealthWatcherAO_02_Command",
			"HealthWatcherAO_03_State",
			"HealthWatcherAO_04_Observer",
			"HealthWatcherAO_09_Evolution",
			"HealthWatcherAO_10_ExceptionHandling"
		};
		for (String inputProject : inputProjects) {
			setInputProject(inputProject);
			run();
		}
	}
	
	/*
	 * Initialize variables
	 */
	protected void initialize() {
		getICompilationUnits();	// Initialize iCompilationUnits
		getClassesAndMethods();	// Initialize classMap and methodMap (raw information)
		parseJavaFiles();		// Initialize classMap and methodMap (detailed information)
		readOracle();			// Initialize oracleGroups
		
		new File(outputFolder).mkdir(); // Create the output folder
	}
	
	/*
	 * Get all ICompilationUnits in the project
	 */
	private void getICompilationUnits() {
		// Get all projects in the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		
		// Select one project to examine
		IJavaProject javaProject = null;
		for (IProject project : projects) {
			if (project.getName().equals(inputProject)) {
				javaProject = JavaCore.create(project);
				break;
			}
		}

		// Scan all java source files in the project
		iCompilationUnits = new ArrayList<ICompilationUnit>();
		if (javaProject == null) {
			System.out.println("\tERROR. Project name is incorrect: " + inputProject);
		} else
		try {
			IPackageFragment[] packages = javaProject.getPackageFragments();
			for (IPackageFragment mypackage : packages) {
				if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) { // Ignore binary packages
					for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
						if (unit.getElementName().contains(".java")) 
							iCompilationUnits.add(unit);
					}
				}
			}		
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Get all classes and methods in the project
	 */
	private void getClassesAndMethods() {		
		classMap  = new HashMap<String, ClassNode>(1000);
		methodMap = new HashMap<String, MethodNode>(10000);
		
		for (ICompilationUnit unit : iCompilationUnits) {
			System.out.println("\t[Stage 1] Parsing " + unit.getPath() + "...");
			
			// Create the AST tree
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true); // Support binding
			parser.setSource(unit);
			ASTNode astRoot = parser.createAST(null);
	    	
	    	// Traverse the AST tree to get information
	    	astRoot.accept(new FirstASTVisitor(this));
		}
	}
	
    /*
     * Create a new classID
     */
    ClassNode enlistClass(String classID, boolean isInterface) {
    	ClassNode classNode = new ClassNode(classID, isInterface);
    	classMap.put(classID, classNode);
    	return classNode;
    }
      
    /*
     * Create a new methodID and link it to the classNode
     */
    MethodNode enlistMethod(String methodID, ClassNode classNode) {
    	MethodNode methodNode = new MethodNode(methodID, classNode);
    	classNode.addMethodNode(methodNode);
    	methodMap.put(methodID, methodNode);
    	return methodNode;
    }
    
	/*
	 * Parse java source files to create the class hierarchy and Groums
	 */
	private void parseJavaFiles() {
		for (ICompilationUnit unit : iCompilationUnits) {
			System.out.println("\t[Stage 2] Parsing " + unit.getPath() + "...");
			
			// Create the AST tree
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true); // Support binding
			parser.setSource(unit);			
			ASTNode astRoot = parser.createAST(null);
	    	
	    	// Traverse the AST tree to get information
	    	astRoot.accept(new SecondASTVisitor(this));
  		}
	}
	
	/*
	 * Read the Oracle file.
	 */
	private void readOracle() {
		oracleGroups = new ArrayList<OracleGroup>();
		// Read Oracle data from input file
		ArrayList<OracleGroup> tempOracleGroups;
		if (new File(oracleDataFile).exists())
			tempOracleGroups = FileAccess.readOracleFile(oracleDataFile);
		else {
			System.out.println("\tOracle data file does not exist: " + oracleDataFile);
			return;
		}
		
		// Map Oracle method strings to method nodes in CallGraph.methodMap
		for (OracleGroup tempOracleGroup : tempOracleGroups) {
			OracleGroup oracleGroup = new OracleGroup();
			oracleGroup.setHeader(tempOracleGroup.getHeader());
			for (MethodNode tempOracleMethod : tempOracleGroup.getMethodNodes()) {
				MethodNode oracleMethod = methodMap.get(tempOracleMethod.getMethodID());
				if (oracleMethod == null)
		    		System.out.println("\tError in Aspect " + tempOracleGroup.getHeader() + ". Oracle method not recognized: " + tempOracleMethod.getMethodID());
		    	else
		    		oracleGroup.add(oracleMethod);
			}
			if (oracleGroup.size() < 2)
				System.out.println("\tWarning. Aspect " + oracleGroup.getHeader() + " contains " + oracleGroup.size() + " method(s). => DISCARDED.");
			else
				oracleGroups.add(oracleGroup);
		}
	}
	
	/*
	 * Find method groups using different algorithms: AspectWiz, CBFA, etc.
	 * The concrete implementation is written in the overriding methods. 
	 */
	protected abstract void findMethodGroups();
	
	/*
	 * Rank method groups.
	 * The concrete implementation is written in the overriding methods. 
	 */
	protected abstract void rankMethodGroups();
	
	/*
	 * Compare the detected method groups to the Oracle
	 */
	protected void compareToOracle() {
		matchedMethodGroups = new HashMap<OracleGroup, MethodGroup>();
		
		// Map Oracle groups to peer/CBFA groups
		for (OracleGroup oracleGroup : oracleGroups) {
			double maxFscore = 0;
			MethodGroup maxFscorePeerGroup = null;
			
			// Compute the fscore of the Oracle group with each method group, update the maxFscore
			for (MethodGroup methodGroup : methodGroups) {
				double fscore = oracleGroup.fscoreBy(methodGroup);
				if (fscore > maxFscore) {
					maxFscore = fscore;
					maxFscorePeerGroup = methodGroup;
				}
			}
			
			// Map the Oracle group to the method group with the maximum fscore
			if (maxFscore > 0)
				matchedMethodGroups.put(oracleGroup, maxFscorePeerGroup);
		}
	}
	
	/*
	 * Output results
	 */
	protected void outputResults() {
		StringBuilder results;
		
		/*
		 * Print some statistics
		 */
	    results = new StringBuilder();
	    results.append("*** STATISTICS ***\r\n");
	    results.append("Project:           " + inputProject + "\r\n");
		results.append("Number of files:   " + iCompilationUnits.size() + "\r\n");
		int numOfClasses = 0;
		for (ClassNode classNode : classMap.values())
			if (!classNode.isLibraryClass())
				numOfClasses++;
		int numOfMethods = 0;
		//int numOfMethodsWithBigFanIn = 0;
		for (MethodNode methodNode : methodMap.values())
			if (!methodNode.getClassNode().isLibraryClass() && !methodNode.isPseudoMethod()) {
				numOfMethods++;
				//if (methodNode.computeFanIn() >= 10)
				//	numOfMethodsWithBigFanIn++;
			}
		results.append("Number of classes: " + numOfClasses + "\r\n");
		results.append("Number of methods: " + numOfMethods + "\r\n");
		//results.append("Number of methods with big Fan-in values:\t" + numOfMethodsWithBigFanIn + "\r\n");
		int totalLOC = 0;
		for (ICompilationUnit unit : iCompilationUnits)
			if (unit.getElementName().contains(".java")) {
				try {
					totalLOC += StringUtils.getLOC(unit.getSource());
				} catch (JavaModelException e) {
				}
			}
		results.append("Lines of code:     " + totalLOC + "\r\n");
	    results.append("Running time:      " + runningTime / 1000 + " second(s)\r\n");
	    results.append("Detection time:    " + detectionTime / 1000 + " second(s)\r\n");
	    FileAccess.writeFileContent(outputFolder + "\\Statistics.txt", results.toString());		
				
		/*
		 * Print the list of classes
		 */
		ArrayList<String> classSortedList = new ArrayList<String>(classMap.keySet());
		Collections.sort(classSortedList);
	    results = new StringBuilder();
	    results.append("*** LIST OF CLASSES ***\r\n");
		for (String item : classSortedList) {
			results.append(item + "\r\n");
		}
		FileAccess.writeFileContent(outputFolder + "\\List of Classes.txt", results.toString());
		
		/*
		 * Print the list of methods
		 */
		ArrayList<String> methodSortedList 	= new ArrayList<String>(methodMap.keySet()); 
		Collections.sort(methodSortedList);
	    results = new StringBuilder();
	    results.append("*** LIST OF METHODS ***\r\n");
		for (String item : methodSortedList) {
			results.append(item + "\r\n");
			//results.append(item + "\tFan-in: " + methodMap.get(item).computeFanIn() + "\r\n");
		}
		FileAccess.writeFileContent(outputFolder + "\\List of Methods.txt", results.toString());
		
		/*
		 * Print the list of classes and methods together
		 */
		results = new StringBuilder();
		results.append("*** LIST OF CLASSES & METHODS ***\r\n");
		for (String classID : classSortedList) {
	    	ClassNode classNode = classMap.get(classID);
	    	results.append(classID + "\r\n"); // Print the class name
	    	
	    	// Print the methods declared by that class
	    	for (MethodNode methodNode : classNode.getMethodNodes()) {        	
	    		results.append("\t" + methodNode.getMethodID() + "\r\n");
	    	}
	    }
		FileAccess.writeFileContent(outputFolder + "\\List of Classes - Methods.txt", results.toString());
		
		/*
		 * Print the class hierarchy
		 */
	    results = new StringBuilder();
	    results.append("*** CLASS HIERARCHY ***\r\n");
	    for (String classID : classSortedList) {
	    	ClassNode classNode = classMap.get(classID);
	    	results.append(classID + "\r\n"); // Print the class name
	
	    	// Print the superclass of that class
	    	ClassNode superclass = classNode.getSuperclassNode();
	    	if (superclass != null)
	    		results.append("\textends:\t\t" + superclass.getClassID() + "\r\n");        		
	    	
	    	// Print the interfaces of that class
	    	for (ClassNode interfaceNode : classNode.getInterfaceNodes())
	    		results.append("\timplements:\t\t" + interfaceNode.getClassID() + "\r\n");        		
	
	       	// Print the subclasses of that class
	    	for (ClassNode subclass : classNode.getSubclassNodes())
	    		results.append("\textended by:\t" + subclass.getClassID() + "\r\n");
	    	
	    	// Print the classes that implement the classNode (in case classNode is an interface)
	    	for (ClassNode implementer : classNode.getImplementerNodes())
	    		results.append("\timplemented by:\t" + implementer.getClassID() + "\r\n");
	    }
		FileAccess.writeFileContent(outputFolder + "\\Class Hierarchy.txt", results.toString());
		
		/*
		 * Print the call graph
		 */
	    results = new StringBuilder();
	    results.append("*** CALL GRAPH ***\r\n");
		for (String methodID : methodSortedList) {
			MethodNode methodNode = methodMap.get(methodID);
			results.append(methodID + "\r\n");
			
			// Print the callees of the method
			results.append("\t+ Callees:\r\n");
			for (MethodNode calledMethodNode : methodNode.getCalledMethodNodes()) {
				results.append("\t\t" + calledMethodNode.getMethodID() + "\r\n");
			}
			
			// Print the callers of the method
			results.append("\t+ Callers:\r\n");
			for (MethodNode callingMethodNode : methodNode.getCallingMethodNodes()) {
				results.append("\t\t" + callingMethodNode.getMethodID() + "\r\n");
			}
		}
		FileAccess.writeFileContent(outputFolder + "\\Call Graph.txt", results.toString());
		
		/*
		 * Print the list of Groums
		 */
	    results = new StringBuilder();
	    results.append("*** LIST OF GROUMS ***\r\n");
		for (String methodID : methodSortedList) {
			MethodNode methodNode = methodMap.get(methodID);
			results.append(methodID + "\r\n");
			
			// Print the Groum
			for (GroumNode groumNode : methodNode.getGroumNodes()) {
				results.append("\tNode: " + groumNode.getMethodNode().getMethodID() + " [NodeID: " + groumNode.toString().substring(groumNode.toString().indexOf('@')) + "]\r\n");
				results.append("\t\tData:\t" + groumNode.getData().toString() + "\r\n");
				for (GroumNode nextNode : groumNode.getNextNodes())
					results.append("\t\tEdge to:\t" + nextNode.getMethodNode().getMethodID() + "[NodeID: " + nextNode.toString().substring(nextNode.toString().indexOf('@')) + "]\r\n");
			}
		}
		FileAccess.writeFileContent(outputFolder + "\\Groums.txt", results.toString());
	}
	
	/*
	 * Print mapping results between the Oracle groups and method groups
	 */
	protected void printOracleCoverage(String fileName) {
		StringBuilder results = new StringBuilder();
	    results.append("*** ORACLE COVERAGE ***\r\n");
	    
	    /* SUMMARY REPORT */
	    results.append("\r\nSUMMARY REPORT\r\n");
	    int numCoveredOracleGroups = 0;
	    int totalOracleMethods = 0, totalRecommendedMethods = 0, totalCorrectlyRecommendedMethods = 0;
	    double avgCoverage = 0, avgPrecision = 0, avgFscore = 0;
	    double maxCoverage = 0, maxPrecision = 0, maxFscore = 0;
	    double minCoverage = 0, minPrecision = 0, minFscore = 0;
		
	    for (MethodGroup oracleGroup : oracleGroups) {
			MethodGroup matchedMethodGroup = matchedMethodGroups.get(oracleGroup);
			if (matchedMethodGroup != null) {
				// TODO: Policy 7 - Decide if an Oracle group is covered based on a threshold
				if (oracleGroup.fscoreBy(matchedMethodGroup) >= 0.5)
					numCoveredOracleGroups++;
				
				double coverage  = oracleGroup.coverageBy(matchedMethodGroup);
				double precision = oracleGroup.precisionBy(matchedMethodGroup);
				
				totalOracleMethods 				 += oracleGroup.size();
				totalRecommendedMethods 		 += matchedMethodGroup.size();
				totalCorrectlyRecommendedMethods += oracleGroup.size() * coverage;
			
				if (coverage > maxCoverage)
					maxCoverage = coverage;
				if (precision > maxPrecision)
					maxPrecision = precision;
				
				if (coverage < minCoverage || minCoverage == 0)
					minCoverage = coverage;
				if (precision < minPrecision || minPrecision == 0)
					minPrecision = precision;
			}
		}
		if (oracleGroups.size() > 0) {
			avgCoverage 	= (totalOracleMethods == 0) ? 0 :(double) totalCorrectlyRecommendedMethods / totalOracleMethods;
			avgPrecision	= (totalRecommendedMethods == 0) ? 0 : (double) totalCorrectlyRecommendedMethods / totalRecommendedMethods;
			
			avgFscore = MethodGroup.fscoreBy(avgCoverage, avgPrecision);
			maxFscore = MethodGroup.fscoreBy(maxCoverage, maxPrecision);
			minFscore = MethodGroup.fscoreBy(minCoverage, minPrecision);
			
			results.append("Overall coverage:\t" + numCoveredOracleGroups + " / " + oracleGroups.size() + " = " + StringUtils.toPercentage((double)numCoveredOracleGroups / oracleGroups.size()) + "\r\n");
			results.append("Average coverage: " + StringUtils.padLeft(StringUtils.toPercentage(avgCoverage), 6) + "\t\tAverage precision: " + StringUtils.padLeft(StringUtils.toPercentage(avgPrecision), 6) + "\t\tAverage fscore: " + StringUtils.padLeft(StringUtils.toPercentage(avgFscore), 6) + "\r\n");
			results.append("Maximum coverage: " + StringUtils.padLeft(StringUtils.toPercentage(maxCoverage), 6) + "\t\tMaximum precision: " + StringUtils.padLeft(StringUtils.toPercentage(maxPrecision), 6) + "\t\tMaximum fscore: " + StringUtils.padLeft(StringUtils.toPercentage(maxFscore), 6) + "\r\n");
			results.append("Minimum coverage: " + StringUtils.padLeft(StringUtils.toPercentage(minCoverage), 6) + "\t\tMinimum precision: " + StringUtils.padLeft(StringUtils.toPercentage(minPrecision), 6) + "\t\tMinimum fscore: " + StringUtils.padLeft(StringUtils.toPercentage(minFscore), 6) + "\r\n");    		
		}
		
		// Print results with unsorted Oracle groups
		results.append("\r\n" + StringUtils.padRight("Oracle Group", 70) + StringUtils.padLeft("Method Group", 13) + StringUtils.padLeft("Coverage", 12) + StringUtils.padLeft("Precision", 12) + StringUtils.padLeft("Fscore", 12) + "\r\n");
		int numeratorAverageIndex = 0;
		int denominatorAverageIndex = 0;
		for (OracleGroup oracleGroup : oracleGroups) {
			String[] oracleGroupNameArray = oracleGroup.getHeader().split("\t");
			String oracleGroupName = "";
			try {
				oracleGroupName = StringUtils.padRight(oracleGroupNameArray[0], 10) + StringUtils.padRight(oracleGroupNameArray[1], 47) + StringUtils.padLeft(oracleGroupNameArray[2], 13);
			}
			catch (Exception e) { // In case of Index Out of Bound Error
			}
			
			MethodGroup matchedMethodGroup = matchedMethodGroups.get(oracleGroup);			
			if (matchedMethodGroup != null) {
				numeratorAverageIndex += methodGroups.indexOf(matchedMethodGroup);
				denominatorAverageIndex++;
				results.append(StringUtils.padRight(oracleGroupName, 70) + StringUtils.padLeft("#" + methodGroups.indexOf(matchedMethodGroup), 13) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.coverageBy(matchedMethodGroup)), 12) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.precisionBy(matchedMethodGroup)), 12) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.fscoreBy(matchedMethodGroup)), 12) + "\r\n");
			}
			else
				results.append(StringUtils.padRight(oracleGroupName, 70) + StringUtils.padLeft("---", 13) + StringUtils.padLeft("---", 12) + StringUtils.padLeft("---", 12) + StringUtils.padLeft("---", 12) + "\r\n");
		}
		int averageIndex = denominatorAverageIndex > 0 ? numeratorAverageIndex / denominatorAverageIndex : -1;
		results.append(StringUtils.padLeft("Average: #" + averageIndex, 83) + StringUtils.padLeft(StringUtils.toPercentage(avgCoverage), 12) + StringUtils.padLeft(StringUtils.toPercentage(avgPrecision), 12) + StringUtils.padLeft(StringUtils.toPercentage(avgFscore), 12) + "\r\n");
		
		// Sort the Oracle groups
		ArrayList<OracleGroup> unsortedOracleGroups = new ArrayList<OracleGroup>(oracleGroups); // Backup the unsorted version 
		Collections.sort(oracleGroups, new Comparator<MethodGroup>() {
			@Override
			public int compare(MethodGroup oracleGroup1, MethodGroup oracleGroup2) {
				/*
				// Sort by fscores (descending)
				double rank1 = matchedMethodGroups.containsKey(oracleGroup1) ? oracleGroup1.fscoreBy(matchedMethodGroups.get(oracleGroup1)) : 0;
				ouble rank2 = matchedMethodGroups.containsKey(oracleGroup2) ? oracleGroup2.fscoreBy(matchedMethodGroups.get(oracleGroup2)) : 0;
				int order = -1;
				*/
				// Sort by method group ranks (ascending)
				double rank1 = matchedMethodGroups.containsKey(oracleGroup1) ? methodGroups.indexOf(matchedMethodGroups.get(oracleGroup1)) : methodGroups.size();
				double rank2 = matchedMethodGroups.containsKey(oracleGroup2) ? methodGroups.indexOf(matchedMethodGroups.get(oracleGroup2)) : methodGroups.size();
				int order = 1;
				if (rank1 > rank2)
					return order;
				else if (rank1 < rank2)
					return -order;
				else
					return 0;
			}
		});
		
		// Print results with sorted Oracle groups
		int totalOracleMethodsTop10 = 0, totalRecommendedMethodsTop10 = 0, totalCorrectlyRecommendedMethodsTop10 = 0;
	    results.append("\r\n" + StringUtils.padRight("Oracle Group", 70) + StringUtils.padLeft("Method Group", 13) + StringUtils.padLeft("Coverage", 12) + StringUtils.padLeft("Precision", 12) + StringUtils.padLeft("Fscore", 12) + "\r\n");
		for (MethodGroup oracleGroup : oracleGroups) {
			String[] oracleGroupNameArray = oracleGroup.getHeader().split("\t");
			String oracleGroupName = "";
			try {
				oracleGroupName = StringUtils.padRight(oracleGroupNameArray[0], 10) + StringUtils.padRight(oracleGroupNameArray[1], 47) + StringUtils.padLeft(oracleGroupNameArray[2], 13);
			}
			catch (Exception e) { // In case of Index Out of Bound Error
			}
			
			MethodGroup matchedMethodGroup = matchedMethodGroups.get(oracleGroup);			
			if (matchedMethodGroup != null) {			
				if (oracleGroups.indexOf(oracleGroup) < 10) {					
					totalOracleMethodsTop10 			  += oracleGroup.size();
					totalRecommendedMethodsTop10 		  += matchedMethodGroup.size();
					totalCorrectlyRecommendedMethodsTop10 += oracleGroup.size() * oracleGroup.coverageBy(matchedMethodGroup);
				}
				
				results.append(StringUtils.padRight(oracleGroupName, 70) + StringUtils.padLeft("#" + methodGroups.indexOf(matchedMethodGroup), 13) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.coverageBy(matchedMethodGroup)), 12) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.precisionBy(matchedMethodGroup)), 12) + StringUtils.padLeft(StringUtils.toPercentage(oracleGroup.fscoreBy(matchedMethodGroup)), 12) + "\r\n");
			}
			else
				results.append(StringUtils.padRight(oracleGroupName, 70) + StringUtils.padLeft("---", 13) + StringUtils.padLeft("---", 12) + StringUtils.padLeft("---", 12) + StringUtils.padLeft("---", 12) + "\r\n");
		}
		double avgCoverageTop10  = (totalOracleMethodsTop10 == 0) ? 0 :(double) totalCorrectlyRecommendedMethodsTop10 / totalOracleMethodsTop10;
		double avgPrecisionTop10 = (totalRecommendedMethodsTop10 == 0) ? 0 : (double) totalCorrectlyRecommendedMethodsTop10 / totalRecommendedMethodsTop10;
		double avgFscoreTop10 	 = MethodGroup.fscoreBy(avgCoverageTop10, avgPrecisionTop10);
		results.append(StringUtils.padLeft("Top-10 Average:", 83) + StringUtils.padLeft(StringUtils.toPercentage(avgCoverageTop10), 12) + StringUtils.padLeft(StringUtils.toPercentage(avgPrecisionTop10), 12) + StringUtils.padLeft(StringUtils.toPercentage(avgFscoreTop10), 12) + "\r\n");
		
		/* DETAILED REPORT */
		oracleGroups = unsortedOracleGroups; // Revert to the unsorted version
	  	results.append("\r\nDETAILED REPORT\r\n");
		for (OracleGroup oracleGroup : oracleGroups) {
			String oracleGroupName = oracleGroup.getHeader();
			MethodGroup matchedMethodGroup = matchedMethodGroups.get(oracleGroup);			
			if (matchedMethodGroup != null) {
				results.append(oracleGroupName + ":\tcovered by method group #" + methodGroups.indexOf(matchedMethodGroup) + "\twith fscore " + StringUtils.toPercentage(oracleGroup.fscoreBy(matchedMethodGroup)) + "\tcoverage " + StringUtils.toPercentage(oracleGroup.coverageBy(matchedMethodGroup)) + "\tprecision " + StringUtils.toPercentage(oracleGroup.precisionBy(matchedMethodGroup)) + "\r\n");
				
				HashSet<MethodNode> correctMethods = oracleGroup.intersectionWith(matchedMethodGroup);
				results.append("\tCorrect methods: " + correctMethods.size() + "\r\n");
				HashSet<MethodNode> calleesOfCorrectMethods = new HashSet<MethodNode>();
				for (MethodNode methodNode : correctMethods) {
					results.append("\t\t" + methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
					calleesOfCorrectMethods.addAll(methodNode.getCalledMethodNodes());
				}
				
				HashSet<MethodNode> missedMethods = oracleGroup.getMethodNodes();
				missedMethods.removeAll(matchedMethodGroup.getMethodNodes());
				results.append("\tMissed methods: " + missedMethods.size() + "\r\n");
				for (MethodNode methodNode : missedMethods)
					results.append("\t\t" + methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
				
				HashSet<MethodNode> incorrectMethods = matchedMethodGroup.getMethodNodes();
				incorrectMethods.removeAll(oracleGroup.getMethodNodes());
				results.append("\tIncorrect methods: " + incorrectMethods.size() + "\r\n");
				HashSet<MethodNode> calleesOfIncorrectMethods = new HashSet<MethodNode>();
				for (MethodNode methodNode : incorrectMethods) {
					results.append("\t\t" + methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
					calleesOfIncorrectMethods.addAll(methodNode.getCalledMethodNodes());
				}
			} 
			else {
				results.append(oracleGroupName + ":\tnot covered\r\n");
				results.append("\tMissed methods: " + oracleGroup.getMethodNodes().size() + "\r\n");
				for (MethodNode methodNode : oracleGroup.getMethodNodes())
					results.append("\t\t" + methodNode.getMethodID() + "\tFan-in: " + methodNode.computeFanIn() + "\r\n");
			}
		}
		FileAccess.writeFileContent(outputFolder + "\\" + fileName, results.toString());
	}

	/*
	 * Merge Oracle groups for better results. 
	 * Also merge the corresponding matched method groups.
	 */
	protected void mergeOracleGroups() {
		String[] mergedOracleGroups = null;
		if (inputProject.equals("jhotdraw60b1")) {
			mergedOracleGroups = new String[] {
				"ASPECT-TOP5POPULAR-[Undo]",
				"ASPECT-TOP5POPULAR-[Observer]",
				"ASPECT-TOP5POPULAR-[Iterator]",
				"ASPECT-TOP5POPULAR-[Visitor]",
				"ASPECT-TOP5POPULAR-[Persistence]",
				"ASPECT-OLD-MouseHandler",
				"ASPECT-UPDATE-undo(+factory)",
				"ASPECT-OLD-ManageHandles",
				"ASPECT-OLD-ConsistentBehavior(view.checkDamage)",
				"ASPECT-NEW-CommandObserver",
				"ASPECT-NEW-FigureSelectionObserver",
				"ASPECT-NEW-DrawingChangeObserver",
				"ASPECT-NEW-ToolObserver",			    
				"ASPECT-NEW-Handle.invoke",
				"ASPECT-NEW-markDirty"
			};
		}
		else if (inputProject.contains("HealthWatcher")) {
			mergedOracleGroups = new String[] {
				"ASPECT-HWLocalSynchronization",
				"ASPECT-HW*ExceptionHandler",
				"ASPECT-HWTransactionManagement"
			};
		} else if (inputProject.equals("jfreechart-1.0.13")) {
			mergedOracleGroups = new String[] {
				"ASPECT-Renderer.drawItem"
			};
		};
		for (String mergedOracleGroup : mergedOracleGroups) {
			// Merge corresponding method groups
			boolean isFirstMethodGroup = true; // Used to recognize the first matched method group found
			MethodGroup firstMatchedMethodGroup = null;
			for (OracleGroup oracleGroup : oracleGroups) {
				if (oracleGroup.getName().contains(mergedOracleGroup) && matchedMethodGroups.containsKey(oracleGroup)) {
					if (isFirstMethodGroup) { // Remember the first matched method group
						isFirstMethodGroup = false;
						firstMatchedMethodGroup = matchedMethodGroups.get(oracleGroup);
					} else { // Merge with the first matched method group
						MethodGroup matchedMethodGroup = matchedMethodGroups.get(oracleGroup);
						if (matchedMethodGroup == firstMatchedMethodGroup)
							continue;
						for (MethodNode methodNode : matchedMethodGroup.getMethodNodes()) {
							firstMatchedMethodGroup.add(methodNode);
							if (firstMatchedMethodGroup instanceof PeerGroup)
								methodNode.setPeerGroup((PeerGroup)firstMatchedMethodGroup);
						}
						// Update methodGroups and matchedMethodGroups
						methodGroups.remove(matchedMethodGroup);
						for (OracleGroup myOracleGroup : oracleGroups) {
							if (matchedMethodGroups.get(myOracleGroup) == matchedMethodGroup)
								matchedMethodGroups.put(myOracleGroup, firstMatchedMethodGroup);
						}
					}
				}
			}
			
			// Merge Oracle groups
			boolean isFirstOracleGroup = true; // Used to recognize the first Oracle group found
			OracleGroup firstOracleGroup = null;
			for (OracleGroup oracleGroup : new ArrayList<OracleGroup>(oracleGroups)) {
				if (oracleGroup.getName().contains(mergedOracleGroup)) {
					if (isFirstOracleGroup) { // Remember the first Oracle group
						isFirstOracleGroup = false;
						firstOracleGroup = oracleGroup;
					} else { // Merge with the first Oracle group
						for (MethodNode methodNode : oracleGroup.getMethodNodes()) {
							firstOracleGroup.add(methodNode);
						} 
						oracleGroups.remove(oracleGroup);
						matchedMethodGroups.remove(oracleGroup);
					}
				}
			}
			if (firstOracleGroup != null)
				firstOracleGroup.makeHeader(oracleGroups.indexOf(firstOracleGroup), mergedOracleGroup);
		}
		// Refresh the indices of Oracle groups in their headers (they might not be consecutive after the merging)
		for (OracleGroup oracleGroup : oracleGroups)
			oracleGroup.makeHeader(oracleGroups.indexOf(oracleGroup), oracleGroup.getName());
	}
	
}

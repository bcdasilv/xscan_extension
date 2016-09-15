/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.main;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aspect_mining.structures.ClassNode;
import aspect_mining.structures.GroumNode;
import aspect_mining.structures.MethodNode;

/*
 * The function of this class is to create a Groum inside each method declaration.
 */
public class GroumCreator extends ASTVisitor {
	
	/*
	 * Static fields
	 */
	// The main program that calls FirstASTVisitor.
	private static AbstractImpl abstractImpl;
	
	// The current method to create the Groum for
	private static MethodNode currentMethodNode; 
	
	// Mapping of variables to their dynamic real types
	private static HashMap<String, String> variableBinding; 
	
	// The field below is used to create pseudo variables (for example,
	// in cases like A.m1(B.m2), we can consider x = B.m2 is a pseudo variable).
	// @see GroumCreator.createGroumForMethod
	private static int pseudoVariableCount;	
	
	/*
	 * Non-static fields
	 */
	private HashSet<GroumNode> pendingNodes; 	// The nodes waiting to be connected to the not-yet-visited nodes
	
	/*
	 * Groum building algorithm:
	 * A Groum can be seen as links between smaller Groums.
	 * The variable pendingNodes represents the nodes waiting to be connected to the lower Groums
	 *   when traversing the program statements.
	 * After the temporary Groum is built, we manipulate the edges (contract, add, remove) to create
	 *   the final Groum.
	 */
	public static void create(AbstractImpl abstractImpl, BodyDeclaration bodyDeclaration, MethodNode methodNode) {
		GroumCreator.abstractImpl 			= abstractImpl;
		GroumCreator.currentMethodNode 		= methodNode;
		GroumCreator.variableBinding 		= new HashMap<String, String>();
		GroumCreator.pseudoVariableCount 	= 0;
		HashSet<GroumNode> pendingNodes 	= new HashSet<GroumNode>();
		
		// Create a new instance of GroumCreator
		GroumCreator groumCreator = new GroumCreator(pendingNodes);
		
		// Visit the method to build the temporary Groum
		bodyDeclaration.accept(groumCreator);
		
		// Contract identical Groum nodes
		contractIdenticalGroumNodes();
		
		// Add new edges to the temporary Groum (based on data dependency)
		addEdgesWithDataDependency();
		
		// Remove edges that don't have data dependency
		removeEdgesWithoutDataDedendency();		
	}

	GroumCreator(HashSet<GroumNode> pendingNodes) {
		this.pendingNodes = new HashSet<GroumNode>(pendingNodes);
	}

	/*
	 * Create the Groum for an AST Node (when there's no branching).
	 * Then, update the pendingNodes.
	 */
	private void visit(ASTNode astNode) {
		if (astNode == null)
			return;
    	
		GroumCreator groumCreator = new GroumCreator(pendingNodes);
    	astNode.accept(groumCreator);
    	
	    // Update the pendingNodes
    	pendingNodes = groumCreator.pendingNodes;	
	}
	
	/*
	 * Create the Groum for an AST Node in a branch.
	 * Store new pending nodes in the branch to the newPendingNodes variable.
	 */
	private void visitBranch(ASTNode astNode, HashSet<GroumNode> newPendingNodes) {
		if (astNode == null)
			return;
    	
		GroumCreator groumCreator = new GroumCreator(pendingNodes);
    	astNode.accept(groumCreator);
    	
	    if (! pendingNodes.equals(groumCreator.pendingNodes)) // If there are new pendingNodes
	    	newPendingNodes.addAll(groumCreator.pendingNodes); // Save them for later reference	
	}
	
	/*
	 * Create a new Groum node with methodID and data
	 */
	private GroumNode createGroumNode(String methodID, HashSet<String> data) {    	
    	MethodNode methodNode = abstractImpl.methodMap.get(methodID);
    	
    	// If methodNode == null, then the method might be in a library, let's create a new method node for it.
    	if (methodNode == null) {        	
    		String className = ClassNode.getClassIDForLibraryClass();
        	ClassNode classNode = abstractImpl.classMap.get(className);
        	if (classNode == null) { 
        		// Create a special class to hold all methods that are in the libraries
        		// Set isInterface = true because we can't see the body of each method
        		classNode = abstractImpl.enlistClass(className, true); 
        	}        	
        	methodNode = abstractImpl.enlistMethod(methodID, classNode);
    	}

    	// Create a new Groum node
        GroumNode groumNode = new GroumNode(methodNode, data);
        		
		// Update the Groum, add edges from pendingNodes to groumNode
		groumNode.addEdgesFrom(pendingNodes);
		
		// Update pendingNodes
		pendingNodes.clear();
		pendingNodes.add(groumNode);
		
    	// Update call graph
    	currentMethodNode.addCalledMethodNode(methodNode);
    	methodNode.addCallingMethodNode(currentMethodNode);
    	
    	// Add the new Groum node to MethodNode.groumNodes
    	currentMethodNode.addGroumNode(groumNode);
    	
    	return groumNode;
	}	
	    
	/*
	 * Visit the parameters of a method.
	 * Then, create a Groum node for the method.
	 */
	private GroumNode createGroumForMethod(String methodID, ArrayList<Expression> expressions) {
		HashSet<GroumNode> newPendingNodes = new HashSet<GroumNode>();
		HashSet<String> data = new HashSet<String>();
    	
    	// Visit all Expressions
    	for (Expression expression : expressions) {
    		this.visitBranch(expression, newPendingNodes);
    		
    		String identifier = getIdentifierFromExpression(expression);
    		if (identifier != null)
    			data.add(identifier);
    	}
    	
    	/* Establish data dependency between the newPendingNodes and this current node.
    	 * For example: {A.m1(B.m2());} can be written as: {x = B.m2(); A.m1(x);}.
    	 * Therefore, A.m1 and B.m2 implicitly have a data dependency (the x variable). 
    	 * We call x a pseudo variable.
    	 */   
    	for (GroumNode newPendingNode : newPendingNodes) {
    		String pseudoVariable = "PseudoVariable#" + pseudoVariableCount++;
    		newPendingNode.addData(pseudoVariable);	// Add the pseudoVariable to the newPendingNode
    		data.add(pseudoVariable); // Add the pseudoVariable the the data of this current node.
    	}
    	
    	// Update pendingNodes if there are new pending nodes
    	if (! newPendingNodes.isEmpty())
    		pendingNodes = newPendingNodes;
    	
    	// Create a new Groum node with methodID and data
    	return this.createGroumNode(methodID, data);
	}
	
	/*
	 * Returns an identifier if the expression is a Name object; otherwise, returns null.
	 */
	private String getIdentifierFromExpression(Expression expression) {
		if (expression != null)
			if (expression instanceof Name) 
				return ((Name) expression).getFullyQualifiedName();
			else if (expression instanceof ThisExpression)
				return "this";

		return null;
	}
	
	public boolean visit(TypeDeclaration type) {
		return false; // Don't visit any class within the current method
	}
	
	public boolean visit(AnonymousClassDeclaration type) {
		return false; // Don't visit any anonymous class within the current method
	}
	
	/*
	 *	MethodInvocation:
     *		[ Expression . ]
     *    		[ < Type { , Type } > ]
     *    		Identifier ( [ Expression { , Expression } ] ) 
	 */
    @SuppressWarnings("unchecked")
	public boolean visit(MethodInvocation invocation) {
		// In some rare cases, we cannot resolve binding for the method, so just ignore it.
		if (invocation.resolveMethodBinding() == null) {
			System.out.println("\t\tCan't resolve binding for method: " + invocation.getName());
			return false;
		}
		
    	// Get binding information for the method, try to do real-type binding first    	
    	String methodID = null;
    	if (invocation.getExpression() instanceof Name) {
    		String variableName = ((Name)invocation.getExpression()).getFullyQualifiedName();
    		String variableType = variableBinding.get(variableName);
    		if (variableType != null) {
    			methodID = MethodNode.getMethodIDByRealTypeBinding(variableType, invocation.resolveMethodBinding());
    			if (! abstractImpl.methodMap.containsKey(methodID))
    				methodID = null;
    		}
    	}
    
    	// If we can't resolve real-type binding, then do normal binding
    	if (methodID == null)
    		methodID = MethodNode.getMethodIDByBinding(invocation.resolveMethodBinding());
    		
    	// Get the list of parameters to visit
    	ArrayList<Expression> expressions = new ArrayList<Expression>();
    	expressions.add(invocation.getExpression());
    	expressions.addAll(invocation.arguments());
    	
    	// Create the Groum corresponding to the method invocation and its parameters
    	GroumNode groumNode = this.createGroumForMethod(methodID, expressions);
    	
    	// If invocation.getExpression() is null, add "this" data to the Groum node just created
    	if (invocation.getExpression() == null)
    		if ((invocation.resolveMethodBinding().getModifiers() & Modifier.STATIC) != Modifier.STATIC)
    			groumNode.addData("this");
    	
    	return false;
    }
    
    /*
     *	SuperMethodInvocation:
     *		[ ClassName . ] super .
     *   		[ < Type { , Type } > ]
     *    		Identifier ( [ Expression { , Expression } ] )
  	 */
    @SuppressWarnings("unchecked")
	public boolean visit(SuperMethodInvocation invocation) {
		// In some rare cases, we cannot resolve binding for the method, so just ignore it.
		if (invocation.resolveMethodBinding() == null) {
			System.out.println("\t\tCan't resolve binding for method: " + invocation.getName());
			return false;
		}
		
    	// Get the list of parameters to visit
    	ArrayList<Expression> expressions = new ArrayList<Expression>();
    	expressions.addAll(invocation.arguments());
    	   	
    	// Create the Groum corresponding to the super method invocation and its parameters
    	String methodID = MethodNode.getMethodIDByBinding(invocation.resolveMethodBinding());
    	GroumNode groumNode = this.createGroumForMethod(methodID, expressions);
    	
    	// Add the "super" data to the Groum node just created
    	groumNode.addData("super");
    	
    	return false;
    }
	
    /*
     *	ClassInstanceCreation:
     *		[ Expression . ]
     *			new [ < Type { , Type } > ]
     *			Type ( [ Expression { , Expression } ] )
     *			[ AnonymousClassDeclaration ]
     */
    @SuppressWarnings("unchecked")
	public boolean visit(ClassInstanceCreation creation) {
    	// Get the list of parameters to visit
    	ArrayList<Expression> expressions = new ArrayList<Expression>();
    	expressions.add(creation.getExpression());
    	expressions.addAll(creation.arguments());
    	
    	// Create the Groum corresponding to the class instance creation and its parameters
    	String methodID = MethodNode.getMethodIDByBinding(creation.resolveConstructorBinding());
    	this.createGroumForMethod(methodID, expressions);

    	return false;
    }
    
    /*
     *	InstanceofExpression:
     *		Expression instanceof Type
     */
    public boolean visit(InstanceofExpression instanceofExpression) {
    	// Get the list of parameters to visit
    	ArrayList<Expression> expressions = new ArrayList<Expression>();
    	expressions.add(instanceofExpression.getLeftOperand());
    	
    	// Create the Groum corresponding to the instanceofExpression and its parameters
    	ITypeBinding typeBinding = instanceofExpression.getRightOperand().resolveBinding();
    	String methodID = MethodNode.getMethodIDForInstanceofExpression(typeBinding);  	
    	this.createGroumForMethod(methodID, expressions);
    	
    	return false;	
    }
    
    /*
     *	CastExpression:
     *		( Type ) Expression
     */
    public boolean visit(CastExpression castExpression) {
    	// Get the list of parameters to visit
    	ArrayList<Expression> expressions = new ArrayList<Expression>();
    	expressions.add(castExpression.getExpression());
    	
    	// Create the Groum corresponding to the castExpression and its parameters
    	ITypeBinding typeBinding = castExpression.getType().resolveBinding();
    	String methodID = MethodNode.getMethodIDForCastExpression(typeBinding);
    	this.createGroumForMethod(methodID, expressions);
    	
    	return false;
    }
    
    /*
     *	Assignment:
     *		Expression AssignmentOperator Expression
     */
    public boolean visit(Assignment assignment) {
    	HashSet<GroumNode> oldPendingNodes = new HashSet<GroumNode>(pendingNodes);
    	
    	this.visit(assignment.getRightHandSide());
    	
    	// If there is exactly one pending Groum node on the right hand side, then add data to the Groum node
    	if (pendingNodes.size() == 1 && !pendingNodes.equals(oldPendingNodes)) {    		
    		GroumNode newGroumNode = pendingNodes.iterator().next();
    		String data = getIdentifierFromExpression(assignment.getLeftHandSide());
    		if (data != null)
    			newGroumNode.addData(data);
    	}
    	
    	// Update the variableBinding mapping
    	if (assignment.getLeftHandSide() instanceof Name) {
    		String variableName = ((Name)assignment.getLeftHandSide()).getFullyQualifiedName();
    		String variableType = ClassNode.getClassIDByBinding(assignment.getRightHandSide().resolveTypeBinding());
    		variableBinding.put(variableName, variableType);
    	}    		
    	
    	return false;
    }
    
    /*
     *	VariableDeclarationFragment:
     *		Identifier { [] } [ = Expression ]
     */
    public boolean visit(VariableDeclarationFragment fragment) {
    	if (fragment.getInitializer() == null)
    		return false;
    	
		// In some rare cases, we cannot resolve binding for the initializer, so just ignore it.
		if (fragment.getInitializer().resolveTypeBinding() == null) {
			System.out.println("\t\tCan't resolve binding for expression: " + fragment.getInitializer().toString());
			return false;
		}
    	
    	HashSet<GroumNode> oldPendingNodes = new HashSet<GroumNode>(pendingNodes);
    	
    	this.visit(fragment.getInitializer());
    	
    	// If there is exactly one pending Groum node on the right hand side, then add data to the Groum node
    	if (pendingNodes.size() == 1 && !pendingNodes.equals(oldPendingNodes)) {
    		GroumNode newGroumNode = pendingNodes.iterator().next();	
        	String data = this.getIdentifierFromExpression(fragment.getName());
        	if (data != null)
        		newGroumNode.addData(data);
    	}
    	
    	// Update the variableBinding mapping
   		String variableName = fragment.getName().getFullyQualifiedName();
    	String variableType = ClassNode.getClassIDByBinding(fragment.getInitializer().resolveTypeBinding());
    	variableBinding.put(variableName, variableType);
    	
    	return false;
    }
    
	/*
	 *	IfStatement:
     *		if ( Expression ) Statement [ else Statement]
	 */
	public boolean visit(IfStatement ifStatement) {
    	// Visit Expression
		this.visit(ifStatement.getExpression());
    	
    	// Visit Then-Statement
		HashSet<GroumNode> thenNewPendingNodes = new HashSet<GroumNode>();
		this.visitBranch(ifStatement.getThenStatement(), thenNewPendingNodes);
		
		// Visit Else-Statement
		HashSet<GroumNode> elseNewPendingNodes = new HashSet<GroumNode>();
		this.visitBranch(ifStatement.getElseStatement(), elseNewPendingNodes);
		
		// Update pendingNodes
		if (!thenNewPendingNodes.isEmpty() && !elseNewPendingNodes.isEmpty())
			pendingNodes.clear();
		pendingNodes.addAll(thenNewPendingNodes);
    	pendingNodes.addAll(elseNewPendingNodes);
    	
    	return false;
    }
	
	/*
 	 *	SwitchStatement:
	 *		switch ( Expression ) 
 	 *			{ { SwitchCase | Statement } } }
	 *	SwitchCase:
	 *		case Expression  :
	 *		default :
	 */
	@SuppressWarnings("unchecked")
	public boolean visit(SwitchStatement switchStatement) {
    	// Visit first Expression
		this.visit(switchStatement.getExpression());
    	
		// Visit Switch cases
		Iterator<Statement> statements = switchStatement.statements().iterator();
		HashSet<GroumNode> newPendingNodes = new HashSet<GroumNode>();
		HashSet<GroumNode> branchPendingNodes = new HashSet<GroumNode>();
		boolean hasDefaultSwitchCase = false;
		for ( ; statements.hasNext(); ) {
			Statement statement = statements.next();
			switch (statement.getNodeType()) {
				case ASTNode.SWITCH_CASE:
					if (((SwitchCase)statement).getExpression() == null)
						hasDefaultSwitchCase = true;
					branchPendingNodes.addAll(pendingNodes);
					break;
				case ASTNode.EXPRESSION_STATEMENT: 
					GroumCreator groumCreator = new GroumCreator(branchPendingNodes);
			    	statement.accept(groumCreator);
			    	branchPendingNodes = groumCreator.pendingNodes;
					break;					
				case ASTNode.BREAK_STATEMENT:
					if (! pendingNodes.equals(branchPendingNodes)) // If there are new pendingNodes
					   	newPendingNodes.addAll(branchPendingNodes); // Save them for later reference	
					branchPendingNodes.clear();
					break;
				default: break;
			}
		}
		if (! pendingNodes.equals(branchPendingNodes)) // If there are new pendingNodes
		   	newPendingNodes.addAll(branchPendingNodes); // Save them for later reference
		
		// Update pendingNodes if there are new pending nodes
    	if (! newPendingNodes.isEmpty()) {
    		if (hasDefaultSwitchCase)
    			pendingNodes = newPendingNodes;
    		else
    			pendingNodes.addAll(newPendingNodes);
    	}
		
    	return false;
    }
	
	/*
	 *	ConditionalExpression:
	 *		Expression ? Expression : Expression
	 */
	public boolean visit(ConditionalExpression condExpression) {
    	// Visit first Expression
		this.visit(condExpression.getExpression());
    	
    	// Visit Then-Expression
		HashSet<GroumNode> thenNewPendingNodes = new HashSet<GroumNode>();
		this.visitBranch(condExpression.getThenExpression(), thenNewPendingNodes);
		
		// Visit Else-Expression
		HashSet<GroumNode> elseNewPendingNodes = new HashSet<GroumNode>();
		this.visitBranch(condExpression.getElseExpression(), elseNewPendingNodes);
		
		// Update pendingNodes
		if (!thenNewPendingNodes.isEmpty() && !elseNewPendingNodes.isEmpty())
			pendingNodes.clear();
		pendingNodes.addAll(thenNewPendingNodes);
    	pendingNodes.addAll(elseNewPendingNodes);
    	
    	return false;
    }
	
    /*
     *	InfixExpression:
     *		Expression InfixOperator Expression { InfixOperator Expression } 
     */
    @SuppressWarnings("unchecked")
	public boolean visit(InfixExpression infixExpression) {
    	HashSet<GroumNode> newPendingNodes = new HashSet<GroumNode>(3); 
    	
       	// Visit left operand
   		this.visitBranch(infixExpression.getLeftOperand(), newPendingNodes);
    	
	    // Visit right operand
   		this.visitBranch(infixExpression.getRightOperand(), newPendingNodes);
    	    	
	    // Visit extended operands    
    	ListIterator<Expression> operands = infixExpression.extendedOperands().listIterator();
    	for ( ; operands.hasNext(); ) { 
    		this.visitBranch(operands.next(), newPendingNodes);
    	}
    	
    	// Update pendingNodes if there are new pending nodes
    	if (! newPendingNodes.isEmpty())
    		pendingNodes = newPendingNodes;
    	
    	return false;
    }
    
    /*
     *  ForStatement:
     *		for (
 	 *			[ ForInit ];
 	 *			[ Expression ] ;
 	 *			[ ForUpdate ] )
 	 *			Statement
 	 *	ForInit:
 	 *		Expression { , Expression }
 	 *	ForUpdate:
 	 *		Expression { , Expression }
     */
    @SuppressWarnings("unchecked")
	public boolean visit(ForStatement forStatement) {
    	// Visit ForInit   
    	ListIterator<Expression> initializers = forStatement.initializers().listIterator();
    	for ( ; initializers.hasNext(); ) { 
    		this.visit(initializers.next());
    	}
    	
    	// Visit Expression
		this.visit(forStatement.getExpression());
		
    	// Visit the body of ForStatement   
		this.visit(forStatement.getBody());
		
    	// Visit ForUpdate
    	ListIterator<Expression> updaters = forStatement.updaters().listIterator();
    	for ( ; updaters.hasNext(); ) { 
    		this.visit(updaters.next());
    	}
		
    	return false;
    }
    
    /*
     *	CatchClause:
     */
	public boolean visit(CatchClause catchClause) {
		return false; // Ignore catch clauses
	}
	
	/*
	 *  ReturnStatement:
     *		return [ Expression ] ;
	 */
	public boolean visit(ReturnStatement returnStatement) {
		// Visit the expression
		this.visit(returnStatement.getExpression());
		
		return false;
	}

	/*
	 * Build the final Groum from the temporary Groum:
	 * 	+ Contract identical Groum nodes
	 *  + Add new edges to the temporary Groum (based on data dependency)
	 *  + Remove edges that don't have data dependency
	 */
	
	/*
	 * Contract identical Groum nodes
	 */
	private static void contractIdenticalGroumNodes() {		
		for (GroumNode currentNode : currentMethodNode.getGroumNodes()) {
			if (currentNode.getPrevNodes().size() == 1) {
				GroumNode prevNode = currentNode.getPrevNodes().iterator().next();
				// If the prevNode and the currentNode have the same method ID and data, then combine them.
				if (prevNode.isIdenticalTo(currentNode))
					contract2GroumNodes(prevNode, currentNode);
				
				if (prevNode.getPrevNodes().size() == 1 && currentNode.getNextNodes().size() == 1) {
					GroumNode prevPrevNode = prevNode.getPrevNodes().iterator().next();
					GroumNode nextNode = currentNode.getNextNodes().iterator().next();
					// If the (prevPrevNode, prevNode) and (currentNode, nextNode) pairs have the same methodID and data, then combine them.
					if (nextNode.getPrevNodes().size() == 1 && prevPrevNode.isIdenticalTo(currentNode) && (prevNode.isIdenticalTo(nextNode))) 
						contract2GroumPairs(prevPrevNode, prevNode, currentNode, nextNode);
				}
			}
		}		
	}
	
	private static void contract2GroumNodes(GroumNode prevNode, GroumNode currentNode) {
		prevNode.addEdgesTo(currentNode.getNextNodes());					
		currentNode.removeEdgesTo(currentNode.getNextNodes());
		prevNode.removeEdgeTo(currentNode);
		
		// Remove the contracted node from the list
		currentMethodNode.removeGroumNode(currentNode);
	}
	
	private static void contract2GroumPairs(GroumNode prevPrevNode, GroumNode prevNode, GroumNode currentNode, GroumNode nextNode) {
		prevNode.addEdgesTo(nextNode.getNextNodes());
		nextNode.removeEdgesTo(nextNode.getNextNodes());
		prevNode.removeEdgeTo(currentNode);
		currentNode.removeEdgeTo(nextNode);
		
		// Remove the contracted nodes from the list
		currentMethodNode.removeGroumNode(currentNode);
		currentMethodNode.removeGroumNode(nextNode);
	}
	
	/*
	 * Add new edges to the temporary Groum (based on data dependency)
	 */
	private static void addEdgesWithDataDependency() {
		// For each Groum node, visit all the nodes that it can reach
		for (GroumNode currentNode : currentMethodNode.getGroumNodes())
		for (GroumNode nextNode : currentNode.getAllNextNodes()) {
			// If there is data dependency between currentNode and nextNode, then add an edge between them.
			if (currentNode.hasDataDependencyWith(nextNode))
				currentNode.addEdgeTo(nextNode);		
		}
	}

	/*
	 * Remove edges that don't have data dependency
	 */
	private static void removeEdgesWithoutDataDedendency() {
		for (GroumNode currentNode : currentMethodNode.getGroumNodes())			
		for (GroumNode nextNode : currentNode.getNextNodes()) {
			// If there does not exist data dependency between currentNode and nextNode, then remove the edge between them.
			if (! currentNode.hasDataDependencyWith(nextNode))
				currentNode.removeEdgeTo(nextNode);
		}
	}
	
}
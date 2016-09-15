/**
 * 
 * @author HUNG
 *
 */

package aspect_mining.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

/*
 * A method node represents a method in the Java language.
 * Several special statements imply method invocations, such as creating a new object or type-casting. 
 * All the corresponding AST nodes are:
 * 	+ MethodInvocation: 		e.g. obj.m() 					=> Method ID: Type.m()
 *  + ClassInstanceCreation: 	e.g. new Type() 				=> Method ID: Type.[constructor]()
 *  + Initializer:				e.g. called on a type creation	=> Method ID: Type.<<initializer>>()
 *  + InstanceofExpression: 	e.g. obj instanceof Type 		=> Method ID: Type.<<instanceof>>(Object)
 *  + CastExpression: 			e.g. (Type) obj 				=> Method ID: Type.<<typecast>>(Object)
 */
public class MethodNode {
	
	private String methodID; 		// The ID of the method
	private ClassNode classNode; 	// The class that declares this method
	
	private HashSet<MethodNode> calledMethodNodes 	= new HashSet<MethodNode>(); // Callees of this method
	private HashSet<MethodNode> callingMethodNodes 	= new HashSet<MethodNode>(); // Callers of this method	
	
	// List of Groum nodes in the Groum of this method	
	private ArrayList<GroumNode> groumNodes = new ArrayList<GroumNode>(); 
	
	// The clones of this method, each clone pair has a similarity value (type Double)
	private HashMap<MethodNode, Double> clonedMethodNodes = new HashMap<MethodNode, Double>();
	
	// The peer group that this method belongs to
	private PeerGroup peerGroup = null;	
	
	/*
	 * Start of method implementation.
	 */
	
	/*
	 * Returns the methodID from a method key
	 * Example: Method key = LShape;.toString()Ljava/lang/String; => MethodID = LShape;.toString()
	 */
	public static String getMethodIDFromMethodKey(String methodKey) {
		String className  = methodKey.substring(0, methodKey.indexOf('.'));
		String methodName = methodKey.substring(methodKey.indexOf('.') + 1, methodKey.indexOf(')')+1);
		if (methodName.indexOf('(') == 0)
			methodName = "[constructor]" + methodName;
		return className + "." + methodName;
	}
	
	/*
	 * Returns the method ID of a method invocation
	 * Example: obj.m()	=> Method ID: Type.m()
	 */
	public static String getMethodIDByBinding(IMethodBinding methodBinding) {
		/* 
		 * NOTES: Currently using getKey() instead of building the method ID manually
		 */
		
		/*
		// Get the qualified class name and simple method name
		String className = methodBinding.getDeclaringClass().getQualifiedName();
		String methodName = (methodBinding.isConstructor()) ? "[constructor]" : methodBinding.getName();
			
		// Get the parameters of the method
		StringBuilder strBuilder = new StringBuilder();
		for (ITypeBinding parameter : methodBinding.getParameterTypes()) 
			strBuilder.append(parameter.getName() + ", ");
		if (strBuilder.length() > 0) // Remove the ", " string at the end
			strBuilder.delete(strBuilder.length() - 2, strBuilder.length()); 
		String parameters = strBuilder.toString();
		
		// Return the method ID
		return className + "." + methodName + "(" + parameters + ")";
		*/
		
		return getMethodIDFromMethodKey(methodBinding.getKey());
	}
	
	/*
	 * Returns the method ID of a method invocation with real-type binding
	 * Example: Given Shape obj = new Circle(), the method ID of obj.getArea() may be Circle.getArea()
	 */
	public static String getMethodIDByRealTypeBinding(String variableType, IMethodBinding methodBinding) {
		String methodID = MethodNode.getMethodIDByBinding(methodBinding);
		return variableType + methodID.substring(methodID.indexOf('.'));
	}
	
	/* Returns the method ID for the default class instance creation.
	 * Example: new Type() => Method ID: Type.[constructor]()
	 * This method implementation should be consistent with MethodNode.getMethodIDByBinding
	 */
	public static String getMethodIDForDefaultClassInstanceCreation(ITypeBinding typeBinding) {
		/* 
		 * NOTES: Currently using getKey() instead of the qualified type name
		 */
		
		/*
		return typeBinding.getQualifiedName() + ".[constructor]()";
		*/
		
		return typeBinding.getKey() + ".[constructor]()";
	}
	
	/* Returns the method ID for the initializer of a class. Because there may be more than one
	 * initializer, we need a count variable to distinguish them.
	 * Example: class A { static {} } => Method ID: Type.<<initializer>>[count]()
	 */
	public static String getMethodIDForInitializer(ITypeBinding typeBinding, int count) {
		/* 
		 * NOTES: Currently using getKey() instead of the qualified type name
		 */
		
		/*
		return typeBinding.getQualifiedName() + ".<<initializer>>[" + count + "]()";
		*/
		
		return typeBinding.getKey() + ".<<initializer>>[" + count + "]()";
	}
	
	/* Returns the method ID for instanceof expression.
	 * Example: obj instanceof Type => Method ID: Type.<<instanceof>>(Object)
	 */
	public static String getMethodIDForInstanceofExpression(ITypeBinding typeBinding) {
		/* 
		 * NOTES: Currently using getKey() instead of the qualified type name
		 */
		
		/*
		return typeBinding.getQualifiedName() + ".<<instanceof>>(Object)";
		*/
		
		return typeBinding.getKey() + ".<<instanceof>>(Object)";
	}
	
	/* Returns the method ID for cast expression.
	 * Example: (Type) obj => Method ID: Type.<<typecast>>(Object)
	 */
	public static String getMethodIDForCastExpression(ITypeBinding typeBinding) {
		/* 
		 * NOTES: Currently using getKey() instead of the qualified type name
		 */
		
		/*
		return typeBinding.getQualifiedName() + ".<<typecast>>(Object)";
		*/
		
		return typeBinding.getKey() + ".<<typecast>>(Object)";
	}
	
	public MethodNode(String methodID, ClassNode classNode) {
		this.methodID = methodID;
		this.classNode = classNode;
	}
	
	public String getMethodID() {
		return methodID;
	}
	
	public ClassNode getClassNode() {
		return classNode;
	}

	public void addCalledMethodNode(MethodNode methodNode) {
		calledMethodNodes.add(methodNode);
	}
	
	public HashSet<MethodNode> getCalledMethodNodes() {
		return new HashSet<MethodNode>(calledMethodNodes);
	}
	
	public void addCallingMethodNode(MethodNode methodNode) {
		callingMethodNodes.add(methodNode);
	}
	
	public HashSet<MethodNode> getCallingMethodNodes() {
		return new HashSet<MethodNode>(callingMethodNodes);
	}
	
	public void addGroumNode(GroumNode groumNode) {
		groumNodes.add(groumNode);
	}
	
	public void removeGroumNode(GroumNode groumNode) {
		groumNodes.remove(groumNode);
	}
	
	public ArrayList<GroumNode> getGroumNodes() {
		return new ArrayList<GroumNode>(groumNodes);
	}
	
	public void addClonedMethodNode(MethodNode methodNode, Double similarity) {
		clonedMethodNodes.put(methodNode, similarity);
	}
	
	public HashMap<MethodNode, Double> getClonedMethodNodes() {
		return new HashMap<MethodNode, Double>(clonedMethodNodes);
	}
	
	public void setPeerGroup(PeerGroup peerGroup) {
		this.peerGroup = peerGroup;
	}
	
	public PeerGroup getPeerGroup() {
		return peerGroup;
	}
	
	/*
	 * Returns true if the method is a constructor
	 */
	public boolean isConstructor() {
		return methodID.contains("[constructor]");	
	}
	
	/*
	 * Returns true if the method is a <<initializer>>, <<instanceof>>, or <<typecast>>
	 */
	public boolean isPseudoMethod() {
		return methodID.contains("<<");
	}
	
	/*
	 * Returns true if the method is a "test" method
	 */
	public boolean isTestMethod() {
		return methodID.contains("test/");
	}
	
	/*
	 * Returns true if the method is a "get-set" method
	 */
	public boolean isGetSetMethod() {
		String simpleName = this.getSimpleNameAndParameters();
		
		if (simpleName.startsWith("get") && (Character.isUpperCase(simpleName.charAt(3)) || simpleName.charAt(3) == '('))
			return true;
		if (simpleName.startsWith("set") && Character.isUpperCase(simpleName.charAt(3)))
			return true;
		if (simpleName.startsWith("is") && Character.isUpperCase(simpleName.charAt(2)))
			return true;
		if (simpleName.startsWith("has") && Character.isUpperCase(simpleName.charAt(3)) && (!simpleName.contains("hasNext")))
			return true;
		
		return false;
	}
	
	/*
	 * Returns true if the method is a "utility" method
	 */
	public boolean isUtilityMethod(String projectName) {
		// TODO: [*] Manually add utility methods
		if (projectName.equals("jhotdraw60b1")) {
			if (methodID.contains("[constructor]")
				|| methodID.contains(".displayBox(")
				|| methodID.contains(".containsPoint(")
				|| methodID.contains(".getAttribute(")
				|| methodID.contains(".handles(")
				|| methodID.contains(".basicDisplayBox(")
				|| methodID.contains(".clone(")
				|| methodID.contains(".invokeStart(")
				|| methodID.contains(".figures(")
				|| methodID.contains(".moveBy(")
				|| methodID.contains(".includes(")
				|| methodID.contains(".animationStep(")
				|| methodID.contains(".view(")
				|| methodID.contains(".pointCount(")
				|| methodID.contains(".canConnect(")
				|| methodID.contains(".basicMoveBy(")
				|| methodID.contains(".connectionInsets(")
				|| methodID.contains(".getContent(")
				|| methodID.contains(".tool(")
				|| methodID.contains(".textDisplayBox(")
				|| methodID.contains(".getAllFromDesktop(")
				|| methodID.contains(".drawing(")
				|| methodID.contains(".createList(")
				|| methodID.contains(".owner(")
				|| methodID.contains(".connectorAt(")
				|| methodID.contains(".editor(")
				|| methodID.contains(".name(")
				|| methodID.contains(".center(")
				|| methodID.contains(".selectionCount("))			
				return true;
			else
				return false;
		} else if (projectName.equals("columba-1.4-src")) {
			if (methodID.contains("exists")
				|| methodID.contains("print")
				|| methodID.contains("[constructor]"))
				return true;
			else
				return false;
		} else if (projectName.equals("apache-tomcat-6.0.26-src")) {
			if (methodID.contains("compiler/")
				|| methodID.contains("sendMessage(")
				|| methodID.contains("addHeader"))
				return true;
			else
				return false;
		} else if (projectName.contains("HealthWatcher")) {
			if (methodID.contains("Llib/util/Date;.main("))
				return true;
			else
				return false;
		} else if (projectName.equals("jEdit-4.3.1")) {
			if (methodID.contains("Get")
				|| methodID.contains("Set")
				|| methodID.contains("paramString(")
				|| methodID.startsWith("Lcom/")
				|| methodID.contains("Lorg/gjt/sp/jedit/textarea/Selection$Rect;.[constructor](II)")
				|| methodID.contains("._")
				|| methodID.contains("toString")
				|| methodID.contains("showDockableWindow("))
				return true;
			else
				return false;
		} else if (projectName.equals("jfreechart-1.0.13")) {
			if (methodID.contains("clone(")
				|| methodID.contains("equals")
				|| methodID.contains("valueToJava2D")
				|| methodID.contains("java2DToValue")
				|| methodID.contains("hashCode")
				|| methodID.contains("addChangeListener")
				|| methodID.contains("next")
				|| methodID.contains("draw(") && this.computeFanIn() == 0)
				return true;
			else
				return false;
		} else if (projectName.equals("jarp-source-1.0.1")) {
			if (methodID.contains("owner(")
				|| methodID.contains("displayBox(")
				|| methodID.contains("basicDisplayBox(")
				|| methodID.contains("startComponent(")
				|| methodID.contains("endComponent(")
				|| methodID.contains("connectorAt(")
				|| methodID.contains("canConnect(")
				|| methodID.contains("start(")
				|| methodID.contains("end(")
				|| methodID.contains("clone("))
				return true;
			else 
				return false;
		}
		else
			return false;
	}
	
	/*
	 * Returns true if the method body is empty.
	 */
	public boolean hasEmptyBody() {
		return groumNodes.isEmpty();
	}
	
	/*
	 * Returns the simple name of the method (without class and parameters)
	 */
	public String getSimpleName() {
		return methodID.substring(methodID.indexOf('.') + 1, methodID.indexOf('('));
	}
	
	/*
	 * Returns the simple name and parameters of the method
	 */
	public String getSimpleNameAndParameters() {
		return methodID.substring(methodID.indexOf('.') + 1);
	}
	
	/*
	 * Returns all the method nodes that this method overrides
	 */
	public HashSet<MethodNode> getOverriddenMethods() {
		HashSet<MethodNode> overriddenMethods = new HashSet<MethodNode>();
		
		// If the method is a constructor, <<initializer>>, <<instanceof>>, or <<typecast>>,
		// 	 then there are no overridden methods.
		if (this.isConstructor() || this.isPseudoMethod())
			return overriddenMethods;
			
		// Otherwise, search in the superclasses for overridden methods
		for (ClassNode superclassNode : this.classNode.getAllSuperclasses())
		for (MethodNode superclassMethodNode : superclassNode.getMethodNodes()) {
			if (this.getSimpleNameAndParameters().equals(superclassMethodNode.getSimpleNameAndParameters()))
				overriddenMethods.add(superclassMethodNode);
		}
		
		return overriddenMethods;
	}
	
	/*
	 * Returns all the method nodes that override this method
	 */
	public HashSet<MethodNode> getOverridingMethods() {
		HashSet<MethodNode> overridingMethods = new HashSet<MethodNode>();
		
		// If the method is a constructor, <<initializer>>, <<instanceof>>, or <<typecast>>,
		// 	 then there are no overriding methods.
		if (this.isConstructor() || this.isPseudoMethod())
			return overridingMethods;
			
		// Otherwise, search in the subclasses for overriding methods		
		for (ClassNode subclassNode : this.classNode.getAllSubclasses())
		for (MethodNode subclassMethodNode : subclassNode.getMethodNodes()) {
			if (this.getSimpleNameAndParameters().equals(subclassMethodNode.getSimpleNameAndParameters()))
				overridingMethods.add(subclassMethodNode);
		}
		
		return overridingMethods;
	}
	
	/*
	 * Returns all the constructors of the parent classes of this method (supposing
	 *   this method is also a constructor).
	 */
	public HashSet<MethodNode> getParentConstructors() {
		HashSet<MethodNode> parentConstructors = new HashSet<MethodNode>();
					
		if (! this.isConstructor())
			return parentConstructors;
				
		for (ClassNode superclassNode : this.classNode.getAllSuperclasses())
		for (MethodNode superclassMethodNode : superclassNode.getMethodNodes()) {
			if (this.getSimpleNameAndParameters().equals(superclassMethodNode.getSimpleNameAndParameters()))
				parentConstructors.add(superclassMethodNode);
		}
		
		return parentConstructors;
	}
	
	/*
	 * Returns all the constructors of the children classes of this method (supposing
	 *   this method is also a constructor).
	 */
	public HashSet<MethodNode> getChildrenConstructors() {
		HashSet<MethodNode> childrenConstructors = new HashSet<MethodNode>();
					
		if (! this.isConstructor())
			return childrenConstructors;
				
		for (ClassNode subclassNode : this.classNode.getAllSubclasses())
		for (MethodNode subclassMethodNode : subclassNode.getMethodNodes()) {
			if (this.getSimpleNameAndParameters().equals(subclassMethodNode.getSimpleNameAndParameters()))
				childrenConstructors.add(subclassMethodNode);
		}
		
		return childrenConstructors;
	}
	
	/*
	 * Compute the fan-in value of this method 
	 */
	public int computeFanIn() {
		HashSet<MethodNode> callSites = new HashSet<MethodNode>();
		callSites.addAll(callingMethodNodes);
		
		for (MethodNode methodNode: this.getOverriddenMethods()) {
			callSites.addAll(methodNode.callingMethodNodes);
		}
		for (MethodNode methodNode: this.getOverridingMethods()) {
			callSites.addAll(methodNode.callingMethodNodes);
		}
		
		return callSites.size();
	}
	
}

/**
 * 
 * @author HUNG
 *
 */

package aspect_mining.structures;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ITypeBinding;

/*
 * A class node represents a class or an interface in the Java language.
 */
public class ClassNode {
	
	private String classID; 		// The ID of the class, see ClassNode.getClassIDByBinding
	private boolean isInterface; 	// Indicates if it is a real Java class or an interface
	
	// List of methods declared in this class
	private ArrayList<MethodNode> methodNodes = new ArrayList<MethodNode>();
	
	private ClassNode superclassNode = null;	// The superclass of this class
	private ArrayList<ClassNode> interfaceNodes		= new ArrayList<ClassNode>(); // List of interfaces
	private ArrayList<ClassNode> subclassNodes 		= new ArrayList<ClassNode>(); // List of subclasses
	private ArrayList<ClassNode> implementerNodes 	= new ArrayList<ClassNode>(); // List of classes that implement this interface	 
	
	/*
	 * Returns the class ID of a class
	 */
	public static String getClassIDByBinding(ITypeBinding typeBinding) {
		/* 
		 * NOTES: Currently using getKey() instead of the qualified type name
		 */
		
		/*
		return typeBinding.getQualifiedName();
		*/
		
		return typeBinding.getKey();
	}
	
	/*
	 * Returns the class ID for a library class
	 */
	public static String getClassIDForLibraryClass() {
		return "LibraryClass";
	}
	
	public ClassNode(String classID, boolean isInterface) {
		this.classID = classID;
		this.isInterface = isInterface;
	}
	
	public String getClassID() {
		return classID;
	}
	
	public boolean isInterface() {
		return isInterface;
	}
	
	public void addMethodNode(MethodNode methodNode) {
		methodNodes.add(methodNode);
	}
	
	public ArrayList<MethodNode> getMethodNodes() {
		return new ArrayList<MethodNode>(methodNodes);
	}
	
	public void setSuperclassNode(ClassNode superclassNode) {
		this.superclassNode = superclassNode;
	}
	
	public ClassNode getSuperclassNode() {
		return superclassNode;
	}
	
	public void addInterfaceNode(ClassNode interfaceNode) {
		interfaceNodes.add(interfaceNode);
	}
	
	public ArrayList<ClassNode> getInterfaceNodes() {
		return new ArrayList<ClassNode>(interfaceNodes);
	}
	
	public void addSubclassNode(ClassNode subclassNode) {
		subclassNodes.add(subclassNode);
	}
	
	public ArrayList<ClassNode> getSubclassNodes() {
		return new ArrayList<ClassNode>(subclassNodes);
	}
	
	public void addImplementerNode(ClassNode implementerNode) {
		implementerNodes.add(implementerNode);
	}
	
	public ArrayList<ClassNode> getImplementerNodes() {
		return new ArrayList<ClassNode>(implementerNodes);
	}
	
	/*
	 * Returns true if the class is a library class (not written in the source code, e.g. java.lang.String)
	 */
	public boolean isLibraryClass() {
		return classID.equals(ClassNode.getClassIDForLibraryClass());
	}
	
	/*
	 * Returns the simple name of the class
	 * @see ClassNode.getClassIDByBinding
	 */
	public String getSimpleName() {
		return classID.substring(classID.lastIndexOf('/') + 1, classID.indexOf(';'));
	}
	
	/*
	 * Returns the superclasses of a class, and the interfaces that it implements at ALL levels
	 */
	public HashSet<ClassNode> getAllSuperclasses() {
		HashSet<ClassNode> classNodes = new HashSet<ClassNode>();
		
		// Add its direct superclass and interfaces
		if (superclassNode != null)
			classNodes.add(superclassNode);
		classNodes.addAll(interfaceNodes);
		
		// Add its indirect superclasses and interfaces (higher levels in the hierarchy)
		if (superclassNode != null)
			classNodes.addAll(superclassNode.getAllSuperclasses());
		for (ClassNode interfaceNode : interfaceNodes)
			classNodes.addAll(interfaceNode.getAllSuperclasses());
		
		return classNodes;
	}
	
	/*
	 * Returns the subclasses of a class, or the implementers of an interface at ALL levels
	 */
	public HashSet<ClassNode> getAllSubclasses() {
		HashSet<ClassNode> classNodes = new HashSet<ClassNode>();
		
		// Add its direct subclasses
		classNodes.addAll(subclassNodes);
		classNodes.addAll(implementerNodes);
		
		// Add its indirect subclasses (lower levels in the hierarchy)
		for (ClassNode subclass : subclassNodes)
			classNodes.addAll(subclass.getAllSubclasses());
		for (ClassNode subclass : implementerNodes)
			classNodes.addAll(subclass.getAllSubclasses());
		
		return classNodes;
	}
	
}
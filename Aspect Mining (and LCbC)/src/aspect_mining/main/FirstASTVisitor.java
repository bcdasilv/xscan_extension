/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.main;

import java.util.Iterator;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import aspect_mining.structures.ClassNode;
import aspect_mining.structures.MethodNode;

/*
 * The function of this class is to traverse an AST Tree to get the list of classes and methods.
 * To get the class hierarchy and create a Groum for each method, use SecondASTVisitor. 
 */
public class FirstASTVisitor extends ASTVisitor { 
	
    private AbstractImpl abstractImpl; // The main program that calls FirstASTVisitor.
    
    FirstASTVisitor(AbstractImpl abstractImpl) {
    	this.abstractImpl = abstractImpl;
    }
	
    @SuppressWarnings("unchecked")
	public boolean visit(TypeDeclaration type) {
    	ITypeBinding typeBinding = type.resolveBinding(); // Binding information
    	   	   	
    	// Enlist the class
    	ClassNode classNode = abstractImpl.enlistClass(ClassNode.getClassIDByBinding(typeBinding), type.isInterface()); 
    	
    	// Enlist all the methods (non-nested) declared in the class
    	for (MethodDeclaration method : type.getMethods()) {
    		IMethodBinding methodBinding = method.resolveBinding();
    		abstractImpl.enlistMethod(MethodNode.getMethodIDByBinding(methodBinding), classNode); 
    	}
    	
    	// If the type is an interface, then it's done.
    	if (type.isInterface())
    		return true;    		
    	
    	// Enlist the default constructor if the class does not have a constructor
    	boolean hasConstructor = false;
    	for (MethodDeclaration method : type.getMethods()) {
    		if (method.isConstructor()) {
    			hasConstructor = true;
    			break;
    		}
    	}
    	if (!hasConstructor) {
    		abstractImpl.enlistMethod(MethodNode.getMethodIDForDefaultClassInstanceCreation(typeBinding), classNode);
    	}
    	
    	// Enlist the initializer methods in the class (if any)
    	// Example: class A { static {} } => Method ID: Type.<<initializer>>[count]()
    	int count = 0;
    	for (Iterator<BodyDeclaration> iter = type.bodyDeclarations().iterator(); iter.hasNext(); ) {
    		if (iter.next().getNodeType() == ASTNode.INITIALIZER)
    			abstractImpl.enlistMethod(MethodNode.getMethodIDForInitializer(typeBinding, count++), classNode);
    	}
    	
    	/* For each class, also create 2 default methods below. */
 
    	// InstanceofExpression: e.g. obj instanceof Type => Method ID: Type.<<instanceof>>(Object)
    	abstractImpl.enlistMethod(MethodNode.getMethodIDForInstanceofExpression(typeBinding), classNode);
    	
    	// CastExpression: e.g. (Type) obj => Method ID: Type.<<typecast>>(Object)
    	abstractImpl.enlistMethod(MethodNode.getMethodIDForCastExpression(typeBinding), classNode);

    	return true; 
    }
    
 }
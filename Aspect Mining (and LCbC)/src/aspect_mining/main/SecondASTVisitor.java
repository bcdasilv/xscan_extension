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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import aspect_mining.structures.ClassNode;
import aspect_mining.structures.MethodNode;

/*
 * The function of this class is to get the class hierarchy and create a Groum for each method.
 * The list of classes and methods are obtained by FirstASTVistior.
 */
public class SecondASTVisitor extends ASTVisitor {
	
	private AbstractImpl abstractImpl; // The main program that calls FirstASTVisitor.
	    
	SecondASTVisitor(AbstractImpl abstractImpl) {
	  	this.abstractImpl = abstractImpl;
	}
	    
    @SuppressWarnings("unchecked")
	public boolean visit(TypeDeclaration type) {
    	ITypeBinding typeBinding = type.resolveBinding(); // Binding information
    	   	
    	// Get the classNode representing this class
    	String classID = ClassNode.getClassIDByBinding(typeBinding);
    	ClassNode classNode = abstractImpl.classMap.get(classID);
    	
    	// Create the links between the currentClassNode and its superclass
    	ITypeBinding superclassBinding = typeBinding.getSuperclass();
    	if (superclassBinding != null) {    		
    		String superclassID = ClassNode.getClassIDByBinding(superclassBinding);
    		ClassNode superclassNode = abstractImpl.classMap.get(superclassID);
    		if (superclassNode != null) {
    			classNode.setSuperclassNode(superclassNode);
    			superclassNode.addSubclassNode(classNode);
    		}
    	}
    	
    	// Create the links between the currentClassNode and its interfaces
    	for (ITypeBinding interfaceBinding : typeBinding.getInterfaces()) {
    		String interfaceID = ClassNode.getClassIDByBinding(interfaceBinding);
    		ClassNode interfaceNode = abstractImpl.classMap.get(interfaceID);
    		if (interfaceNode != null) {
    			classNode.addInterfaceNode(interfaceNode);
    			interfaceNode.addImplementerNode(classNode);
    		}	
    	}
    	
    	// If the type is an interface, then it's done.
    	if (type.isInterface())
    		return true;

    	// Visit all the methods (non-nested) declared in the class
    	for (MethodDeclaration method : type.getMethods()) {
    		String methodID = MethodNode.getMethodIDByBinding(method.resolveBinding());
    		MethodNode methodNode = abstractImpl.methodMap.get(methodID);
    		
    		GroumCreator.create(abstractImpl, method, methodNode); // Create the Groum for this method
    	}

    	// Visit the initializer methods in the class (if any)
    	int count = 0;
    	for (Iterator<BodyDeclaration> iter = type.bodyDeclarations().iterator(); iter.hasNext(); ) {
    		BodyDeclaration bodyDeclaration = iter.next();
    		if (bodyDeclaration.getNodeType() == ASTNode.INITIALIZER) {
    			String methodID = MethodNode.getMethodIDForInitializer(typeBinding, count++);
    			MethodNode methodNode = abstractImpl.methodMap.get(methodID);
    			
    			GroumCreator.create(abstractImpl, bodyDeclaration, methodNode); // Create the Groum for this method
    			break;
    		}
    	}
    	
    	return true; 
    }
    
 }
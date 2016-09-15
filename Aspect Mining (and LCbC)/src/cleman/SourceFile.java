package cleman;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
import java.io.Serializable;
//import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * This class will be used to hold the files information for the application.
 * @author Jafar Al-Kofahi
 *
 */
public class SourceFile implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	
	public static int nextID = 1;
	//public static HashSet<SourceFile> all = new HashSet<SourceFile>();
	
	public static HashMap<String, SourceFile> all = new HashMap<String, SourceFile>();
	public static HashSet<SourceFile> addeds = new HashSet<SourceFile>();
	/**
	 * This will contain the file ID, which will be the hashCode of the file path
	 */
	public int fileID;
	/**
	 * This will contain the full path for this file
	 */
	public String fileLocation; 
	/**
	 * The last modified time for this file 
	 */
	public long lastModified;
	/**
	 * This file size in KB
	 */
	public long fileSize;
	/**
	 * This will contain the total number of code lines in the file  
	 */
	//public int fileCodeLength;
	/**
	 * This will contain the total number of code lines in the file  
	 */
	public int fileCommentLength;
	/**
	 * This DS will contain all vectors that belong to this file.
	 */
	public HashSet<Fragment> fragments = new HashSet<Fragment>();
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.fileID;
	}
	public SourceFile(ICompilationUnit unit) throws JavaModelException
	{
		this.fileID = nextID++;
		this.fileLocation = unit.getPath().toString();;
		ASTParser parser = ASTParser.newParser(AST.JLS3);
    	parser.setKind(ASTParser.K_COMPILATION_UNIT);
    	parser.setSource(unit);
    	parser.setResolveBindings(true);
    	ASTNode ast = parser.createAST(null); 
    	Visitor visitor = new Visitor(this);
    	ast.accept(visitor);
    	this.fragments.addAll(new HashSet<Fragment>(visitor.fragments));
    	/*for(Fragment f : visitor.fragments)
    		Fragment.all.put(f.getId(), f);*/
    	visitor = null;
	}
	/*public void printFragments() {
		System.out.println("File " + this.fileLocation + "\t" + fragments.size() + "fragments");
    	for (int fragmentID  : fragments) {
    		Data.fragments.get(fragmentID).printFragment();
    	}
	}*/
}

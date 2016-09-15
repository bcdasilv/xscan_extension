package cleman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

/**
 * @author Nguyen Anh Hoan
 *
 */
class Visitor extends ASTVisitor {
	public static byte[] indexer = new byte[84];	//category of this node type (Class, Method, Block, Statement, ..)
	//public ArrayList<Integer> lines = new ArrayList<Integer>();
	private SourceFile sourceFile;
	private String fileName;
    public ArrayList<Fragment> fragments = new ArrayList<Fragment>();	//fragments of one file
    /*
	 * Stack of children's n-gram vectors
	 */
    private Stack<ArrayList<HashMap<Integer, Integer>>> stackChildrenVectors = new Stack<ArrayList<HashMap<Integer, Integer>>>();
	/*
	 * Stack of VERTICAL n-grams starting from the ROOT of the subtrees 
	 */
	private Stack<ArrayList<HashMap<Integer, Integer>>> stackChildrenRootVGrams = new Stack<ArrayList<HashMap<Integer, Integer>>>();
	/*
	 * Stack of HORIZONTAL sequences of children
	 */
	private Stack<ArrayList<Byte>> stackChildrenHSequences = new Stack<ArrayList<Byte>>();
	
	Stack<ArrayList<ASTNode>> stackChildrenNodes = new Stack<ArrayList<ASTNode>>();
	
	int lastNode = 0;	//for end token of vector
    Stack<Integer> stackCurrentNode = new Stack<Integer>();	//for start token of vector
    
    Stack<String> currentMethodName = new Stack<String>();

    public Visitor() {

    }
    public Visitor(String fileName) {
    	this.fileName = fileName;
    }
    public Visitor(SourceFile sourceFile) {
    	this.sourceFile = sourceFile;
    	this.fileName = sourceFile.fileLocation;
    }
    static {
    	int index=0;
		for(byte i=0; i<84; i++) {
			if (i==0 || i==6  || i==11 || i==12 || i==17 || i==19 || i==20 || i==22 || i==26 || i==28 ||  i==30 || i==35 || 
					  i==46 || i==47 || i==52 || i==53 || i==54 || i==54 || i==63 || i==64 || (i>=71 && i<=83)
					  //|| i == ASTNode.SIMPLE_NAME
					  //|| i == ASTNode.BLOCK
					  ){
						indexer[i] = Fragment.NotConsideredFrags;
			}
			else
			{
				int gram = i << 24;
				Fragment.gram2Index.put(gram, index);
				Fragment.index2Gram.put(index++, gram);
				
				switch (i) {
				//case ASTNode.COMPILATION_UNIT: indexer[i] = Fragment.ClassFragment; break;
				//case ASTNode.TYPE_DECLARATION_STATEMENT: indexer[i] = Fragment.ClassFragment; break;
				case ASTNode.TYPE_DECLARATION: indexer[i] = Fragment.ClassFragment; break;
				case ASTNode.METHOD_DECLARATION: indexer[i] = Fragment.MethodFragment; break;
				//case ASTNode.BLOCK: indexer[i] = Fragment.BlockFragment; break;
				case ASTNode.DO_STATEMENT: indexer[i] = Fragment.LoopStatementFragment; break;
				case ASTNode.FOR_STATEMENT: indexer[i] = Fragment.LoopStatementFragment; break;
				case ASTNode.ENHANCED_FOR_STATEMENT: indexer[i] = Fragment.LoopStatementFragment; break;
				case ASTNode.WHILE_STATEMENT: indexer[i] = Fragment.LoopStatementFragment; break;
				case ASTNode.IF_STATEMENT: indexer[i] = Fragment.IfStatementFragment; break;
				//case ASTNode.SWITCH_CASE: indexer[i] = Fragment.IfStatementFragment; break;
				case ASTNode.SWITCH_STATEMENT: indexer[i] = Fragment.SwitchStatementFragment; break;
				
				case ASTNode.CONDITIONAL_EXPRESSION: indexer[i] = Fragment.Expression; break;
				case ASTNode.INFIX_EXPRESSION: indexer[i] = Fragment.Expression; break;
				case ASTNode.POSTFIX_EXPRESSION: indexer[i] = Fragment.Expression; break;
				case ASTNode.PREFIX_EXPRESSION: indexer[i] = Fragment.Expression; break;
				case ASTNode.PARENTHESIZED_EXPRESSION: indexer[i] = Fragment.Expression; break;
				case ASTNode.INSTANCEOF_EXPRESSION: indexer[i] = Fragment.Expression; break;
				
				case ASTNode.METHOD_INVOCATION: indexer[i] = Fragment.MethodState; break;
				case ASTNode.SUPER_METHOD_INVOCATION: indexer[i] = Fragment.MethodState; break;
				
				//case ASTNode.VARIABLE_DECLARATION_EXPRESSION: indexer[i] = Fragment.DeclarationExp; break;
				//case ASTNode.VARIABLE_DECLARATION_FRAGMENT: indexer[i] = Fragment.VarState; break;
				case ASTNode.VARIABLE_DECLARATION_STATEMENT: indexer[i] = Fragment.DeclarationState; break;
				case ASTNode.FIELD_DECLARATION: indexer[i] = Fragment.DeclarationState; break;
				
				case ASTNode.SIMPLE_NAME: indexer[i] = Fragment.SimpleName; break;
				case ASTNode.BOOLEAN_LITERAL: indexer[i] = Fragment.Literal; break;
				case ASTNode.CHARACTER_LITERAL: indexer[i] = Fragment.Literal; break;
				case ASTNode.STRING_LITERAL: indexer[i] = Fragment.Literal; break;
				case ASTNode.NUMBER_LITERAL: indexer[i] = Fragment.Literal; break;
				
				case ASTNode.ARRAY_ACCESS: indexer[i] = Fragment.ArrayState; break;
				case ASTNode.ARRAY_CREATION: indexer[i] = Fragment.ArrayState; break;
				case ASTNode.ARRAY_INITIALIZER: indexer[i] = Fragment.ArrayState; break;
				case ASTNode.ARRAY_TYPE: indexer[i] = Fragment.ArrayState; break;
				
				case ASTNode.ASSERT_STATEMENT: indexer[i] = Fragment.AssertState; break;
				case ASTNode.ASSIGNMENT: indexer[i] = Fragment.AssignState; break;
				
				case ASTNode.MEMBER_REF: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.METHOD_REF: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.METHOD_REF_PARAMETER: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.PRIMITIVE_TYPE: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.QUALIFIED_NAME: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.TAG_ELEMENT: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.TEXT_ELEMENT: indexer[i] = Fragment.OtherFragments; break;
				case ASTNode.TYPE_PARAMETER: indexer[i] = Fragment.OtherFragments; break;
				default: indexer[i] = Fragment.OtherStatementFragment; break;
				}
			}
		}
    }
    public boolean visit(MethodDeclaration node)
    {
    	IMethodBinding binding = node.resolveBinding();
    	currentMethodName.push(binding.getMethodDeclaration().getKey());
    	//System.out.println(binding.getMethodDeclaration().getKey());
    	
    	return true;
    }
    public boolean visit(Block node)
    {
    	stackChildrenNodes.push(new ArrayList<ASTNode>());
    	
    	return true;
    }
    public void preVisit(ASTNode node) {
    	int aNode = node.getNodeType(); 
    	if (aNode == ASTNode.JAVADOC || aNode == ASTNode.BLOCK_COMMENT || aNode == ASTNode.LINE_COMMENT)
    		return;
    	stackCurrentNode.push(++lastNode); 
    	
    	stackChildrenVectors.push(new ArrayList<HashMap<Integer, Integer>>());
    	stackChildrenRootVGrams.push(new ArrayList<HashMap<Integer, Integer>>());
    	stackChildrenHSequences.push(new ArrayList<Byte>());
    }
    public void postVisit(ASTNode node) {
    	int aNode = node.getNodeType();
    	if (aNode == ASTNode.JAVADOC || aNode == ASTNode.BLOCK_COMMENT || aNode == ASTNode.LINE_COMMENT)
    		return;
    	
    	ASTNode parent = node.getParent();
    	if(parent != null)
    	{
    		int pType = parent.getNodeType();
    		if(pType == ASTNode.BLOCK)
    		{
    			if(aNode == ASTNode.BLOCK)
    			{
    				ArrayList<ASTNode> children = stackChildrenNodes.pop();
        			stackChildrenNodes.peek().addAll(new ArrayList<ASTNode>(children));
    				stackChildrenNodes.push(children);
    			}
    			else
    				stackChildrenNodes.peek().add(node);
    		}
    		else if(pType == ASTNode.EXPRESSION_STATEMENT || pType == ASTNode.CAST_EXPRESSION)
    			parent.setProperty("childType", node.getNodeType());
    	}
    	
    	buildFragment(node);
    }
    private void buildFragment(ASTNode node) {
    	int nodeType = node.getNodeType();
    	int currentNode = stackCurrentNode.pop();

    	ArrayList<HashMap<Integer, Integer>> childrenVectors = stackChildrenVectors.pop();
    	ArrayList<HashMap<Integer, Integer>> childrenRootVGrams = stackChildrenRootVGrams.pop();
    	ArrayList<Byte> childrenHSequence = stackChildrenHSequences.pop();
    	/*
    	 * VERTICAL n-grams starting from this node
    	 */
    	HashMap<Integer, Integer> myRootVGrams = new HashMap<Integer, Integer>();
    	
    	HashMap<Integer, Integer> vector = new HashMap<Integer, Integer>();
    	/*
    	 * Adding vectors of all children
    	 */
    	if (!childrenVectors.isEmpty()) {
    		vector.putAll(new HashMap<Integer, Integer>(childrenVectors.get(0)));
    		for(int i = 1; i < childrenVectors.size(); i++) {
    			HashMap<Integer, Integer> childVector = childrenVectors.get(i);
    			for (int index : childVector.keySet()) {
    				if (vector.containsKey(index))
    					vector.put(index, (vector.get(index) + childVector.get(index)));
    				else 
    					vector.put(index, childVector.get(index));
    			}
    		}
    	}
    	/*
    	 * Adding all new horizontal n-grams
    	 */
    	if (nodeType != ASTNode.BLOCK && nodeType != ASTNode.EXPRESSION_STATEMENT)
	    	for (int i = 0; i < childrenHSequence.size()-1; i++) {
	    		int gram = childrenHSequence.get(i);
	    		for (int j = 2; j <= Fragment.maxSizeOfGram; j++) {
	    			if (i+j-1 < childrenHSequence.size()) {
	    				gram = (gram << 4) + childrenHSequence.get(i+j-1);
	    				int index;
	    				if (Fragment.gram2Index.containsKey(gram))
	    					index = Fragment.gram2Index.get(gram);
	    				else {
	    					index = Fragment.gram2Index.size();
	    					Fragment.gram2Index.put(gram, index);
	    					Fragment.index2Gram.put(index, gram);
	    				}
	    				if (vector.containsKey(index))
	    					vector.put(index, vector.get(index) + 1);
	    				else 
	    					vector.put(index, 1);
	    			}
	    			else break;
	    		}
	    	}
    	else if(nodeType == ASTNode.BLOCK)
    	{
    		int width = childrenVectors.size() ;
    		ArrayList<ASTNode> childrenNodes = stackChildrenNodes.peek();
    		/*if(childrenNodes.size() != childrenHSequence.size())
    			System.err.println(node.toString());*/
    		if(childrenNodes.size() > 1)
    		{
    			for(int i = 0; i < width-1; i++)
    			{
    				for(int j = i+Detector.mergeWindow-1; j < width; j++)
    				{
    					HashMap<Integer, Integer> mergeVector = new HashMap<Integer, Integer>();
    					mergeVector.putAll(new HashMap<Integer, Integer>(childrenVectors.get(i)));
    		    		for(int k = i+1; k <= j; k++) {
    		    			HashMap<Integer, Integer> childVector = childrenVectors.get(k);
    		    			for (int index : childVector.keySet()) {
    		    				if (mergeVector.containsKey(index))
    		    					mergeVector.put(index, (mergeVector.get(index) + childVector.get(index)));
    		    				else 
    		    					mergeVector.put(index, childVector.get(index));
    		    			}
    		    		}
    		    		for (int ii = i; ii < j; ii++) {
    		    			int type = childrenNodes.get(ii).getNodeType();
    			    		if(type == ASTNode.EXPRESSION_STATEMENT)
    			    			type = (Integer)childrenNodes.get(ii).getProperty("childType");
    			    		int gram = type;
    			    		for (int jj = 2; jj <= Fragment.maxSizeOfGram; jj++) {
    			    			if (ii+jj-1 <= j) {
    			    				type = childrenNodes.get(ii+jj-1).getNodeType();
    	    			    		if(type == ASTNode.EXPRESSION_STATEMENT)
    	    			    			type = (Integer)childrenNodes.get(ii+jj-1).getProperty("childType");
    	    			    		gram = (gram << 4) + type;
    			    				int index;
    			    				if (Fragment.gram2Index.containsKey(gram))
    			    					index = Fragment.gram2Index.get(gram);
    			    				else {
    			    					index = Fragment.gram2Index.size();
    			    					Fragment.gram2Index.put(gram, index);
    			    					Fragment.index2Gram.put(index, gram);
    			    				}
    			    				if (mergeVector.containsKey(index))
    			    					mergeVector.put(index, mergeVector.get(index) + 1);
    			    				else 
    			    					mergeVector.put(index, 1);
    			    			}
    			    			else break;
    			    		}
    			    	}
		    			Fragment fragment = new Fragment();
		    			fragment.setSourceFile(this.sourceFile);
		    			//fragment.setFileName(this.fileName);
		    	    	//fragment.setCode(node.toString());
		    			int startChar = childrenNodes.get(i).getStartPosition();
		    	    	fragment.setStartChar(startChar);
		    	    	fragment.setLength(childrenNodes.get(j).getStartPosition()-startChar+childrenNodes.get(j).getLength());
		    	    	fragment.setGramVector(mergeVector);
		    	    	fragment.setType((byte)Fragment.BlockFragment);
		    	    	fragment.setVectorLength();
		    	    	if(!currentMethodName.isEmpty())
		    	    		fragment.setMethodSignature(currentMethodName.peek());
		    	    	fragments.add(fragment);	//add to this file's fragments
    				}
    			}
    		}
    	}
    	/*
    	 * This node is also a single node type in the vector
    	 */
		if ((indexer[nodeType] != Fragment.NotConsideredFrags)) {
    	//if(indexer[nodeType] <= 11) {
			int gram = nodeType << 24;
			int index =	Fragment.gram2Index.get(gram);
			if (vector.containsKey(index))
				vector.put(index, vector.get(index) + 1);
			else 
				vector.put(index, 1);
		}
		/*
		 * This node is also a 1-gram in the vector
		 */
		if(indexer[nodeType] <= 11) {
			int gram = -indexer[nodeType];
			int tmpIndex;
			if (Fragment.gram2Index.containsKey(gram))
				tmpIndex = Fragment.gram2Index.get(gram);
			else {
				tmpIndex = Fragment.gram2Index.size();
				Fragment.gram2Index.put(gram, tmpIndex);
				Fragment.index2Gram.put(tmpIndex, gram);
			}
			if (myRootVGrams.containsKey(tmpIndex))
				myRootVGrams.put(tmpIndex, myRootVGrams.get(tmpIndex) + 1);
			else 
				myRootVGrams.put(tmpIndex, 1);
		}
		/*
		 * Building all n-grams starting from this node (will be used by its parent)
		 */
		if (!childrenRootVGrams.isEmpty()) {
			if(indexer[nodeType] <= 11) {
				for (HashMap<Integer, Integer> childGram : childrenRootVGrams) {
					for (int index : childGram.keySet()) {
						int gram = Fragment.index2Gram.get(index);
						gram = -((indexer[nodeType] << (4 * getSizeOfGram(gram))) - gram);
						int tmpIndex;
						if (Fragment.gram2Index.containsKey(gram))
							tmpIndex = Fragment.gram2Index.get(gram);
	    				else {
	    					tmpIndex = Fragment.gram2Index.size();
	    					Fragment.gram2Index.put(gram, tmpIndex);
	    					Fragment.index2Gram.put(tmpIndex, gram);
	    				}
	    				if (vector.containsKey(tmpIndex))
	    					vector.put(tmpIndex, vector.get(tmpIndex) + childGram.get(index));
	    				else 
	    					vector.put(tmpIndex, childGram.get(index));
	    				if (getSizeOfGram(gram) < Fragment.maxSizeOfGram) {
	    					if (myRootVGrams.containsKey(tmpIndex))
	    						myRootVGrams.put(tmpIndex, myRootVGrams.get(tmpIndex) + childGram.get(index));
		    				else 
		    					myRootVGrams.put(tmpIndex, childGram.get(index));
	    				}
	    			}
				}
			}
			else {
				for (HashMap<Integer, Integer> childGram : childrenRootVGrams) {
					for (int index : childGram.keySet()) {
    					if (myRootVGrams.containsKey(index))
    						myRootVGrams.put(index, myRootVGrams.get(index) + childGram.get(index));
	    				else 
	    					myRootVGrams.put(index, childGram.get(index));
	    			}
				}
			}
		}
		/*
		 * Build the corresponding fragment
		 */
		//if(nodeType == ASTNode.METHOD_DECLARATION || (nodeType != ASTNode.EXPRESSION_STATEMENT && nodeType != ASTNode.BLOCK && lastNode-currentNode >= Main.minFragmentSize))
		if(nodeType != ASTNode.EXPRESSION_STATEMENT && nodeType != ASTNode.BLOCK && lastNode-currentNode >= Detector.minFragmentSize)
		{
			Fragment fragment = new Fragment();
			fragment.setSourceFile(this.sourceFile);
			//fragment.setFileName(this.fileName);
	    	//fragment.setCode(node.toString());
	    	fragment.setStartChar(node.getStartPosition());
	    	fragment.setLength(node.getLength());
	    	fragment.setGramVector(vector);
	    	fragment.setType((byte)((indexer[nodeType] < 8) ? indexer[nodeType] : 0));
	    	fragment.setVectorLength();
	    	if(!currentMethodName.isEmpty())
	    		fragment.setMethodSignature(currentMethodName.peek());
	    	fragments.add(fragment);	//add to this file's fragments
		}
		/*
		 * Pushing to the stacks respectively
		 */
    	if (!stackChildrenVectors.isEmpty()) {	//if not root
    		if (!vector.isEmpty()) {
	    		ArrayList<HashMap<Integer, Integer>> parentVectors = stackChildrenVectors.pop();	//get siblings
	    		parentVectors.add(vector); 	//join them (append this node type)
	    		stackChildrenVectors.push(parentVectors);	//back home
    		}
    		if (!myRootVGrams.isEmpty()) {
    			ArrayList<HashMap<Integer, Integer>> parentGrams = stackChildrenRootVGrams.pop();	//get siblings
        		parentGrams.add(myRootVGrams); 	//join them (append this node type)
        		stackChildrenRootVGrams.push(parentGrams);	//back home
    		}
    		if (indexer[nodeType] <= 11) {
    			ArrayList<Byte> sequence = stackChildrenHSequences.pop();	//get siblings
    			sequence.add(indexer[nodeType]);	//	join them (append this node type)
    			stackChildrenHSequences.push(sequence);
    		}
    		else if (nodeType == ASTNode.EXPRESSION_STATEMENT || nodeType == ASTNode.BLOCK) {
    			ArrayList<Byte> sequence = stackChildrenHSequences.pop();
    			sequence.addAll(childrenHSequence);
    			stackChildrenHSequences.push(sequence);
    		}
    	}
    	if(nodeType == ASTNode.METHOD_DECLARATION)
    	{
    		currentMethodName.pop();
    	}
    	else if(nodeType == ASTNode.BLOCK)
    	{
    		stackChildrenNodes.pop();
    	}
    }
    byte getSizeOfGram(int gram) {
    	byte i = 0;
    	gram = Math.abs(gram);
    	while (gram != 0) {
    		i++;
    		gram = gram / 16;
    	}
    	return i;
    }
 }

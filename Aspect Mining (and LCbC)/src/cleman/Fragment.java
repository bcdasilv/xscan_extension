package cleman;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * @author Nguyen Anh Hoan
 *
 */
public class Fragment implements Serializable {
	protected static final long serialVersionUID = 1L;
	
	public static final int maxSizeOfGram = 2;	// now < 8
	
	/**
	 * Categories of AST node types
	 */
	public static final byte ClassFragment = 1;
	public static final byte MethodFragment = 2;
	public static final byte LoopStatementFragment = 3;
	public static final byte IfStatementFragment = 4;
	public static final byte SwitchStatementFragment = 5;
	public static final byte BlockFragment = 6;
	public static final byte MethodState = 7;
	public static final byte DeclarationState = 8;
	public static final byte AssertState = 9;
	public static final byte AssignState = 10;
	
	public static final byte ArrayState = 13;
	public static final byte Expression = 14;
	public static final byte DeclarationExp = 17;
	public static final byte SimpleName = 18;
	public static final byte Literal = 19;
	public static final byte StatementsGroupFragment = 27;
	public static final byte MethodsGroupFragment = 28;
	public static final byte OtherStatementFragment = 29;
	public static final byte OtherFragments = 30;
	public static final byte NotConsideredFrags = 31;

	public static String[] typeName = {"", "Class", "Method", "Loop", "If", "Switch", "", "MethodState", "Decl", "Assert", "Assign"};
	/**
	 * Global Maps between an n-gram and its index and vice versa
	 */
	public static HashMap<Integer, Integer> gram2Index = new HashMap<Integer, Integer>();
	public static HashMap<Integer, Integer> index2Gram = new HashMap<Integer, Integer>();

	//public static HashSet<Fragment> all = new HashSet<Fragment>();
	public static HashMap<Integer, Fragment> all = new HashMap<Integer, Fragment>();
	public static HashMap<Integer, Fragment> addeds = new HashMap<Integer, Fragment>();
	public static HashMap<Integer, Fragment> deleteds = new HashMap<Integer, Fragment>();
	public static int nextID = 1;
	
	int id;
	//String fileName;
	SourceFile sourceFile;
	String methodSignature = "";
	byte type = OtherFragments; 
	String code = null;
	//byte[] tokens;
	int startChar;	//start character
	int length;	//in characters including CrLf
	int startLine;
	int endLine;
	HashSet<Fragment> clones = new HashSet<Fragment>();
	//HashSet<Integer> descendants;
	HashSet<Bucket> buckets;	// = new HashSet<Bucket>();
	//HashSet<Group> groups;
	
	/**
	 * 
	 */
	public Fragment() {
		this.id = nextID++;
	}
	public Fragment(int id)
	{
		this.id = id;
	}
	/**
	 * @return the sourceFile
	 */
	public SourceFile getSourceFile() {
		return sourceFile;
	}
	/**
	 * @param sourceFile the sourceFile to set
	 */
	public void setSourceFile(SourceFile sourceFile) {
		this.sourceFile = sourceFile;
	}
	public String getMethodSignature() {
		return methodSignature;
	}
	public void setMethodSignature(String methodSignature) {
		this.methodSignature = methodSignature;
	}
	/**
	 * @return the buckets
	 */
	public HashSet<Bucket> getBuckets() {
		return buckets;
	}
	/**
	 * @param buckets the buckets to set
	 */
	public void setBuckets(HashSet<Bucket> buckets) {
		this.buckets = buckets;
	}
	/**
	 * @return the clones
	 */
	public HashSet<Fragment> getClones() {
		return clones;
	}
	/**
	 * @param clones the clones to set
	 */
	public void setClones(HashSet<Fragment> clones) {
		this.clones = clones;
	}
	/**
	 * @return the descendants
	 */
	/*public HashSet<Integer> getDescendants() {
		return descendants;
	}*/
	/**
	 * @param descendants the descendants to set
	 */
	/*public void setDescendants(HashSet<Integer> descendants) {
		this.descendants = descendants;
	}*/
	/**
	 * @return the groups
	 *//*
	public HashSet<Group> getGroups() {
		return groups;
	}
	*//**
	 * @param groups the groups to set
	 *//*
	public void setGroups(HashSet<Group> groups) {
		this.groups = groups;
	}*/
	HashMap<Integer, Integer> gramVector = new HashMap<Integer, Integer>();
	double vectorLength;
	
	/**
	 * @return the vectorLength
	 */
	public double getVectorLength() {
		return vectorLength;
	}
	/**
	 * @param vectorLength the vectorLength to set
	 */
	public void setVectorLength() {
		double len = 0;
		for(int val : this.gramVector.values())
			len += val * val;
		len  = Math.sqrt(len);
		this.vectorLength = len;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.id;
	}
	/**
	 * @return the type
	 */
	public byte getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(byte type) {
		this.type = type;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	/*public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}*/
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public int getStartChar() {
		return startChar;
	}
	public void setStartChar(int startChar) {
		this.startChar = startChar;
	}
	public int getStartLine() {
		return startLine;
	}
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	public int getEndLine() {
		return endLine;
	}
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	/*public int getStartToken() {
		return startToken;
	}
	public void setStartToken(int startToken) {
		this.startToken = startToken;
	}
	public int getEndToken() {
		return endToken;
	}
	public void setEndToken(int endToken) {
		this.endToken = endToken;
	}*/
	/**
	 * This map contains only the grams appearing in this fragment
	 */
	public HashMap<Integer, Integer> getSparseGramVector() {
		return this.gramVector;
	}
	/**
	 * This vector has the full size of all grams in the project. Thus, there might be many zeroed elements
	 * @return
	 */
	public int[] getFullGramVector() {
		int[] full = new int[gram2Index.size()];
		for(int index = 0; index < gram2Index.size(); index++)
			full[index] = (this.gramVector.containsKey(index)?this.gramVector.get(index):0);
		return full;
	}
	public void setGramVector(HashMap<Integer, Integer> gramVector) {
		this.gramVector = new HashMap<Integer, Integer>(gramVector);
	}
	public boolean contains(Fragment fragment)
	{
		return this.sourceFile == fragment.sourceFile && this.startChar <= fragment.getStartChar() && this.length >= fragment.getLength();
	}
	public boolean isClonedTo(Fragment fragment)
	{
		//if(fragment.getType() != this.type || this.descendants.contains(fragment.getId()) || fragment.descendants.contains(this.id))
		if(fragment.getType() != this.type || this.contains(fragment) || fragment.contains(this))
			return false;
		else if(this.clones.contains(fragment))
			return true;
		else
		{
			HashMap<Integer, Integer> v1 = new HashMap<Integer, Integer>(this.gramVector);
			HashMap<Integer, Integer> v2 = new HashMap<Integer, Integer>(fragment.getSparseGramVector());
			HashSet<Integer> keys = new HashSet<Integer>(v1.keySet());
			keys.addAll(v2.keySet());
			
			double d = 0;
			for(int key : keys)
			{
				int temp = (v1.containsKey(key) ? v1.get(key) : 0) - (v2.containsKey(key) ? v2.get(key) : 0);
				d += temp * temp;
			}
			//d = Math.sqrt(d) * 2.0 / (this.vectorLength + fragment.getVectorLength());
			d = Math.sqrt(d) / Math.min(this.vectorLength, fragment.getVectorLength());
			return (d <= Detector.threshold);
		}
	}
	/*public void selfDestroy()
	{
		if (this.group != null) {
			this.group.removeFragment(this);
			if (this.group.fragments.isEmpty())
				Group.all.remove(this.group.id);
		}
		HashSet<Bucket> bs = new HashSet<Bucket>(this.buckets);
		for (Bucket b : bs) {
			b.removeFragment(this);
			if(b.fragments.isEmpty())
				Bucket.all.remove(b.id);
		}
		Fragment.all.remove(this.id);
		this = null;
	}*/
	static public String nGramToString(int gram) {
		StringBuffer strGram = new StringBuffer();
		
		if (gram % (1 << 24) == 0) {
			strGram.append((byte)(gram >> 24));
		}
		else {
			String separator;
			if (gram > 0) 
				separator = "--";
			else 
				separator = "<-";
			gram = Math.abs(gram);
			while (gram != 0) {
				if(strGram.length() != 0)
					strGram.insert(0, separator);
				strGram.insert(0, typeName[gram % 16]);
				gram = gram >> 4;
			}
		}
		
		return strGram.toString();
	}
	static public ArrayList<Byte> nGramToArray(int gram) {
		ArrayList<Byte> list = new java.util.ArrayList<Byte>();
		
		if (gram % (1 << 24) == 0) {
			list.add((byte)(gram >> 24));
		}
		else {
			if (gram > 0) 
				list.add((byte)0);
			else 
				list.add((byte)1);
			gram = Math.abs(gram);
			while (gram != 0) {
				list.add(1, (byte)(gram % 16));
				gram = gram >> 4;
			}
		}
		
		return list;
	}
	private byte getSizeOfGram(int gram) {
		byte i = 0;
		if (gram % (1 << 24) == 0) {
			i = 1;
		}
		else {			
	    	gram = Math.abs(gram);
	    	while (gram != 0) {
	    		i++;
	    		gram = gram / 16;
	    	}
		}
    	
    	return i;
    }
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\r\n");
		buf.append("File: " + this.sourceFile.fileLocation + "\t" + this.sourceFile.fileID);
		buf.append("\r\nFragID: " + this.id + "\r\n");
		//buf.append(tokens.length + " tokens from " + startToken + ", ");
		//buf.append("Tokens from " + startToken + " to " + endToken + ", ");
		buf.append(length + " characters from " + startChar + "\r\n");
		buf.append("From line " + this.startLine + " to " + this.endLine + "\r\n");
		buf.append("Vector: " + gramVector.size() + "/" + gram2Index.size() + "\r\n");
		/*for (int j = 0; j < Data.numOfNodeTypes - 1; j++) 
			buf.append((nGramVector.containsKey(j)?nGramVector.get(j):0)+",");
		buf.append(nGramVector.containsKey(Data.numOfNodeTypes - 1)?nGramVector.get(Data.numOfNodeTypes - 1):0);*/
		buf.append("Sparse: ");
		for (int i = 0; i < maxSizeOfGram; i++)
			for (int j = 0; j < gram2Index.size(); j++) 
				if (gramVector.containsKey(j) && (getSizeOfGram(index2Gram.get(j)) == i+1))
					buf.append(nGramToString(index2Gram.get(j)) + "(" + j + ")" + " = " + gramVector.get(j) + ",");
		/*buf.append("\r\n");
 		buf.append("Full: ");
 		int[] full = this.getFullGramVector();
 		for(int index = 0; index < gram2Index.size(); index++)
 			buf.append(full[index] + ", ");*/
		//buf.append("\r\nVector leng: " + this.getVectorLength());
		/*buf.append("\r\nClones: ");
 		for(Fragment f : this.clones)
 		{
 			HashMap<Integer, Integer> v1 = this.gramVector;
			HashMap<Integer, Integer> v2 = f.getSparseGramVector();
			HashSet<Integer> keys = new HashSet<Integer>();
			keys.addAll(v1.keySet());
			keys.addAll(v2.keySet());
			
			double d = 0;
			for(int key : keys)
			{
				int temp = (v1.containsKey(key) ? v1.get(key) : 0) - (v2.containsKey(key) ? v2.get(key) : 0);
				d += temp * temp;
			}
			d = Math.sqrt(d) * 2.0 / (this.vectorLength + f.getVectorLength());
 			
 			buf.append(f.id + ":" + d + "@" + keys.size() + " ");
 		}
 		buf.append("\r\n");*/
		//buf.append("\r\nDescendants: " + this.descendants + "\r\n");
		/*buf.append("\r\nHashcodes: ");
 		for(Bucket b : this.buckets)
 		{
 			buf.append(b.id + " ");
 		}
 		buf.append("\r\n");*/
		buf.append("\r\n-----------------------------------------------------------\r\n");
 		buf.append(code + "\r\n");
 		return buf.toString();
	}
}

/**
 * 
 * @author HUNG
 * 
 */

package user.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;

import aspect_mining.structures.MethodNode;
import aspect_mining.structures.OracleGroup;
import aspect_mining.structures.PeerGroup;

public class FileAccess {
	
	/*
	 * Read file
	 */
	public static String getFileContent(String fileName) {
		String fileContent = "";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
		    String line  = null;
		    StringBuilder stringBuilder = new StringBuilder();
		    String ls = System.getProperty("line.separator");
		    while( ( line = reader.readLine() ) != null ) {
		        stringBuilder.append( line );
		        stringBuilder.append( ls );
		    }
		    fileContent = stringBuilder.toString();
		    reader.close();
		}
		catch (IOException ex) {
			ex.printStackTrace();
	    }
		return fileContent;
	}
	
	/*
	 * Write file
	 */
	public static void writeFileContent(String fileName, String str){
        try {
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
			bufferedWriter.write(str);
			bufferedWriter.close();
		} 
  		catch (IOException ex) {
  			ex.printStackTrace();
	    }
	}
	
	/*
	 * Save a copy of this file as a new file.
	 */
	public static void saveCopyAs(String fromFile, String toFile) {
		try {
			FileChannel inChannel = new	FileInputStream(fromFile).getChannel();
			FileChannel outChannel = new FileOutputStream(toFile).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Read raw Oracle file.
	 * (Group names and method names may not be in the right format.)
	 */
	public static ArrayList<OracleGroup> readRawOracleFile(String rawOracleDataFile, String methodListFile) {
		// Read the list of methods
		StringBuilder methodList = new StringBuilder();
		try {			
			BufferedReader reader = new BufferedReader(new FileReader(methodListFile));
			String line;
			while ((line = reader.readLine()) != null) {
			    if (line.isEmpty() || line.contains("*** LIST OF METHODS ***"))
			    	continue;
			    
			    methodList.append(line);
			    methodList.append("|"); // Format: "method1|method2|...|method nth|"
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Read the methods in the Raw Oracle file, reformat them based on the list above
		ArrayList<OracleGroup> oracleGroups = new ArrayList<OracleGroup>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(rawOracleDataFile));
		    while (true) {
		    	String line;
		    	
		    	// Read the header of the group
		    	while (true) {
		    		line = reader.readLine();
		    		if (line == null)
		    			break;
		    		line = line.trim();
		    		if (!line.isEmpty() && !line.startsWith("//"))
		    			break;
		    	}
		    	if (line == null)
		    		break;
		    	String groupHeader = line;
		    	
		    	// Get the group name from the group header
		    	String groupName = OracleGroup.getNameFromHeader(groupHeader);
		    	
		    	OracleGroup oracleGroup = new OracleGroup();
		    	
		    	// Read all the methods in the group
		    	HashSet<String> methodIDs = new HashSet<String>();
		    	while (true) {
		    		line = reader.readLine();
		    		if (line == null)
		    			break;
		    		line = line.trim();
		    		if (line.isEmpty())
		    			break;
		    		if (line.startsWith("//")) // The method has been commented out
		    			continue;
		    		
		    		// Standardize the form of the methodID
				    if (methodList.indexOf(line) == -1) {
					    if (line.indexOf('.') >= 0 & line.indexOf('(') >=0) // Reformat the constructor method 
					    	if (line.substring(0, line.indexOf('.')).equals(line.substring(line.indexOf('.') + 1, line.indexOf('('))))
					    		line = line.substring(0, line.indexOf('.') + 1) + "[constructor]" + line.substring(line.indexOf('('));
					    
					    if (line.indexOf('(') >= 0) // Remove parameters
					    	line = line.substring(0, line.indexOf('(') + 1);
					    
					    if (line.indexOf('.') >= 0) // Add ';' between class name and method name
					    	line = line.substring(0, line.indexOf('.')) + ";" + line.substring(line.indexOf('.'));
				    }   
		    		
				    // Add the methodID to the list if no errors are detected	    		
		    		if (methodList.indexOf(line) == -1) 
				    	System.out.println("Method not recognized in aspect " + groupHeader + ": " + line);
				    else if (methodList.indexOf(line) != methodList.lastIndexOf(line))
				    	System.out.println("Ambiguous method in aspect " + groupHeader + ": " + line);
				    else if (methodIDs.contains(line))
				    	System.out.println("Duplicate methods in aspect " + groupHeader + ": " + line);
				    else {
				    	line = methodList.substring(0, methodList.indexOf("|", methodList.indexOf(line)));
				    	line = line.substring(line.lastIndexOf("|") + 1);
				    	methodIDs.add(line);
				    	oracleGroup.add(new MethodNode(line, null));
				    }
		    	}
		    	oracleGroups.add(oracleGroup);
		    	oracleGroup.makeHeader(oracleGroups.indexOf(oracleGroup), groupName);		    	 
		    	
		    	if (line == null)
		    		break;
		    }
		    reader.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
	    }
		
		return oracleGroups;
	}
	
	/*
	 * Read standard Oracle file
	 */
	public static ArrayList<OracleGroup> readOracleFile(String oracleDataFile) {
		ArrayList<OracleGroup> oracleGroups = new ArrayList<OracleGroup>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(oracleDataFile));
		    while (true) {
		    	String line;
		    	
		    	// Read the name of the group
		    	while (true) {
		    		line = reader.readLine();
		    		if (line == null)
		    			break;
		    		if (!line.trim().isEmpty())
		    			break;
		    	}
		    	if (line == null)
		    		break;
		    	
		    	OracleGroup oracleGroup = new OracleGroup();
		    	oracleGroup.setHeader(line);
		    	
		    	// Read all the methods in the group
		    	while (true) {
		    		line = reader.readLine();
		    		if (line == null)
		    			break;
		    		if (line.trim().isEmpty())
		    			break;
		    		
		    		MethodNode methodNode = new MethodNode(line, null);
		    		oracleGroup.add(methodNode);
		    	}
		    	oracleGroups.add(oracleGroup); 
		    	
		    	if (line == null)
		    		break;
		    }
		    reader.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
	    }
		
		return oracleGroups;
	}
	
	/*
	 * Write Oracle file
	 */
	public static void writeOracleFile(String oracleDataFile, ArrayList<OracleGroup> oracleGroups) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(oracleDataFile));
		
			for (OracleGroup oracleGroup : oracleGroups) {
				writer.write(oracleGroup.getHeader() + "\r\n");
		  		for (MethodNode oracleMethod : oracleGroup.getMethodNodes())
			  		writer.write(oracleMethod.getMethodID() + "\r\n");
		  		writer.write("\r\n");
			}  		
		
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Read peers file
	 */
	public static ArrayList<PeerGroup> readPeersFile(String peerDataFile) {
		ArrayList<PeerGroup> peerGroups = new ArrayList<PeerGroup>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(peerDataFile));
			
			// Discard the first 3 lines
			reader.readLine();
			reader.readLine();
			reader.readLine();
			
		    while (true) {    	    	
		    	// Read the header of the group
		    	String line = reader.readLine();
		    	PeerGroup peerGroup = new PeerGroup();
		    	peerGroup.setHeader(line);
		    	
		    	// Read all the methods in the group
		    	while (true) {
		    		line = reader.readLine();
		    		if (line == null)
		    			break;
		    		if (line.isEmpty())
		    			break;
		    		if (line.contains("\t"))
		    			line = line.substring(0, line.indexOf('\t'));
		    		
		    		MethodNode methodNode = new MethodNode(line, null);
		    		peerGroup.add(methodNode);
		    	}
		    	peerGroups.add(peerGroup); 
		    	
		    	if (line == null)
		    		break;
		    }
		    reader.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
	    }
		
		return peerGroups;
	}
	
}

/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

/*
 * Data structure for an Oracle group
 */
public class OracleGroup extends MethodGroup {

	/*
	 * Start of code implementation.
	 */
	
	/*
	 * Sets the header of an Oracle group.
	 * Format: "Group #x\tName of the group\t[number of methods] method(s)".
	 */
	public void makeHeader(int groupNumber, String groupName) {
		this.setHeader("Group #" + groupNumber + "\t" + groupName + "\t" + this.size() + " method(s)");
	}
	
	/*
	 * Returns the name of an Oracle group.
	 */
	public String getName() {
		return OracleGroup.getNameFromHeader(this.getHeader());
	}
	
	/*
	 * Returns the group name from the group header.
	 */
	public static String getNameFromHeader(String header) {
		if (header.indexOf('\t') < header.lastIndexOf('\t'))
			return header.substring(header.indexOf('\t') + 1, header.lastIndexOf('\t'));
		else
			return header;
	}
	
}

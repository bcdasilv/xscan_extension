/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

/*
 * Data structure for a CBFA group
 */
public class CBFAGroup extends MethodGroup {

	private double rank = 0; // The rank of this CBFA group
	
	/*
	 * Start of code implementation.
	 */
	
	/*
	 * Sets the header of a CBFA group.
	 * Format: "CBFA Group #x\tRank: x\tMethods: x"
	 */
	public void makeHeader(int groupNumber) {
		this.setHeader("CBFA Group #" + groupNumber + "\tRank: " + getRank() + "\t\tMethods: " + this.size());
	}
	
	public void setRank(double rank) {
		this.rank = rank;
	}
	
	public double getRank() {
		return rank;
	}
			
	/*
	 * Compute the rank of the group.
	 * This method should be called PRIOR TO any use (i.e., CBFAGroup.getRank()).
	 */
	public void computeRank() {
		int totalFanIn = 0;
		for (MethodNode methodNode : methodNodes) {
			totalFanIn += methodNode.computeFanIn();
		}
		this.rank = totalFanIn;
	}
	
}

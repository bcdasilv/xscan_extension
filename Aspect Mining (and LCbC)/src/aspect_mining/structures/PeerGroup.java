/**
 * 
 * @author HUNG
 * 
 */

package aspect_mining.structures;

/*
 * Data structure for a peer group
 */
public class PeerGroup extends MethodGroup {

	private double rank = 0; // The rank of this peer group
	
	private double lowestSimilarity = Integer.MAX_VALUE; // The lowest similarity in all the pairs within this group

	/*
	 * Start of code implementation.
	 */
	
	/*
	 * Sets the header of a peer group.
	 * Format: "Peer Group #x\tRank: x\tMethods: x\tLowest similarity: x"
	 */
	public void makeHeader(int groupNumber) {
		this.setHeader("Peer Group #" + groupNumber + "\tRank: " + getRank() + "\t\tMethods: " + this.size() + "\t\t\t\tLowest similarity: " + this.getLowestSimilarity());
	}
	
	public void setRank(double rank) {
		this.rank = rank;
	}
	
	public double getRank() {
		return rank;
	}
	
	public void setLowestSimilarity(double lowestSimilarity) {
		this.lowestSimilarity = lowestSimilarity;
	}
	
	public double getLowestSimilarity() {
		return lowestSimilarity;
	}
		
	/*
	 * Compute the rank of the group.
	 * This method should be called PRIOR TO any use (i.e., PeerGroup.getRank()).
	 */
	public void computeRank() {
		// TODO: Policy 1 - Compute the rank of a group based on CBFA's idea: Rank of	
		//			a group is the total fan-in values of all methods in the group.
		int totalFanIn = 0;
		for (MethodNode methodNode : methodNodes) {
			totalFanIn += methodNode.computeFanIn();
		}
		this.rank = totalFanIn;
	}
	
}

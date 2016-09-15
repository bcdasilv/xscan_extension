/**
 * 
 */
package cleman;

import java.util.HashSet;
import java.util.HashMap;
import java.io.Serializable;

/**
 * @author Nguyen Thanh Tung
 *
 */
public class Bucket implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//public static HashSet<Bucket> all = new HashSet<Bucket>();
	public static HashMap<Integer, Bucket> all = new HashMap<Integer, Bucket>();
	public static HashMap<Integer, Bucket> addeds = new HashMap<Integer, Bucket>();
	
	public HashSet<Fragment> fragments = new HashSet<Fragment>();

	public int id;	//hashcode
	
	public Bucket(int id) {			
		this.id = id;
	}			
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.id;
	}
	public String toString() {			
		return "Bucket: " + id + ", fragments = " + fragments;
	}			
	/**
	 * 
	 * @param fragment
	 */
	void addFragment(Fragment frag)
	{	
		this.fragments.add(frag);
		if(frag.getBuckets() == null)
			frag.setBuckets(new HashSet<Bucket>());
		frag.getBuckets().add(this);
	}
	/**
	 * 
	 * @param fragment
	 */
	void removeFragment(Fragment fragment)
	{
		fragment.getBuckets().remove(this);
		this.fragments.remove(fragment);
		if(fragment.getBuckets().isEmpty())
			fragment.setBuckets(null);
	}
	/**
	 * 
	 */
	public static HashMap<Integer, Bucket> map(HashSet<Fragment> fragments)
	{
		HashMap<Integer, Bucket> buckets = new HashMap<Integer, Bucket>();
		Hash hash = new Hash();
		for(Fragment f : fragments) 
		{
			int[] h = hash.hashEuclidean(f);
			for(int i = 0; i < h.length; i++)
			{
				Bucket temp;
				if (buckets.containsKey(h[i]))
					temp = buckets.get(h[i]);							
				else 
				{
					temp = new Bucket(h[i]);
				}
				temp.addFragment(f);
				buckets.put(h[i], temp);
			}
		}
		return buckets;
	}
	/**
	 * 
	 */
	public static void map2All(HashSet<Fragment> fragments)
	{
		Hash hash = new Hash();
		for(Fragment f : fragments) 
		{
			int[] h = hash.hashEuclidean(f);
			for(int i = 0; i < h.length; i++)
			{
				Bucket temp;
				if (Bucket.all.containsKey(h[i]))
				{
					temp = Bucket.all.get(h[i]);
					//temp.addFragment(f);
					//Bucket.addeds.put(h[i], temp);
					Bucket.all.remove(h[i]);
				}
				else if(Bucket.addeds.containsKey(h[i]))
				{
					temp = Bucket.addeds.get(h[i]);
				}
				else
				{
					temp = new Bucket(h[i]);
				}
				temp.addFragment(f);
				Bucket.addeds.put(h[i], temp);
			}
		}
	}
	/**
	 * 
	 */
	public static void mapAll()
	{
		Hash hash = new Hash();
		for(Fragment f : Fragment.all.values()) 
		{
			int[] h = hash.hashEuclidean(f);
			for(int i = 0; i < h.length; i++)
			{
				Bucket temp;
				if (Bucket.all.containsKey(h[i]))
					temp = Bucket.all.get(h[i]);							
				else 
				{
					temp = new Bucket(h[i]);
				}
				temp.addFragment(f);
				Bucket.all.put(h[i], temp);
			}
		}
	}
}

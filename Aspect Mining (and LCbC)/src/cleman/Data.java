package cleman;

import java.util.HashMap;
import java.util.Random;
import java.io.Serializable;

public class Data implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	int nextFragmentID = 1;
	int nextGroupID = 1;
	int nextFileID = 1;
	
	//HashMap<Integer, Integer> gram2Index = new HashMap<Integer, Integer>();
	//HashMap<Integer, Integer> index2Gram = new HashMap<Integer, Integer>();
	int[] grams;
	HashMap<Integer, Fragment> fragments;
	HashMap<Integer, Bucket> buckets;
	//HashMap<Integer, Group> groups;
	HashMap<String, SourceFile> files;
	
	Random randGaussian;
	int[] ran;
	double b[][];
	double[][][] aGaussian;
	
	public Data()
	{
		
	}
	public Data(Object o)
	{
		
	}
	/**
	 * @return the nextFragmentID
	 */
	public int getNextFragmentID() {
		return nextFragmentID;
	}
	/**
	 * @param nextFragmentID the nextFragmentID to set
	 */
	public void setNextFragmentID(int nextFragmentID) {
		this.nextFragmentID = nextFragmentID;
	}
	/**
	 * @return the nextGroupID
	 */
	public int getNextGroupID() {
		return nextGroupID;
	}
	/**
	 * @param nextGroupID the nextGroupID to set
	 */
	public void setNextGroupID(int nextGroupID) {
		this.nextGroupID = nextGroupID;
	}
	/**
	 * @return the nextFileID
	 */
	public int getNextFileID() {
		return nextFileID;
	}
	/**
	 * @param nextFileID the nextFileID to set
	 */
	public void setNextFileID(int nextFileID) {
		this.nextFileID = nextFileID;
	}
	/**
	 * @return the fragments
	 */
	public HashMap<Integer, Fragment> getFragments() {
		return fragments;
	}
	/**
	 * @param fragments the fragments to set
	 */
	public void setFragments(HashMap<Integer, Fragment> fragments) {
		this.fragments = fragments;
	}
	/**
	 * @return the grams
	 */
	public int[] getGrams() {
		return grams;
	}
	/**
	 * @param grams the grams to set
	 */
	public void setGrams(int[] grams) {
		this.grams = grams;
	}
	/**
	 * @return the gram2Index
	 *//*
	public HashMap<Integer, Integer> getGram2Index() {
		return gram2Index;
	}
	*//**
	 * @param gram2Index the gram2Index to set
	 *//*
	public void setGram2Index(HashMap<Integer, Integer> gram2Index) {
		this.gram2Index = gram2Index;
	}
	*//**
	 * @return the index2Gram
	 *//*
	public HashMap<Integer, Integer> getIndex2Gram() {
		return index2Gram;
	}
	*//**
	 * @param index2Gram the index2Gram to set
	 *//*
	public void setIndex2Gram(HashMap<Integer, Integer> index2Gram) {
		this.index2Gram = index2Gram;
	}*/
	/**
	 * @return the buckets
	 */
	public HashMap<Integer, Bucket> getBuckets() {
		return buckets;
	}
	/**
	 * @param buckets the buckets to set
	 */
	public void setBuckets(HashMap<Integer, Bucket> buckets) {
		this.buckets = buckets;
	}
	/**
	 * @return the groups
	 */
	/*public HashMap<Integer, Group> getGroups() {
		return groups;
	}*/
	/**
	 * @param groups the groups to set
	 */
	/*public void setGroups(HashMap<Integer, Group> groups) {
		this.groups = groups;
	}*/
	/**
	 * @return the files
	 */
	public HashMap<String, SourceFile> getFiles() {
		return files;
	}
	/**
	 * @param files the files to set
	 */
	public void setFiles(HashMap<String, SourceFile> files) {
		this.files = files;
	}
	/**
	 * @return the randGaussian
	 */
	public Random getRandGaussian() {
		return randGaussian;
	}
	/**
	 * @param randGaussian the randGaussian to set
	 */
	public void setRandGaussian(Random randGaussian) {
		this.randGaussian = randGaussian;
	}
	/**
	 * @return the ran
	 */
	public int[] getRan() {
		return ran;
	}
	/**
	 * @param ran the ran to set
	 */
	public void setRan(int[] ran) {
		this.ran = ran;
	}
	/**
	 * @return the b
	 */
	public double[][] getB() {
		return b;
	}
	/**
	 * @param b the b to set
	 */
	public void setB(double[][] b) {
		this.b = b;
	}
	/**
	 * @return the aGaussian
	 */
	public double[][][] getAGaussian() {
		return aGaussian;
	}
	/**
	 * @param gaussian the aGaussian to set
	 */
	public void setAGaussian(double[][][] gaussian) {
		aGaussian = gaussian;
	}
}

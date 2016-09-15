/**
 * 
 */
package user.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * @author hoan
 *
 */
public class Util {
	public static double thresholdRecurring = 0.3;
	public static double thresholdSimilarity = 0.667;
	public static HashSet<String> controls = new HashSet<String>();
	public static HashMap<String, HashSet<Integer>> exceptRevisions = new HashMap<String, HashSet<Integer>>();
	
	public static String getSimpleFileName(String fileName)
	{
		char separator = '/';
		if(fileName.lastIndexOf('\\') > -1)
			separator = '\\';
		int start = fileName.lastIndexOf(separator) + 1;
		int end = fileName.lastIndexOf('.');
		if(end <= start)
			end = fileName.length() + 1;
		
		return fileName.substring(start, end);
	}
	public static String getSimpleClassName(String className)
	{
		return className.substring(className.lastIndexOf('.') + 1, className.length());
	}
	public static String getFileContent(File file)
	{
		StringBuffer strBuf = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
	    	String line = "";
	    	while ((line = in.readLine()) != null) { 
	    		strBuf.append(line + "\r\n");
	    	}
	    	in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strBuf.toString();
	}
	public static void getJars(File dir, ArrayList<String> jars)
	{
		for(File sub : dir.listFiles())
		{
			if(sub.isDirectory())
			{
				if(!sub.getName().equals("src") && !sub.getName().equals("source"))
				{
					boolean isSource = false;
					for(String s : sub.list())
						if(s.endsWith(".java") || s.endsWith(".class"))
						{
							isSource = true;
							break;
						}
					if(!isSource)
						getJars(sub, jars);
				}
			}
			else if(sub.getName().endsWith(".jar"))
			{
				jars.add(sub.getAbsolutePath());
			}
		}
	}
	public static String getVersion(String src)
	{
		int start = 0, end = src.length()-1;
		while(!Character.isDigit(src.charAt(start)))
			start++;
		while(!Character.isDigit(src.charAt(end)))
			end--;
		/*if(start >= src.length() || end < 0)
			return "";*/
		return src.substring(start, end+1);
	}
	
	public static double distanceManhattan(HashMap<String, Integer> v1, HashMap<String, Integer> v2)
	{
		if(v1.isEmpty() || v2.isEmpty())
			return 1;
		return 1 - match(new ArrayList<String>(v1.keySet()), new ArrayList<String>(v2.keySet())).size()*2.0/(v1.size() + v2.size());
	}
	public static double distanceEuclidean(HashMap<String, Integer> v1, HashMap<String, Integer> v2)
	{
		double d = 0;
		HashMap<String, String> map = match(new ArrayList<String>(v1.keySet()), new ArrayList<String>(v2.keySet()));
		for(String key1 : map.keySet())
		{
			String key2 = map.get(key1);
			d += (v1.get(key1) - v2.get(key2)) * (v1.get(key1) - v2.get(key2));
		}
		HashSet<String> keys = new HashSet<String>(v1.keySet());
		keys.removeAll(map.keySet());
		for(String key : keys)
			d += v1.get(key) * v1.get(key);
		keys = new HashSet<String>(v2.keySet());
		keys.removeAll(map.values());
		for(String key : keys)
			d += v2.get(key) * v2.get(key);
		
		return Math.sqrt(d) * 2.0 / (v1.size() + v2.size());
	}
	public static HashMap<String, String> match(ArrayList<String> v1, ArrayList<String> v2)
	{
		HashMap<String, String> map1 = new HashMap<String, String>();
		ArrayList<String> temp = new ArrayList<String>(v2);
		for(String term1 : v1)
		{
			for(int i = 0; i < temp.size(); i++)
			{
				String term2 = temp.get(i);
				if(isRelative(term1, term2))
				{
					map1.put(term1, term2);
					temp.remove(i);
					break;
				}
			}
		}
		HashMap<String, String> map2 = new HashMap<String, String>();
		temp = new ArrayList<String>(v1);
		for(String term2 : v2)
		{
			for(int i = 0; i < temp.size(); i++)
			{
				String term1 = temp.get(i);
				if(isRelative(term1, term2))
				{
					map2.put(term1, term2);
					temp.remove(i);
					break;
				}
			}
		}
		if(map1.size() > map2.size())
			return map1;
		else
			return  map2;
	}
	public static int match(HashSet<ArrayList<String>> v1, HashSet<ArrayList<String>> v2)
	{
		int score1 = 0, score2 = 0;
		ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>(v2);
		for(ArrayList<String> term1 : v1)
		{
			for(int i = 0; i < temp.size(); i++)
			{
				ArrayList<String> term2 = temp.get(i);
				if(isRelative(term1, term2))
				{
					score1++;
					temp.remove(i);
					break;
				}
			}
		}
		temp = new ArrayList<ArrayList<String>>(v1);
		for(ArrayList<String> term2 : v2)
		{
			for(int i = 0; i < temp.size(); i++)
			{
				ArrayList<String> term1 = temp.get(i);
				if(isRelative(term1, term2))
				{
					score2++;
					temp.remove(i);
					break;
				}
			}
		}
		if(score1 > score2)
			return score1;
		else
			return  score2;
	}
	public static ArrayList<String> serialize(String text)
	{
		ArrayList<String> list = new ArrayList<String>();
		int start = 0;
		while(start < text.length())
		{
			String word = "";
			while(start < text.length() && !Character.isLetterOrDigit(text.charAt(start)))
				start++;
			if(start < text.length())
			{
				int end = start;
				if(Character.isDigit(text.charAt(start)))
				{
					while(end+1 < text.length() && Character.isDigit(text.charAt(end+1)))
						end++;
				}
				else if(Character.isLowerCase(text.charAt(start)))
				{
					while(end+1 < text.length() && Character.isLowerCase(text.charAt(end+1)))
						end++;
				}
				else if(Character.isUpperCase(text.charAt(start)))
				{
					if(end+1 < text.length())
					{
						char ch = text.charAt(end+1);
						if(Character.isUpperCase(ch))
						{
							end++;
							while(end+1 < text.length() && Character.isUpperCase(text.charAt(end+1)))
								end++;
							if(end+1 < text.length() && Character.isLowerCase(text.charAt(end+1)))
								end--;
						}
						else if(Character.isLowerCase(ch))
						{
							end++;
							while(end+1 < text.length() && Character.isLowerCase(text.charAt(end+1)))
								end++;
						}
					}
				}
				else
				{
					System.out.println("What should be else???");
				}
				word = text.substring(start, end+1);
				start = end + 1;
			}
			list.add(word);
		}
		
		return list;
	}
	public static ArrayList<String> split(String str)
	{
		ArrayList<String> parts = new ArrayList<String>();
		int start = 0, end = 0;
		while(end < str.length())
		{
			start = end;
			while(start < str.length() && (str.charAt(start) == ' ' || str.charAt(start) == '\t'))
				start++;
			if(start == str.length())
				break;
			end = start;
			while(end < str.length() && str.charAt(end) != ' ' && str.charAt(end) != '\t')
				end++;
			parts.add(str.substring(start, end));
		}
		
		return parts;
	}
	public static HashMap<String, Integer> sum(HashMap<String, Integer> v1, HashMap<String, Integer> v2)
	{
		HashMap<String, Integer> v = new HashMap<String, Integer>(v1);
		for(String word : v2.keySet())
			if(v.containsKey(word))
				v.put(word, v.get(word) + v2.get(word));
			else
				v.put(word, v2.get(word));
		
		return v;
	}
	public static boolean isRelative2(ArrayList<String> term1, ArrayList<String> term2) {
		HashSet<String> x = new HashSet<String>(term1);
		x.retainAll(term2);
		return (x.size() >= 2); 
	}
	public static boolean isExact(ArrayList<String> term1, ArrayList<String> term2) {
		return  (term1.equals(term2));  
	}

	/*
	 * Written by HUNG.
	 * @see isRelative.
	 */
	public static double computeSimilarity(ArrayList<String> term1, ArrayList<String> term2)
	{
		int lenM = term1.size(), lenN = term2.size();
		int[][] d = new int[lenM+1][lenN+1];
		String[] codeM = new String[lenM+1];
		String[] codeN = new String[lenN+1];
		String[][] p = new String[lenM+1][lenN+1];
		d[0][0] = 0;
		for(int i = 1; i <= lenM; i++)
		{
			d[i][0] = 0;
			codeM[i] = term1.get(i-1).toString();
		}
		for(int i = 1; i <= lenN; i++)
		{
			d[0][i] = 0;
			codeN[i] = term2.get(i - 1).toString();
		}
		for(int i = 1; i <= lenM; i++)
		{
			for(int j = 1; j <= lenN; j++)
			{
				if(codeM[i].equals(codeN[j]))
				{
					d[i][j] = d[i-1][j-1] + 1;
					p[i][j] = "LU";
				}
				else if(d[i-1][j] >= d[i][j-1])
				{
					d[i][j] = d[i-1][j];
					p[i][j] = "U";
				}
				else
				{
					d[i][j] = d[i][j-1];
					p[i][j] = "L";
				}
			}
		}
		ArrayList<Integer> lcsM = new ArrayList<Integer>();
		ArrayList<Integer> lcsN = new ArrayList<Integer>();
		printLCS(p, lenM, lenN, lcsM, lcsN);
		
		double common = lcsM.size() * 2.0 / (lenM + lenN);
		return common;
	}
	
	public static boolean isRelative(ArrayList<String> term1, ArrayList<String> term2)
	{
		int lenM = term1.size(), lenN = term2.size();
		int[][] d = new int[lenM+1][lenN+1];
		String[] codeM = new String[lenM+1];
		String[] codeN = new String[lenN+1];
		String[][] p = new String[lenM+1][lenN+1];
		d[0][0] = 0;
		for(int i = 1; i <= lenM; i++)
		{
			d[i][0] = 0;
			codeM[i] = term1.get(i-1).toString();
		}
		for(int i = 1; i <= lenN; i++)
		{
			d[0][i] = 0;
			codeN[i] = term2.get(i - 1).toString();
		}
		for(int i = 1; i <= lenM; i++)
		{
			for(int j = 1; j <= lenN; j++)
			{
				if(codeM[i].equals(codeN[j]))
				{
					d[i][j] = d[i-1][j-1] + 1;
					p[i][j] = "LU";
				}
				else if(d[i-1][j] >= d[i][j-1])
				{
					d[i][j] = d[i-1][j];
					p[i][j] = "U";
				}
				else
				{
					d[i][j] = d[i][j-1];
					p[i][j] = "L";
				}
			}
		}
		ArrayList<Integer> lcsM = new ArrayList<Integer>();
		ArrayList<Integer> lcsN = new ArrayList<Integer>();
		printLCS(p, lenM, lenN, lcsM, lcsN);
		
		double common = lcsM.size() * 2.0 / (lenM + lenN);
		/*double parallel = 1;
		if(!lcsM.isEmpty())
		{
			int imparallel = Math.abs(lcsM.get(0) - lcsN.get(0));
			for(int i = 1; i < lcsM.size(); i++)
				imparallel += Math.abs((lcsM.get(i) - lcsM.get(i-1)) - (lcsN.get(i) - lcsN.get(i-1)));
			imparallel += Math.abs((lenM - lcsM.get(lcsM.size()-1)) - (lenN - lcsN.get(lcsN.size()-1)));
			parallel = 1 - imparallel*1.0/(lcsM.size()+1);
		}*/
		//return (common + parallel) / 2 >= 0.75;
		return common >= 0.5;
	}
	private static void printLCS(String[][] p, int lenM, int lenN, ArrayList<Integer> lcsM, ArrayList<Integer> lcsN)
	{
		int i = lenM, j = lenN;
		while(i > 0 && j > 0)
		{
			if(p[i][j].equals("LU"))
			{
				lcsM.add(0, i-1);
				lcsN.add(0, j-1);
				i--; j--;
			}
			else if(p[i][j].equals("U"))
				i--;
			else
				j--;
		}
	}
	public static boolean isRelative(String method1, String method2)
	{
		String[] terms1 = method1.split("\\.");
		String[] terms2 = method2.split("\\.");
		if((terms1[1].startsWith("get") && terms2[1].startsWith("set")) || 
				(terms1[1].startsWith("set") && terms2[1].startsWith("get")))
			return false;
		/*if(terms1[1].equals(terms2[1]) && 
				!terms1[1].equals("equals") && !terms1[1].equals("toString") && !terms1[1].equals("getName") && 
				!terms1[1].equals("write") && !terms1[1].equals("writeln"))
			return true;*/
		if((terms1[1].startsWith("get") && terms2[1].startsWith("get")))
		{
			if(terms1[1].length() > 3)
				terms1[1] = terms1[1].substring(3);
			if(terms2[1].length() > 3)
				terms2[1] = terms2[1].substring(3);
		}
		else if((terms1[1].startsWith("set") && terms2[1].startsWith("set")))
		{
			if(terms1[1].length() > 3)
				terms1[1] = terms1[1].substring(3);
			if(terms2[1].length() > 3)
				terms2[1] = terms2[1].substring(3);
		}
		/*if(terms1[0].equals(terms2[0]))
		{
			if(terms1[1].equals(terms2[1]))
				return true;
			else 
				return false;
		}*/
		
		return isRelative(serialize(terms1[0]), serialize(terms2[0])) && isRelative(serialize(terms1[1]), serialize(terms2[1]));
	}
	public static int[] getPrefix(ArrayList<String> name1, ArrayList<String> name2)
	{
		int[] prefix = {0, 0};
		prefix[1] = name1.size();
		if(prefix[1] < name2.size())
			prefix[1] = name2.size();
		for(int i = 0; i < prefix[1]; i++)
			if(isRelative(serialize(name1.get(i)), serialize(name2.get(i))))
				prefix[0]++;
			else
				break;
		
		return prefix;
	}
	public static HashMap<String, Integer> groupNames(ArrayList<String> names)
	{
		ArrayList<HashSet<String>> groups = new ArrayList<HashSet<String>>();
		while(!names.isEmpty())
		{
			String name = names.get(0);
			HashSet<String> group = new HashSet<String>();
			group.add(name);
			names.remove(0);
			int i = 0;
			while(i < names.size())
			{
				String name1 = names.get(i);
				boolean joinable = false;
				for(String name2 : group)
					if(Util.isRelative(name1, name2))
					{
						group.add(name1);
						names.remove(i);
						joinable = true;
						break;
					}
				if(!joinable)
					i++;
			}
			groups.add(group);
		}
		HashMap<String, Integer> ids = new HashMap<String, Integer>();
		for(int id = 0; id < groups.size(); id++)
		{
			HashSet<String> group = groups.get(id);
			for(String name : group)
				ids.put(name, id);
			File dir = new File("C:\\temp\\groups");
			if (!dir.exists()) dir.mkdirs();
			try {
				BufferedWriter fout = new BufferedWriter(new FileWriter(dir.getAbsoluteFile() + "\\" + String.format("%05d", id) + ".txt"));
				for (String name : group) {			
			 		fout.write(name + "\r\n");
				}
				fout.flush();
				fout.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ids;
	}
	public static HashMap<String, HashMap<String, String>> getVersionMap(File file)
	{
		HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String,String>>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
	    	String line = in.readLine();
	    	String[] apis = line.toLowerCase().split("\t");
	    	while ((line = in.readLine()) != null) { 
	    		String[] api = line.toLowerCase().split("\t");
	    		HashMap<String, String> version = new HashMap<String, String>();
	    		for(int i = 1; i < api.length; i++)
	    			if(!api[i].contains("x") && !api[i].contains("?"))
	    				version.put(apis[i], api[i]);
	    		map.put(api[0], version);
	    	}
	    	in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	public static HashSet<String> getAPIChanges(File file)
	{
		System.err.println(file.listFiles().length);
		HashSet<String> changes = new HashSet<String>();
		for(File version : file.listFiles())
			if(version.isDirectory())
			{
				for(File api : version.listFiles())
					if(api.isDirectory())
						changes.add(api.getName());
			}
		
		return changes;
	}
	public static HashSet<String> getAPIChangesbyVersion(File file)
	{
		HashSet<String> changes = new HashSet<String>();
		//System.err.println(file.getAbsolutePath());
		for(File api : file.listFiles())
			if(api.isDirectory())
				changes.add(api.getName());
		
		return changes;
	}
	public static ArrayList<String> getVersions(File file)
	{
		ArrayList<String> versions = new ArrayList<String>();
		try {
			Scanner scanner = new Scanner(file);
			while(scanner.hasNextLine())
			{
				String version = scanner.nextLine();
				version = version.trim().toLowerCase();
				if(!version.isEmpty())
					versions.add(version);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return versions;
	}
	/*public static HashSet<String> getAPIChanges(File file)
	{
		HashSet<String> changes = new HashSet<String>();
		if(file.isDirectory())
		{
			for(File sub : file.listFiles())
				changes.addAll(getAPIChanges(sub));
		}
		else
		{
			try {
				Scanner scanner = new Scanner(file);
				while(scanner.hasNextLine())
				{
					String line = scanner.nextLine().trim();
					if(!line.isEmpty())
						changes.add(line);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return changes;
	}*/
	public static double computeLCS(ArrayList<Integer> term1, ArrayList<Integer> term2)
	{
		int lenM = term1.size(), lenN = term2.size();
		int[][] d = new int[lenM+1][lenN+1];
		int[] codeM = new int[lenM+1];
		int[] codeN = new int[lenN+1];
		String[][] p = new String[lenM+1][lenN+1];
		d[0][0] = 0;
		for(int i = 1; i <= lenM; i++)
		{
			d[i][0] = 0;
			codeM[i] = term1.get(i-1);
		}
		for(int i = 1; i <= lenN; i++)
		{
			d[0][i] = 0;
			codeN[i] = term2.get(i-1);
		}
		for(int i = 1; i <= lenM; i++)
		{
			for(int j = 1; j <= lenN; j++)
			{
				if(codeM[i] == (codeN[j]))
				{
					d[i][j] = d[i-1][j-1] + 1;
					p[i][j] = "LU";
				}
				else if(d[i-1][j] >= d[i][j-1])
				{
					d[i][j] = d[i-1][j];
					p[i][j] = "U";
				}
				else
				{
					d[i][j] = d[i][j-1];
					p[i][j] = "L";
				}
			}
		}
		ArrayList<Integer> lcsM = new ArrayList<Integer>();
		ArrayList<Integer> lcsN = new ArrayList<Integer>();
		printLCS(p, lenM, lenN, lcsM, lcsN);
		
		return lcsM.size()*2.0 / (lenM+lenN);
	}
	public static ArrayList<String[]> maxStringMatch(ArrayList<String> l1, ArrayList<String> l2)
	{
		ArrayList<String[]> pairs = new ArrayList<String[]>();
		for(String n2 : l2)
		{
			String s2 = getSimpleFileName(n2);
			int prefixSim = 2, suffixSim = 0;
			double ratio = 0;
			String mapped = "";
			for(String n1 : l1)
			{
				String s1 = getSimpleFileName(n1);
				if(s1.equals(s2))
				{
					mapped = n1;
					break;
				}
				int ps = getLCPrefix(s1, s2);
				if(ps > prefixSim)
				{
					prefixSim = ps;
					suffixSim = getLCSuffix(n1.substring(0, n1.length()-s1.length()), n2.substring(0, n2.length()-s2.length()));
					ratio = ps*2.0 / (s1.length()+s2.length());
					mapped = n1;
				}
				else if(ps == prefixSim)
				{
					int ss = getLCSuffix(n1.substring(0, n1.length()-s1.length()), n2.substring(0, n2.length()-s2.length()));
					if(ss > suffixSim)
					{
						suffixSim = ss;
						ratio = ps*2.0 / (s1.length()+s2.length());
						mapped = n1;
					}
					else if (ss == suffixSim)
					{
						double r = ps*2.0 / (s1.length()+s2.length());
						if(r > ratio)
						{
							ratio = r;
							mapped = n1;
						}
					}
				}
			}
			if(!mapped.isEmpty())
			{
				l1.remove(mapped);
				String[] pair = new String[2];
				pair[0] = mapped;
				pair[1] = n2;
				pairs.add(pair);
			}
		}
		
		return pairs;
	}
	public static int getLCPrefix(String s1, String s2)
	{
		int i = 0;
		while(i < s1.length() && i < s2.length() && s1.charAt(i) == s2.charAt(i))
			i++;
		
		return i;
	}
	public static int getLCSuffix(String s1, String s2)
	{
		int i = 0, l1 = s1.length(), l2 = s2.length();
		while(i < l1 && i < l2 && s1.charAt(l1-1-i) == s2.charAt(l2-1-i))
			i++;
		
		return i;
	}
}

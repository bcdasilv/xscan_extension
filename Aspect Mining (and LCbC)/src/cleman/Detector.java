/**
 * 
 */
package cleman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author hoan
 *
 */
public class Detector {
	public static int numOfHash = 32, numOfDim = 4, winSize = 4, minFragmentSize = 20, mergeWindow = 3;
	public static double threshold = 0.15;
	public static boolean modeIncremental = false;
	
	public Detector()
	{
		
	}
	public HashMap<String, HashSet<String>> detect(String projectName, String dataDir) throws JavaModelException
	{
		Runtime rt = Runtime.getRuntime();
		long initMem = rt.freeMemory();
		long startTime = System.currentTimeMillis();
		long stopTime = System.currentTimeMillis();
		// Get the root of the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject project = root.getProject(projectName);
		IJavaProject javaProject = JavaCore.create(project);
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for (IPackageFragment mypackage : packages) {
			// Package fragments include all packages in the classpath
			// We will only look at the package from the source folder
			// K_BINARY would include also included JARS, e.g. rt.jar
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				System.out.println("Package " + mypackage.getElementName());
				for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
					SourceFile sf = new SourceFile(unit);
					sf.fileSize = unit.getSource().length();
					SourceFile.all.put(sf.fileLocation, sf);
				}
			}
		}
		String applicationPath = project.getLocation().toString();
		System.out.println(applicationPath);
		
		loadData(dataDir);
		System.out.println(Hash.aGaussian[0][0].length);
		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");
		Date date = new Date();
		String time = dateFormat.format(date);
		File logFile = new File(applicationPath + "\\_clemanX_log_" + time + "_" + 
				(1-threshold) + "_" + numOfHash + "_" + numOfDim + "_" + winSize + "_" + minFragmentSize + "_" + mergeWindow + ".txt");
		try{
			if (!logFile.exists()) logFile.createNewFile();
			System.setOut(new PrintStream(logFile));
		}
		catch (IOException e){
			e.printStackTrace();
		}
		
		System.out.println(applicationPath);
		System.out.println("Similarity: " + (1-threshold));
		System.out.println("Number of hash functions: " + numOfHash);
		System.out.println("Number of dimensions: " + numOfDim);
		System.out.println("Window size: " + winSize);
		System.out.println("Minimum fragment size: " + minFragmentSize);
		System.out.println("Merge window size: " + mergeWindow);
		
		HashSet<Fragment> fragments = new HashSet<Fragment>();
		for(SourceFile sf : SourceFile.all.values())
			fragments.addAll(sf.fragments);
		System.out.println(SourceFile.all.size() + "\t" + " files");
		System.out.println(fragments.size() + "\t" + " fragments");
		stopTime = System.currentTimeMillis();
		System.out.println((stopTime - startTime)/1000.0 + "s"); 

		/*for(Fragment f : Fragment.all.values())
			System.out.println(f);*/
		
		//Hash.init(numOfHash, numOfDim, Fragment.gram2Index.size());
		Hash.reset(numOfHash, numOfDim, Fragment.gram2Index.size());
		Hash.setWSize(winSize);
		
		//Bucket.all = Bucket.map(new HashSet<Fragment>(Fragment.all.values()));
		HashMap<Integer, Bucket> buckets = Bucket.map(fragments);
		System.out.println(buckets.size() + " buckets");
		stopTime = System.currentTimeMillis();
		System.out.println((stopTime - startTime)/1000.0 + "s"); 
		
		HashMap<String, HashSet<String>> pairs = pair(new HashSet<Bucket>(buckets.values()));
		int numOfPairs = 0;
		for(String m1 : pairs.keySet())
			for(String m2 : pairs.get(m1))
			{
				System.out.println(m1 + "\t" + m2);
				numOfPairs++;
			}
		System.out.println(pairs.size() + " methods in " + numOfPairs / 2.0 + " clone pairs");
		System.err.println(pairs.size() + " methods in " + numOfPairs / 2.0 + " clone pairs");
		stopTime = System.currentTimeMillis();
		System.out.println((stopTime - startTime)/1000.0 + "s"); 
		
		
		/*Group.all = Group.crossFilter(Group.all);
		System.out.println(Group.all.size() + " group(s)");
		stopTime = System.currentTimeMillis();
		System.out.println((stopTime - startTime)/1000.0 + "s");*/
		
		System.out.println("Max memory usage: " + (rt.totalMemory() - initMem)/1024/1024 + " MBs");
		System.out.println("Done");
		
		return pairs;
	}
	public HashMap<String, HashSet<String>> pair(HashSet<Bucket> buckets)
	{
		HashMap<String, HashSet<String>> pairs = new HashMap<String, HashSet<String>>();
		
		for(Bucket b : buckets) 
		{
			ArrayList<Fragment> tmpFragments = new ArrayList<Fragment>(b.fragments);
			for(int i = 0; i < tmpFragments.size()-1; i++)
				for(int j = i+1; j < tmpFragments.size(); j++)
					if(tmpFragments.get(i).isClonedTo(tmpFragments.get(j)))
					{
						Fragment fi = tmpFragments.get(i);
						Fragment fj = tmpFragments.get(j);
						fi.getClones().add(fj);
						fj.getClones().add(fi);
						if(!fi.getMethodSignature().isEmpty() && !fj.getMethodSignature().isEmpty() && !fi.getMethodSignature().equals(fj.getMethodSignature()))
						{
							String m1 = fi.getMethodSignature(), m2 = fj.getMethodSignature();
							HashSet<String> temp = pairs.get(m1);
							if(temp == null)
								temp = new HashSet<String>();
							temp.add(m2);
							pairs.put(m1, temp);
							temp = pairs.get(m2);
							if(temp == null)
								temp = new HashSet<String>();
							temp.add(m1);
							pairs.put(m2, temp);
						}
					}
		}
		
		return pairs;
	}
	public Object loadObjectFromFile(String rFileName)
	{	   
		Object result = null;
		File data = new File(rFileName);
		if (data.exists()) {
			try{
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(data)); 			     
				result =  in.readObject();
				in.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	public void loadData(String inputDataPath)
	{
		try 
		{
			Data data = (Data)loadObjectFromFile(inputDataPath + "\\clemanX.dat");
			Hash.b = data.getB();
			Hash.ran = data.getRan();
			Hash.randGaussian = data.getRandGaussian();
			//Hash.aGaussian = data.getAGaussian();
			Hash.aGaussian = (double[][][])loadObjectFromFile(inputDataPath + "\\aGaussian.dat");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*This method will be used to write from a file*/
	public void saveObjectToFile(Object c, String wFileName)
	{
		File data = new File(wFileName);
		//To remove the previous existed file
		if(data.exists()) data.delete();
		try {
			data.createNewFile();	  
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(data));
			out.writeObject(c);
			out.flush();
			out.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
}

/**
 * 
 * @author HUNG
 * 
 */

package user.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class StringUtils {

	public static String padRight(String string, int length) {
		string = string.replace('\t', ' ');
		if (string.length() > length) {
			string = string.substring(0, length);
			if (length > 3)
				string = string.substring(0, length-3) + "...";
		}
		else
			while (string.length() < length)
				string = string + " ";
		return string;
	}
	
	public static String padLeft(String string, int length) {
		string = string.replace('\t', ' ');
		if (string.length() > length) {
			string = string.substring(0, length);
			if (length > 3)
				string = string.substring(0, length-3) + "...";
		}
		else
			while (string.length() < length)
				string = " " + string;
		return string;
	}
	
	public static String padRight(int number, int length) {
		return padRight(new Integer(number).toString(), length);
	}
	
	public static String padLeft(int number, int length) {
		return padLeft(new Integer(number).toString(), length);
	}
	
	public static String toPercentage(double number) {
		return Math.round(number * 100) + "%";
	}
	
	/*
	 * Returns LOC
	 */
	public static int getLOC(String fileContent) {
		int LOC = 0;
		BufferedReader reader = new BufferedReader(new StringReader(fileContent));     
		try {
			while (reader.readLine() != null)
				LOC++;
		} catch(IOException e) {
		}
		
		return LOC;	
	}
	
}

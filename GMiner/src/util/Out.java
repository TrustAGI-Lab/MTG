package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class Out {

	public static void println(String fileName, String str){
		PrintWriter pw;
		try {
			pw = new PrintWriter(
					new BufferedWriter(  
							new FileWriter(fileName)));
			
			pw.println(str);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		
	}
	
	public static <T> void outputDataFile(String strFileName, List<T> listData){
		PrintWriter pw=null;
		int i;

		try {
			// Open input file
			pw = new PrintWriter(
					new BufferedWriter( 
							new FileWriter(strFileName)));

			// Write data
			for (i=0;i<listData.size();i++) {
				pw.println(listData.get(i));   
			}

			// close file
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static <T> void outputDataFile(String strFileName, Set<T> set){
		PrintWriter pw=null;

		try {
			// Open input file
			pw = new PrintWriter(
					new BufferedWriter(  // here, if i don't use BufferedWriter, the output of java is error, i don't know why, JAVA BUG?
							new FileWriter(strFileName)));

			Iterator<T> it = set.iterator();
			// Write data
			while(it.hasNext()){
				pw.println(it.next());   
			}
				

			// close file
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	


}

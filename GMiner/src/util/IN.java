package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class IN {
	
	public static ArrayList<String> readToList(String input){
		ArrayList<String> list = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(input));
			String line=null;
			while((line=reader.readLine())!=null){
				list.add(line);
			}
			
			reader.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

}

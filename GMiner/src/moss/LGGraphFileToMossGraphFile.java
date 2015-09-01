package moss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class LGGraphFileToMossGraphFile {

	public static void convertFile(String input, String output) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(input));
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(output)));
		
		String line = null;
		int index = 0;
		String graphId = "";
		while((line = reader.readLine())!=null){
			
			if(line.startsWith("t #")){
				 graphId = line.split(" ")[2];
				 if(index != 0){
					 writer.print("g Graph "+graphId+"\n\n");
					// writer.println("x 0");
					 //writer.println();
				 }
			}
			else if(line.startsWith("v ")){
				String items[] = line.split(" ");
				String newLine = items[0] + " "+(Integer.parseInt(items[1])+1)+" "+items[2];
				writer.println(newLine);
			}
			else if(line.startsWith("e ")){
				String items[] = line.split(" ");
				String newLine = items[0] + " "+(Integer.parseInt(items[1])+1)+" "+(Integer.parseInt(items[2])+1)+" "+items[3];
				writer.println(newLine);
			}
			
			index++;
		}
		writer.print("g Graph "+graphId+"\n\n");
		 //writer.println("g Graph "+graphId);
		// writer.println("x 0");
		// writer.println();
		 writer.close();
		 reader.close();
	}
	public static void main(String[] args) throws IOException {
		LGGraphFileToMossGraphFile.convertFile("query.lg", "query.nel");
	}

}

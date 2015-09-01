package util;

import java.io.IOException;
import java.util.ArrayList;

import moss.Graph;
import moss.GraphReader;
import moss.NamedGraph;

public class DataSetChecking {
	
	public void checking(String file) throws IOException{
		ArrayList<Graph> graphs = GraphUtils.readGraphs(file, GraphReader.GRAPHS);
		int size = graphs.size();
		double aveNode = 0,aveEdge=0;
		int pos = 0;
		for(int i = 0; i < size; i++){
			NamedGraph g = (NamedGraph) graphs.get(i);
			aveNode += g.getNodeCount();
			aveEdge += g.getEdgeCount();
			
			if(g.getValue()==1)
				pos++;
		}
		
		aveNode /= size;
		aveEdge /= size;
		//Math.round(a)
		
		System.out.println("Databases:\t"+pos+"\t"+size+"\t"+Math.round(aveNode)+"\t"+(Math.round(aveEdge)));
		
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
/*		String files[]={"1","33","41","47","81","83","109","123","145"};
		
		for(int i = 0; i < files.length; i++){
			String id=files[i];
			String file =  "ninedataset/"+id+"total-connect.sdf";
			DataSetChecking app = new DataSetChecking();
			app.checking(file);
		}*/
		
		String file =  "ninedataset/TWITTER-Real-Graph-Partial.nel";
		DataSetChecking app = new DataSetChecking();
		app.checking(file);
		
		
	}
	
	

}

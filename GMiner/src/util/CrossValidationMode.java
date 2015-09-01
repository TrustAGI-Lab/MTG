package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import moss.Graph;
import moss.NamedGraph;

public class CrossValidationMode {
	
	Random rnd;
	
	public CrossValidationMode(){
		rnd = new Random(1);
	}
	/**
	 * create cross validation dataset, consider the class 
	 * @param graphs
	 * @param rnd
	 * @param numFold
	 * @return
	 */
	public ArrayList<ArrayList<Graph>> createBinGraphs(ArrayList<Graph> graphs, int numFold){
		
		//positive or negative
		ArrayList<Graph> posg = new ArrayList<Graph>();
		ArrayList<Graph> negg = new ArrayList<Graph>();
		for(Graph g : graphs){
			NamedGraph ng  = (NamedGraph) g;
			if(ng.getValue() == 1)
				posg.add(ng);
			else
				negg.add(ng);
		}
		
		Collections.shuffle(posg, rnd);
		Collections.shuffle(negg, rnd);
		
		ArrayList<ArrayList<Graph>> foldGraphs = new ArrayList<ArrayList<Graph>>(numFold);
		this.addrandomSample(foldGraphs, posg, numFold,rnd);
		this.addrandomSample(foldGraphs, negg, numFold,rnd);
		
		return foldGraphs;
	}
	
	public void addrandomSample(ArrayList<ArrayList<Graph>> foldGraphs, ArrayList<Graph> graphs, int numFold,Random rnd){
		int binsize = graphs.size()/numFold;
		for(int i = 0; i < numFold; i++){
			ArrayList<Graph> list = null;
			if(foldGraphs.size()!=numFold){
				list = new ArrayList<Graph>();
				foldGraphs.add(list);
			}
			else
				list = foldGraphs.get(i);
				
			
			//create its subgraphs
			for(int j = 0; j < binsize; j++){
				list.add(graphs.get(i*binsize+j));
			}
			
		}
		
		for(int k=binsize * numFold; k < graphs.size(); k++){ //there may be some graph left
			int index = rnd.nextInt(numFold);
			foldGraphs.get(index).add(graphs.get(k));
		}
	}
	
	public ArrayList<Graph> trainCV(ArrayList<ArrayList<Graph>> foldGraphs, int foldID){
		ArrayList<Graph> list = new ArrayList<Graph>();
		
		for(int i = 0; i < foldGraphs.size(); i++){
			if(foldID !=i){
				list.addAll(foldGraphs.get(i));
			}
		}
		
		return list;
	}
	
	public ArrayList<Graph> testCV(ArrayList<ArrayList<Graph>> foldGraphs, int foldID){
		
		return foldGraphs.get(foldID);
	}
	
	public double[] getLabels(ArrayList<Graph> graphs){
		double[] Y = new double[graphs.size()];

		for(int i = 0; i <Y.length; i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			Y[i] = ng.getValue();
		}
		
		return Y;
	}
	
	public void setRandom(Random random) {
		// TODO Auto-generated method stub
		this.rnd = random;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

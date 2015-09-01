package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import moss.Graph;
import moss.NamedGraph;

public class TrainTestSplit {

	public ArrayList<Graph> trainG;
	public ArrayList<Graph> testG;
	public Random random;
	public double[] trainY;
	public double[] testY;
	
	public TrainTestSplit(){
		random = new Random(1);
		trainG = new ArrayList<Graph>();
		testG = new ArrayList<Graph>();
	}
	
	public void setSeed(long seed){
		this.random.setSeed(seed);
	}
	
	public void split(ArrayList<Graph> graphs, int pencentageTrain){
		
		ArrayList<Graph> pgs = new ArrayList<Graph>();
		ArrayList<Graph> ngs = new ArrayList<Graph>();
		
		for(int i = 0; i < graphs.size(); i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			if(ng.getValue() == 1)
				pgs.add(ng);
			else
				ngs.add(ng);
		}
		
		System.out.println("Number of Pos:"+pgs.size()+"\tNeg:"+ngs.size());
		
		
		int nop = (int) (pgs.size() * pencentageTrain/100.0);
		int non = (int) (ngs.size() * pencentageTrain/100.0);
		
		Collections.shuffle(pgs, random);
		Collections.shuffle(ngs,random);
		
		for(int i = 0; i < pgs.size(); i++){
			if(i < nop)
				trainG.add(pgs.get(i));
			else
				testG.add(pgs.get(i));
		}
		
		for(int i = 0; i < ngs.size(); i++){
			if(i < non)
				trainG.add(ngs.get(i));
			else{
				testG.add(ngs.get(i));
			}
		}
	
		
		
		trainY = new double[trainG.size()];
		testY = new double[testG.size()];
		for(int i = 0; i <trainY.length; i++){
			NamedGraph ng = (NamedGraph) trainG.get(i);
			trainY[i] = ng.getValue();
		}
		
		for(int i = 0; i <testY.length; i++){
			NamedGraph ng = (NamedGraph) testG.get(i);
			testY[i] = ng.getValue();
		}
		
		
	}
	
	public void splitbyNo(ArrayList<Graph> graphs, int totalNumber) throws Exception{
		
		int len = graphs.size();
		if(totalNumber > len){
			throw new Exception("Error, incorrect number of graphs!");
		}
		
		ArrayList<Graph> pgs = new ArrayList<Graph>();
		ArrayList<Graph> ngs = new ArrayList<Graph>();
		
		for(int i = 0; i < graphs.size(); i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			if(ng.getValue() == 1)
				pgs.add(ng);
			else
				ngs.add(ng);
		}
		
		System.out.println("Number of Pos:"+pgs.size()+"\tNeg:"+ngs.size());
		
		
		int nop = totalNumber/2;
		int non = totalNumber - nop;
		
		Collections.shuffle(pgs, random);
		Collections.shuffle(ngs,random);
		
		for(int i = 0; i < pgs.size(); i++){
			if(i < nop)
				trainG.add(pgs.get(i));
			else
				testG.add(pgs.get(i));
		}
		
		for(int i = 0; i < ngs.size(); i++){
			if(i < non)
				trainG.add(ngs.get(i));
			else{
				testG.add(ngs.get(i));
			}
		}
	
		
		
		trainY = new double[trainG.size()];
		testY = new double[testG.size()];
		for(int i = 0; i <trainY.length; i++){
			NamedGraph ng = (NamedGraph) trainG.get(i);
			trainY[i] = ng.getValue();
		}
		
		for(int i = 0; i <testY.length; i++){
			NamedGraph ng = (NamedGraph) testG.get(i);
			testY[i] = ng.getValue();
		}
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

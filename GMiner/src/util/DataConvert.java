package util;

import java.io.IOException;
import java.util.ArrayList;

import moss.AtomTypeMgr;
import moss.BondTypeMgr;
import moss.Embedding;
import moss.FrequentGraph;
import moss.Graph;
import moss.NamedGraph;
import moss.TypeMgr;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.matrix.Matrix;

public class DataConvert {

	public int []masks;
	public boolean warning=true;;
	public static int GeneralGraphs=1; //dblp or others graph with .nel format
	public static int Chemical =2;
	public static int datatype = Chemical;
	public DataConvert(){
	    masks    = new int[4];        
	    
	    if(datatype==Chemical){
	    	masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;//TypeMgr.BASEMASK; //Node.TYPEMASK;
	    	masks[1] = masks[3] = BondTypeMgr.BONDMASK;//TypeMgr.BASEMASK;//Edge.TYPEMASK;
	    }else{
	    
			masks[0] = masks[2] = TypeMgr.BASEMASK;
			masks[1] = masks[3] = TypeMgr.BASEMASK;

	    }
	}
	
	public void setMarks(int datatype){
		if(datatype == Chemical){
			masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
			masks[1] = masks[3] = BondTypeMgr.BONDMASK;
		}
		else{
			masks[0] = masks[2] = TypeMgr.BASEMASK;
			masks[1] = masks[3] = TypeMgr.BASEMASK;
		}
	}
	
	public  Instances createPredefinedKernelInstances(Instances data) throws IOException{
		
		Instances ins = new Instances(getStructurePreMatrix(data.numInstances()),0);
		data.setClassIndex(data.numAttributes()-1);
		double	row[];
		for(int i = 0; i < data.numInstances(); i++){
			row = new double[2];
			row[0]= i;
			row[1]=data.get(i).classValue();
			ins.add(new DenseInstance(1,row));
		}
		
		return ins;
	}
	
	/**
	 * Return as two part of dataset, instance[0]: labeled data, instances[1] unlabeled data
	 * @param graphs
	 * @param feature
	 * @param weights if use the instance weight other than 0 or 1
	 * @return
	 * @throws IOException
	 */
	public Instances[] graphToWekaInstances(ArrayList<Graph> graphs, ArrayList<FrequentGraph> feature, String classLabels[]) throws IOException{
		
		if(feature.size()==0){
			try {
				throw new  Exception("Feature set is NULL!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for(Graph g : graphs){
			if(g.getEdgeCount()<1){
				System.err.println(GraphUtils.notation.describe(g));
				System.err.println("graph has no edge!");
				System.exit(0);
			}
			if(!g.isConnected()){
				System.err.print("graph is not connected!");
				System.exit(0);
			}
			g.decode();
			g.maskTypes(masks);
			g.prepare();
		}
		
		double featureVector[][] = createFeatureVector(graphs,feature);
		Matrix x = new Matrix(featureVector);
		System.out.println("Length:"+featureVector.length+"\tWidth:"+featureVector[0].length);
		//x=x.transpose();
		
		
		//double	row[];
		int numAtt = featureVector.length;
		int numIns = featureVector[0].length;
		//System.out.println("Num instance:"+numIns);
		//System.out.println("Num Att:"+numAtt);
		Instances[] data = new Instances[2]; 
		Instances structure = getStructure(numAtt,classLabels);
		//System.out.println(structure);
		
		data[0]=new Instances(structure,0);
		data[1]=new Instances(structure,0);
		for(int i = 0; i < numIns; i++){
			
			
			NamedGraph ng = (NamedGraph) graphs.get(i);
			
			double []row = new double[numAtt+1];
			for(int j = 0; j < numAtt; j++){
				row[j]=featureVector[j][i];
			}
			
			//System.out.println("row "+i+": "+Utils.arrayToString(row));
			
			double cl = ng.getValue();//==1?0:1;
			//System.out.println("cl:"+cl);
			//System.out.println(data[0].attribute(row.length-1).indexOfValue(""+(int) cl));
			row[row.length-1]=data[0].attribute(row.length-1).indexOfValue(""+(int) cl);
			//System.out.println("Row "+i+" :"+Utils.arrayToString(row));
			
			Instance instance = new DenseInstance(ng.getWeight(),row);
			
			if(ng.getLabel()){ 
				data[0].add(instance);
			
			}
			else  {
				data[1].add(instance);
			}
			//System.out.println("inst 1 "+i+": "+instance);
			
		}
		
		return data;
	}
	
	public Instances[] graphToWekaInstances1(ArrayList<Graph> graphs, ArrayList<FrequentGraph> feature, String classLabels[]) throws IOException{
		
		if(feature.size()==0){
			try {
				throw new  Exception("Feature set is NULL!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for(Graph g : graphs){
			if(g.getEdgeCount()<1){
				System.err.println(GraphUtils.notation.describe(g));
				System.err.println("graph has no edge!");
				System.exit(0);
			}
			if(!g.isConnected()){
				System.err.print("graph is not connected!");
				System.exit(0);
			}
			g.decode();
			g.maskTypes(masks);
			g.prepare();
		}
		
		double featureVector[][] = createFeatureVector(graphs,feature);
		Matrix x = new Matrix(featureVector);
		System.out.println("Length:"+featureVector.length+"\tWidth:"+featureVector[0].length);
		//x=x.transpose();
		
		
		//double	row[];
		int numAtt = featureVector.length;
		int numIns = featureVector[0].length;
		//System.out.println("Num instance:"+numIns);
		//System.out.println("Num Att:"+numAtt);
		Instances[] data = new Instances[2]; 
		Instances structure = getStructure(numAtt,classLabels);
		System.out.println(structure);
		
		data[0]=new Instances(structure,0);
		data[1]=new Instances(structure,0);
		for(int i = 0; i < numIns; i++){
			
			
			NamedGraph ng = (NamedGraph) graphs.get(i);
			
			double []row = new double[numAtt+1];
			for(int j = 0; j < numAtt; j++){
				row[j]=featureVector[j][i];
			}
			
			//System.out.println("row "+i+": "+Utils.arrayToString(row));
			
			double cl = ng.getValue();//==1?0:1;
		//	System.out.println(cl);
			//System.out.println(data[0].attribute(row.length-1).indexOfValue(""+(int) cl));
			row[row.length-1]=data[0].attribute(row.length-1).indexOfValue(""+(int) cl);
			//System.out.println("Row "+i+" :"+Utils.arrayToString(row));
			
			Instance instance = new DenseInstance(ng.getWeight(),row);
			
			if(ng.getLabel()){ 
				data[0].add(instance);
			
			}
			else  {
				data[1].add(instance);
			}
			//System.out.println("inst 1 "+i+": "+instance);
			//System.out.println("inst 2 "+i+": "+data[0].instance(i));
		}
		
		return data;
	}
	
	public Instances[] graphToWekaInstancesTFIDF(ArrayList<Graph> graphs, ArrayList<FrequentGraph> feature, String classLabels[]) throws IOException{
		
		if(feature.size()==0){
			try {
				throw new  Exception("Feature set is NULL!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for(Graph g : graphs){
			g.decode();
			g.maskTypes(masks);
			g.prepare();
		}
		
		
		double featureVector[][] = new double[feature.size()][graphs.size()];
		
		for(int i = 0; i < feature.size(); i++){
			
			featureVector[i]=subgraphInDataVectorTFIDF(feature.get(i), graphs);
			
			

		}
		
		
		
		double	row[];
		int numAtt = featureVector.length;
		int numIns = featureVector[0].length;
		//System.out.println("Num instance:"+numIns);
		//System.out.println("Num Att:"+numAtt);
		Instances[] data = new Instances[2]; 
		Instances structure = getStructure(numAtt,classLabels);
		data[0]=new Instances(structure,0);
		data[1]=new Instances(structure,0);
		for(int i = 0; i < numIns; i++){
			
			NamedGraph ng = (NamedGraph) graphs.get(i);
			
			row = new double[numAtt+1];
			for(int j = 0; j < numAtt; j++){
				row[j]=featureVector[j][i];
			}
			

			double cl = ng.getValue();//==1?0:1;
			//	System.out.println(cl);
				//System.out.println(data[0].attribute(row.length-1).indexOfValue(""+(int) cl));
			row[row.length-1]=data[0].attribute(row.length-1).indexOfValue(""+(int) cl);
				
			
			Instance instance = new DenseInstance(ng.getWeight(),row);
			if(ng.getLabel()) data[0].add(instance);
			else  data[1].add(instance);
				
		}
		
		return data;
	}
	
	public Instances getStructure(int numAtt,String classLabeles[]) throws IOException {
	    ArrayList<Attribute>		atts;
	    int			i;
	    
		// generate header for nominal attribute
		atts = new ArrayList<Attribute>(numAtt);
/*		for (i = 0; i < numAtt; i++){
			ArrayList<String> c = new ArrayList<String>();		
			c.add("0");
			c.add("1");
			Attribute att = new Attribute("att_"+i, c);
			
			atts.add(att);
		}*/
		
		//attribute for adding numeric attribute
		for (i = 0; i < numAtt; i++){
			
			atts.add(new Attribute("att_" + (i+1)));
		}
		
		
		ArrayList<String> cl = new ArrayList<String>();	
		for(int x = 0; x < classLabeles.length; x++){
			cl.add(classLabeles[x]);
		}
		Attribute classAtt = new Attribute("Class", cl);
		
		atts.add(classAtt);
		
		
		Instances m_structure = new Instances("Graph Data Set", atts, 0);
		m_structure.setClassIndex(m_structure.numAttributes() - 1);
		
		return m_structure;
	}
	
	public Instances getStructurePreMatrix(int numInstance) throws IOException {
	    ArrayList<Attribute>		atts;
	    
		// generate header
		atts = new ArrayList<Attribute>(2);
		ArrayList<String> na = new ArrayList<String>();
		for(int j = 0; j < numInstance; j++){
			na.add(""+j);
		}
			
		
		atts.add(new Attribute("Att",na));
		
		ArrayList<String> cl = new ArrayList<String>();
		
		cl.add("Active");
		cl.add("Inactive");
		Attribute classAtt = new Attribute("Class", cl);
		
		atts.add(classAtt);
		
		Instances m_structure = new Instances("Graph Data Set", atts, 0);
		m_structure.setClassIndex(m_structure.numAttributes() - 1);
		
		return m_structure;
	}
	
	/**
	 * 
	 * @param graphs
	 * @param feature
	 * @return a maxix of M * N; M:feature size, N:graph size
	 */
	public   double[][] createFeatureVector(ArrayList<Graph> graphs, ArrayList<FrequentGraph> feature) {
		double vector[][] = new double[feature.size()][graphs.size()];
		
		for(int i = 0; i < feature.size(); i++){
			Graph sub = (Graph) feature.get(i).getGraph().clone();
			vector[i]=subgraphInDataVector(sub, graphs);
			
			int fre = (int) feature.get(i).getSupport();
			int cal =(int) sumOf(vector[i]);
			
			if(this.warning){
				if(fre!=cal){
					System.err.println(i);
					System.out.println("SSUUPPOT:"+fre+"\tComput:"+cal);
	/*				try {
						throw new Exception("SSUUPPOT:"+fre+"\tComput:"+cal);
					} catch (Exception e) {
						e.printStackTrace();
					}*/
				}
			}
			

		}
		
		
		return vector;
	}
	
	private double[] subgraphInDataVectorTFIDF(FrequentGraph frag, ArrayList<Graph> graphs){
		double[] exist = new double[graphs.size()];
		Graph sub = frag.getGraph();
		sub.decode();
    
	    sub.maskTypes(masks); 
	    sub.prepareEmbed();
	    
	    int fre = 0;
		for(int i = 0; i < exist.length; i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
/*			ng.decode();
			ng.maskTypes(masks);
			ng.prepare();*/
			Embedding em = ng.embed(sub);
			if(em!=null){
				fre++;
			}
			//term frequency
			double term_fre = em==null? 0 : (1+Math.log(em.size()));
			
			//document frequency
			double doc_fre = Math.log(graphs.size()*1.0/frag.getSupport());
			
			
			exist[i]=ng.contains(sub)?1:0;
			exist[i] = term_fre * doc_fre;
		}
		if(fre!=frag.getSupport())
			System.err.println("Error:DataConvert::subgraphInDataVectorTFIDF");
		
		return exist;
	}
	
	private double[] subgraphInDataVector(Graph sub, ArrayList<Graph> graphs){
		double[] exist = new double[graphs.size()];
		sub.decode();
    
	    sub.maskTypes(masks); 
	    sub.prepareEmbed();
	    
		for(int i = 0; i < exist.length; i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
/*			ng.decode();
			ng.maskTypes(masks);
			ng.prepare();*/
			
			exist[i]=ng.contains(sub)?1:0;
		}
		
		return exist;
	}
	
	private double sumOf(double a[]){
		int s = 0;
		for(double v : a){
			s+=v;
		}
		return s;
	}

	public void writeInstanceToFile(String file, Instances data){
		
	}

	//compute pearson correlation, the array element is either 0 OR 1
	public double PearsonCorrelation(double a[], double b[]){
		
		int size = a.length;
		double AB = 0, A=0, B=0;
		
		for(int i = 0; i <a.length; i++){
			if(a[i] ==1 ){
				A++;
			}
			
			if(b[i] ==1 ){
				B++;
			}
			
			if(a[i]==1 && b[i] ==1 ){
				AB++;
			}
		}
		
		double correlation = (size*AB-A*B)/(Math.sqrt(A*(size-A)*B*(size-B)));
		return correlation;
		
	}


}

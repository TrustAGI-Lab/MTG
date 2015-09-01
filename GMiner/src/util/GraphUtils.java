package util;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import moss.AtomTypeMgr;
import moss.BondTypeMgr;
import moss.Embedding;
import moss.FrequentGraph;
import moss.FrequentSubMiner;
import moss.Graph;
import moss.GraphReader;
import moss.GraphWriter;
import moss.NamedGraph;
import moss.Node;
import moss.Notation;
import moss.Recoder;
import moss.SMILES;
import moss.TypeMgr;
import weka.core.Utils;

public class GraphUtils {
	public static int SPECTRAL=1;
	public static int KERNEL=2;
	
	public static Notation notation;
	
	public static int masks[]=new int[4];
	
	public static void testHelloWord(){
		System.out.println("Hello,world");
	}

	public static void setGraphType(int type){
		//int []masks    = new int[4];  
		if(type==DataConvert.GeneralGraphs){
			masks[0] = masks[2] = TypeMgr.BASEMASK;
			masks[1] = masks[3] = TypeMgr.BASEMASK;
		}
		else{
	    	masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;//TypeMgr.BASEMASK; //Node.TYPEMASK;
	    	masks[1] = masks[3] = BondTypeMgr.BONDMASK;
		}
	}
	
	public static ArrayList<Graph> readGraphs(String file, int mod) throws IOException{
		String format = file.substring(file.lastIndexOf(".")+1);
		ArrayList<Graph>  graphs = new ArrayList<Graph>();
		GraphReader reader = GraphReader.createReader(new FileReader(file),mod, format);
		if(notation == null)
			notation = reader.getNotation();
		else{
			reader.settNotation(notation);
		}
		while(reader.readGraph()){
			
			float value = reader.getValue();
			boolean isLabel = reader.getIsLabel();
			
			NamedGraph graph = new NamedGraph(reader.getGraph(),
					reader.getName(), value, 0, isLabel);
			if(graph.isConnected())
				graphs.add(graph);
		}
		
		reader.close();
		
		//setting the graphs types, which is important for subgraph isomorphism test  for graph classification
		if(format.equalsIgnoreCase("nel")){ //General graphs
			DataConvert.datatype=DataConvert.GeneralGraphs;
		}else{//chemical compounds as graphs
			DataConvert.datatype=DataConvert.Chemical;
		}
		
		GraphUtils.setGraphType(DataConvert.datatype);

		
		
		return graphs;
	}
	
	public static void writeGraphs(String output, List<Graph> graphs, int writemod) throws IOException{
		String format = output.substring(output.lastIndexOf(".")+1);
		GraphWriter writer = GraphWriter.createWriter(new FileWriter(output),writemod, format);
		for(Graph g : graphs){
			NamedGraph ng = (NamedGraph) g;
			writer.setName(ng.getName());
			writer.setValue(ng.getValue());
			writer.setGraph(ng);
			writer.setIsLabel(ng.getLabel());
			writer.writeGraph();
			writer.flush();
		}
		
	}
	
	public static void writeGraphs(String output, ArrayList<Graph> graphs, int writemod) throws IOException{
		String format = output.substring(output.lastIndexOf(".")+1);
		GraphWriter writer = GraphWriter.createWriter(new FileWriter(output),writemod, format);
		for(Graph g : graphs){
			NamedGraph ng = (NamedGraph) g;
			writer.setName(ng.getName());
			writer.setValue(ng.getValue());
			writer.setGraph(ng);
			writer.setIsLabel(ng.getLabel());
			writer.writeGraph();
			writer.flush();
		}

	}
	
	
	
	public static void writeSubgraphsAsSmiles(ArrayList<Graph> graphs,ArrayList<Double> scores, String fileName){
		ArrayList<String> strs = new ArrayList<String>();
		for(Graph g : graphs){
			strs.add(new SMILES().describe(g));
		}
		
		String strscore = "";
		for(Double d : scores){
			strscore += ""+d+"\t";
		}
		
		strs.add(strscore);
		Out.outputDataFile(fileName, strs);
	}
	
	public static void writeSubgraphsAsSmiles(ArrayList<FrequentGraph> graphs,boolean selectedIndex[], String fileName){
		ArrayList<String> strs = new ArrayList<String>();
		for(int i = 0; i < selectedIndex.length; i++){
			if(selectedIndex[i]){
				FrequentGraph g = graphs.get(i);
				strs.add(new SMILES().describe(g.getGraph()));

			}
		}
		
		System.out.println("Number of subgraphs from matlab:"+strs.size());
		Out.outputDataFile(fileName, strs);
	}
	
	public static void writeSubgraphsFromStumpsAsSmiles(ArrayList<DecisionStump> graphs,boolean selectedIndex[], String fileName){
		ArrayList<String> strs = new ArrayList<String>();
		for(int i = 0; i < selectedIndex.length; i++){
			if(selectedIndex[i]){
				DecisionStump g = graphs.get(i);
				strs.add(new SMILES().describe(g.getGraphFeature()));

			}
		}
		
		System.out.println("Number of subgraphs from matlab:"+strs.size());
		Out.outputDataFile(fileName, strs);
	}
	
	public static void writeFrequentSubgraphsAsSmiles(ArrayList<FrequentGraph> graphs,String fileName){
		ArrayList<String> strs = new ArrayList<String>();
		for(FrequentGraph g : graphs){
			strs.add(new SMILES().describe(g.getGraph()));
		}
		
		
		Out.outputDataFile(fileName, strs);
	}
	
	public static ArrayList getTopKSubGraphs(ArrayList<Graph> graphs,double []values,int K){

		
		SortItem sort = new SortItem();
		sort.sort(graphs, values);
		
		return sort.getTopKItem(K);
		
		
		
	}
	
	public static double[] getClassLabels(ArrayList<Graph> graphs){
		double y[] = new double[graphs.size()];
		for(int i = 0; i < graphs.size(); i++){
			
			NamedGraph ng = (NamedGraph) graphs.get(i);
			
			y[i] = ng.getValue();
		}
		return y;
	}
	
	
	public static ArrayList getTopKScoresOnTest(ArrayList<Graph> subs, double []values,int K, ArrayList<Graph> testGraphs){

		
		SortItem sort = new SortItem();
		sort.sort(subs, values);
		
		ArrayList graphs = sort.getTopKItem(K);
		
		ArrayList<Double> accs = new ArrayList<Double> ();
		for(int i = 0; i<graphs.size(); i++){
			Graph g = (Graph) graphs.get(i);
			double acc = getAccOnTest(g,testGraphs);
			accs.add(acc);
		}
		
		System.out.println(accs);
		return accs;
		
	}
	
	private static double getAccOnTest(Graph sub, ArrayList<Graph> graphs){
		double supp = 0;
		double cor = 0;
		for(Graph ng: graphs){
			
			NamedGraph graph = new NamedGraph((NamedGraph) ng);
			double value = graph.getValue();
			
			
			double classvalue = -1;
			if(GraphUtils.subgraphIsomorphism(sub, ng)){
				classvalue = 1;
			}
			
			if(value == classvalue){
				cor ++;
			}
			
		}
		double a = cor/graphs.size();
		double b = 1-a;
		
		return Math.max(a, b);
	}
	
	public static void writeGraphs(String output, List<Graph> graphs, int writemod, Notation ntn) throws IOException{
		String format = output.substring(output.lastIndexOf(".")+1);
		GraphWriter writer = GraphWriter.createWriter(new FileWriter(output),writemod, format);
		writer.setNotation(ntn);
		for(Graph g : graphs){
			writer.setGraph(g);
			if(writemod != GraphWriter.SUBS){
				NamedGraph ng = (NamedGraph) g;
				writer.setName(ng.getName());
				writer.setValue(ng.getValue());				
				writer.setIsLabel(ng.getLabel());
				writer.setValue(ng.getValue());
			}
			writer.writeGraph();
			//ntn.write(g, writer);
			writer.flush();
		}
		
	}
	/**
	 * Can only write .nel graphs
	 * @param output
	 * @param graphs
	 * @param writemod
	 * @param ntn
	 * @throws IOException
	 */
	public static void writeSubGraphs(String output, List<Graph> graphs, int writemod, Notation ntn) throws IOException{
		String format = output.substring(output.lastIndexOf(".")+1);
		GraphWriter writer = GraphWriter.createWriter(new FileWriter(output),writemod, format);
		writer.setNotation(ntn);
		for(Graph g : graphs){
			writer.setGraph(g);
			writer.writeGraph();
			//ntn.write(g, writer);
			writer.flush();
		}
		
	}
	
	public static ArrayList<FrequentGraph> getFrequentGraphs(ArrayList<Graph> graphs, double support) throws IOException{
		FrequentSubMiner miner = new FrequentSubMiner();
		return miner.getFrequentSubgraphs(graphs, support);
	}
	
	public static ArrayList<FrequentGraph> getFrequentGraphs(ArrayList<Graph> graphs, int support, int minSize) throws IOException{
		FrequentSubMiner miner = new FrequentSubMiner();
		return miner.getFrequentSubgraphs(graphs, support, minSize);
	}
	
	public static ArrayList<FrequentGraph> getFrequentGraphs(ArrayList<Graph> graphs, int support, int minSize, int maxSize) throws IOException{
		FrequentSubMiner miner = new FrequentSubMiner();
		return miner.getFrequentSubgraphs(graphs, support, minSize, maxSize);
	}
	
	
/*	public static Collection<Fragment> getFrequentSubgraphs(
			Collection<Fragment> Dq, int frequency) throws IOException {
		ArrayList<Graph> database = new ArrayList<Graph>();
		for(Fragment fra: Dq){
			NamedGraph graph = new NamedGraph(fra.getGraph());
			database.add(graph);
		}
		
		return getFrequentGraphs(database, frequency);
	}*/
	


	
	public static boolean graphIsomorphism(Graph graph, Graph query) {
		if(graph.getEdgeCount() != query.getEdgeCount() || graph.getNodeCount() != query.getNodeCount())
			return false;
		
/*		if(graph.isConnected())
			graph.prepareEmbed();
		if(query.isConnected())
			query.prepareEmbed();
		return graph.contains(query);*/
		
		return subgraphIsomorphism(graph,query);
	}

	public static boolean subgraphIsomorphism(Graph sub, Graph graph){
		if(graph.getEdgeCount() < sub.getEdgeCount() || graph.getNodeCount() < sub.getNodeCount())
			return false;
		
		
		//graph.decode();
		
/*		masks = new int[4];  init. the edge and node masks 
		masks[0] = masks[2] = Node.TYPEMASK;
		masks[1] = masks[3] = Edge.TYPEMASK;*/
		
/*	    int []masks    = new int[4];        
	    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
	    masks[1] = masks[3] = BondTypeMgr.BONDMASK;*/
		
		
		sub.decode();
	    sub.maskTypes(masks); 
	    sub.prepareEmbed();
	    
		graph.decode();
		graph.maskTypes(masks);
		graph.prepare();
		
		return graph.contains(sub);
	}
	
	public static int numberEmbedding(Graph sub, Graph graph){
		int num = 0;
		if(graph.getEdgeCount() < sub.getEdgeCount() || graph.getNodeCount() < sub.getNodeCount())
			return num;
		
		sub.decode();
		graph.decode();
		
/*	    int []masks    = new int[4];        
	    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
	    masks[1] = masks[3] = BondTypeMgr.BONDMASK;*/
	    
	    sub.maskTypes(masks); 
	    sub.prepareEmbed();
	    
		graph.decode();
		graph.maskTypes(masks);
		graph.prepare();
		
		Embedding emb = graph.embed(sub);
		if(emb!=null)
			num = emb.size();
		return num;
	}
	


	public static int getFrequencyOfGraph(Collection<Graph> database,
			Graph query) {
		return getProjectedDatabase(database,query).size();
	}

	public static Collection<Graph> getProjectedDatabase(
			Collection<Graph> database, Graph query) {
		 
		query.decode();
		ArrayList<Graph> result = new ArrayList<Graph>();
	    int []masks    = new int[4];        
	    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
	    masks[1] = masks[3] = BondTypeMgr.BONDMASK;

	    query.maskTypes(masks);  
/*		for(Graph graph: database){
			graph.maskTypes(masks);
			Fragment f = new Fragment(graph, query);
			if(f.getEmbCount()>0){
				result.add(graph);
			}
		}*/
		

		query.prepareEmbed();
		
		for(Graph graph : database){
			graph.decode();
			graph.maskTypes(masks);
			graph.prepare();
				
			if(graph.contains(query)){
				result.add((Graph) graph.clone());
			}
			
			
		}
		
		return result;
	}
	
	public static Collection<Graph> getProjectedDatabase2(
			Collection<Graph> database, Graph query) {

		Recoder coder = new Recoder(); /* create a node type recoder */
		
		
		for(Graph graph : database){
			for (int i = graph.getNodeCount(); --i >= 0;) {
				Node node = graph.getNode(i); /* traverse the nodes and */
				if (!node.isSpecial()) /* count the elements in the focus */
					coder.count(coder.add(node.getType()));
			} /* commit after each graph */
			coder.commit(); /* (to determine the type support) */
		}
		
		for (int i = query.getNodeCount(); --i >= 0;) {
			Node node = query.getNode(i); /* traverse the nodes and */
			if (!node.isSpecial()) /* count the elements in the focus */
				coder.count(coder.add(node.getType()));
		} /* commit after each graph */
		coder.commit(); /* (to determine the type support) */
		
		coder.sort(); /* sort the elements by frequency */
		for(Graph graph : database){
			graph.encode(coder); /* encode the graphs, */
			graph.prepare(); /* (re)prepare the graph, */
			graph.mark(-1); /* and clear all markers */
		}
		
		query.encode(coder); /* encode the graphs, */
		query.prepare(); /* (re)prepare the graph, */
		query.mark(-1); /* and clear all markers */
		
		ArrayList<Graph> result = new ArrayList<Graph>();
		if(query.isConnected()){
			//query.prepare();
			query.prepareEmbed();
		}
		for(Graph graph : database){
			if(graph.isConnected()){
				graph.prepare();
			}
			//	graph.prepareEmbed();
			
			if(graph.contains(query)){
				result.add(graph);
			}
			
			
		}
		
		return result;
	}
	
	
/*	public static void createBalancedGraphDatasets(String inputFile,String outputFile, int posNo) throws IOException{
		Random random = new Random(1);
		ArrayList<Graph> graphs = readGraphs(inputFile, GraphReader.GRAPHS);
		int pos=0, neg = 0, uni=0;
		ArrayList<Graph> posGraph = new ArrayList<Graph>();
		ArrayList<Graph> negGraph = new ArrayList<Graph>();
		for(int i = 0; i < graphs.size(); i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			if(ng.getValue() == 1){
				posGraph.add(ng);
			}else if(ng.getValue() == -1){
				negGraph.add(ng);
			}else{
				uni ++;
			}
		}
		
		for
		
		//random sample for posNo of positive graphs
		
		
	}*/

	public static Graph smilesStrToGraph(String str) throws IOException{
        Notation ntn = new SMILES();    /* with a SMILES notation */
        Graph    mol = ntn.parse(new StringReader(str));
        return mol;
	}
	
	
	
	public static void testFre() throws IOException{
		ArrayList<Graph> graphs = GraphUtils.readGraphs("data/1-balance.sdf", GraphReader.GRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
	//	GraphUtils.writeGraphs("test.smiles", graphs);
		
		ArrayList<FrequentGraph> frequent = GraphUtils.getFrequentGraphs(graphs, 40);
		System.out.println("No. of frequent:"+frequent.size());
		
		Graph g = frequent.get(0).getGraph();
		g.decode();
		String sm1 =g.toString();
		System.out.println("sm1:"+g);
		System.out.println("support:"+frequent.get(0).getSupport());
		
    	//String str = "C12(-[Fe]345678(-C9(-C-3(=C-4(-C-5(=C-6-9))))(-C10%11(-[Fe]%12%13%14%15%16%17%18(-C%19(-C-%12(=C-%13(-C-%14(=C-%15-%19))))(-C%20%21(-[Fe]%22%23%24%25%26%27%28(-C(-C-%22(=C-%23-%20))(=C-%21-%24))(-C6-C-%25(-C45(-C3-[Fe]12%30%31%32-4(-C=3(-C-1(=C-2-5)))(-C%29-C-C-%30(=C-%31(-C=%29-%32)))))(-C-%26(=C-%27(-C-%28=6)))))))(-C(-C-%16(=C-%17-10))(=C-%11-%18)))))(-C(-C-C-7=1)(=C-2-8)))";
	    
		for(int i = 0; i < 10; i++){
			Graph gr = frequent.get(i).getGraph();
			//gr.decode();
/*			String sm1 =g.toString();
			System.out.println("sm1:"+sm1);*/
			int fre = getProjectedDatabase(graphs,gr).size();//getFrequencyOfGraph(graphs, frequent.get(i).getGraph());
			int fre2 = (int) frequent.get(i).getSupport();
			if(fre !=fre2){
				System.err.println(fre+"\t"+fre2);
			}
			else System.out.println(fre+"\t"+fre2);
		}

	}
	
	public static void setWeights(ArrayList<Graph> graphs, double[] weights){
		for(int i = 0; i < graphs.size(); i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			ng.setWeight(weights[i]);
		}
	}
	
	public static ArrayList<DecisionStump> getStumpsFromTreeset(TreeSet<DecisionStump>stumps){
		
		ArrayList<DecisionStump> gs = new ArrayList<DecisionStump>();
		for(DecisionStump st : stumps){
			gs.add(st);
		}
		
		return gs;
	}
	
	public static ArrayList<DecisionStump> mergeTwoGraphSet(ArrayList<DecisionStump> set1, ArrayList<DecisionStump> set2){
		ArrayList<DecisionStump> set = null, toAddSet = null;
		if(set1.size() > set2.size()){
			set = set1;
			toAddSet = set2;
		}else{
			set = set2;
			toAddSet = set1;
		}
		
		for(DecisionStump g : toAddSet){
			if(isUnique(set,g)){
				set.add(g);
			}
		}
		
		return set;
		
	}
	

	
	private static boolean isUnique(ArrayList<DecisionStump> set, DecisionStump graph){
		for(DecisionStump stm : set){
			Graph ng = stm.getGraphFeature();
			Graph g = graph.getGraphFeature();
			if(GraphUtils.graphIsomorphism(ng, g)){
				return false;
			}
		}
		return true;
	}
	
	public static ArrayList<Graph> getGraphFromStumps(ArrayList<DecisionStump>stumps){
		
		ArrayList<Graph> gs = new ArrayList<Graph>();
		for(DecisionStump st : stumps){
			gs.add(st.getGraphFeature());
		}
		
		return gs;
	}
	
	public static double[][] graph2Vector(ArrayList<Graph> graphs, ArrayList<DecisionStump> stumps){
		int n = graphs.size();
		int m = stumps.size();
		double result[][] = new double[n][m];
		for(int i = 0; i < m; i++){
			DecisionStump st = stumps.get(i);
			for(int j = 0; j < n; j++){
				result[j][i] = st.classifyGraph(graphs.get(j));
			}
		}
		return result;
	}
	
	public  static double[][] createFeatureVector(ArrayList<Graph> graphs, ArrayList<FrequentGraph> feature) {
		double vector[][] = new double[feature.size()][graphs.size()];
		
		for(int i = 1; i < graphs.size();i++){
			NamedGraph ng = (NamedGraph) graphs.get(i);
			ng.decode();
			ng.maskTypes(masks);
			ng.prepare();
		}
		
		for(int i = 0; i < feature.size(); i++){
			Graph sub = (Graph) feature.get(i).getGraph().clone();
			vector[i]=subgraphInDataVector(sub, graphs);
			
			int fre = (int) feature.get(i).getSupport();
			int cal =(int) sumOf(vector[i]);
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
		
		
		return vector;
	}
	
	private static double[] subgraphInDataVector(Graph sub, ArrayList<Graph> graphs){
		double[] exist = new double[graphs.size()];
		sub.decode();
    
	    sub.maskTypes(masks); 
	    sub.prepareEmbed();
	    
		for(int i = 0; i < exist.length; i++){
			

			NamedGraph ng = (NamedGraph) graphs.get(i);
			boolean c = GraphUtils.subgraphIsomorphism( sub,  ng);
/*			ng.decode();
			ng.maskTypes(masks);
			ng.prepare();*/
			
			exist[i]=c?1:-1;
		}
		
		return exist;
	}
	
	private static double sumOf(double a[]){
		int s = 0;
		for(double v : a){
			s+=v;
		}
		return s;
	}
	
	public static double evaluateStump(ArrayList<Graph> graphs, DecisionStump stumpt){
		double acc = 0, sum=0,supp=0;
		for(Graph ng: graphs){
			NamedGraph graph = new NamedGraph((NamedGraph) ng);
			double value = graph.getValue();
			double predict = stumpt.classifyGraph(ng);
			if(predict==0){
				predict=-1;
			}
			if(value == predict)
				acc ++;
			
			Graph sub = stumpt.getGraphFeature();
			if(GraphUtils.subgraphIsomorphism(sub, ng)){
				supp++;
			}
			
			sum++;
		}
		
		if(supp != stumpt.getSupport()){
			//System.err.println("Error for subgraph checking!support:"+supp+"\tReal_Support:"+stumpt.getSupport());
			//System.exit(0);
		}
		double acc_true = Math.max(acc/sum, 1-acc/sum);
		System.out.println("Gain:"+Utils.doubleToString(stumpt.getGain(), 4)+"\tACC:"
				+Utils.doubleToString(acc_true, 3)+" ("+acc+"/"+sum+")"+
				"\t"+new SMILES().describe(stumpt.getGraphFeature())+"\tSup:"+supp+"\tReal_Sup:"+stumpt.getSupport()
				);
		
		return acc_true;
	}
	
	
	
	public static void evaluateAllStumps(ArrayList<DecisionStump> stumps, ArrayList<Graph> test){
		double accs[] = new double[stumps.size()];
		for(int i = 0; i < stumps.size(); i++){
			//System.out.println(st.getGain());
			 accs[i] = evaluateStump(test,stumps.get(i));
		}
		
		
		
		SortItem item = new SortItem();
		item.sort(stumps, accs);
		
		ArrayList sortItem = item.getSortedItem();
		double value[] = item.getSortedValue();
		
		
		
		for(int i = 0; i < sortItem.size(); i++){
			DecisionStump stm = (DecisionStump) sortItem.get(i);
			System.out.println(new SMILES().describe(stm.getGraphFeature()));
		}
		
		for(int i = 0; i < sortItem.size(); i++){
			System.out.println(value[i]);
		}
		
		System.out.println("MEAN ACC:"+MathUtils.sum(accs)/accs.length);
	}
	
	public static void writeChemicalStumps(ArrayList<DecisionStump> stms,double weights[],String file1){
		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < weights.length; i++){
			DecisionStump st = stms.get(i);
			if(Math.abs(weights[i])>1e-5){
				String x = new SMILES().describe(st.getGraphFeature());
				list.add(x);
			}
		}
		System.out.println("Orignial number of stumps "+weights.length+"\t active:"+list.size());
		Out.outputDataFile(file1, list);

	}
	
	public static void checkOverlaps(String file1, String file2){
		ArrayList<String> list1 = IN.readToList(file1);
		ArrayList<String> list2 = IN.readToList(file2);
		
		ArrayList<Graph> graph1 = new ArrayList<Graph>();
		ArrayList<Graph> graph2 = new ArrayList<Graph>();

		int common = 0;
		try {
			for(String str : list1){
				graph1.add(new SMILES().parse(new StringReader(str)));
			}
			
			for(String str : list2){
				graph2.add(new SMILES().parse(new StringReader(str)));
			}	
			
			GraphUtils.setGraphType(DataConvert.Chemical);
			
			for(int i = 0; i < graph1.size(); i++){
				Graph g1 = graph1.get(i);
				for(int j=0; j < graph2.size(); j++){
					Graph g2 = graph2.get(j);
					
					if(GraphUtils.graphIsomorphism(g1, g2)){
						common ++;
					}
				}
			}
			
			System.out.println("graph set 1: "+graph1.size()+"\t graph set 2: "+graph2.size()+"\t common graphs:"+common);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//GraphUtils.testFre();
		ArrayList<Graph> graphs = GraphUtils.readGraphs("data/1-balance500.sdf", GraphReader.SEMIGRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
		
		int support = 10; 
		DataConvert.datatype=DataConvert.GeneralGraphs;
		//Mine frequent subgraphs:
/*		Long time = System.currentTimeMillis();
		ArrayList<FrequentGraph> frequentFeatures = GraphUtils.getFrequentGraphs(graphs, support);
		time = System.currentTimeMillis() - time;
		System.err.println("Time for frequent subgraph mining: "+time+" ms");
		
		GraphUtils.writeFrequentSubgraphsAsSmiles(frequentFeatures, "baselinedata/fre_"+support+".txt");*/
		
/*		for(int i = 0; i < 10; i++){
			Graph g = frequentFeatures.get(i).getGraph();
			System.out.println("node "+i+":\t"+g.getNodeMgr().getName(g.getNodeType(0)));
		}
		
		ArrayList<Graph > subgraphs = new ArrayList<Graph>();
		for(int i = 0; i < frequentFeatures.size();i++){
			subgraphs.add(frequentFeatures.get(i).getGraph());
		}*/
		
		String path = "/home/span/softwares/MALSAR1.1/pan/data/mtg-tkde/";
		String f25="fre_vector_25_347_0.68092_0.74434.txt";
		String f20="fre_vector_20_518_0.69386_0.75873.txt",
		f15="fre_vector_15_990_0.70324_0.77244.txt",
		f10="fre_vector_10_2648_0.7215_0.7904.txt",
		f5="fre_vector_5_15121_0.72971_0.80119.txt";
		
		GraphUtils.checkOverlaps(path+f5, path+"Mtg_216_0.75006_0.82003.txt");
		//GraphUtils.writeSubGraphs("testsub.sdf", subgraphs, GraphWriter.GRAPHS, GraphUtils.notation);
		
		//graphs = graphs.subList(0, 100);
		//maxCommonGraphMatrix(graphs);

		//GraphUtils.testFre();

		
	/*	FrequentMiner miner = new FrequentMiner();
		miner.getFrequentGraphs(graphs, 500, 1);*/
		//miner.featureSelction(graphs, 20, -2);
		//miner.getFrequentGraphs(graphs, 30, 0);
		
	}

}

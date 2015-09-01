package moss;

import java.util.ArrayList;
import java.util.TreeSet;

import util.DecisionStump;
import util.GraphUtils;


public abstract class MinerAbs {

	public boolean completeSearch=true;
	public boolean saveFrequent=false;
	public boolean zeroOneRepresentation;

	/**
	 * 
	 * @param graphs Trainig graphs
	 * @param excludedSubs already selected graphs (can be null), if it is not null, the returning graphs will not included in excludedSubs
	 * @param support
	 * @param wSupp
	 * @param numStumps
	 * @param maxSize
	 * @param remine
	 * @return
	 */
	public abstract TreeSet<DecisionStump> getMaxGainDecisionStumps(ArrayList<Graph> graphs, ArrayList<Graph> excludedSubs, int support, double wSupp, int numStumps, int maxSize,boolean remine);

	public abstract TreeSet<DecisionStump> getMaxGainDecisionStumps_LU(
			ArrayList<Graph> trainGraphs, ArrayList<Graph> uGraphs, ArrayList<Graph> excludedSubs,
			int support, double ws, int numStump, int maxPad, boolean b);
	
	public void evaluateStumpList(ArrayList<Graph> graphs,DecisionStump stumpt){
		double acc = 0, sum=0,supp=0;
		for(Graph ng: graphs){
			NamedGraph graph = new NamedGraph((NamedGraph) ng);
			double value = graph.getValue();
			double predict = stumpt.classifyGraph(ng);
			predict = predict >0?1:-1;
			if(value == predict)
				acc ++;
			
			Graph sub = stumpt.getGraphFeature();
			if(GraphUtils.subgraphIsomorphism(sub, ng)){
				supp++;
			}
			
			sum++;
		}
		if(supp != stumpt.getSupport()){
			System.err.println("Error for subgraph checking!support:"+supp+"\tReal_Support:"+stumpt.getSupport());
			System.exit(0);
		}
		//System.out.println("Gain:"+stumpt.getGain()+"\tACC:"+acc/sum +"\t("+acc+"/"+sum+")\tsupport:"+supp+"\tReal_Support:"+stumpt.getSupport());
	}
}

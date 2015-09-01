package util;

import java.io.Serializable;

import moss.Graph;

public class DecisionStump implements Comparable<DecisionStump>,Serializable {


	private static final long serialVersionUID = -358344429016734060L;
	private Graph graphFeature;
	private double classValue;
	
	private double gain;
	
	private int support;
	
	private double upperScore;
	
	boolean  zeroOneRepresentation;
	private boolean isValidStump = true;
	double secondUpscores;
	double mTaskNormScore;
	
	
	
	public DecisionStump(Graph graphFeature, double classValue, double gain,double up, int support, boolean zeroOneRepresentation){
		this.graphFeature = graphFeature;
		this.classValue = classValue;
		this.gain = gain;
		this.support = support;
		this.upperScore = up;
		this.zeroOneRepresentation = zeroOneRepresentation;
	}
	
	public double classifyGraph(Graph graph){
		boolean sub = GraphUtils.subgraphIsomorphism( graphFeature,  graph);
		double value = 0;
		
		//System.out.println(sub);
		if(sub){
			value = this.classValue;
			//return classValue;
		}
		else {
			if(this.zeroOneRepresentation)
				value = 0;
			else
				value = -1 * classValue;
		}
		
		return value;
		
	}
	
	public Graph getGraphFeature() {
		return graphFeature;
	}

	public double getClassValue() {
		return classValue;
	}

	public double getGain() {
		return gain;
	}
	
	public double getSupport() {
		return support;
	}
	
	public boolean equals(Object obj){
		DecisionStump g = (DecisionStump) obj;
		return g.graphFeature.equals(this.graphFeature);
		//return false;
	}

	@Override
	public int compareTo(DecisionStump o) {
		if(this.getGain() < o.getGain())
			return -1;
		else if (this.getGain() > o.getGain())
			return 1;
		else if (!this.equals(o))
			return -1;
		
		return 0;
	}

	public double getUpperScore() {
		return upperScore;
	}

	public void setUpperScore(double upperScore) {
		this.upperScore = upperScore;
	}

	public void setValidStump(boolean b) {
		this.isValidStump  = b;
	}
	
	public boolean isValidStump(){
		return this.isValidStump;
	}

	public double getSecondUpscores() {
		return secondUpscores;
	}

	public void setSecondUpscores(double secondUpscores) {
		this.secondUpscores = secondUpscores;
	}

	public double getmTaskNormScore() {
		return mTaskNormScore;
	}

	public void setmTaskNormScore(double mTaskNormScore) {
		this.mTaskNormScore = mTaskNormScore;
	}
}

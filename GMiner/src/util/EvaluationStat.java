package util;

import java.util.ArrayList;

public class EvaluationStat {

	private double accuracy;
	private double auc;
	private double precision;
	private double recall;
	private double F1;
	private double cost; // for cost-sensitive learning
	private double aveCost;
	
	private ArrayList<Double> accAll;
	private ArrayList<Double> aucAll;
	
	
	private long trainingTime;
	
	
	public EvaluationStat(double acc, double auc){
		this.accuracy=acc;
		this.auc=auc;
	}
	
	public double getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}
	public double getAuc() {
		return auc;
	}
	public void setAuc(double auc) {
		this.auc = auc;
	}
	public double getPrecision() {
		return precision;
	}
	public void setPrecision(double precision) {
		this.precision = precision;
	}
	public double getRecall() {
		return recall;
	}
	public void setRecall(double recall) {
		this.recall = recall;
	}
	public double getF1() {
		return F1;
	}
	public void setF1(double f1) {
		F1 = f1;
	}

	public double getCost() {
		return cost;
	}
	
	

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getTrainingTime() {
		return trainingTime;
	}

	public void setTrainingTime(long trainingTime) {
		this.trainingTime = trainingTime;
	}

	public double getAveCost() {
		return aveCost;
	}

	public void setAveCost(double aveCost) {
		this.aveCost = aveCost;
	}

	public ArrayList<Double> getAccAll() {
		return accAll;
	}

	public ArrayList<Double> getAucAll() {
		return aucAll;
	}

	public void setAccAll(ArrayList<Double> accAll) {
		this.accAll = accAll;
	}

	public void setAucAll(ArrayList<Double> aucAll) {
		this.aucAll = aucAll;
	}
	
}

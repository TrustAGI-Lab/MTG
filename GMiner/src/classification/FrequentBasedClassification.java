package classification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import moss.FrequentGraph;
import moss.Graph;
import moss.GraphReader;
import moss.GraphWriter;
import moss.NamedGraph;
import util.CrossValidationMode;
import util.DataConvert;
import util.EvaluationStat;
import util.GraphUtils;
import util.MathUtils;
import util.SortItem;
import util.WekaUtils;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.SMO;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.matrix.Matrix;

@SuppressWarnings("deprecation")
public class FrequentBasedClassification {
	
	public double support=5; //0-100.0
	private Random random;
	
	public boolean featureSelection = false;
	
	public boolean topKFrequent = false;
	public int numtopKFeature = 50;
	
	//Be careful of the class label 
	public   String classLabels[]={"1","-1"};//{"0","1","2","3"};
	public int pencentageTrain=70; //0-100
	private boolean use_tfidf = false  ;
	
	private ArrayList<FrequentGraph> frequentFeatures;
	
	private int minSize = 2; //mimimum number of nodes for frequent subgraph mining
	private int maxSize = 10;
	private ArrayList<Graph> graphs;
	private ArrayList<Graph> train;
	private ArrayList<Graph> test;
	private int numFold=10;
	
	public FrequentBasedClassification(){
		random = new Random(1);
		//frequent = new ArrayList<FrequentGraph>();
	}
	
	public void init(String input){
		//Read Graph
		try {
			graphs = GraphUtils.readGraphs(input, GraphReader.GRAPHS);
			System.out.println("Total number of graphs:"+graphs.size());
			//split data into training and test
			train = new ArrayList<Graph>();
			test = new ArrayList<Graph>();
			
			
			ArrayList<Graph> pgs = new ArrayList<Graph>();
			ArrayList<Graph> ngs = new ArrayList<Graph>();
			
			int nop=0,non=0;
			
			for(int i = 0; i < graphs.size(); i++){
				NamedGraph ng = (NamedGraph) graphs.get(i);
				if(ng.getValue() == 1)
					pgs.add(ng);
				else
					ngs.add(ng);
			}
			nop = (int) (pgs.size() * pencentageTrain/100.0);
			non = (int) (ngs.size() * pencentageTrain/100.0);
			//only consider graph with at least 1 edges
			
			System.out.println("Pos:"+nop+"\tNeg:"+non);
			Collections.shuffle(pgs, random);
			Collections.shuffle(ngs,random);
			
			for(int i = 0; i < pgs.size(); i++){
				if(i < nop)
					train.add(pgs.get(i));
				else
					test.add(pgs.get(i));
			}
			
			for(int i = 0; i < ngs.size(); i++){
				if(i < non)
					train.add(ngs.get(i));
				else{
					test.add(ngs.get(i));
				}
			}
			
			System.out.println("Number of training:"+train.size());
			System.out.println("NUmber of testing:"+test.size());
			
			//graphs.get(0).getEdgeMgr().g
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void mineSubgraphs() throws IOException{
		//Mine frequent subgraphs:
		Long time = System.currentTimeMillis();
		frequentFeatures = GraphUtils.getFrequentGraphs(train, support);
		
		//if only use the top-K frequent subgraphs
		if(this.topKFrequent && this.numtopKFeature < frequentFeatures.size()){
			System.out.println("ONLY RETURN THE TOP  "+numtopKFeature+"  PATTERNS");
			double supports[] = new double[frequentFeatures.size()];
			for(int i = 0; i < frequentFeatures.size(); i++){
				supports[i] = frequentFeatures.get(i).getSupport();
			}
			
			SortItem sortApp = new SortItem();
			sortApp.sort(frequentFeatures, supports);
			
			ArrayList newlist = sortApp.getTopKItem(this.numtopKFeature);
			frequentFeatures = newlist;
			
/*			for(int x = 0; x< this.numtopKFeature; x ++){
				System.out.println(new SMILES().describe(frequentFeatures.get(x).getGraph()));
			}*/
		}
		
		
		
		//write(frequentFeatures,"test.nel");
		
		
		time = System.currentTimeMillis() - time;
		System.err.println("Time for frequent subgraph mining: "+time+" ms");

	}
	
	public EvaluationStat doClassification() throws Exception{
		mineSubgraphs();
		EvaluationStat eval = graphClassiffication();
		
		return eval;
	}
	
	public EvaluationStat singleGraphClassification(String file) throws Exception{
		this.init(file);
		mineSubgraphs();
		EvaluationStat eval = graphClassiffication();
		
		return eval;
	}
	
	public ArrayList<EvaluationStat>  crossValidation(String file) throws Exception{
		
		ArrayList<EvaluationStat> allEval = new ArrayList<EvaluationStat>();
		CrossValidationMode mode = new CrossValidationMode();
		mode.setRandom(this.random);
		graphs = GraphUtils.readGraphs(file, GraphReader.GRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
		double []accs = new double[numFold];
		double []aucs = new double[numFold];
		ArrayList<ArrayList<Graph>> foldGraphs = mode.createBinGraphs(graphs, numFold);
		for(int i = 0; i < numFold; i++){
			this.train = mode.trainCV(foldGraphs, i);
			this.test = mode.testCV(foldGraphs, i);
			
			//mineSubgraphs();
			EvaluationStat eval = doClassification();
			accs[i] = eval.getAccuracy();
			aucs[i] = eval.getAuc();
			
			allEval.add(eval);
			
		}
		
		System.out.println(MathUtils.arrayToString(accs));
		System.out.println(MathUtils.arrayToString(aucs));
		System.out.println("Mean ACC:"+MathUtils.sum(accs)/numFold);
		System.out.println("Mean AUCC:"+MathUtils.sum(accs)/numFold);
		
		return allEval;
	}
	
	public void init(String trainfile, String testfile){
		//Read Graph
		try {
			train = GraphUtils.readGraphs(trainfile, GraphReader.GRAPHS);
			test = GraphUtils.readGraphs(testfile, GraphReader.GRAPHS);
			//only consider graph with at least 1 edges
			
			
			
			System.out.println("Number of training:"+train.size());
			System.out.println("NUmber of testing:"+test.size());
			
			//graphs.get(0).getEdgeMgr().g
			
			
			//Mine frequent subgraphs:
			Long time = System.currentTimeMillis();
			frequentFeatures = GraphUtils.getFrequentGraphs(train, support);
			
			
			
			write(frequentFeatures,"test.nel");
			
			
			time = System.currentTimeMillis() - time;
			System.err.println("Time for frequent subgraph mining: "+time+" ms");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void write(ArrayList<FrequentGraph> frequentFeatures, String string) throws IOException {
		ArrayList<Graph> graphs = new ArrayList<Graph>();
		for(FrequentGraph g : frequentFeatures){
			graphs.add(g.getGraph());
		}
		
		GraphUtils.writeGraphs(string, graphs, GraphWriter.SUBS, GraphUtils.notation);
	}

	public EvaluationStat graphClassiffication() throws Exception{
		EvaluationStat evalstat = null;
		try {
			Long time = System.currentTimeMillis();
			//convert graph into vectors
			DataConvert convert = new DataConvert();

			Instances train_data[] = null;
			Instances test_data[] = null;
			if(this.use_tfidf){
			
				train_data = convert.graphToWekaInstancesTFIDF(train, frequentFeatures, classLabels);
				test_data = convert.graphToWekaInstancesTFIDF(test, frequentFeatures, classLabels);
			}else{
				train_data = convert.graphToWekaInstances(train, frequentFeatures, classLabels) ;
				System.out.println("Transfer training over!---");
		
				convert.warning=false;
				test_data = convert.graphToWekaInstances(test, frequentFeatures, classLabels) ;
				//System.out.println("Number of train:"+train_data[0].size());
				convert.warning=true;
				//WekaUtils.writeInstances( "train.arff", train_data[0]);
				
				//for(int i = 0; i < train_data[0].size(); i++)
					//System.out.println("data"+i+": "+train_data[0].get(i));
			}
			
			time = System.currentTimeMillis() - time;
			System.err.println("Time for transfering graph into vectors: "+time+" ms");
			
			//System.out.println(data[0]);
			
			//feature selection
			if(featureSelection && numtopKFeature < train_data[0].numAttributes()-1){
				time = System.currentTimeMillis();
				System.out.println("Feature selection based on IG....");
				//data[0] = WekaUtils.featureSelectByInforGain(data[0], numFeature);
				Instances reduceIns[] = WekaUtils.attributeSelection(train_data[0],test_data[0], numtopKFeature);
				train_data[0] = reduceIns[0];
				test_data[0] = reduceIns[1];
				
				System.out.println("Number of features after selection: "+(train_data[0].numAttributes()-1));
				time = System.currentTimeMillis() - time;
				System.out.println("Feature selection over...");
				System.err.println("Time for feature selection: "+time+" ms");
			}
			
			
			Evaluation eval = evaluate(train_data[0],test_data[0]);
			
			double acc = 1 - eval.errorRate();
			double AUC = eval.areaUnderROC(0);
			double aveCost = eval.avgCost();
			double cost = aveCost * test_data[0].numInstances();
			
			evalstat = new EvaluationStat(acc,AUC);
			evalstat.setCost(cost);
			evalstat.setAveCost(aveCost);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return evalstat;
		//Mine frequent subgraphs
		
	}
	
	
	public EvaluationStat graphCostsensitiveClassiffication(double costs[]) throws Exception{
		mineSubgraphs();
		EvaluationStat evalstat = null;
		try {
			Long time = System.currentTimeMillis();
			//convert graph into vectors
			DataConvert convert = new DataConvert();
			
			Instances train_data[] = null;
			Instances test_data[] = null;
			if(this.use_tfidf){
			
				train_data = convert.graphToWekaInstancesTFIDF(train, frequentFeatures, classLabels);
				test_data = convert.graphToWekaInstancesTFIDF(test, frequentFeatures, classLabels);
			}else{
				train_data = convert.graphToWekaInstances(train, frequentFeatures, classLabels) ;
				System.out.println("Transfer training over!---");
				
				convert.warning=false;
				test_data = convert.graphToWekaInstances(test, frequentFeatures, classLabels) ;
				//System.out.println("Number of train:"+train_data[0].size());
				
				//WekaUtils.writeInstances( "train.arff", train_data[0]);
				
				//for(int i = 0; i < train_data[0].size(); i++)
					//System.out.println("data"+i+": "+train_data[0].get(i));
			}
			
			time = System.currentTimeMillis() - time;
			System.err.println("Time for transfering graph into vectors: "+time+" ms");
			System.err.println("NUmber of subgraphs: "+frequentFeatures.size()+" ");
			
			//System.out.println(data[0]);
			
			//feature selection
			if(featureSelection && numtopKFeature < train_data[0].numAttributes()-1){
				time = System.currentTimeMillis();
				System.out.println("Feature selection based on IG....");
				//data[0] = WekaUtils.featureSelectByInforGain(data[0], numFeature);
				Instances reduceIns[] = WekaUtils.attributeSelection(train_data[0],test_data[0], numtopKFeature);
				train_data[0] = reduceIns[0];
				test_data[0] = reduceIns[1];
				
				System.out.println("Number of features after selection: "+(train_data[0].numAttributes()-1));
				time = System.currentTimeMillis() - time;
				System.out.println("Feature selection over...");
				System.err.println("Time for feature selection: "+time+" ms");
				
				//WekaUtils.
			}
			
			Evaluation eval = evaluateCostSensitive(train_data[0],test_data[0],costs);
			
			double acc = 1 - eval.errorRate();
			double AUC = eval.areaUnderROC(0);
			double aveCost = eval.avgCost();
			double cost = aveCost * test_data[0].numInstances();
			
			evalstat = new EvaluationStat(acc,AUC);
			evalstat.setCost(cost);
			evalstat.setAveCost(aveCost);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return evalstat;
		//Mine frequent subgraphs
		
	}

	private Evaluation evaluate(Instances train_data, Instances test_data) {
		
		Long time = System.currentTimeMillis();
		train_data.setClassIndex(train_data.numAttributes()-1);
		
		
		System.out.println("Number of training data:"+train.size());
		System.out.println("Number of test data:"+test.size());
		Evaluation eval = null;
		//Classifier classifier = new IBk(1); //KNN
		//Classifier classifier =  new NaiveBayes(); //bayes
		//Classifier classifier =  new SMO(); //SVM
		//LibSVM classifier = new LibSVM();
			Classifier classifier = new SMO();
			 
			try {
				System.out.println("\nbuilding classifier:"+classifier.getClass());
				classifier.buildClassifier(train_data);
				eval = new Evaluation(test_data);
				eval.evaluateModel(classifier, test_data);
				
				System.out.println("No of features:"+(train_data.numAttributes()-1));
				System.out.println("Feature Selection:"+this.featureSelection);
				System.out.println("Use TFIDF:"+this.use_tfidf);
				
				//System.out.println(Utils.arrayToString(predict));
				System.out.println(eval.toSummaryString());
				System.out.println(eval.toClassDetailsString());
				System.out.println(eval.toMatrixString());
				
				//this.getAUCValueWeka(classifier, test);
				//this.getAUCValue(classifier, test);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			time =  System.currentTimeMillis() - time;
			System.err.println("Time for evaluation: "+time+" ms");
			
			return eval;
	}
	
	private Evaluation evaluateCostSensitive(Instances train_data, Instances test_data, double costs[]) {
		
		Long time = System.currentTimeMillis();
		train_data.setClassIndex(train_data.numAttributes()-1);
		
		System.out.println("Number of training data:"+train.size());
		System.out.println("Number of test data:"+test.size());
		Evaluation eval = null;
		//Classifier classifier = new IBk(1); //KNN
		//Classifier classifier =  new NaiveBayes(); //bayes
		LibSVM classifier = new LibSVM();

		//Classifier classifier =  new SMO(); //SVM
			
			//Classifier classifier = new SMO();
			 
			try {//-h 0
				//String svmopts[] = {"-W",""+costs[0]+" "+costs[1]};
				String svmopts[] = {"-h 0 -W",""+costs[0]+" "+costs[1]};
				classifier.setOptions(svmopts);
				System.out.println("\nbuilding classifier:"+classifier.getClass());
				classifier.buildClassifier(train_data);
				
				CostMatrix costmatrix = new CostMatrix(2);
				costmatrix.setElement(0,1,costs[0]);
				costmatrix.setElement(1, 0, costs[1]);
				eval = new Evaluation(test_data,costmatrix);
				//eval = new Evaluation(test_data);
				eval.evaluateModel(classifier, test_data);
				
				System.out.println("No of features:"+(train_data.numAttributes()-1));
				System.out.println("Feature Selection:"+this.featureSelection);
				System.out.println("Use TFIDF:"+this.use_tfidf);
				System.out.println("Ave cost:"+eval.avgCost());
				
				//System.out.println(Utils.arrayToString(predict));
				System.out.println(eval.toSummaryString());
				System.out.println(eval.toClassDetailsString());
				System.out.println(eval.toMatrixString());
				
				//this.getAUCValueWeka(classifier, test);
				//this.getAUCValue(classifier, test);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			time =  System.currentTimeMillis() - time;
			System.err.println("Time for evaluation: "+time+" ms");
			
			return eval;
	}

	public double getAUCValueWeka(Classifier classifier, Instances data){
		double value = 0;
		try {
		      
		        data.setClassIndex(data.numAttributes() - 1);
		        ThresholdCurve tc = new ThresholdCurve();
		        EvaluationUtils eu = new EvaluationUtils();
		        FastVector predictions = new FastVector();
		        
		        predictions.appendElements(eu.getTestPredictions(classifier, data));
		        System.out.println(predictions);
		        Instances result = tc.getCurve(predictions);
		       
		       // System.out.println(result);
		        System.out.println();
		        System.out.println(tc.getROCArea(result));
		        value = tc.getROCArea(result);
		    } catch (Exception ex) {
		      ex.printStackTrace();
		    }
		
		return value;
	}
	
	public double getAUCValue(Classifier classifier, Instances data){
		double auc = 0;
		try {
			ArrayList<Double> positive = new ArrayList<Double>();
			ArrayList<Double> negative = new ArrayList<Double>();
			int classIndex = 1;
			
			for(int i = 0; i < data.size();i ++){
				double distribution[] = classifier.distributionForInstance(data.get(i));
				
				if(data.get(i).classValue()==classIndex){
					positive.add(distribution[classIndex]);
				}else{
					negative.add(distribution[classIndex]);
				}
			}
			
			System.out.println("Num 1:"+positive.size()+"\tNum 2:"+negative.size());
			double sum = 0;
			for(int i =0; i < positive.size(); i++){
				for(int j = 0; j < negative.size(); j++){
					if(MathUtils.gr(positive.get(i), negative.get(j))){
						sum++;
					}
					else if (MathUtils.eq(positive.get(i), negative.get(j))){
						sum +=0.5;
					}
				}
			}
			
			auc = sum/(positive.size()*negative.size());
			
			System.out.println("AUC:"+auc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return auc;
	}
	
	public void setFrequentFeatures(ArrayList<FrequentGraph> frequentFeatures) {
		this.frequentFeatures = frequentFeatures;
	}

	public void setGraphs(ArrayList<Graph> graphs) {
		this.graphs = graphs;
	}

	public void setUse_tfidf(boolean use_tfidf) {
		this.use_tfidf = use_tfidf;
	}

	public void setNumFeature(int numFeature) {
		this.numtopKFeature = numFeature;
	}
	
	private Instances getSubAttInstances(Instances train2, int noAtt) {
		int org = train2.numAttributes();
		Instances newins = new Instances(train2);
		
		//for int i
		for(int i = noAtt; i < org-1; i++){
			newins.deleteAttributeAt(noAtt);
		}
		
		return newins;
	}
	
	public String crossValidation_VaryAtt(String file, int total, int step) throws IOException{
		int iteration = total/step;
		ArrayList<Double> aucstr = new ArrayList<Double>();
		ArrayList<Double> accstr = new ArrayList<Double>();
		double accall[][] = new double[iteration][numFold];
		double [][]aucall = new double[iteration][numFold];
		ArrayList<Double> stdaucstr = new ArrayList<Double>();
		ArrayList<Double> stdaccstr = new ArrayList<Double>();
		this.numtopKFeature = total;
		DataConvert convert = new DataConvert();
		CrossValidationMode mode = new CrossValidationMode();
		mode.setRandom(this.random);
		graphs = GraphUtils.readGraphs(file, GraphReader.GRAPHS);
		System.out.println("No graphs:"+graphs.size());
		ArrayList<ArrayList<Graph>> foldGraphs = mode.createBinGraphs(graphs, numFold);
		for(int i = 0; i < numFold; i++){
			this.train = mode.trainCV(foldGraphs, i);
			this.test = mode.testCV(foldGraphs, i);
			
			System.out.println("Mining subgraphs....");
			
			//mine subgraphs
			mineSubgraphs();
			
			Instances[] trainData = convert.graphToWekaInstances(train, frequentFeatures, classLabels) ;
			System.out.println("Transfer training over!---");
	
			convert.warning=false;
			Instances[] testData = convert.graphToWekaInstances(test, frequentFeatures, classLabels) ;
			//System.out.println("Number of train:"+train_data[0].size());
			convert.warning=true;
			
			
			//feature selection
			//feature selection
			if(featureSelection && numtopKFeature < trainData[0].numAttributes()-1){
				Long time = System.currentTimeMillis();
				System.out.println("Feature selection based on IG....");
				//data[0] = WekaUtils.featureSelectByInforGain(data[0], numFeature);
				Instances reduceIns[] = WekaUtils.attributeSelection(trainData[0],testData[0], numtopKFeature);
				trainData[0] = reduceIns[0];
				testData[0] = reduceIns[1];
				
				System.out.println("Number of features after selection: "+(trainData[0].numAttributes()-1));
				time = System.currentTimeMillis() - time;
				System.out.println("Feature selection over...");
				System.err.println("Time for feature selection: "+time+" ms");
			}

			
			//varying features:
			for(int j = 0; j < total/step;j++){
				int no_att = (j+1) * step;
			
			//int no_att= 40;
				
				System.out.println("Ori ins:"+trainData[0].numAttributes());
				//Create new training and testing instances
				Instances newtrain = getSubAttInstances(trainData[0], no_att);
				Instances newtest = getSubAttInstances(testData[0], no_att);
				
				System.out.println("New ins:"+newtrain.numAttributes());
				System.out.println("Ori ins:"+trainData[0].numAttributes());
				
				 Evaluation eval = evaluate(newtrain,newtest);
				accall[j][i] = 1-eval.errorRate();
				aucall[j][i] = eval.areaUnderROC(0);				
			}
			
		}
		
		Matrix accmatrix = new Matrix(accall);
		Matrix aucmatrix = new Matrix(aucall);
		
		
		for(int j = 0; j < iteration; j++){
			aucstr.add(MathUtils.mean(aucall[j]));
			accstr.add(MathUtils.mean(accall[j]));
			stdaucstr.add(Math.sqrt(MathUtils.variance(aucall[j])));
			stdaccstr.add(Math.sqrt(MathUtils.variance(accall[j])));
	
		}
		
		String resultStr="";
		resultStr  += accmatrix.toString()+"\n"+aucmatrix.toString()+"\n";
		resultStr+="Mean ACC:"+accstr+"\n";
		resultStr+="STD ACC:"+stdaccstr+"\n";

		resultStr+="Mean AUC:"+aucstr+"\n";
		resultStr+="STD AUC:"+stdaucstr+"\n";

		return resultStr;
		
		
	}
	
	public String experiment_NoFeatures(String file) throws Exception{
		int iteration = 20;
		ArrayList<Double> aucstr = new ArrayList<Double>();
		ArrayList<Double> accstr = new ArrayList<Double>();
		double accall[][] = new double[iteration][numFold];
		double [][]aucall = new double[iteration][numFold];
		
		
		for(int i = 0; i< iteration;i++){
			this.numtopKFeature =(i+1) * 10;
			ArrayList<EvaluationStat> all = crossValidation(file);
			
			for(int j = 0; j < all.size(); j++){

				
				accall[i][j]=all.get(j).getAccuracy();
				aucall[i][j]=all.get(j).getAuc();
				
			}
			
			aucstr.add(MathUtils.mean(aucall[i]));
			accstr.add(MathUtils.mean(accall[i]));
		}
		
		Matrix accmatrix = new Matrix(accall);
		Matrix aucmatrix = new Matrix(aucall);
		
		String resultStr="";
		resultStr  += accmatrix.toString()+"\n"+aucmatrix.toString()+"\n";
		resultStr+="Mean ACC:"+accstr+"\n";
		resultStr+="Mean AUC:"+aucstr;
		
		return resultStr;
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		
		FrequentBasedClassification fs = new FrequentBasedClassification();
		fs.support=10; // support : 2 for DBLP, 5 for 1-balance800.sdf
		
		// Be careful of the classLabels
		String []classLabels={"1","-1"};
		fs.classLabels=classLabels;
		

		fs.setUse_tfidf(false);
		

		//String file="data/DBLP_v1.nel";
		String file="data/1-balance800.sdf";
		fs.pencentageTrain=60;
		fs.singleGraphClassification(file);
		
	}



}

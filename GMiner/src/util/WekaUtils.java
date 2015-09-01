package util;

import java.io.File;
import java.util.ArrayList;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

public class WekaUtils {
	
	public static void writeInstances(String fileName, Instances data){
		Out.println(fileName, data.toString());
	}
	
	public static Instances readInstances(String fileName){
		 Instances data = null;
		try {
			ArffLoader source = new ArffLoader();
			source.setFile(new File(fileName));
			data = source.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//System.out.println(data);
		return data;
	}
	

	public static Instances mergeInstances(Instances d1, Instances d2){
		Instances data = new Instances(d1);
		for(int i = 0; i < d2.size(); i++){
			data.add(d2.get(i));
		}
		return data;
	}
	
	public static Instances featureSelectByInforGain(Instances data, int num) throws Exception{
		data.setClassIndex(data.numAttributes()-1);
		
		AttributeSelection selection = new AttributeSelection();
		
		ASEvaluation eval = new InfoGainAttributeEval();
		selection.setEvaluator(eval);
		
		Ranker search = new Ranker();
		search.setNumToSelect(num);
		selection.setSearch(search);
		
		selection.setInputFormat(data);
		
		
		
		Instances newdata = Filter.useFilter(data, selection);
		
		return newdata;
	}
	
	public static Instances[] attributeSelection(Instances train, Instances test, int num){
		train.setClassIndex(train.numAttributes()-1);
		
		int index[]= null;
		
		Instances newdata[] = new Instances[2];
		try {
			
			weka.attributeSelection.AttributeSelection selection = new weka.attributeSelection.AttributeSelection();
			ASEvaluation eval = new InfoGainAttributeEval();
			selection.setEvaluator(eval);
			
			Ranker search = new Ranker();
			search.setNumToSelect(num);
			selection.setSearch(search);
			
			selection.SelectAttributes(train);
			
			//index of the attribute!!!!!!!!!
			index= selection.selectedAttributes();
			System.out.println("index:"+Utils.arrayToString(index));
			System.out.println("NUM OF FINAL ATTRIBUTES:"+index.length);
			
			//construct the instances
			ArrayList<Attribute> atts = new ArrayList<Attribute>();
			for(int i =0; i<index.length; i++){
				atts.add(train.attribute(index[i]));
			}
			newdata[0] = new Instances("New Graph Data Train", atts, 0);
			newdata[1] = new Instances("New Graph Data Test", atts, 0);
			newdata[0].setClassIndex(newdata[0].numAttributes()-1);
			newdata[1].setClassIndex(newdata[1].numAttributes()-1);
			
			for(int i = 0; i < train.size(); i++){
				//Instance instance = selection.reduceDimensionality(train.get(i));
				Instance instance = getSubInstance(train.get(i),index);
				newdata[0].add(instance);
			}
			
			for(int i = 0; i < test.size(); i++){
				//Instance instance = selection.reduceDimensionality(test.get(i));
				Instance instance = getSubInstance(test.get(i),index);
				newdata[1].add(instance);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return newdata;
	}
	
	//After selection, the features are also sorted by its information gain, i.e., IG(m1) > IG(m2) > IG(m3)....
	public static Instances[] attributeSelectionSort(Instances train, Instances test, int num){
		train.setClassIndex(train.numAttributes()-1);
		
		int index[]= null;
		
		Instances newdata[] = new Instances[2];
		try {
			
			weka.attributeSelection.AttributeSelection selection = new weka.attributeSelection.AttributeSelection();
			ASEvaluation eval = new InfoGainAttributeEval();
			selection.setEvaluator(eval);
			
			Ranker search = new Ranker();
			search.setNumToSelect(num);
			selection.setSearch(search);
			
			selection.SelectAttributes(train);
			
			//index of the attribute!!!!!!!!!
			index= selection.selectedAttributes();
			System.out.println("index:"+Utils.arrayToString(index));
			System.out.println("NUM OF FINAL ATTRIBUTES:"+index.length);
			
			//construct the instances
			ArrayList<Attribute> atts = new ArrayList<Attribute>();
			for(int i =0; i<index.length; i++){
				atts.add(train.attribute(index[i]));
			}
			newdata[0] = new Instances("New Graph Data Train", atts, 0);
			newdata[1] = new Instances("New Graph Data Test", atts, 0);
			newdata[0].setClassIndex(newdata[0].numAttributes()-1);
			newdata[1].setClassIndex(newdata[1].numAttributes()-1);
			
			for(int i = 0; i < train.size(); i++){
				//Instance instance = selection.reduceDimensionality(train.get(i));
				Instance instance = getSubInstance(train.get(i),index);
				newdata[0].add(instance);
			}
			
			for(int i = 0; i < test.size(); i++){
				//Instance instance = selection.reduceDimensionality(test.get(i));
				Instance instance = getSubInstance(test.get(i),index);
				newdata[1].add(instance);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return newdata;
	}
	
	private static Instance getSubInstance(Instance inst, int index[]){
		double value[] = new double[index.length];
		for(int i = 0; i < value.length; i++){
			value[i] = inst.value(index[i]);
		}
		
		return new DenseInstance(1, value);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
/*		Instances data = WekaUtils.readInstances("weather.arff");
		WekaUtils.writeInstances("a.arff", data);*/
		
		String str ="";
		for(int i = 0; i < 400; i++){
			str+=""+i+",";
		}
		
		System.out.println(str);
	}

}

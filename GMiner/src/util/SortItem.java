package util;

import java.util.ArrayList;
import java.util.Collections;

import weka.core.Utils;

class ItemPair implements Comparable<ItemPair>{
	
	Object obj;
	double value;
	
	public ItemPair(Object obj, double value){
		this.obj = obj;
		this.value = value;
	}
	
	@Override
	public int compareTo(ItemPair o) {
		if(this.value < o.value){
			return -1;
		}else if (this.value > o.value){
			return 1;
		}
		
		return 0;
	}
}

public class SortItem {
	
	//ArrayList<Object> objs = new ArrayList<Object>();
	ArrayList<Double> newvalues = new ArrayList<Double>();
	
	ArrayList<ItemPair> items;
	
	
	public void sort(ArrayList objs, double []values){
		
		items = new ArrayList<ItemPair>();
		for(int i = 0; i <objs.size(); i++){
			items.add(new ItemPair(objs.get(i),values[i]));
		}
		
		Collections.sort(items, Collections.reverseOrder());
		for(int i = 0; i < items.size(); i++){
			newvalues.add(items.get(i).value);
		}
		
		System.out.println(newvalues);
	}
	
	
	public ArrayList<Object> getTopKItem(int K){
		ArrayList<Object> objs = new ArrayList<Object>();
		for(int i=0; i < K;i++){
			objs.add(items.get(i).obj);
		}
		
		return objs;
		
	}
	
	
	public ArrayList<Object> getSortedItem(){
		ArrayList<Object> objs = new ArrayList<Object>();
		for(int i=0; i < items.size();i++){
			objs.add(items.get(i).obj);
		}
		
		return objs;
		
	}
	
	public double[] getSortedValue(){
		double values[] = new double[newvalues.size()];
		for(int i = 0; i < values.length; i++){
			values[i] = newvalues.get(i);
		}
		return values;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}



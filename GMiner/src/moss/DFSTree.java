package moss;

import java.util.ArrayList;

public class DFSTree {
	
	Fragment node;
	
	ArrayList<DFSTree> children;
	
	int height;
	int childsize;
	
	double[] indVec; //indicating vector;
	
	boolean skip = false;
	

	public boolean isPrunedBefore = false;

	public CanonicalForm cnf;;
	
	
	public DFSTree(){
		children = new ArrayList<DFSTree>();
	}
	
	public DFSTree(Fragment frag, int height){
		this();
		this.node = frag;
		this.height = height;
	}
	
	public void setIndVec(double[] vec){ 
		this.indVec = vec;
	}
	
	public void addChild(DFSTree childTree){
		children.add(childTree);
		
		childsize ++;
	}
	
	
	public void dfsSearch(){
		//System.out.println("height:"+this.height);
		
		String s ="";
		for(int i = 0; i < this.height; i++){
			s+="-";
		}
		System.out.println(s+height);
		if(isLeaf()){ //leaf node
			s +="leaf";
			return;
		}
		
		
		
		for(int i = 0; i < children.size();i++){
			children.get(i).dfsSearch();
		}
	}
	
	public boolean isLeaf(){
		return this.childsize == 0;
	}

	public boolean isIndVectSet(){
		return indVec != null;
	}
	
	public void setIndV(double v[]){
		indVec =new double[v.length];
		System.arraycopy(v, 0, indVec, 0, v.length);
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}

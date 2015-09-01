package moss;



public class FrequentGraph {
	
	private Graph object;
	
	public Graph getGraph() {
		return object;
	}

	public double getSupport() {
		return support;
	}

	private double support;
	
	public FrequentGraph(Graph obj, double support){
		this.object = obj;
		this.support = support;
	}

	
	
}

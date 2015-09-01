package util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import moss.AtomTypeMgr;
import moss.BondTypeMgr;
import moss.Ctab;
import moss.Edge;
import moss.Graph;
import moss.GraphReader;
import moss.NamedGraph;
import moss.Node;
import moss.Notation;
import moss.TypeMgr;

/**
 * Convert graphs into a file for matlab input, from which matlab can convert it into a graphs
 * @author span
 *
 */
public class GraphsToMatrix {

	private StringBuffer desc;

	/**
	 *  Transfer the graph file for Matlab use (boosting)
	 * @param graphs
	 * @param file
	 */
	public void writeFile(ArrayList<Graph> graphs,String file, int type){
		try {
			PrintWriter out = new PrintWriter(new FileWriter(file));
			//out.print("Total ");
			//out.println(graphs.size());
			for(int i = 0; i < graphs.size();i++){
				Graph g = graphs.get(i);
				//System.out.println("graph "+(i+1)+":\t"+g.getNodeCount()+"\t"+g.getEdgeCount());
				if(type == DataConvert.Chemical)
					out.print(this.describe_Chemical(g));
				else
					out.print(this.describe_Other(g));
			}
			
			out.flush();
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String describe_Other(Graph mol){
	    if (this.desc == null)      /* create a description buffer */
		      this.desc = new StringBuffer();
		this.desc.setLength(0);     /* clear the description buffer */
		
		
		desc.append(mol.getNodeCount()+"  "+mol.getEdgeCount());
		desc.append("\n");
		
		for(int i = 0; i < mol.getNodeCount(); i++){
			Node node = mol.getNode(i);
			desc.append("  "+node.getBaseType());
		}
		desc.append("\n");
		
		int i = 0;
	    for (i = mol.getEdgeCount(); --i >= 0; )
		      mol.getEdge(i).mark = 1;    /* mark all getEdge */
		for (i =  0; i < mol.getNodeCount(); i++) {
		      Node s = mol.getNode(i);
		      s.mark = i+1;
		}
		
		for(i = 0; i < mol.getEdgeCount(); i++){
			Edge ed = mol.getEdge(i);
			Node src = ed.getSource();
			Node des = ed.getDest();
			
			desc.append("  "+src.mark+"  "+des.mark+"  "+ed.mark);
			desc.append("\n");
		}
		
		
	    NamedGraph ng = (NamedGraph) mol;
	    desc.append(""+ng.getValue()+"\n");
	    desc.append(""+ng.getName()+"\n");
		    
		return desc.toString();
	}
	
	 public String describe_Chemical (Graph mol)
	  {                             /* --- create a string description */
	    int    i, k, n, t;          /* loop variables, type buffer */
	    Node   s, d;                /* source and destination atom */
	    Edge   b;                   /* incident bond */
	    String e;                   /* element name, buffer */

	    if (this.desc == null)      /* create a description buffer */
	      this.desc = new StringBuffer();
	    this.desc.setLength(0);     /* clear the description buffer */
	    for (i = mol.getEdgeCount(); --i >= 0; )
	      mol.getEdge(i).mark = 1;    /* mark all getEdge */
	    for (i = n = 0; i < mol.getNodeCount(); i++) {
	      s = mol.getNode(i);         /* traverse the nodes */
	      if ((s.getDegree() != 1)          /* check for non-terminal node */
	      ||  !BondTypeMgr.isSingle(s.getEdge(0).getType())) {
	        s.mark = ++n; continue; }
	      t = (mol.getRecoder() != null) ? mol.getRecoder().decode(s.getType()) : s.getType();
	      if (AtomTypeMgr.getElem(t) != AtomTypeMgr.HYDROGEN) {
	        s.mark = ++n; continue; }  /* check for non-hydrogen */
	      b = s.getEdge(0);           /* get the destination node */
	      d = (b.src != s) ? b.src : b.dst;
	      t = (mol.getRecoder() != null) ? mol.getRecoder().decode(d.getType()) : d.getType();
	      if (AtomTypeMgr.getElem(t) == AtomTypeMgr.HYDROGEN) {
	        s.mark = ++n; continue; }  /* check for hydrogen pair */
	      s.mark = b.mark = 0;      /* mark hydrogen and bond as */
	    }                           /* implicit (shorthand hydrogens) */
	    for (i = k = 0; i < mol.getEdgeCount(); i++)
	      if (mol.getEdge(i).mark != 0) k++;
	    e   = "" +n;              /* store number of atoms */
	    this.desc.append(e);
	    e   = "  " +k;              /* store number of bonds */
	    this.desc.append(e);
	    this.desc.append("\n");
	    for (i = 0; i < mol.getNodeCount(); i++) {
	      s = mol.getNode(i);         /* traverse the nodes again */
	      if (s.mark == 0) continue;/* skip implicit hydrogens */

	      t = (mol.getRecoder() != null) ? mol.getRecoder().decode(s.getType()) : s.getType();
	      e="  "+t;
	     // e = AtomTypeMgr.getElemName(t);   /* get the element name */
	      this.desc.append(e);      /* and store it */
	      

	    }    
	    desc.append("\n");
	    /* store additional dummy fields */
	    for (i = 0; i < mol.getEdgeCount(); i++) {
	      b = mol.getEdge(i);         /* traverse the getEdge */
	      if (b.mark != 1) continue;/* skip unmarked getEdge */
	      e = "  " +b.src.mark;     /* store source node index */
	      this.desc.append(e);
	      e = "  " +b.dst.mark;     /* store destination node index */
	      this.desc.append(e);
	      e = "  " +Ctab.ENCODE_MAP[BondTypeMgr.getBond(b.getType())];
	      this.desc.append(e);
	      desc.append("\n");
	    }                           /* store bond type and dummy fields */
	   // this.desc.append("M  END"); /* store end of connection table */
	    NamedGraph ng = (NamedGraph) mol;
	    desc.append(""+ng.getValue()+"\n");
	    
	    return this.desc.toString();/* return the created description */
	  }  /* describe() */
	 
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		int graphtype = DataConvert.Chemical;
		
		String id="1";
		GraphsToMatrix app = new GraphsToMatrix();
		ArrayList<Graph> graphs = GraphUtils.readGraphs("data/"+id+"-balance500.sdf", GraphReader.SEMIGRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
		
		app.writeFile(graphs, "data/"+id+"-balance500_t.txt", graphtype);
		
		
/*		ArrayList<Graph> graphs = GraphUtils.readGraphs("imbalance_dblp.nel", GraphReader.GRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
		
		app.writeFile(graphs, "ninedataset/imbalance_dblp"+".txt", graphtype);*/
		
/*		ArrayList<Graph> graphs = GraphUtils.readGraphs("ninedataset/TWITTER-Real-Graph-Partial.nel", GraphReader.GRAPHS);
		System.out.println("Number of graphs:"+graphs.size());
		
		app.writeFile(graphs, "ninedataset/TWITTER-Real-Graph-Partial.txt", graphtype);*/
		
	}

}

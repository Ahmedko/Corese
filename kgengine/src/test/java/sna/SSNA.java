package sna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.core.Regex;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.core.Memory;
import fr.inria.edelweiss.kgram.event.ResultListener;
import fr.inria.edelweiss.kgram.path.Path;

/**
 * SNA Processor
 * 
 * ?x sa(foaf:knows+) :: $path ?y
 * group by ?x ?y
 * bindings ?x {(<someone>)}
 * 
 * Compute betweeness of intermediate nodes  with shortest paths between x, y 
 * For each x,y : consider all shortest paths ; store and count intermediate nodes
 * 
 * Based on Guillaume Erétéo SNA query library
 * 
 * Olivier Corby, Edelweiss, INRIA, 2011
 *
 */
public class SSNA implements ResultListener {
	
	static int MAXNODE 	 = 1000;

	boolean isFake = false, 
	speedUp = true;
			
	int maxNode = MAXNODE;
		
	// number of target node where node occur
	int  acdeg[], 
	// index of nodes of current path
	lindex[];
	// sum of degrees for each node
	float adeg[];
	
	
	Node[] 	     
	     // all nodes of network which have a degree
	     allNodes;
	
	// One record for each target node
	// Record store intermediate nodes, their counter, 
	// min path length, number of min path
	Record[] records ;

	
	// SNA result handler
	SSNAHandler handler;
	
	Node start;
	
	
	int 
	index = 0,
	count = 0, 
	maxPathLength = 0, 
	countPath 	= 0, 
	countResult = 0, 
	totalResult = 0;
	
	class ListPath extends ArrayList<Path> {}
	
	SSNA(){
		this(MAXNODE);
	}

	SSNA(int lnode){
		
		maxNode = lnode;
		records = new Record[lnode];
		acdeg 	= new int[lnode];
		adeg 	= new float[lnode];

		allNodes	= new Node[lnode];
		
		for (int i = 0; i<lnode; i++){
			acdeg[i] = 0;
			adeg[i]  = 0;
		}
		
		lindex = new int[lnode];
						
		handler = new SSNAHandler();
	}
	
	
	public static SSNA create(){
		return  new SSNA();
	}
	
	public static SSNA create(int lnode){
		return  new SSNA(lnode);
	}
	
	public void set(SSNAHandler h){
		handler = h;
	}
	
	void setFake(boolean b){
		isFake = b;
	}
	
	boolean isFake(){
		return isFake;
	}
	
	
	/**
	 * ?x knows+ :: $path ?y
	 * group by ?x ?y
	 * 
	 * Mappings with same value of ?x
	 */
	
	public void start(){
		
	}

	/**
	 * After all source x target paths have been found
	 * Compute the final degrees
	 */
	public void complete(){
		if (isFake()){
			return;
		}
		
		int n = 0;
		for (Node node : allNodes){
			if (node != null){
				float deg = getDegree(n);
				if (deg!=0){
					float degree = deg / getCountDegree(n); 
					setDegree(n, degree);
					handler.degree(node, degree);
				}
			}
			n++;
		}
	}

	

	
	
	List<Node> result(){
		ArrayList<Node> list = new ArrayList<Node>();
		
		for (Node node : allNodes){
			if (node!=null && getDegree(node)>0){
				list.add(node);
			}
		}
		
		Collections.sort(list, new  Compare());
		
		Collections.reverse(list);
		
		return list;
	}
	
	
	class Compare implements Comparator<Node> {
		
		public int compare(Node o1, Node o2) {
			float d1 = getDegree(o1);
			float d2 = getDegree(o2);
			int res = Float.compare(d1, d2);
			if (res == 0){
				return o2.getLabel().compareTo(o1.getLabel());
			}
			return res;
		}
	}
	
	
	void display(List<Node> list){
		for (Node node : list){
			System.out.println(node + " " + getDegree(node));
		}
		System.out.println("___");
	}
	
	
	
	/****************************************************************/
	 
	/**
	 * Generate a unique index for node
	 * Nodes are stored in arrays at this index
	 */
	int index(Node node){
		return node.getIndex();
	}

	
	int getCountDegree(int n){
		return acdeg[n];
	}
	
	void addDegree(int n, float f){
		adeg[n] += f;
		acdeg[n] += 1;
	}

	
	float getDegree(int n){
		return adeg[n];
	}
	
	void setDegree(int n, float d){
		adeg[n] = d;
	}
	
	float getDegree(Node n){
		return getDegree(index(n));
	}
	
	int getMaxPathLength(){
		return maxPathLength;
	}
	
	int getCountPath(){
		return countPath;
	}
	

	
	/****************************************************
	 * 
	 * SNA as ResultListener
	 * 
	 */
	
	/**
	 * Start a new source Node
	 */
	void reset(){
		if (index != 0){
			System.out.println("** Error: index not zero: " + index);
		}
		countResult = 0;		
	}
	
	/**
	 * Number of results between two reset()
	 */
	int nbResult(){
		return countResult;
	}
	
	int totalResult(){
		return totalResult;
	}
	

	
	
	/**
	 * kgram call process(env) for each result
	 * here we store the path and that's it
	 */
	public boolean process(Environment env){
		Memory mem = (Memory) env;
		Path path = mem.getPath();
		return process(path);
	}
	
	public boolean process(Path path){
		countResult++;
		totalResult++;
		
		if (path.size() > maxPathLength){
			maxPathLength = path.size();
		}
		
		if (isFake()){
			return false;
		}
		
		handle(path);		
		
		return false;
	}


	public boolean enter(Node node, Regex exp, int size) {
		//System.out.println("** Enter: " + node + " " + size);
		return false;
	}


	public boolean leave(Node node, Regex exp, int size) {
		//System.out.println("** Leave: " + node + " " + size);
		return false;
	}
	
	
	/**
	 * 
	 */
	public boolean enter(Entity ent, Regex exp, int size) {
		if (speedUp){
			enter(ent.getNode(1));
		}
		return false;
	}	

	public boolean leave(Entity ent, Regex exp, int size) {
		if (speedUp){
			leave();
		}
		return false;
	}
	
	/**
	 * Add new target node to current path
	 * Store it's index in lindex array
	 * hence lindex[i] = index of ith node of current path
	 * speedUp enumeration of node index in handle(path)
	 */
	void enter(Node node){
		int id = index(node);
		lindex[index] = id;
		allNodes[id] = node;
		index++;
	}

	void leave(){
		lindex[index--] = -1;
	}
	
	/****************************************************************************
	 * 
	 * Store intermediate nodes (with their counter) between each pair x,y
	 * instead of storing all paths between x,y
	 * 
	 **/
	
			
	/**
	 * Handle one path on the fly
	 */
	void handle(Path path){
		int size = path.size();
		
		if (size==0){
			return;
		}
		
		Node source = path.getSource();
		Node target = path.getTarget();
		
		if (source.same(target)){
			return;
		}
		
		// intermediate nodes between source, target
		Record r = getRecord(target);
				
		if (size < r.size()){
			// this is a shorter path: reset counters
			r.reset();
		}
		
		if (size>1){
			
			if (speedUp){
				r.count();
			}
			else {
				int n = 1;
				
				for (Entity ent : path.getEdges()){
					// count inter
					if (n++ < size){
						// skip last node which is target
						r.count(ent.getNode(1));
					}
				}
			}
		}

		r.countPath(size);

	}
	
	
	/**
	 * 
	 */
	Record getRecord(Node target){
		int id = index(target);
		Record r = records[id];
		if (r == null){
			r = new Record(target);
			records[id] = r;
		}
		return r;
	}
	
	

	/**
	 * For one x and for each y
	 * Process intermediate nodes between x,y
	 * Compute degrees of intermediate nodes
	 */
	void process(){
		if (isFake()){
			return;
		}
		
		int i = 0;
		
		for (Record r : records){
			// process one target y
			if (r!=null){
				if (r.size()>1){
					r.process();
				}
			}
			records[i] = null;
			i++;
		}
	}
	
	
	
	
	
	
	/**
	 * Store intermediate node & counters between x, y
	 * Size of min path
	 * number of paths with this min size
	 * 
	 */
	class Record {
		
		int size = 0, count = 0;
		Node target;
		// one counter for each intermediate node
		int[] counter;
		

		Record(Node t){
			target = t;
			counter = new int[maxNode];
			init(counter);
		}
		
		void reset(){
			init(counter);
			count = 0;
			size = 0;
		}
		
		void init(int[] counter){
			for (int i=0; i<counter.length; i++){
				counter[i] = 0;
			}
		}
		
		void count(Node node){
			Integer id = index(node);
			allNodes[id] = node;
			counter[id] += 1;
		}
		
		void count(int id){
			counter[id] += 1;
		}
		
		/**
		 * Handle new path:
		 * increment counter of each intermediate node of path
		 * path is represented as array of index of node 
		 * lindex[i] = index of ith node
		 * counter[id] is counter of node with index = id
		 */
		void count(){
			int max = index-1;

			for (int i = 0; i<max; i++){
				// lindex[i] = index of ith node of path
				// this loop is at the core of SNA, hence we enumerate index array to speed up
				counter[lindex[i]] += 1;
			}
		}
		
		
		Integer getCount(Node node){
			return counter[index(node)];
		}
		
		void countPath(int size){
			this.size = size;
			count++;
		}
		
		int size(){
			return size;
		}
		
		
		/**
		 * Compute degree of intermediate nodes between x,y
		 */
		void process(){
			int i = 0;
			float fcount = (float) count;
			for (int cc : counter){
				if (cc != 0){
					float deg = (float) cc / fcount;
					addDegree(i, deg);
				}
				i++;
			}
		}
	}
	

}

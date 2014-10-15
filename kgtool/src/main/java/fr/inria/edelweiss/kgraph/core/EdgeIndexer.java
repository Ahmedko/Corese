package fr.inria.edelweiss.kgraph.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.tool.MetaIterator;
import java.util.HashMap;

/**
 * Table property node -> List<Edge>
 * Sorted by getNode(index), getNode(other) At the beginning, only table of
 * index 0 is fed with edges Other index are built at runtime only if needed Use
 * a new Edge Index based on Node int index
 *
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 *
 */
public class EdgeIndexer //extends HashMap<Node, ArrayList<Entity>> 
        implements Index {

    private static final String NL = System.getProperty("line.separator");
    static final int IGRAPH = Graph.IGRAPH;
    static final int ILIST = Graph.ILIST;
    private static Logger logger = Logger.getLogger(EdgeIndex.class);
    static boolean byKey = Graph.valueOut;
    private boolean byIndex = true;
    int index = 0, other = 1;
    int count = 0;
    int scoIndex = -1, typeIndex = -1;
    boolean isDebug = !true,
            isUpdate = true,
            isIndexer = false,
            // do not create entailed edge in kg:entailment if it already exist in another graph
            isOptim = false;
    Comparator<Entity> comp;
    Graph graph;
    List<Node> sortedProperties;
    HashMap<Node, EdgeList> table;

    EdgeIndexer(Graph g, boolean bi, int n) {
        init(g, bi, n);
        table = new HashMap();
    }

    void init(Graph g, boolean bi, int n) {
        graph = g;
        index = n;
        byIndex = bi;
        switch (index) {
            case 0:
                other = 1;
                break;
            default:
                other = 0;
                break;
        }
    }

    public int size() {
        return table.size();
    }

    void put(Node n, EdgeList l) {
        table.put(n, l);
    }

    public EdgeList get(Node n) {
        return table.get(n);
    }

     int intCompare(int n1, int n2) {
        if (n1 < n2) {
            return -1;
        } else if (n1 == n2) {
            return 0;
        } else {
            return +1;
        }
    }
     
     int nodeCompare(Node n1, Node n2){
         return intCompare(n1.getIndex(), n2.getIndex());
     }

    public boolean same(Node n1, Node n2) {
        return n1.getIndex() == n2.getIndex();
    }

   

    Node getNode(Entity ent, int n) {
        if (n == IGRAPH) {
            return ent.getGraph();
        }
        return ent.getNode(n);
    }

    /**
     * Ordered list of properties For pprint TODO: optimize it
     */
    public List<Node> getSortedProperties() {
        if (isUpdate) {
            sortProperties();
            isUpdate = false;
        }
        return sortedProperties;
    }

    synchronized void sortProperties() {
        sortedProperties = new ArrayList<Node>();
        for (Node pred : getProperties()) {
            sortedProperties.add(pred);
        }
        Collections.sort(sortedProperties, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                // TODO Auto-generated method stub
                return o1.compare(o2);
            }
        });
    }

    public Iterable<Node> getProperties() {
        return table.keySet();
    }

    public void setDuplicateEntailment(boolean b) {
        isOptim = !b;
    }

    public int getIndex() {
        return index;
    }

    public Iterable<Entity> getEdges() {
        MetaIterator<Entity> meta = new MetaIterator<Entity>();
        for (Node pred : getProperties()) {
            Iterable<Entity> it = get(pred);
            meta.next(it);
        }
        if (meta.isEmpty()) {
            return null;
        }
        return meta;
    }

    public String toString() {
        return toRDF();
    }

    public String toRDF() {
        Serializer sb = new Serializer();
        sb.open("kg:Index");
        sb.appendPNL("kg:index ", index);
        int total = 0;
        for (Node pred : getSortedProperties()) {
            int i = get(pred).size();
            if (i > 0) {
                total += i;
                sb.append("kg:item [ ");
                sb.appendP("rdf:predicate ", pred);
                sb.append("rdf:value ", i);
                sb.appendNL("] ;");
            }
        }
        sb.appendNL("kg:total ", total);
        sb.close();
        return sb.toString();
    }

    public int cardinality() {
        int total = 0;
        for (Node pred : getProperties()) {
            total += get(pred).size();
        }
        return total;
    }

    /**
     * Clean the content of the Index but keep the properties Hence Index can be
     * reused
     */
    public void clean() {
        for (Node p : getProperties()) {
            get(p).clear();
        }
    }

    public void clear() {
        isUpdate = true;
        if (index == 0) {
            logClear();
        }
        table.clear();
    }

    public void clearIndex(Node pred) {
        EdgeList l = get(pred);
        if (l != null && l.size() > 0) {
            l.clear();
        }
    }

    public void clearCache() {
        for (Node pred : getProperties()) {
            clearIndex(pred);
        }
    }

    /**
     * Add a property in the table
     */
    public Entity add(Entity edge) {
        return add(edge, false);
    }

    /**
     * noGraph == true => if edge already exists in another graph, do not add
     * edge
     */
    public Entity add(Entity edge, boolean duplicate) {
        if (index != IGRAPH && edge.nbNode() <= index) {
            // use case:  additional node is not present, do not index on this node
            // never happens for subject object and graph
            return null;
        }

        EdgeList el = define(edge.getEdge().getEdgeNode());
       
        if (isSort(edge)) {
            // edges are sorted, check presence by dichotomy
            int i = el.getPlace(edge);
            if (i == -1) {
                count++;
                return null;
            }       

            if (onInsert(edge)) {
                el.add(i, edge);
                logInsert(edge);
            } else {
                return null;
            }
        } else {
            // edges are not already sorted (load time)
            // add all edges, duplicates will be removed later when first query occurs
            if (onInsert(edge)) {
                el.add(edge);
                logInsert(edge);
            } else {
                return null;
            }
        }

        //complete(edge);
        return edge;
    }

    /**
     * PRAGMA: All Edge in list have p as predicate Use case: Rule Engine
     */
    public void add(Node p, List<Entity> list) {
        EdgeList l = define(p);
        if (index == 0 || l.size() > 0) {
            l.add(list);
        }
        if (index == 0) {
            isUpdate = true;
        }
    }

    Entity tag(Entity ent) {
        graph.tag(ent);
        return ent;
    }


    EdgeList getListByLabel(Entity e) {
        Node pred = graph.getPropertyNode(e.getEdge().getEdgeNode().getLabel());
        if (pred == null) {
            return null;
        }
        return get(pred);
    }

    public boolean exist(Entity edge) {
        if (index != IGRAPH && edge.nbNode() <= index) {
            // use case:  additional node is not present, do not index on this node
            // never happens for subject object and graph
            return false;
        }
        EdgeList list = getListByLabel(edge);
        if (list == null) {
            return false;
        }
        return list.exist(edge);
    }

    boolean isSort(Entity edge) {
        return !graph.isIndex();
    }

    /**
     * Store that the property exist by creating an empty list It may be fed
     * later if we need a join at getNode(index) If the list already contains
     * edges, we add it now.
     */
    public void declare(Entity edge) {
        declare(edge, true);
    }

    public void declare(Entity edge, boolean duplicate) {
        EdgeList list = define(edge.getEdge().getEdgeNode());
        if (list.size() > 0) {
            add(edge, duplicate);
        }
    }

    /**
     * Create and store an empty list if needed
     */
    private EdgeList define(Node predicate) {
        EdgeList list = get(predicate);
        if (list == null) {
            list = new EdgeList(predicate, index);
            setComparator(list);
            put(predicate, list);
        }
        return list;
    }
    
    /**
     * 
     * All EdgeList(index) share same Comparator
     */       
    void setComparator(EdgeList el){
        if (comp == null){
            comp = el.getComparator(index);
        }
        el.setComparator(comp);
    }

    /**
     * Sort edges by values of first Node and then by value of second Node
     */
    public void index() {
        index(true);
    }

    public void index(boolean reduce) {
        for (Node pred : getProperties()) {
            index(pred);
        }
        if (reduce && index == 0) {
            reduce();
        }
    }

    public void index(Node pred, boolean reduce) {
        index(pred);
        if (reduce) {
            reduce(pred);
        }
    }

    public void index(Node pred) {
        EdgeList el = get(pred);
        el.sort();
    }

    public void indexNode() {
        for (Node pred : getProperties()) {
            for (Entity ent : get(pred)) {
                graph.define(ent);
            }
        }
    }

    /**
     * eliminate duplicate edges
     */
    private void reduce() {
        for (Node pred : getProperties()) {
            reduce(pred);
        }
    }

    private void reduce(Node pred) {
        EdgeList el = get(pred);
        int rem = el.reduce();
        if (rem > 0) {
            graph.setSize(graph.size() - rem);
        }
    }

    public int duplicate() {
        return count;
    }

    /**
     * Check that this table has been built and initialized (sorted) for this
     * predicate If not, copy edges from table of index 0 and then sort these
     * edges
     */
      EdgeList checkGet(Node pred) {
        if (index == 0) {
            return get(pred);
        } else {
            return synCheckGet(pred);
        }
    }


    /**
     * Create the index of property pred on nth argument It is synchronized on
     * pred hence several synGetCheck can occur in parallel on different pred
     */
    private EdgeList synCheckGet(Node pred) {
        synchronized (pred) {
            EdgeList list = get(pred);
            if (list != null && list.size() == 0) {
                EdgeList std = (EdgeList) graph.getIndex().getEdges(pred, null);
                list.copy(std);
                list.sort();
            }
            return list;
        }
    }

    /**
     * Return iterator of Edge with possibly node as element
     */
    public Iterable<Entity> getEdges(Node pred, Node node) {
        return getEdges(pred, node, null);
    }

    public Iterable<Entity> getEdges(Node pred, Node node, Node node2) {
        EdgeList list = checkGet(pred);
        if (list == null || node == null) {
            return list;
        }
        if (node2 == null) {
            return list.getEdges(node);
        }
        return list.getEdges(node, node2);
    }

    public int size(Node pred) {
        EdgeList list = get(pred);
        if (list == null) {
            return 0;
        }
        return list.size();
    }

   
    /**
     * Written for index = 0
     */
    public boolean exist(Node pred, Node n1, Node n2) {
        EdgeList list = checkGet(pred);
        if (list == null) {
            return false;
        }
        return list.exist(n1, n2);
    }

    void trace(List<Entity> list) {
        Node nn = list.get(0).getNode(1);
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).getNode(1).same(nn)) {
                nn = list.get(i).getNode(1);
                logger.debug(nn);
            }
        }
    }

    /**
     * @return the byIndex
     */
    public boolean isByIndex() {
        return byIndex;
    }

    /**
     * @param byIndex the byIndex to set
     */
    public void setByIndex(boolean byIndex) {
        this.byIndex = byIndex;
        index(false);
    }

   

    /**
     * **********************************************************************
     *
     * Update
     *
     * TODO: remove nodes from individual & literal if needed
     *
     * TODO: semantics of default graph (kgraph:default vs union of all graphs)
     *
     */
    

    public Entity delete(Entity edge) {
        if (index != IGRAPH && edge.nbNode() <= index) {
            // use case:  additional node is not present, do not index on this node
            // never happens for subject object and graph
            return null;
        }
        EdgeList list = getListByLabel(edge);
        if (list == null) {
            return null;
        }

        int i = list.findIndex(edge);

        if (i == -1) {
            return null;
        }

        Entity ent = list.remove(i);
        if (getIndex() == 0) {
            graph.setSize(graph.size() - 1);
        }
        logDelete(ent);
        return ent;
    }

    boolean onInsert(Entity ent) {
        return graph.onInsert(ent);
    }

    void logDelete(Entity ent) {
        if (ent != null) {
            isUpdate = true;
            if (getIndex() == 0) {
                graph.logDelete(ent);
            }
        }
    }

    void logInsert(Entity ent) {
        isUpdate = true;
        if (getIndex() == 0) {
            graph.logInsert(ent);
        }
    }

    void logClear() {
        for (Node node : getSortedProperties()) {
            for (Entity ent : get(node)) {
                logDelete(ent);
            }
        }
    }

   

    /**
     * PRAGMA
     * Update functions are called with index = IGRAPH
     */
    public void clear(Node gNode) {
        update(gNode, null, Graph.CLEAR);
    }

    public void copy(Node g1, Node g2) {
        update(g2, null, Graph.CLEAR);
        update(g1, g2, Graph.COPY);
    }

    public void move(Node g1, Node g2) {
        update(g2, null, Graph.CLEAR);
        update(g1, g2, Graph.MOVE);
    }

    public void add(Node g1, Node g2) {
        update(g1, g2, Graph.COPY);
    }

    private void update(Node g1, Node g2, int mode) {
        for (Node pred : getProperties()) {

            EdgeList list = checkGet(pred);
            if (list == null) {
                continue;
            }

            if (isDebug) {
                for (Entity ee : list) {
                    logger.debug("** EI: " + ee);
                }
            }

            int n = list.findIndex(g1);
            if (isDebug) {
                logger.debug("** EI find: " + g1 + " " + (n != -1));
            }

            if (n == -1) {
                continue;
            }

            update(g1, g2, list, n, mode);

        }
    }

    private void update(Node g1, Node g2, EdgeList list, int n, int mode) {
        boolean isBefore = false;
        
        if (g2 != null && nodeCompare(g1, g2) < 0) {
            isBefore = true;
        }
        
        for (int i = n; i < list.size();) {
            Entity ent = list.get(i), ee;
            if (same(getNode(ent, index), g1)) {

                switch (mode) {

                    case Graph.CLEAR:
                        clear(ent);
                        list.remove(i);
                        break;

                    case Graph.MOVE:
                        clear(ent);
                        list.remove(i);
                        ee = copy(g2, ent);
                        if (isBefore) {
                        } else if (ee != null) {
                            i++;
                        }
                        break;

                    case Graph.COPY:
                        ee = copy(g2, ent);
                        // get next ent
                        i++;
                        // g2 is before g1 hence ent was added before hence incr i again
                        if (isBefore) {
                        } else if (ee != null) {
                            i++;
                        }
                        break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * TODO: setUpdate(true)
     */
    private Entity copy(Node gNode, Entity ent) {
        return graph.copy(gNode, ent);
    }

    private void clear(Entity ent) {
        for (Index ei : graph.getIndexList()) {
            if (ei.getIndex() != IGRAPH) {
                Entity rem = ei.delete(ent);
                if (isDebug && rem != null) {
                    logger.debug("** EI clear: " + ei.getIndex() + " " + rem);
                }
            }
        }
    }

    public void delete(Node pred) {
    }
}

package fr.inria.edelweiss.kgraph.core;

import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import static fr.inria.edelweiss.kgraph.core.EdgeIndexer.IGRAPH;
import static fr.inria.edelweiss.kgraph.core.EdgeIndexer.ILIST;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Edge List of a predicate 
 * Edges are sorted according to 
 * 1- edge.getNode(index).getIndex() (subject)
 * 2- other nodes (object, graph)
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2014
 *
 */
public class EdgeList implements Iterable<Entity> {

    Node predicate;
    ArrayList<Entity> list;
    Comparator<Entity> comp;
    int index = 0, other = 0;

    EdgeList(Node p, int i) {
        predicate = p;
        list = new ArrayList<Entity>();
        index = i;
        if (index == 0) {
            other = 1;
        }
    }

    ArrayList<Entity> getList() {
        return list;
    }

    public int size() {
        return list.size();
    }

    void clear() {
        list.clear();
    }

    Entity get(int i) {
        return list.get(i);
    }

    /**
     * Remove duplicate edges
     */
    int reduce() {
        ArrayList<Entity> l = new ArrayList<Entity>();
        Entity pred = null;
        int count = 0;
        for (Entity ent : list) {
            if (pred == null) {
                l.add(ent);
            } else if (comp.compare(ent, pred) != 0) {
                l.add(ent);
            } else {
                count++;
            }
            pred = ent;
        }
        list = l;
        return count;
    }

    void sort() {
        Collections.sort(list, comp);
    }

    /**
     * Copy Index(0) into this index
     */
    void copy(EdgeList el) {
        list.ensureCapacity(el.size());
        if (index < 2) {
            // we are sure that there are at least 2 nodes
            list.addAll(el.getList());
        } else {
            for (Entity ent : el) {
                // if additional node is missing: do not index this edge
                if (ent.nbNode() > index) {
                    list.add(ent);
                }
            }
        }
    }

    void add(Entity ent) {
        list.add(ent);
    }

    void add(int i, Entity ent) {
        list.add(i, ent);
    }

    Entity remove(int i) {
        Entity ent = list.get(i);
        list.remove(i);
        return ent;
    }

    /**
     * PRAGMA: All Edge in list have p as predicate, no duplicates Use case:
     * Rule Engine
     */
    void add(List<Entity> l) {
        list.ensureCapacity(l.size() + list.size());
        list.addAll(l);
    }

    /**
     * Return place where edge should be inserted in this Index return -1 if
     * already exists
     */
    int getPlace(Entity edge) {
        int i = find(edge);

        if (i >= list.size()) {
            i = list.size();
        } else if (index == 0) {
            int res = comp.compare(edge, list.get(i));
            if (res == 0) {
                // eliminate duplicate at load time for index 0                   
                return -1;
            }
        }

        return i;
    }

    /**
     * Place of edge in this Index
     */
    int find(Entity edge) {
        return find(edge, 0, list.size());
    }

    int find(Entity edge, int first, int last) {
        if (first >= last) {
            return first;
        } else {
            int mid = (first + last) / 2;
            int res = comp.compare(list.get(mid), edge);
            if (res >= 0) {
                return find(edge, first, mid);
            } else {
                return find(edge, mid + 1, last);
            }
        }
    }

    /**
     * Test if an edge (n1 p n2) exist in this Index (in any named graph)
     */
    boolean exist(Node n1, Node n2) {
        int n = find(n1.getIndex(), n2.getIndex(), 0, list.size());
        if (n >= 0 && n < list.size()) {
            Entity ent = list.get(n);
            if (n1.getIndex() == getNodeIndex(ent, 0)
                    && n2.getIndex() == getNodeIndex(ent, 1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find index of edge If not found, return -1
     */
    int findIndex(Entity ent) {
        int i = find(ent);

        if (i >= size()) {
            return -1;
        }

        int res = comp.compare(ent, list.get(i));

        if (res == 0) {
            return i;
        }

        return -1;
    }

    /**
     * Find index of node as node(index) -1 if not found
     */
    int findIndex(Node n) {
        int i = find(n.getIndex(), 0, list.size());
        if (i >= 0 && i < list.size()
                && getNodeIndex(i, index) == n.getIndex()) {
            return i;
        }
        return -1;
    }

    boolean exist(Entity ent) {
        int i = findIndex(ent);
        return i != -1;
    }

    /**
     * Iterate edges with index node node1 (and other node node2)
     */
    Iterable<Entity> getEdges(Node node1, Node node2) {
        int n = find(node1.getIndex(), node2.getIndex(), 0, list.size());
        if (n >= 0 && n < list.size()) {
            int n1 = getNodeIndex(n, index);
            if (n1 == node1.getIndex()) {
                int n2 = getNodeIndex(n, other);
                if (n2 != node2.getIndex()) {
                    return null;
                }
                Iterate it = new Iterate(this, n);
                return it;
            }
        }
        return null;
    }

    /**
     * Iterate edges with index node node1
     */
    Iterable<Entity> getEdges(Node node1) {
        // node is bound, enumerate edges where node = edge.getNode(index)
        int n = find(node1.getIndex(), 0, list.size());
        if (n >= 0 && n < list.size()) {
            int n1 = getNodeIndex(n, index);
            if (n1 == node1.getIndex()) {
                Iterate it = new Iterate(this, n);
                return it;
            }
        }
        return null;
    }

    /**
     * n1 = node.getIndex() return index of edge where edge.getNode(index) ==
     * node
     */
    int find(int n1, int first, int last) {
        if (first >= last) {
            return first;
        } else {
            int mid = (first + last) / 2;
            int res = intCompare(getNodeIndex(mid, index), n1);
            if (res >= 0) {
                return find(n1, first, mid);
            } else {
                return find(n1, mid + 1, last);
            }
        }
    }

    /**
     * n1 = node1.getIndex() n2 = node2.getIndex() return index of edge where
     * edge.getNode(index) == node1 and edge.getNode(other) == node2
     */
    int find(int n1, int n2, int first, int last) {
        if (first >= last) {
            return first;
        } else {
            int mid = (first + last) / 2;
            if (compare(list.get(mid), n1, n2) >= 0) {
                return find(n1, n2, first, mid);
            } else {
                return find(n1, n2, mid + 1, last);
            }
        }
    }

    /**
     * n1 n2 are node.getIndex()
     */
    int compare(Entity ent, int n1, int n2) {
        int res = intCompare(getNodeIndex(ent, index), n1);
        if (res == 0) {
            res = intCompare(getNodeIndex(ent, other), n2);
        }
        return res;

    }

    /**
     * n1 n2 are node.getIndex()
     */
    int intCompare(int n1, int n2) {
        if (n1 < n2) {
            return -1;
        } else if (n1 == n2) {
            return 0;
        } else {
            return +1;
        }
    }

    @Override
    public Iterator<Entity> iterator() {
        return list.iterator();
    }

    /**
     * edge = list.get(n) node = edge.getNode(index) Return an iterator of edges
     * with node as index element
     */
    class Iterate implements Iterable<Entity>, Iterator<Entity> {

        List<Entity> list;
        int node;
        int ind, start;
        EdgeQuad edge;

        Iterate(EdgeList l, int n) {
            list = l.getList();
            node = getNodeIndex(list.get(n), index);
            start = n;
            // draft test
            edge = new EdgeQuad();
        }

        @Override
        public Iterator<Entity> iterator() {
            ind = start;
            return this;
        }

        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            boolean b = ind < list.size()
                    && getNodeIndex(list.get(ind), index) == node;
            return b;
        }

        @Override
        public Entity next() {
            // TODO Auto-generated method stub
            Entity ent = list.get(ind++);
            return ent; // getResult(ent);
        }

        @Override
        public void remove() {
            // TODO Auto-generated method stub
        }
        
        Entity getResult(Entity ent){
            edge.setNode(0, ent.getNode(0));
            edge.setNode(1, ent.getNode(1));
            edge.setEdgeNode(ent.getEdge().getEdgeNode());
            edge.setGraph(ent.getGraph());
            return edge;
        }
    }

    int getNodeIndex(Entity ent, int n) {
        if (n == IGRAPH) {
            return ent.getGraph().getIndex();
        }
        return ent.getNode(n).getIndex();
    }

    int getNodeIndex(int i, int n) {
        Entity ent = list.get(i);
        if (n == IGRAPH) {
            return ent.getGraph().getIndex();
        }
        return ent.getNode(n).getIndex();
    }

    /**
     * **************************************************************
     */
    Comparator<Entity> getComparator(int n) {
        switch (n) {
            case ILIST:
                return getListComparator();
        }
        return getComparator();
    }
    
    void setComparator(Comparator<Entity> c){
        comp = c;
    }


    /**
     *
     */
    Comparator<Entity> getComparator() {

        return new Comparator<Entity>() {
            
            public int compare(Entity o1, Entity o2) {

                // first check the index node
                int res = intCompare(getNodeIndex(o1, index), getNodeIndex(o2, index));

                if (res != 0) {
                    return res;
                }

                // common arity
                int min = Math.min(o1.nbNode(), o2.nbNode());

                for (int i = 0; i < min; i++) {
                    // check other common arity nodes
                    if (i != index) {
                        res = intCompare(getNodeIndex(o1, i), getNodeIndex(o2, i));
                        if (res != 0) {
                            return res;
                        }
                    }
                }

                if (o1.nbNode() == o2.nbNode()) {
                    // same arity, common arity nodes are equal
                    // check graph

                    return intCompare(getNodeIndex(o1, IGRAPH), getNodeIndex(o2, IGRAPH));
                }
                else if (o1.nbNode() < o2.nbNode()) {
                    // smaller arity edge is before
                    return -1;
                } else {
                    return 1;
                }

            }
        };
    }

    // sort in *reverse* order of edge timestamp index
    Comparator<Entity> getListComparator() {

        return new Comparator<Entity>() {
            public int compare(Entity o1, Entity o2) {
                int i1 = o1.getEdge().getIndex();
                int i2 = o2.getEdge().getIndex();
                
                if (i1 > i2) {
                    return -1;
                } else if (i1 == i2) {
                    return 0;
                } else {
                    return 1;
                }
            }
        };

    }

}

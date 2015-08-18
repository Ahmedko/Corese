package fr.inria.edelweiss.kgtool.transform;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.logic.Entailment;
import fr.inria.edelweiss.kgraph.logic.RDF;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Transformer Visitor to be used in a transformation
 * Store and/or log visited nodes, enumerate visited nodes
 * 
 * st:visit(st:start, st:trace)
 * st:visit(st:silent, false)
 * st:visit(st:exp, ?x)
 * bind(st:visited() as ?node)
 * 
 * Olivier Corby, Wimmics INRIA I3S - 2015
 *
 */
public class DefaultVisitor implements TemplateVisitor {
    static final String STL         = NSManager.STL;
    static final String TRACE       = STL + "trace";
    static final String START       = STL + "start";
    static final String TRANSFORM   = STL + "transform";
    static final String SILENT      = STL + "silent";
    static final String ACCEPT      = STL + "accept";
    static final String DEFAULT     = STL + "default";
    static final String SET         = STL + "set";
    static final String GET         = STL + "get";
   
    Graph graph, visitedGraph;
    IDatatype visitedNode;
    HashMap <String, Boolean> map;
    ArrayList<IDatatype> list;
    private HashMap<IDatatype, IDatatype> distinct, value;
    
    private String transform = Transformer.TURTLE;
    private boolean silent = true;
    private String NL = System.getProperty("line.separator");
    // boolean value (if any) that means that visitor must consider visited node
    // use case: st:visit(st:exp, ?x, ?suc)
    // if (?suc = acceptValue) node ?x is considered
    private boolean acceptValue = false;
    // by default accept (or not) all exp such as st:subexp
    private boolean defaultAccept = true;
    boolean isDistinct = true;
    
    
    DefaultVisitor(){
        map      = new HashMap();
        distinct = new HashMap();
        value    = new HashMap();
        list     = new ArrayList();
        visitedGraph = Graph.create();
        visitedNode = DatatypeMap.createObject("graph", visitedGraph);
    }
       
    public void visit(IDatatype name, IDatatype obj, IDatatype arg) {
       visit(name.getLabel(), obj, arg);
    }
    
    void visit(String name, IDatatype obj, IDatatype arg){
        if (name.equals(TRACE) || name.equals(START)){
            define(name, obj.getLabel(), arg);
        }
        else {
           process(name, obj, arg);
        }
    }
    
    /**
     * st:visit(st:trace, st:subexp, true)
     */
    void define(String name, String obj, IDatatype arg){
        if (arg == null){
            return;
        }
        if (obj.equals(TRACE)){
            silent = ! getValue(arg);
        }
        else if (obj.equals(TRANSFORM)){
            setTransform(arg.getLabel());
        }
        else if (obj.equals(SILENT)){
            silent = getValue(arg);
        }
        else if (obj.equals(ACCEPT)){
            // accept node when boolean value is arg
            // e.g. trace node with ?suc = false
            acceptValue = getValue(arg);
        }       
        else if (obj.equals(DEFAULT)){
            // by default accept (or not) all exp such as st:subexp
            defaultAccept = getValue(arg);
        }     
        else { 
            //st:visit(st:trace, st:subexp, true)
            map.put(obj, getValue(arg));
        }
    }
 
    void process(String name, IDatatype obj, IDatatype arg) {
       if (accept(name) && accept(arg)){
            store(name, obj);
            if (! silent){
                trace(name, obj);
            }
        }
    }
    
    void trace(String name, IDatatype obj) {
        Transformer t = Transformer.create(graph, getTransform());
        IDatatype dt = t.process(obj);
        System.out.println(name);
        System.out.println((dt != null) ? dt.getLabel() : obj);
        System.out.println();
    }

    
    boolean accept(String name){
        Boolean b = map.get(name);
        if (b == null){
            return defaultAccept;
        }
        return b;
    }
    
    boolean accept(IDatatype arg){
        if (arg == null){
            return true;
        }
        boolean b = getValue(arg);
        return b == isAcceptValue();
    }
    
 
    
    void store(String name, IDatatype obj){
        if (isDistinct){
            if (! distinct.containsKey(obj)){
                list.add(obj);
                distinct.put(obj, obj);
                //storeGraph(name, obj);
            }
        }
        else {
            list.add(obj);
        }
    }
    
    void storeGraph(String name, IDatatype obj){
        Entity ent = visitedGraph.add(DatatypeMap.newResource(Entailment.DEFAULT), 
                obj, DatatypeMap.newResource(RDF.TYPE), DatatypeMap.newResource(name));
    }
    
    StringBuilder toSB(){
        StringBuilder sb = new StringBuilder();
        Transformer t = Transformer.create(graph, getTransform());
        for (IDatatype dt : list){
            IDatatype res = t.process(dt);
            if (res != null){
                sb.append(res.getLabel());
                sb.append(NL);
                sb.append(NL);
            }
        }
        return sb;
    }
    
    public IDatatype display(){
        return DatatypeMap.newStringBuilder(toSB());     
    }
    
    public String toString(){
        return toSB().toString();
    }
    
    public Collection<IDatatype> visited(){  
        return list;
    }
    
    public IDatatype visitedGraph(){
        visitedGraph.init();
        return visitedNode;
    }
    
    public boolean isVisited(IDatatype dt){
        if (isDistinct){
            return distinct.containsKey(dt);
        }
        return list.contains(dt);
    }
     
     boolean getValue(IDatatype arg){
        try {
            return arg.isTrue();
        } catch (CoreseDatatypeException ex) {          
        }
        return false;
     }
     
     public void setGraph(Graph g){
         graph = g;
     }

    /**
     * @return the transform
     */
    public String getTransform() {
        return transform;
    }

    /**
     * @param transform the transform to set
     */
    public void setTransform(String transform) {
        this.transform = transform;
    }

    /**
     * @return the acceptValue
     */
    public boolean isAcceptValue() {
        return acceptValue;
    }

    /**
     * @param acceptValue the acceptValue to set
     */
    public void setAcceptValue(boolean acceptValue) {
        this.acceptValue = acceptValue;
    }

    /**
     * @return the distinct
     */
    public HashMap<IDatatype, IDatatype> getDistinct() {
        return distinct;
    }

    /**
     * @param distinct the distinct to set
     */
    public void setDistinct(HashMap<IDatatype, IDatatype> distinct) {
        this.distinct = distinct;
    }

    public IDatatype set(IDatatype obj, IDatatype prop, IDatatype arg) {
          value.put(obj, arg);
          return arg;
    }
    
    public IDatatype get(IDatatype obj, IDatatype prop) {
          return value.get(obj);
    }

}

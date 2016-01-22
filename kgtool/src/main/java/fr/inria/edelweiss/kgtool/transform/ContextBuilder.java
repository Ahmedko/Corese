package fr.inria.edelweiss.kgtool.transform;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.Context;
import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extract a Transformer Context from a profile.ttl graph st:param object
 * st:cal a st:Profile ;
 *   st:transform st:calendar ;
 *   st:param [ st:arg value ; st:title value ] .
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public class ContextBuilder {
    
    Graph graph;
    Context context;
    HashMap<String, Node> done; 
    
    public ContextBuilder(Graph g){
        this.graph = g;
        done = new HashMap<String, Node>();
    }
    
    public ContextBuilder(String path){
        this(Graph.create());
        Load ld = Load.create(graph);
        try {
            ld.loadWE(path);
        } catch (LoadException ex) {
            Logger.getLogger(ContextBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 
     * Create a Context from content of st:param
     */
    public Context process(){
        Entity ent = graph.getEdges(Context.STL_PARAM).iterator().next();
        if (ent == null){
            return new Context();
        }
        return process(ent.getNode(1));
    }
    
    public Context process(Node ctx){
        context = new Context();
        context(ctx, false);
        return context;
    }
        
    void context(Node ctx, boolean exporter) {
        importer(ctx);

        for (Entity ent : graph.getEdgeList(ctx)) {
            String label = ent.getEdge().getLabel();
            Node object = ent.getNode(1);
            
            if (label.equals(Context.STL_EXPORT) && object.isBlank()){
                // st:export [ st:lod (<http://dbpedia.org/>) ]
                context(object, true);
            }
            else if (! label.equals(Context.STL_IMPORT)) {

                if (object.isBlank()) {
                    IDatatype list = list(graph, object);
                    if (list != null) {
                        set(label, list, exporter);
                        continue;
                    }
                }
                set(label, (IDatatype) object.getValue(), exporter);
            }
        }        
    }
    
    void set(String name, IDatatype dt, boolean b){
        if (b){
            context.export(name, dt);
        }
        else {
            context.set(name, dt);
        }
    }
    
     /** 
      *           
      */
     void importer(Node n) {         
        for (Entity ent : graph.getEdges(Context.STL_IMPORT, n, 0)) {
            if (ent != null){
                Node imp = ent.getNode(1);
                if (done(imp)){
                    continue;
                }
                Edge par = graph.getEdge(Context.STL_PARAM, imp, 0);
                if (par != null) {
                    context(par.getNode(1), false);
                }
            }
        }
     }
     
     boolean done(Node n) {
        if (done.containsKey(n.getLabel())) {
            return true;
        } else {
            done.put(n.getLabel(), n);
        }
        return false;
    }
             
    IDatatype list(Graph g, Node object) {
        List<IDatatype> list = g.reclist(object);
        if (list == null) {           
            return null;
        }
       IDatatype dt = DatatypeMap.createList(list);
       return dt;
    }
    

}

package fr.inria.edelweiss.kgraph.query;

import java.util.Hashtable;

import org.apache.log4j.Logger;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.acacia.corese.triple.parser.Processor;
import fr.inria.edelweiss.kgenv.eval.ProxyImpl;
import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Expr;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Evaluator;
import fr.inria.edelweiss.kgram.api.query.Matcher;
import fr.inria.edelweiss.kgram.api.query.Producer;
import fr.inria.edelweiss.kgram.core.Mapping;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Memory;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgram.path.Path;
import fr.inria.edelweiss.kgraph.api.Loader;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.logic.Distance;
import fr.inria.edelweiss.kgtool.load.LoadException;
import fr.inria.edelweiss.kgtool.load.QueryLoad;
import fr.inria.edelweiss.kgtool.transform.Transformer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * Plugin for filter evaluator Compute semantic similarity of classes and
 * solutions for KGRAPH
 *
 * @author Olivier Corby, Edelweiss, INRIA 2011
 *
 */
public class PluginImpl extends ProxyImpl {

    static Logger logger = Logger.getLogger(PluginImpl.class);
    static String DEF_PPRINTER = Transformer.PPRINTER;
    public static boolean readWriteAuthorized = true;
    private static final String NL = System.getProperty("line.separator");
    static final String alpha = "abcdefghijklmnoprstuvwxyz";
    static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
   
    
    String PPRINTER = DEF_PPRINTER;
    // for storing Node setProperty() (cf Nicolas Marie store propagation values in nodes)
    // idem for setObject()
    static Table table;
    MatcherImpl match;
    Loader ld;
    private Object dtnumber;
    boolean isCache = false;
    TreeNode cache;
    
    ExtendGraph ext;
    private PluginTransform pt;
   

    PluginImpl(Matcher m) {
        if (table == null) {
            table = new Table();
        }
        if (m instanceof MatcherImpl) {
            match = (MatcherImpl) m;
        }
        dtnumber = getValue(Processor.FUN_NUMBER);
        cache = new TreeNode();
        ext = new ExtendGraph(this);
        pt  = new PluginTransform(this);
    }
    

    public static PluginImpl create(Matcher m) {
        return new PluginImpl(m);
    }  
    
    public void setMode(int mode){
        switch (mode){
            
            case Evaluator.CACHE_MODE:
                isCache = true;
            break;
                
            case Evaluator.NO_CACHE_MODE:
                isCache = false;
                cache.clear();
            break;                
        }
    }
    
    // DRAFT: store current query in the Graph
    public void start(Producer p, Environment env){
        
    }
    
    public void finish(Producer p, Environment env){
        Graph g = getGraph(p);
        if (g != null){
            g.getContext().setQuery(env.getQuery());
        }
    }

    public Object function(Expr exp, Environment env, Producer p) {

        switch (exp.oper()) {         

            case GRAPH:
                return getGraph(p);
                           
                          
            case SIM:
                Graph g = getGraph(p);
                if (g == null){
                    return null;
                }
                // solution similarity
                return similarity(g, env);
                
            case DESCRIBE:
                return ext.describe(p, exp, env); 
                
            default: 
                return pt.function(exp, env, p);
                
        }

    }

    public Object function(Expr exp, Environment env, Producer p, Object o) {
        IDatatype dt = datatype(o);

        switch (exp.oper()) {

            case KGRAM:
            case NODE:
           // case LOAD:
            case DEPTH:
            case SKOLEM:
                
                Graph g = getGraph(p);
                if (g == null){
                    return null;
                }
                
                switch (exp.oper()) {
                    case KGRAM:
                        return kgram(g, o);

                    case NODE:
                        return node(g, o);

//                    case LOAD:
//                        return load(g, o);

                    case DEPTH:
                        return depth(g, o);
                        
                     case SKOLEM:               
                        return g.skolem(dt);    
                }                
                                        
            case READ:
                return read(dt, env, p);
                                         

            case EVEN: 
                return even(exp, dt);
                
            case ODD:
                return odd(exp, dt);

            case GET:
                return getObject(o);

            case SET:
                return setObject(o, null);

            case QNAME:
                return qname(o, env);
                
            case PROVENANCE:
                return provenance(exp, env, o);
                
            case TIMESTAMP:
                return timestamp(exp, env, o);
                
            case INDEX:
               return index(p, exp, env, o); 
          
            case ID:
               return id(exp, env, dt); 
                
            case TEST:
                return test(p, exp, env, dt);
                
             case LOAD:
                return ext.load(p, exp, env, dt);
                 
             case EXTENSION:
                return ext.extension(p, exp, env, dt); 
                 
             case QUERY:
                return ext.query(p, exp, env, dt); 
                 
             case XT_GRAPH:
             case XT_SUBJECT:
             case XT_PROPERTY:
             case XT_OBJECT:
             case XT_INDEX:
                 return access(exp, env, p, dt);
                 
             case REVERSE:
                 return reverse(dt);
                 
                 
             default:
                 return pt.function(exp, env, p, dt);
           
        }
        
    }

 
    public Object function(Expr exp, Environment env, Producer p, Object o1, Object o2) {
        IDatatype dt1 = (IDatatype) o1,
                dt2 = (IDatatype) o2;
        switch (exp.oper()) {

            case GETP:
                return getProperty(dt1, dt2.intValue());

            case SETP:
                return setProperty(dt1, dt2.intValue(), null);

            case SET:
                return setObject(dt1, dt2);

               
            case SIM:              
            case PSIM:               
            case ANCESTOR:
                
                Graph g = getGraph(p);
                if (g == null){
                    return null;
                }
                switch (exp.oper()) {
                    case SIM:
                        // class similarity
                        return similarity(g, dt1, dt2);

                    case PSIM:
                        // prop similarity
                        return pSimilarity(g, dt1, dt2);


                    case ANCESTOR:
                        // common ancestor
                        return ancestor(g, dt1, dt2);
                }

             case WRITE:                
                return write(dt1, dt2);   
                
             case XT_VALUE:
                 return access(exp, env, p, dt1, dt2);
                
            case STORE:
                return ext.store(p, env, dt1, dt2);
                
            default:
                return pt.function(exp, env, p, dt1, dt2);
        }

    }

    public Object eval(Expr exp, Environment env, Producer p, Object[] args) {
       
        switch (exp.oper()) {
            
            case SETP:
                
                IDatatype dt1 =  (IDatatype) args[0];
                IDatatype dt2 =  (IDatatype) args[1];
                IDatatype dt3 =  (IDatatype) args[2];
                return setProperty(dt1, dt2.intValue(), dt3);
                
            case LIST:
                return list(args);
                
            case IOTA:
                return iotag(args);

            default: 
                return pt.eval(exp, env, p, args);  
        }

    }
    
    IDatatype iotag(Object[] args){
        IDatatype dt = ((IDatatype) args[0]);
        if (dt.isNumber()){
            return iota(args);
        }
        return iotas(args);
    }
    
    IDatatype iota(Object[] args){
        int start = 1;
        int end = 1;
        
        if (args.length > 1){
            start = ((IDatatype) args[0]).intValue();
            end =   ((IDatatype) args[1]).intValue();
        }
        else {
            end =   ((IDatatype) args[0]).intValue();
        }
        
        int step = 1;
        if (args.length == 3){
            step = ((IDatatype) args[2]).intValue();
        }
        int length = (end - start + step) / step;
        IDatatype[] ldt = new IDatatype[length];
        
        for (int i=0; i<length; i++){
            ldt[i] = DatatypeMap.newInstance(start);
            start += step;
        }
        IDatatype dt = DatatypeMap.createList(ldt);
        return dt;
    }
    
    IDatatype iotas(Object[] args){
        String fst = ((IDatatype) args[0]).stringValue();
        String snd = ((IDatatype) args[1]).stringValue();
        int step = 1;
        if (args.length == 3){
            step = ((IDatatype) args[2]).intValue();
        }               
        String str = alpha;
        int start = str.indexOf(fst);
        int end   = str.indexOf(snd);
        if (start == -1){
            str = ALPHA;
            start = str.indexOf(fst);
            end   = str.indexOf(snd);
        }
        if (start == -1 || end == -1){
            return null;
        }
       
        
        int length = (end - start + step) / step;
        IDatatype[] ldt = new IDatatype[length];
        
        for (int i=0; i<length; i++){
            ldt[i] = DatatypeMap.newInstance(str.substring(start, start+1));
            start += step;
        }
        IDatatype dt = DatatypeMap.createList(ldt);
        return dt;
    }
    
    
    IDatatype list(Object[] args){
        IDatatype[] ldt = new IDatatype[args.length];
        for (int i=0; i<ldt.length; i++){
            ldt[i] = (IDatatype) args[i];
        }
        IDatatype dt = DatatypeMap.createList(ldt);
        return dt;
    }
    
    IDatatype reverse(IDatatype dt){
        if ( ! dt.isArray()){
            return dt;
        }
        IDatatype[] value = dt.getValues();
        IDatatype[] res   = new IDatatype[value.length];
        int n = value.length - 1;
        for (int i = 0; i<value.length; i++){
            res[i] = value[n - i];
        }
        
        return DatatypeMap.createList(res);
    }
    

    IDatatype similarity(Graph g, IDatatype dt1, IDatatype dt2) {

        Node n1 = g.getNode(dt1.getLabel());
        Node n2 = g.getNode(dt2.getLabel());
        if (n1 == null || n2 == null) {
            return null;
        }

        Distance distance = g.setClassDistance();
        double dd = distance.similarity(n1, n2);
        return getValue(dd);
    }

    IDatatype ancestor(Graph g, IDatatype dt1, IDatatype dt2) {
        Node n1 = g.getNode(dt1.getLabel());
        Node n2 = g.getNode(dt2.getLabel());
        if (n1 == null || n2 == null) {
            return null;
        }

        Distance distance = g.setClassDistance();
        Node n = distance.ancestor(n1, n2);
        return (IDatatype) n.getValue();
    }

    IDatatype pSimilarity(Graph g, IDatatype dt1, IDatatype dt2) {
        Node n1 = g.getNode(dt1.getLabel());
        Node n2 = g.getNode(dt2.getLabel());
        if (n1 == null || n2 == null) {
            return null;
        }

        Distance distance = g.setPropertyDistance();
        double dd = distance.similarity(n1, n2);
        return getValue(dd);
    }

    /**
     * Similarity of a solution with Corese method Sum distance of approximate
     * types Divide by number of nodes and edge
     *
     * TODO: cache distance in Environment during query proc
     */
    public IDatatype similarity(Graph g, Environment env) {
        if (!(env instanceof Memory)) {
            return getValue(0);
        }
        Memory memory = (Memory) env;
        if (memory.getQueryEdges() == null){
            return getValue(0);
        }
        Hashtable<Node, Boolean> visit = new Hashtable<Node, Boolean>();
        Distance distance = g.setClassDistance();

        // number of node + edge in the answer
        int count = 0;
        float dd = 0;

        for (Edge qEdge : memory.getQueryEdges()) {

            if (qEdge != null) {
                Entity edge = memory.getEdge(qEdge);

                if (edge != null) {
                    count += 1;

                    for (int i = 0; i < edge.nbNode(); i++) {
                        // count nodes only once
                        Node n = edge.getNode(i);
                        if (!visit.containsKey(n)) {
                            count += 1;
                            visit.put(n, true);
                        }
                    }

                    if ((g.isType(qEdge) || env.getQuery().isRelax(qEdge))
                            && qEdge.getNode(1).isConstant()) {

                        Node qtype = g.getNode(qEdge.getNode(1).getLabel());
                        Node ttype = g.getNode(edge.getNode(1).getLabel());

                        if (qtype == null) {
                            // query type is undefined in ontology
                            qtype = qEdge.getNode(1);
                        }
                        if (ttype == null) {
                            // target type is undefined in ontology
                            ttype = edge.getNode(1);
                        }

                        if (!subClassOf(g, ttype, qtype, env)) {
                            dd += distance.distance(ttype, qtype);
                        }
                    }
                }
            }
        }

        if (dd == 0) {
            return getValue(1);
        }

        double sim = distance.similarity(dd, count);

        return getValue(sim);

    }

    boolean subClassOf(Graph g, Node n1, Node n2, Environment env) {
        if (match != null) {
            return match.isSubClassOf(n1, n2, env);
        }
        return g.isSubClassOf(n1, n2);
    }

    
    private IDatatype write(IDatatype dtfile, IDatatype dt) {
        if (readWriteAuthorized){
            QueryLoad ql = QueryLoad.create();
            ql.write(dtfile.getLabel(), dt.getLabel());
        }
        return dt;
    }
   


    Path getPath(Expr exp, Environment env){
        Node qNode = env.getQueryNode(exp.getExp(0).getLabel());
        if (qNode == null) {
            return null;
        }
        Path p = env.getPath(qNode);
        return p;
    }
    
    Entity getEdge(Expr exp, Environment env){
        Memory mem = (Memory) env;
        return mem.getEdge(exp.getExp(0).getLabel());
    }
    
    private Object provenance(Expr exp, Environment env, Object o) {
       Entity e = getEdge(exp, env);
       if (e == null){
           return  null;
       }
        return e.getProvenance();
    }
    
    // index of rule provenance object
     private Object id(Expr exp, Environment env, IDatatype dt) {
       Object obj = dt.getObject();
       if (obj != null && obj instanceof Query){
           Query q = (Query) obj;
           return getValue(q.getID());
       }
       return null;
    }

    private Object timestamp(Expr exp, Environment env, Object o) {
         Entity e = getEdge(exp, env);
        if (e == null){
            return  null;
        }
        int level = e.getEdge().getIndex();
        return getValue(level);
    }
    
    
    public IDatatype index(Producer p, Expr exp, Environment env, Object o){
        IDatatype dt = (IDatatype) o;
        Node n = p.getNode(dt);
        return getValue(n.getIndex());
    }
    
    private Object test(Producer p, Expr exp, Environment env, IDatatype dt) {
        IDatatype res = DatatypeMap.createObject("rule", env.getQuery());
        return res;
    }

    private Object even(Expr exp, IDatatype dt) {
        boolean b = dt.intValue() % 2 == 0 ;
        return getValue(b);
    }
    
    private Object odd(Expr exp, IDatatype dt) {
        boolean b = dt.intValue() % 2 != 0 ;
        return getValue(b);        
    }

    private IDatatype bool(Expr exp, Environment env, Producer p, IDatatype dt) {
        if (dt.stringValue().contains("false")){
            return FALSE;
        }
        return TRUE;
    }

    /**
     * @return the pt
     */
    public PluginTransform getPluginTransform () {
        return pt;
    }

    private Object access(Expr exp, Environment env, Producer p, IDatatype dt) {
        Object obj = dt.getObject();
        if (obj == null || ! (obj instanceof Entity)){
            return null;
        }
        Entity ent = (Entity) obj;        
        switch (exp.oper()){
            case XT_GRAPH:
                return ent.getGraph().getValue();
                
            case XT_SUBJECT:
                return ent.getNode(0).getValue();
                
            case XT_OBJECT:
                return ent.getNode(1).getValue();
                
            case XT_PROPERTY:
                return ent.getEdge().getEdgeNode().getValue();
                
            case XT_INDEX:
                return getValue(ent.getEdge().getIndex());
        }
        return null;
    }
    
    private Object access(Expr exp, Environment env, Producer p, IDatatype dtmap, IDatatype dtvar) {
        Object obj = dtmap.getObject();
        if (obj == null || ! (obj instanceof Mapping)){
            return null;
        }
        Mapping map = (Mapping) obj;  
        
        switch (exp.oper()){
            case XT_VALUE:
                Node n = map.getNode(dtvar.getLabel());
                if (n != null){
                    return n.getValue();
                }
        }
        
        return null;
    }

    
  
    class Table extends Hashtable<Integer, PTable> {
    }

    class PTable extends Hashtable<Object, Object> {
    }

    PTable getPTable(Integer n) {
        PTable t = table.get(n);
        if (t == null) {
            t = new PTable();
            table.put(n, t);
        }
        return t;
    }

    Object getObject(Object o) {
        return getProperty(o, Node.OBJECT);
    }

    IDatatype setObject(Object o, Object v) {
        setProperty(o, Node.OBJECT, v);
        return TRUE;
    }

    IDatatype setProperty(Object o, Integer n, Object v) {
        PTable t = getPTable(n);
        t.put(o, v);
        return TRUE;
    }

    Object getProperty(Object o, Integer n) {
        PTable t = getPTable(n);
        return t.get(o);
    }

    Node node(Graph g, Object o) {
        IDatatype dt = (IDatatype) o;
        Node n = g.getNode(dt, false, false);
        return n;
    }

    IDatatype depth(Graph g, Object o) {
        Node n = node(g, o);
        if (n == null || g.getClassDistance() == null) {
            return null;
        }
        Integer d = g.getClassDistance().getDepth(n);
        if (d == null) {
            return null;
        }
        return getValue(d);
    }

    IDatatype load(Graph g, Object o) {
        if (! readWriteAuthorized){
            return FALSE;
        }
        loader(g);
        IDatatype dt = (IDatatype) o;
        try {
            ld.loadWE(dt.getLabel());
        } catch (LoadException e) {
            logger.error(e);
            return FALSE;
        }
        return TRUE;
    }

    void loader(Graph g) {
        if (ld == null) {
            ld = ManagerImpl.getLoader();
            ld.init(g);
        }
    }

    Node kgram(Graph g, Object o) {
        IDatatype dt = (IDatatype) o;
        String query = dt.getLabel();
        return kgram(g, query);
    }
        
    Node kgram(Graph g, String  query) {    
        QueryProcess exec = QueryProcess.create(g, true);
        try {
            Mappings map = exec.sparqlQuery(query);
            if (map.getGraph() == null){
                return DatatypeMap.createObject("Mappings", map, IDatatype.MAPPINGS);
            }
            else {
                return DatatypeMap.createObject("Graph", map.getGraph(), IDatatype.GRAPH);
            }
        } catch (EngineException e) {
            return DatatypeMap.createObject("Mappings", new Mappings(), IDatatype.MAPPINGS);
        }
    }

    IDatatype qname(Object o, Environment env) {
        IDatatype dt = (IDatatype) o;
        if (!dt.isURI()) {
            return dt;
        }
        Query q = env.getQuery();
        if (q == null) {
            return dt;
        }
        ASTQuery ast = (ASTQuery) q.getAST();
        NSManager nsm = ast.getNSM();
        String qname = nsm.toPrefix(dt.getLabel(), true);
        if (qname.equals(dt.getLabel())) {
            return dt;
        }
        return getValue(qname);
    }
    
    
  
    
    IDatatype read(IDatatype dt, Environment env, Producer p){
        if (! readWriteAuthorized){
            return null;
        }
        QueryLoad ql = QueryLoad.create();
        String str = ql.read(dt.getLabel());
        if (str == null){
            str = "";
        }
        return DatatypeMap.newInstance(str);
    }


  
    String getLabel(IDatatype dt) {
        if (dt == null) {
            return null;
        }
        return dt.getLabel();
    }

    Graph getGraph(Producer p) {
        if (p.getGraph() instanceof Graph) {
            return (Graph) p.getGraph();
        }
        return null;
    }

    Transformer getTransformer(Environment env, Producer p) {
        return pt.getTransformer(env, p);
    } 
    
    public Expr decode(Expr exp, Environment env, Producer p){
        return pt.decode(exp, env, p);
    }

    public void setPPrinter(String str) {
        PPRINTER = str;
    }
    
    /**
     * create concat(str, st:number(), str)
     */
    public Expr createFunction(String name, List<Object> args, Environment env){
        return pt.createFunction(name, args, env);
    }
    
    
     public class TreeNode extends TreeMap<IDatatype, IDatatype> {

         TreeNode(){
            super(new Compare());
        }
         
      }

    /**
     * This Comparator enables to retrieve an occurrence of a given Literal
     * already existing in graph in such a way that two occurrences of same
     * Literal be represented by same Node in graph It (may) represent (1
     * integer) and (1.0 float) as two different Nodes Current implementation of
     * EdgeIndex sorted by values ensure join (by dichotomy ...)
     */
     class Compare implements Comparator<IDatatype> {

        public int compare(IDatatype dt1, IDatatype dt2) {

            // xsd:integer differ from xsd:decimal 
            // same node for same datatype 
            if (dt1.getDatatypeURI() != null && dt2.getDatatypeURI() != null) {
                int cmp = dt1.getDatatypeURI().compareTo(dt2.getDatatypeURI());
                if (cmp != 0) {
                    return cmp;
                }
            }

            int res = dt1.compareTo(dt2);
            return res;
        }
    }
    
    
    
}

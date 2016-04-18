package fr.inria.corese.kgtool.workflow;

import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgtool.load.Load;
import fr.inria.edelweiss.kgtool.load.LoadException;

/**
 * Load a directory.
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class LoadProcess extends WorkflowProcess {
    public static final String FILE = "file://";
    
    String name;
    boolean rec = false;
    
    public LoadProcess(String path){
        this.path = path;
    }
    
    public LoadProcess(String path, String name, boolean rec){
        this.path = path;
        this.rec = rec;
        this.name = name;
    }
   
    
    @Override
    void start(Data data){
        if (isDebug()){
            System.out.println("Load: " + path);
        }
    }
    
     @Override
    void finish(Data data){
         
    }
    
    @Override
    public Data run(Data data) throws EngineException {
        Graph g = data.getGraph();
        Load ld = Load.create(g);
        try {
            if (path.startsWith(FILE)){
                path = path.substring(FILE.length());
            }
            ld.parseDir(path, name, rec);
        } catch (LoadException ex) {
            throw new EngineException(ex);
        }
        return new Data(this, null, g);
    }
    
    @Override
    public String stringValue(Data data){
        return data.getGraph().toString();
    }

}

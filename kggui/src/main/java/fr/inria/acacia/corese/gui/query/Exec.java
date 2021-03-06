package fr.inria.acacia.corese.gui.query;

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import fr.inria.acacia.corese.exceptions.EngineException;
import fr.inria.acacia.corese.gui.core.MainFrame;
import fr.inria.acacia.corese.gui.event.MyEvalListener;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.event.Event;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.query.QueryProcess;
import fr.inria.edelweiss.kgtool.util.SPINProcess;
import org.apache.logging.log4j.Level;


/**
 * Exec KGRAM Query in a // thread to enable interacting with EvalListener through the GUI
 */
public class Exec extends Thread {
	private static Logger logger = LogManager.getLogger(Exec.class);
        
        static final String qvalidate = "template { st:apply-templates-with(st:spintypecheck) } where {}";
        //static final String qvalidate = "template {st:apply-templates-with('/home/corby/AData/template/spintypecheck/template/')} where {}";
        static final String qgraph = NSManager.STL+"query";
        
	MainFrame frame;
	String query;
	Buffer buffer;
	MyJPanelQuery panel;
	boolean debug = false;
        private boolean validate = false;
	
	
	public Exec(MainFrame f,  String q, boolean b){
		frame = f;
		query = q;
		debug = b;
	}
	
	/**
	 * run the thread in //
	 * the buffer is used by listener to wait for user interaction 
	 * with buttons: next, quit, etc.
	 */
	public void process(){
		buffer = new Buffer();
		start();
	}
	
	/**
	 * run the thread in //
	 */
        @Override
	public void run(){
		Mappings res;
                MyJPanelQuery panel = frame.getPanel();
                if (isValidate()){
                    res = validate();
                }
                else {
                    res = query();
                }
		frame.setBuffer(null);
                panel.display(res, frame);
		//frame.getPanel().display(res,frame);
	}
	
	
	Mappings query(){
		QueryExec exec =  QueryExec.create(frame.getMyCorese());
		if (debug) debug(exec);
		Date d1 = new Date();
		try {
			Mappings l_Results = exec.SPARQLQuery(query);
			Date d2 = new Date();
                        System.out.println("** Time : " + (d2.getTime() - d1.getTime()) / (1000.0));
			return l_Results;
		} catch (EngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			frame.getPanel().getTextArea().setText(e.toString());
		} 
		return null;
	}
         
        /**
         * Translate SPARQL query to SPIN graph
         * Apply spin typecheck transformation
         */
        Mappings validate(){
            try {
                SPINProcess sp = SPINProcess.create();
                Graph qg = sp.toSpinGraph(query);
                qg.init();
                Graph gg = ((GraphEngine) frame.getMyCorese()).getGraph();
                gg.setNamedGraph(qgraph, qg);
                QueryProcess exec = QueryProcess.create(gg, true);
                Mappings map = exec.query(qvalidate);
                return map;                
            } catch (EngineException ex) {
                LogManager.getLogger(Exec.class.getName()).log(Level.ERROR, "", ex);
            }
            return new Mappings();
        }

	
	/**
	 * Create EvalListener
	 */
	void debug(QueryExec exec){
		MyEvalListener el = MyEvalListener.create();
		el.handle(Event.ALL, true);

		el.setFrame(frame);
		el.setUser(buffer);
		
		frame.setEvalListener(el);
		frame.setBuffer(buffer);
		
		exec.addEventListener(el);
	}

    /**
     * @return the validate
     */
    public boolean isValidate() {
        return validate;
    }

    /**
     * @param validate the validate to set
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

}

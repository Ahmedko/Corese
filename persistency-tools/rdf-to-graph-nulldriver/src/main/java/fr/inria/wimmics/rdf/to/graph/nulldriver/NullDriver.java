/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.wimmics.rdf.to.graph.nulldriver;

import fr.inria.corese.rdftograph.driver.GdbDriver;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgraph.core.edge.EdgeQuad;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.openrdf.model.Value;

import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * @author edemairy
 */
public class NullDriver extends GdbDriver {
    private static final Logger logger = Logger.getLogger(NullDriver.class.getName());

    @Override
    public Graph openDatabase(String string) {
        logger.fine("Opening db " + string);
        return null;
    }

    @Override
    public void closeDatabase() {
        logger.fine("Closing db");
    }

    @Override
    public Object createRelationship(Value o, Value o1, String string, Map<String, Object> map) {
        logger.fine("Creating relationship bw " + o.toString() + " and " + o1.toString());
        return null;
    }

    @Override
    public void commit() {
        logger.fine("Commiting");
    }

    @Override
    public Function<GraphTraversalSource, GraphTraversal<? extends Element, ? extends Element>> getFilter(String key, String s, String p, String o, String g) {
        logger.fine("getFilter");
        return null;
    }

    @Override
    public EdgeQuad buildEdge(Element e) {
        return null;
    }

    @Override
    public Node buildNode(Element e) {
        return null;
    }

    @Override
    public boolean isGraphNode(String label) {
        return true;
    }
}

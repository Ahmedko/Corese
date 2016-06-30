package fr.inria.edelweiss.kgraph.core.producer;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.acacia.corese.triple.parser.Processor;
import fr.inria.edelweiss.kgram.api.core.Node;

/**
 * ******************************************************
 *
 * API to add filters to iterate() Use case:
 *
 * g.getDefault().iterate().filter(
 * new DataFilterFactory().object(ExprType.GE, 50)) -- object >= 50
 * g.getNamed().iterate().filter(
 * new DataFilterFactory().compare(ExprType.EQ, 0, 1)) -- subject = object
 * g.getDefault().iterate().filter(
 * new DataFilterFactory().and().subject(AA).object(BB)) -- and/or are binary
 * g.getDefault().iterate().filter(
 * new DataFilterFactory().not().or().subject(AA).object(BB)) -- not is unary
 *
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2016
 *
 */
public class DataFilterFactory {
    
    DataFilter filter;
    
    public DataFilterFactory(){}
    
    
      /**
     * @param filter the filter to set
     */
    public void setFilter(DataFilter f) {
        if (filter == null){
            filter = f;
        }
        else if (filter.isBoolean()){
           filter.setFilter(f);
        }
        else {
            // use case: filter = from(g1)
            filter = new DataFilterAnd(filter, f);
        }
    }
    
    public DataFilter getFilter(){
        return filter;
    }
    
    
   public DataFilterFactory filter(int test){
        setFilter(new DataFilter(test));
        return this;
    }
    
    public DataFilterFactory filter(int test, IDatatype dt){
        setFilter(new DataFilter(test, dt));
        return this;
    }
    
    public DataFilterFactory filter(int test, IDatatype dt, int index){
        setFilter(new DataFilter(test, dt, index));
        return this;
    }
    
    public DataFilterFactory filter(int test, Node node, int index){
        setFilter(new DataFilter(test, (IDatatype)node.getValue(), index));
        return this;
    }
    
    public DataFilterFactory property(int test, Node node){
        return filter(test, node, DataFilter.PROPERTY_INDEX);
    }
    
    public DataFilterFactory graph(int test, Node node){
        return filter(test, node, DataFilter.GRAPH_INDEX);
    }
    
    public DataFilterFactory subject(int test, Node node){
        return filter(test, node, 0);
    }
     
    public DataFilterFactory object(int test, Node node){
        return filter(test, node, 1);
    } 
    
    public DataFilterFactory object(int test, String value){
        return filter(test, value, 1);
    } 
    
    public DataFilterFactory object(int test, int value){
        return filter(test, value, 1);
    } 
    
    public DataFilterFactory object(int test, double value){
        return filter(test, value, 1);
    } 
    
    /**
     * subject(ExprType.ISBLANK)
     */
    public DataFilterFactory subject(int test){
        return filter(test, (IDatatype)null, 0);
    }
     
    /**
     * subject(ExprType.ISLITERAL)
     */
    public DataFilterFactory object(int test){
        return filter(test, (IDatatype)null, 1);
    } 
    
    /**
     * and().subject(AA).object(BB)
     */
    public DataFilterFactory and(){
        setFilter(new DataFilterAnd());
        return this;
    }
    
    /**
     * or().subject(AA).object(BB)
     */
    public DataFilterFactory or(){
        setFilter(new DataFilterOr());
        return this;
    }
    
    /**
     * not().or().subject(AA).object(BB)
     */
    public DataFilterFactory not(){
        setFilter(new DataFilterNot());
        return this;
    }
    
    /*
     * Compare edge node values 
     * DataFilter.PROPERTY_INDEX and DataFilter.GRAPH_INDEX
     * DataFilter.SUBJECT_INDEX (0) and DataFilter.OBJECT_INDEX (1)
     * compare(ExprType.EQ, DataFilter.PROPERTY_INDEX, DataFilter.SUBJECT_INDEX)
     */
    public DataFilterFactory compare(int test, int i1, int i2){
        setFilter(new DataFilter(test, i1, i2));
        return this;
    }
       
    public DataFilterFactory filter(int test, int value){
         return filter(test, DatatypeMap.newInstance(value));
    }
    
    public DataFilterFactory filter(int test, double value){
         return filter(test, DatatypeMap.newInstance(value));
    }

    public DataFilterFactory filter(int test, String value){
         return filter(test, DatatypeMap.newInstance(value));
    }
    
     public DataFilterFactory filter(int test, int value, int index){
         return filter(test, DatatypeMap.newInstance(value), index);
    }
    
    public DataFilterFactory filter(int test, double value, int index){
         return filter(test, DatatypeMap.newInstance(value), index);
    }

    public DataFilterFactory filter(int test, String value, int index){
         return filter(test, DatatypeMap.newInstance(value), index);
    }
    
    
    public DataFilterFactory filter(String test){
        setFilter(new DataFilter(oper(test)));
        return this;
    }
    
    public DataFilterFactory filter(String test, IDatatype dt){
        setFilter(new DataFilter(oper(test), dt));
        return this;
    }
    
    public DataFilterFactory filter(String test, IDatatype dt, int index){
        setFilter(new DataFilter(oper(test), dt, index));
        return this;
    }
    
    public DataFilterFactory filter(String test, int value){
         return filter(oper(test), DatatypeMap.newInstance(value));
    }
    
    public DataFilterFactory filter(String test, double value){
         return filter(oper(test), DatatypeMap.newInstance(value));
    }

    public DataFilterFactory filter(String test, String value){
         return filter(oper(test), DatatypeMap.newInstance(value));
    }
    
    int oper(String str){
        return Processor.getOper(str);
    }

}

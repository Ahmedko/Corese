package fr.inria.acacia.corese.triple.parser;

import java.util.List;
import java.util.ArrayList;

import fr.inria.acacia.corese.triple.cst.KeywordPP;

/**
 * 
 * @author corby
 *
 */
public class Values extends Exp {
	
	private List<Variable> lvar;
	private List<List<Constant>> lval;
        private Expression exp;
        Exp bind;
        private boolean moved = false;
	
	Values(List<Variable> var, List<List<Constant>> lval){
		
	}
	
	Values(){
		lval = new ArrayList ();
	}
	
	
	public static Values create(List<Variable> var, List<Constant> val){
		Values vv = new Values();
		vv.setVariables(var);
		vv.addValues(val);
		return vv;
	}
	
	public static Values create(){
		return new Values();
	}
	
        @Override
	public StringBuffer toString(StringBuffer sb){
		String SPACE = " ";
		String NL = ASTQuery.NL;

		sb.append(KeywordPP.BINDINGS);
		sb.append(SPACE);

		sb.append(KeywordPP.OPEN_PAREN);

		for (Atom var : getVariables()){
			sb.append(var.getName());
			sb.append(SPACE);
		}
		sb.append(KeywordPP.CLOSE_PAREN);

		sb.append(KeywordPP.OPEN_BRACKET);
		sb.append(NL);

		for (List<Constant> list : getValues()){
			sb.append(KeywordPP.OPEN_PAREN);

			for (Constant value : list){
				if (value == null){
					sb.append(KeywordPP.UNDEF);
				}
				else {
					sb.append(value);
				}
				sb.append(SPACE);
			}
			sb.append(KeywordPP.CLOSE_PAREN);
			sb.append(NL);
		}

		sb.append(KeywordPP.CLOSE_BRACKET);
		sb.append(NL);
		return sb;
	}
	
        @Override
	public boolean isValues(){
		return true;
	}

	public void setVariables(List<Variable> lvar) {
		this.lvar = lvar;
	}

	public List<Variable> getVariables() {
		return lvar;
	}

	void setValues(List<List<Constant>> lval) {
		this.lval = lval;
	}
	
	public void addValues(List<Constant> l) {
		lval.add(l);
	}
        
        public void addExp(Expression exp) {
            setExp(exp);
	}

	public List<List<Constant>> getValues() {
		return lval;
	}
	
        @Override
	public boolean validate(ASTQuery ast, boolean exist){
		for (Variable var : getVariables()){
			ast.bind(var);
			if (! exist){
				ast.defSelect(var);
			}
		}
		return true;
	}

    /**
     * @return the moved
     */
    public boolean isMoved() {
        return moved;
    }

    /**
     * @param mooved the moved to set
     */
    public void setMoved(boolean moved) {
        this.moved = moved;
    }

    /**
     * @return the exp
     */
    public Expression getExp() {
        return exp;
    }

    /**
     * @param exp the exp to set
     */
    public void setExp(Expression exp) {
        this.exp = exp;
    }
    
    public boolean hasExpression(){
        return exp != null;
    }

    /**
     * @return the bind
     */
    public Exp getBind() {
        return bind;
    }

    /**
     * @param bind the bind to set
     */
    public void setBind(Exp bind) {
        this.bind = bind;
    }


}

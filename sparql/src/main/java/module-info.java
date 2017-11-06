module sparql {
  requires transitive kgram;
  requires java.sql;
  requires java.xml;
  requires log4j.api;

  exports fr.inria.acacia.corese.api;
  exports fr.inria.acacia.corese.exceptions;
  exports fr.inria.acacia.corese.triple.parser;
  exports fr.inria.acacia.corese.cg.datatype.function;
  exports fr.inria.acacia.corese.cg.datatype;
  exports fr.inria.corese.triple.function.term;
  exports fr.inria.acacia.corese.triple.cst;
  exports fr.inria.corese.compiler.java;
  exports fr.inria.acacia.corese.triple.update;
}
module kgram {
    requires java.desktop;
    requires log4j.api;

    exports fr.inria.edelweiss.kgram.core;
    exports fr.inria.edelweiss.kgram.api.core;
    exports fr.inria.edelweiss.kgram.api.query;
}
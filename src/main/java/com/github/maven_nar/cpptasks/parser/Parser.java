package com.github.maven_nar.cpptasks.parser;
import java.io.IOException;
import java.io.Reader;
/**
 * A parser that extracts #include statements from a Reader.
 * 
 * @author Curt Arnold
 */
public interface Parser {
    String[] getIncludes();
    void parse(Reader reader) throws IOException;
}

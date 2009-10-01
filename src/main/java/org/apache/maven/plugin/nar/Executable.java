package org.freehep.maven.nar;

import java.util.List;

public interface Executable {

    public boolean shouldRun();
    
    public List/*<String>*/ getArgs();
}

package com.github.maven_nar;

import java.util.List;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 */
public interface Executable
{

    boolean shouldRun();

    List/* <String> */getArgs();
}

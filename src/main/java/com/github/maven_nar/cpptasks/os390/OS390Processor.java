package com.github.maven_nar.cpptasks.os390;
import java.util.Vector;
/**
 * A add-in class for IBM (r) OS/390 compilers and linkers
 * 
 * @author Hiram Chirino (cojonudo14@hotmail.com)
 */
public class OS390Processor {
    public static void addWarningSwitch(Vector args, int level) {
        switch (level) {
        /*
         * case 0: args.addElement("/W0"); break;
         * 
         * case 1: args.addElement("/W1"); break;
         * 
         * case 2: break;
         * 
         * case 3: args.addElement("/W3"); break;
         * 
         * case 4: args.addElement("/W4"); break;
         */
        }
    }
    public static String getCommandFileSwitch(String cmdFile) {
        StringBuffer buf = new StringBuffer("@");
        if (cmdFile.indexOf(' ') >= 0) {
            buf.append('\"');
            buf.append(cmdFile);
            buf.append('\"');
        } else {
            buf.append(cmdFile);
        }
        return buf.toString();
    }
    public static String getIncludeDirSwitch(String includeDir) {
        return "-I" + includeDir;
    }
    public static String[] getOutputFileSwitch(String outPath) {
        StringBuffer buf = new StringBuffer("-o ");
        if (outPath.indexOf(' ') >= 0) {
            buf.append('\"');
            buf.append(outPath);
            buf.append('\"');
        } else {
            buf.append(outPath);
        }
        String[] retval = new String[]{buf.toString()};
        return retval;
    }
    public static boolean isCaseSensitive() {
        return true;
    }
    private OS390Processor() {
    }
}

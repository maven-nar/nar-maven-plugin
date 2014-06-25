package com.github.maven_nar.cpptasks;
import com.github.maven_nar.cpptasks.compiler.Processor;
/**
 * One entry in the arrays used by the CompilerEnum and LinkerEnum classes.
 * 
 * @author Curt Arnold
 * @see CompilerEnum
 * @see LinkerEnum
 *  
 */
public class ProcessorEnumValue {
    public static String[] getValues(ProcessorEnumValue[] processors) {
        String[] values = new String[processors.length];
        for (int i = 0; i < processors.length; i++) {
            values[i] = processors[i].getName();
        }
        return values;
    }
    private String name;
    private Processor processor;
    public ProcessorEnumValue(String name, Processor processor) {
        this.name = name;
        this.processor = processor;
    }
    public String getName() {
        return name;
    }
    public Processor getProcessor() {
        return processor;
    }
}

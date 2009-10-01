// Copyright FreeHEP, 2005-2007.
package org.freehep.maven.nar;

import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

/**
 * Logger to connect the Ant logging to the Maven logging.
 *
 * @author <a href="Mark.Donszelmann@slac.stanford.edu">Mark Donszelmann</a>
 * @version $Id: plugin/src/main/java/org/freehep/maven/nar/NarLogger.java 9589202406dd 2007/07/23 17:42:54 duns $
 */
public class NarLogger implements BuildListener {
    
    private Log log;
    
    public NarLogger(Log log) {
        this.log = log;
    }
    
    public void buildStarted(BuildEvent event) {
    }
    
    public void buildFinished(BuildEvent event) {
    }
    
    public void targetStarted(BuildEvent event) {
    }
    
    public void targetFinished(BuildEvent event) {
    }
    
    public void taskStarted(BuildEvent event) {
    }
    
    public void taskFinished(BuildEvent event) {
    }

    public void messageLogged(BuildEvent event) {
    	String msg = event.getMessage();
        switch (event.getPriority()) {
            case Project.MSG_ERR:
            	if (msg.indexOf("ar: creating archive") >= 0) {
            		log.debug(msg);
            	} else if (msg.indexOf("warning") >= 0) {
            		log.warn(msg);
            	} else {
            		log.error(msg);
            	}
                break;    
            case Project.MSG_WARN:
                log.warn(msg);
                break;    
            case Project.MSG_INFO:
            	if ((msg.indexOf("files were compiled") >= 0) || (msg.indexOf("Linking...") >= 0)) {
            		log.info(msg);
            	} else if (msg.indexOf("error") >= 0) {
            		log.error(msg);
            	} else if (msg.indexOf("warning") >= 0) {
            		log.warn(msg);
            	} else {
            		log.debug(msg);
            	}
                break;    
            case Project.MSG_VERBOSE:
                log.debug(msg);
                break;    
            default:    
            case Project.MSG_DEBUG:
                log.debug(msg);
                break;
        }
    }
}

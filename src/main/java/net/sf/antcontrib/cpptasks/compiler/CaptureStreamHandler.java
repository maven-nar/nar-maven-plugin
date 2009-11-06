/*
 * 
 * Copyright 2001-2004 The Ant-Contrib project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// BEGINFREEHEP, fully replaced with a runner with threads
package net.sf.antcontrib.cpptasks.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;

import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
/**
 * Implements ExecuteStreamHandler to capture the output of a Execute to an
 * array of strings
 * 
 * @author Curt Arnold
 */
public class CaptureStreamHandler implements ExecuteStreamHandler {

	String[] output;

	/**
	 * Runs an executable and captures the output in a String array
	 * 
	 * @param cmdline
	 *            command line arguments
	 * @return output of process
	 */
	public static String[] run(String[] cmdline) {
		CaptureStreamHandler handler = new CaptureStreamHandler();
		Execute exec = new Execute(handler);
		exec.setCommandline(cmdline);
		try {
			int status = exec.execute();
		} catch (IOException ex) {
		}
		return handler.getOutput() != null ? handler.getOutput() : new String[0];
	}

	private InputStream processErrorStream;

	private InputStream processOutputStream;

	public CaptureStreamHandler() {
	}

	public String[] getOutput() {
		return output;
	}

	static class Copier extends Thread {
		InputStream is;

		Vector lines;

		Copier(InputStream is) {
			this.is = is;
			lines = new Vector(10);
		}

		public void run() {
    		try {
    			BufferedReader reader = new BufferedReader( new InputStreamReader(is) );
    			while ( true ) {
    				String line = reader.readLine();
    				if ( line == null )
    					break;
    				lines.addElement( line );
    			}
			} catch (IOException e) {
				// Ignore
			}
    	}

		public Vector getLines() {
			return lines;
		}
	}

	/**
	 * Reads concurrently both the process standard output and standard error.
	 * The standard error - if not empty - is copied to the output string array field. Otherwise
	 * the standard output is copied to the output field. The output field is set to an empty array 
	 * in case of any error. 
	 */
	public void gatherOutput() {
		try {
			Copier errorCopier = new Copier(processErrorStream);
			Copier outputCopier = new Copier(processOutputStream);
			errorCopier.start();
			outputCopier.start();
			errorCopier.join();
			outputCopier.join();
			if (errorCopier.getLines().size() > 0) {
				output = new String[errorCopier.getLines().size()];
				errorCopier.getLines().copyInto(output);
			} else {
				output = new String[outputCopier.getLines().size()];
				outputCopier.getLines().copyInto(output);
			}
		} catch (Exception e) {
			output = new String[0];
		}
	}

	/**
	 * Install a handler for the error stream of the subprocess.
	 * 
	 * @param is
	 *            input stream to read from the error stream from the subprocess
	 */
	public void setProcessErrorStream(InputStream is) throws IOException {
		processErrorStream = is;
	}

	/**
	 * Install a handler for the input stream of the subprocess.
	 * 
	 * @param os
	 *            output stream to write to the standard input stream of the
	 *            subprocess
	 */
	public void setProcessInputStream(OutputStream os) throws IOException {
		os.close();
	}

	/**
	 * Install a handler for the output stream of the subprocess.
	 * 
	 * @param is
	 *            input stream to read from the error stream from the subprocess
	 */
	public void setProcessOutputStream(InputStream is) throws IOException {
		processOutputStream = is;
	}

	/**
	 * Start handling of the streams.
	 */
	public void start() throws IOException {
		gatherOutput();
	}

	/**
	 * Stop handling of the streams - will not be restarted.
	 */
	public void stop() {
	}
// ENDFREEHEP
}

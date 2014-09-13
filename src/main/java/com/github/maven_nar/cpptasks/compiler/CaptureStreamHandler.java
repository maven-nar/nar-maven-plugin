/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
// BEGINFREEHEP, fully replaced with a runner with threads
package com.github.maven_nar.cpptasks.compiler;

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

	private String[] stderr;
	private String[] stdout;

	/**
	 * Runs an executable and captures the output in a String array
	 * 
	 * @param cmdline
	 *            command line arguments
	 * @return output of process
	 * @see CaptureStreamHandler#getOutput()
	 */
	public static String[] run(String[] cmdline) {
		CaptureStreamHandler handler = execute(cmdline);
		return handler.getOutput() != null ? handler.getOutput() : new String[0];
	}

	/**
	 * Executes the given command, capturing the output using a newly allocated
	 * {@link CaptureStreamHandler}, which is then returned.
	 * <p>
	 * In contrast to {@link #run(String[])}, this method allows both the
	 * standard error and standard output streams to be inspected after execution
	 * (via the {@link #getStderr()} and {@link #getStdout()} methods,
	 * respectively).
	 * </p>
	 * 
	 * @param cmdline
	 *            command line arguments
	 * @return The {@link CaptureStreamHandler} used to capture the output.
	 */
	public static CaptureStreamHandler execute(String[] cmdline) {
		CaptureStreamHandler handler = new CaptureStreamHandler();
		Execute exec = new Execute(handler);
		exec.setCommandline(cmdline);
		try {
			int status = exec.execute();
		} catch (IOException ex) {
		}
		return handler;
	}

	private InputStream processErrorStream;

	private InputStream processOutputStream;

	public CaptureStreamHandler() {
	}

	/**
	 * Gets the output of the execution. If standard error is not empty,
	 * it is returned; otherwise, standard output is returned.
	 */
	public String[] getOutput() {
		return (null != stderr && stderr.length > 0 ) ? stderr : stdout;
	}

	/** Gets the output of the execution's standard error stream. */
	public String[] getStderr() {
		return stderr;
	}

	/** Gets the output of the execution's standard output stream. */
	public String[] getStdout() {
		return stdout;
	}

	static class Copier extends Thread {
		InputStream is;

		Vector<String> lines;

		Copier(InputStream is) {
			this.is = is;
			lines = new Vector<String>(10);
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

		public Vector<String> getLines() {
			return lines;
		}
	}

	/**
	 * Reads concurrently both the process standard output and standard error.
	 * The standard error is copied to the stderr string array field.
	 * The standard output is copied to the stdout string array field.
	 * Both fields are set to an empty array in case of any error.
	 */
	public void gatherOutput() {
		try {
			Copier errorCopier = new Copier(processErrorStream);
			Copier outputCopier = new Copier(processOutputStream);
			errorCopier.start();
			outputCopier.start();
			errorCopier.join();
			outputCopier.join();
			stderr = new String[errorCopier.getLines().size()];
			errorCopier.getLines().copyInto(stderr);
			stdout = new String[outputCopier.getLines().size()];
			outputCopier.getLines().copyInto(stdout);
		} catch (Exception e) {
			stderr = stdout = new String[0];
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

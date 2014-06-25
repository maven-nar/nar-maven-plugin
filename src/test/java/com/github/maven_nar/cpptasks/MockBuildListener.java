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
package com.github.maven_nar.cpptasks;
import java.util.Vector;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
/**
 * Captures build events
 *  
 */
public class MockBuildListener implements BuildListener {
    private Vector buildFinishedEvents = new Vector();
    private Vector buildStartedEvents = new Vector();
    private Vector messageLoggedEvents = new Vector();
    private Vector targetFinishedEvents = new Vector();
    private Vector targetStartedEvents = new Vector();
    private Vector taskFinishedEvents = new Vector();
    private Vector taskStartedEvents = new Vector();
    /**
     * Signals that the last target has finished. This event will still be
     * fired if an error occurred during the build.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getException()
     */
    public void buildFinished(BuildEvent event) {
        buildFinishedEvents.addElement(event);
    }
    /**
     * Signals that a build has started. This event is fired before any targets
     * have started.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     */
    public void buildStarted(BuildEvent event) {
        buildStartedEvents.addElement(event);
    }
    public Vector getBuildFinishedEvents() {
        return new Vector(buildFinishedEvents);
    }
    /**
     * Gets a list of buildStarted events
     * 
     * @return list of build started events
     */
    public Vector getBuildStartedEvents() {
        return new Vector(buildStartedEvents);
    }
    /**
     * Gets message logged events
     * 
     * @return vector of "MessageLogged" events.
     */
    public Vector getMessageLoggedEvents() {
        return new Vector(messageLoggedEvents);
    }
    /**
     * Gets target finished events
     * 
     * @return vector of "TargetFinished" events.
     */
    public Vector getTargetFinishedEvents() {
        return new Vector(targetFinishedEvents);
    }
    /**
     * Gets target started events
     * 
     * @return vector of "TargetStarted" events.
     */
    public Vector getTargetStartedEvents() {
        return new Vector(targetStartedEvents);
    }
    /**
     * Gets task finished events
     * 
     * @return vector of "TaskFinished" events.
     */
    public Vector getTaskFinishedEvents() {
        return new Vector(taskFinishedEvents);
    }
    /**
     * Gets task started events
     * 
     * @return vector of "TaskStarted" events.
     */
    public Vector getTaskStartedEvents() {
        return new Vector(taskStartedEvents);
    }
    /**
     * Signals a message logging event.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged(BuildEvent event) {
        messageLoggedEvents.addElement(event);
    }
    /**
     * Signals that a target has finished. This event will still be fired if an
     * error occurred during the build.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getException()
     */
    public void targetFinished(BuildEvent event) {
        targetFinishedEvents.addElement(event);
    }
    /**
     * Signals that a target is starting.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getTarget()
     */
    public void targetStarted(BuildEvent event) {
        targetStartedEvents.addElement(event);
    }
    /**
     * Signals that a task has finished. This event will still be fired if an
     * error occurred during the build.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getException()
     */
    public void taskFinished(BuildEvent event) {
        taskFinishedEvents.addElement(event);
    }
    /**
     * Signals that a task is starting.
     * 
     * @param event
     *            An event with any relevant extra information. Must not be
     *            <code>null</code>.
     * 
     * @see BuildEvent#getTask()
     */
    public void taskStarted(BuildEvent event) {
        taskStartedEvents.addElement(event);
    }
}

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
import java.io.File;
import java.io.IOException;

import com.github.maven_nar.cpptasks.CUtil;

import junit.framework.TestCase;
/**
 * Tests for CUtil class
 */
public class TestCUtil extends TestCase {
    public TestCUtil(String name) {
        super(name);
    }
    public void testGetPathFromEnvironment() {
        File[] files = CUtil.getPathFromEnvironment("LIB", ";");
        assertNotNull(files);
    }
    public void testGetRelativePath1() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File(
                "/foo/bar/baz"));
        assertEquals("baz", rel);
    }
    public void testGetRelativePath2() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil
                .getRelativePath(canonicalBase, new File("/foo/bar/"));
        assertEquals(".", rel);
    }
    public void testGetRelativePath3() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/a"));
        assertEquals("a", rel);
    }
    public void testGetRelativePath4() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/foo/"));
        assertEquals("..", rel);
    }
    public void testGetRelativePath5() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File("/a"));
        String expected = ".." + File.separator + ".." + File.separator + "a";
        assertEquals(expected, rel);
    }
    public void testGetRelativePath6() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase, new File(
                "/foo/baz/bar"));
        String expected = ".." + File.separator + "baz" + File.separator
                + "bar";
        assertEquals(expected, rel);
    }
    public void testGetRelativePath7() throws IOException {
        String canonicalBase = new File("/foo/bar/").getCanonicalPath();
        //
        //  skip the UNC test unless running on Windows
        //
        String osName = System.getProperty("os.name");
        if (osName.indexOf("Windows") >= 0) {
            File uncFile = new File("\\\\fred\\foo.bar");
            String uncPath;
            try {
                uncPath = uncFile.getCanonicalPath();
            } catch (IOException ex) {
                uncPath = uncFile.toString();
            }
            String rel = CUtil.getRelativePath(canonicalBase, uncFile);
            assertEquals(uncPath, rel);
        }
    }
    public void testGetRelativePath8() throws IOException {
        String canonicalBase = new File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/something.extension"));
        String expected = ".." + File.separator + "something.extension";
        assertEquals(expected, rel);
    }
    public void testGetRelativePath9() throws IOException {
        String canonicalBase = new
File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/somethingElse"));
        String expected = ".." + File.separator + "somethingElse";
        assertEquals(expected, rel);
    }
    public void testGetRelativePath10() throws IOException {
        String canonicalBase = new
File("/foo/bar/something").getCanonicalPath();
        String rel = CUtil.getRelativePath(canonicalBase,
                new File("/foo/bar/something else"));
        String expected = ".." + File.separator + "something else";
        assertEquals(expected, rel);
    }
    public void testParsePath1() {
        File[] files = CUtil.parsePath("", ";");
        assertEquals(0, files.length);
    }
    public void testParsePath2() {
        String workingDir = System.getProperty("user.dir");
        File[] files = CUtil.parsePath(workingDir, ";");
        assertEquals(1, files.length);
        File workingDirFile = new File(workingDir);
        assertEquals(workingDirFile, files[0]);
    }
    public void testParsePath3() {
        String workingDir = System.getProperty("user.dir");
        File[] files = CUtil.parsePath(workingDir + ";", ";");
        assertEquals(1, files.length);
        assertEquals(new File(workingDir), files[0]);
    }
    public void testParsePath4() {
        String workingDir = System.getProperty("user.dir");
        String javaHome = System.getProperty("java.home");
        File[] files = CUtil.parsePath(workingDir + ";" + javaHome, ";");
        assertEquals(2, files.length);
        assertEquals(new File(workingDir), files[0]);
        assertEquals(new File(javaHome), files[1]);
    }
    public void testParsePath5() {
        String workingDir = System.getProperty("user.dir");
        String javaHome = System.getProperty("java.home");
        File[] files = CUtil.parsePath(workingDir + ";" + javaHome + ";", ";");
        assertEquals(2, files.length);
        assertEquals(new File(workingDir), files[0]);
        assertEquals(new File(javaHome), files[1]);
    }

    /**
     * Test of xmlAttributeEncode.
     *
     * See patch 1267472 and bug 1032302.
     */
    public void testXmlEncode() {
        assertEquals("&lt;&quot;boo&quot;&gt;", CUtil.xmlAttribEncode("<\"boo\">"));
    }
}

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
/**
 * @author Curt Arnold
 */
public final class DependencyInfo {
    /**
     * Last modified time of this file or anything that it depends on.
     * 
     * Not persisted since almost any change could invalidate it. Initialized
     * to long.MIN_VALUE on construction.
     */
// FREEHEP
//	private long compositeLastModified;
    private/* final */String includePathIdentifier;
    private/* final */String[] includes;
    private/* final */String source;
    private/* final */long sourceLastModified;
    private/* final */String[] sysIncludes;
// FREEHEP
    private Object tag = null;
    public DependencyInfo(String includePathIdentifier, String source,
            long sourceLastModified, Vector includes, Vector sysIncludes) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (includePathIdentifier == null) {
            throw new NullPointerException("includePathIdentifier");
        }
        this.source = source;
        this.sourceLastModified = sourceLastModified;
        this.includePathIdentifier = includePathIdentifier;
        this.includes = new String[includes.size()];
// BEGINFREEHEP
//        if (includes.size() == 0) {
//            compositeLastModified = sourceLastModified;
//        } else {
//            includes.copyInto(this.includes);
//            compositeLastModified = Long.MIN_VALUE;
//        }
// ENDFREEHEP
        this.sysIncludes = new String[sysIncludes.size()];
// FREEHEP
        includes.copyInto(this.includes);
        sysIncludes.copyInto(this.sysIncludes);
    }
    /**
     * Returns the latest modification date of the source or anything that it
     * depends on.
     * 
     * @returns the composite lastModified time, returns Long.MIN_VALUE if not
     * set
     */
// BEGINFREEHEP
//    public long getCompositeLastModified() {
//        return compositeLastModified;
//    }
    public void setTag(Object t) {
       tag = t;
    }
// ENDFREEHEP
    public String getIncludePathIdentifier() {
        return includePathIdentifier;
    }
    public String[] getIncludes() {
        String[] includesClone = (String[]) includes.clone();
        return includesClone;
    }
    public String getSource() {
        return source;
    }
    public long getSourceLastModified() {
        return sourceLastModified;
    }
    public String[] getSysIncludes() {
        String[] sysIncludesClone = (String[]) sysIncludes.clone();
        return sysIncludesClone;
    }
// BEGINFREEHEP
    /**
     * Returns true, if dependency info is tagged with object t.
     * 
     * @param t object to compare with
     * 
     * @return boolean, true, if tagged with t, otherwise false
     */
    public boolean hasTag(Object t) {
       return tag == t;
    }
//    public void setCompositeLastModified(long lastMod) {
//        compositeLastModified = lastMod;
//    }
// ENDFREEHEP
}

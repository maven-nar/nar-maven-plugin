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
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Enumerated attribute with the values "none", "severe", "default",
 * "production", "diagnostic", and "aserror".
 */
public final class WarningLevelEnum extends EnumeratedAttribute {
   /**
    * Constructor.
    *
    */
    public WarningLevelEnum() {
        setValue("default");
    }
    /**
     * Get allowable values.
     * @return allowable values
     */
    public String[] getValues() {
        return new String[]{"none", "severe", "default", "production",
              "diagnostic", "aserror"};
    }
}

package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author Mark Donszelmann
 * @version $Id$
 */
public interface NarConstants {
    public final static String NAR_EXTENSION = "nar";
    public final static String NAR_NO_ARCH = "noarch";
    public final static String NAR_ROLE_HINT = "nar-library";
    public final static String NAR_TYPE = "nar";
    
    public final static int LOG_LEVEL_ERROR = 0;
    public final static int LOG_LEVEL_WARNING = 1;
    public final static int LOG_LEVEL_INFO = 2;
    public final static int LOG_LEVEL_VERBOSE = 3;
    public final static int LOG_LEVEL_DEBUG = 4;
}

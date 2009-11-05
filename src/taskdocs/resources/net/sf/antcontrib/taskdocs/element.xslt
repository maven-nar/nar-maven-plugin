<!--
 Licensed to the Ant-Contrib Project under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The Ant-Contrib Project licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
               xmlns:taskdocs="http://ant-contrib.sf.net/taskdocs"
               xmlns:xhtml="http://www.w3.org/1999/xhtml"
               xsl:version="1.0">

   <xsl:output method="xml" indent="yes"/>

   <xsl:apply-templates select="/"/>

   <xsl:template match="/">

  <xsl:comment>

Licensed to the Ant-Contrib Project under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The Ant-Contrib Project licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

  </xsl:comment>
       <document>
           <xsl:apply-templates/>
       </document>
</xsl:template>

    <xsl:template match="xhtml:*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
    
    <xsl:template name="pretty-name">
        <xsl:param name="name"/>
        <xsl:choose>
            <xsl:when test="contains($name, 'SystemLibrarySet')">syslibset</xsl:when>
            <xsl:when test="contains($name, 'LibrarySet')">libset</xsl:when>
            <xsl:when test="contains($name, 'TargetDef')">targetplatform</xsl:when>
            <xsl:when test="string-length(substring-before($name, 'Task'))">
                <xsl:call-template name="pretty-name">
                   <xsl:with-param name="name">
                        <xsl:value-of select="substring-before($name, 'Task')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="string-length(substring-before($name, 'Def'))">
                <xsl:call-template name="pretty-name">
                   <xsl:with-param name="name">
                        <xsl:value-of select="substring-before($name, 'Def')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="string-length(substring-before($name, 'Argument'))">
                <xsl:call-template name="pretty-name">
                   <xsl:with-param name="name">
                        <xsl:value-of select="substring-before($name, 'ument')"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="translate($name, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="taskdocs:class">
        <properties>
            <title>
                <xsl:call-template name='pretty-name'>
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                </xsl:call-template>
            </title>
        </properties>
        <body>

        <section>
            <xsl:attribute name="name">
                <xsl:call-template name='pretty-name'>
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:attribute>
            <subsection name="Description">
                <xsl:apply-templates select="taskdocs:comment"/>
            </subsection>
            <xsl:if test="taskdocs:attributes/taskdocs:attribute">
            <subsection name="parameters">
                <table>
                    <tr>
                        <td><b>Attribute</b></td>
                        <td><b>Description</b></td>
                        <td><b>Type</b></td>
                    </tr>
                    <xsl:apply-templates select="taskdocs:attributes/taskdocs:attribute">
                        <xsl:sort select="@name"/>
                    </xsl:apply-templates>
                </table>
            </subsection>
            </xsl:if>
            <xsl:if test="taskdocs:children/taskdocs:child">
                <subsection name="parameters as nested elements">
                    <dl>
                        <xsl:apply-templates select="taskdocs:children/taskdocs:child">
                            <xsl:sort select="@name"/>
                        </xsl:apply-templates>
                    </dl>

                </subsection>
            </xsl:if>
        </section>
        </body>
        
    </xsl:template>

    <xsl:template match="taskdocs:attribute">
        <tr>
            <td>
                <xsl:value-of select="@name"/>
            </td>
            <td>
                <xsl:apply-templates select="taskdocs:comment"/>
            </td>
            <td>
                <xsl:for-each select="taskdocs:type">
                    <xsl:call-template name="attribute-type"/>
                </xsl:for-each>
            </td>
        </tr>

    </xsl:template>

    <xsl:template name="attribute-type">
        <xsl:choose>
            <xsl:when test="starts-with(@qualifiedTypeName, 'net.sf.antcontrib.cpptasks.')">
                <a href="../apidocs/{translate(@qualifiedTypeName, '.', '/')}.html">
                    <xsl:value-of select="@name"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@name"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="child-type">
        <xsl:param name="anchor"><xsl:value-of select="@name"/></xsl:param>
        <xsl:element name="a">
            <xsl:attribute name="href">
                <xsl:choose>
                    <xsl:when test="starts-with(@qualifiedTypeName, 'net.sf.antcontrib.cpptasks')"><xsl:value-of select="@name"/>.html</xsl:when>
                    <xsl:when test="starts-with(@qualifiedTypeName, 'org.apache.tools.ant.types.PatternSet')">http://ant.apache.org/manual/CoreTypes/patternset.html</xsl:when>
                    <xsl:when test="starts-with(@qualifiedTypeName, 'org.apache.tools.ant.types.Path')">http://ant.apache.org/manual/using.html#path</xsl:when>
                    <xsl:when test="starts-with(@qualifiedTypeName, 'org.apache.tools.ant.types.Commandline')">http://ant.apache.org/manual/CoreTasks/exec.html</xsl:when>
                    <xsl:otherwise>about:blank</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:value-of select="$anchor"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template match="taskdocs:child">
            <dt>
                <xsl:variable name="anchor">
                    <xsl:value-of select="@name"/>
                </xsl:variable>
                <xsl:for-each select="taskdocs:type">
                    <xsl:call-template name="child-type">
                        <xsl:with-param name="anchor">
                            <xsl:value-of select="$anchor"/>
                         </xsl:with-param>
                    </xsl:call-template>
                </xsl:for-each>
            </dt>
            <dd><xsl:value-of select="taskdocs:comment"/></dd>
    </xsl:template>


</xsl:transform>

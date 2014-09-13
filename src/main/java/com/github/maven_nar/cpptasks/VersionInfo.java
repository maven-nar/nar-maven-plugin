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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;

/**
 * Version Information.
 *
 * This information is applied in a platform specific manner
 * to embed version information into executable images.  This
 * behavior is new and subject to change.
 *
 * On the Microsoft Windows platform, a resource is generated and added
 * to the set of files to be compiled.  A resource compiler must
 * be specified to compile the generated file.
 *
 * On Unix platforms, versioninfo is currently not used.
 * Future versions may append fileversion to the output file name,
 * use compatibility version for -soname and possibly create
 * symbolic links.
 */
public final class VersionInfo extends DataType {
        /**
         * if property.
         */
    private String ifCond;
    /**
     * unless property.
     */
    private String unlessCond;

    /**
     * extends property.
     */
    private Reference extendsRef;

    /**
     * file version.
     *
     */
    private String fileVersion;
    /**
     * Product version.
     *
     */
    private String productVersion;
    /**
     * file language.
     *
     */
    private String language;

    /**
     * comments.
     *
     */
    private String fileComments;
    /**
     * Company name.
     *
     */
    private String companyName;
    /**
     * Description.
     *
     */
    private String fileDescription;
    /**
     * internal name.
     */
    private String internalName;
    /**
     * legal copyright.
     *
     */
    private String legalCopyright;
    /**
     * legal trademark.
     *
     */
    private String legalTrademarks;
    /**
     * original filename.
     *
     */
    private String originalFilename;
    /**
     * private build.
     *
     */
    private String privateBuild;
    /**
     * product name.
     *
     */
    private String productName;
    /**
     * Special build
     */
    private String specialBuild;
    /**
     * compatibility version
     *
     */
    private String compatibilityVersion;

    /**
     * prerease build.
     *
     */
    private Boolean prerelease;

    /**
     * prerease build.
     *
     */
    private Boolean patched;


    /**
     * Constructor.
     *
     */
    public VersionInfo() {
    }

    /**
     * Private constructor for merge.
     * @param stack list of version infos with most significant first.
     */
    private VersionInfo(final Vector<VersionInfo> stack) {
            VersionInfo source = null;
            for(int i = stack.size() - 1; i >= 0; i--) {
                    source = stack.elementAt(i);
                    if (source.getIf() != null) {
                            ifCond = source.getIf();
                    }
                    if (source.getUnless() != null) {
                            unlessCond = source.getUnless();
                    }
                    if (source.getFileversion() != null) {
                            fileVersion = source.getFileversion();
                    }
                    if (source.getProductversion() != null) {
                            productVersion = source.getProductversion();
                    }
                    if (source.getLanguage() != null) {
                            language = source.getLanguage();
                    }
                    if (source.getFilecomments() != null) {
                            fileComments = source.getFilecomments();
                    }
                    if (source.getCompanyname() != null) {
                            companyName = source.getCompanyname();
                    }
                    if (source.getFiledescription() != null) {
                            fileDescription = source.getFiledescription();
                    }
                    if (source.getInternalname() != null) {
                            internalName = source.getInternalname();
                    }
                    if (source.getLegalcopyright() != null) {
                            legalCopyright = source.getLegalcopyright();
                    }
                    if (source.getLegaltrademarks() != null) {
                            legalTrademarks = source.getLegaltrademarks();
                    }
                    if (source.getOriginalfilename() != null) {
                            originalFilename = source.getOriginalfilename();
                    }
                    if (source.getPrivatebuild() != null) {
                            privateBuild = source.getPrivatebuild();
                    }
                    if (source.getProductname() != null) {
                            productName = source.getProductname();
                    }
                    if (source.getSpecialbuild() != null) {
                            specialBuild = source.getSpecialbuild();
                    }
                    if (source.getCompatibilityversion() != null) {
                            compatibilityVersion = source.getCompatibilityversion();
                    }
                    if (source.getPrerelease() != null) {
                            prerelease = source.getPrerelease();
                    }
                    if (source.getPatched() != null) {
                            patched = source.getPatched();
                    }
            }
            setProject(source.getProject());
    }

    /**
     * Returns a VersionInfo that reflects any inherited version information.
     * @return merged version information.
\     */
    public VersionInfo merge() {
        if (isReference()) {
            VersionInfo refVersion = (VersionInfo)
                                getCheckedRef(VersionInfo.class,
                    "VersionInfo");
            return refVersion.merge();
        }
            Reference currentRef = this.getExtends();
            if (currentRef == null) {
                    return this;
            }
            Vector<VersionInfo> stack = new Vector<VersionInfo>(5);
            stack.addElement(this);
            while (currentRef != null) {
            Object obj = currentRef.getReferencedObject(getProject());
            if (obj instanceof VersionInfo) {
                    VersionInfo current = (VersionInfo) obj;
                    if (current.isReference()) {
                    current = (VersionInfo)
                                                current.getCheckedRef(VersionInfo.class,
                                    "VersionInfo");
                    }
                    if (stack.contains(current)) {
                            throw this.circularReference();
                    }
                    stack.addElement(current);
                    currentRef = current.getExtends();
            } else {
                    throw new BuildException("Referenced element "
                    + currentRef.getRefId() + " is not a versioninfo.");
            }
            }
            return new VersionInfo(stack);
    }
    /**
     * Methods is required for documentation generation, throws
     * exception if called.
     *
     * @throws org.apache.tools.ant.BuildException if called
     */
    public void execute() throws org.apache.tools.ant.BuildException {
        throw new org.apache.tools.ant.BuildException(
                "Not an actual task, but looks like one for documentation purposes");
    }
    /**
     * Returns true if the define's if and unless conditions (if any) are
     * satisfied.
     *
     * @exception BuildException
     *                throws build exception if name is not set
     */
    public final boolean isActive() throws BuildException {
        return CUtil.isActive(getProject(), ifCond, unlessCond);
    }
    /**
     * Sets an id that can be used to reference this element.
     *
     * @param id
     *            id
     */
    public void setId(String id) {
        //
        //  this is actually accomplished by a different
        //     mechanism, but we can document it
        //
    }

    public Reference getExtends() {
            return this.extendsRef;
    }
    /**
     * Specifies that this element extends the element with id attribute with a
     * matching value. The configuration will be constructed from the settings
     * of this element, element referenced by extends, and the containing cc
     * element.
     *
     * @param extendsRef
     *            Reference to the extended processor definition.
     * @throws BuildException
     *             if this processor definition is a reference
     */
    public void setExtends(Reference extendsRef) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.extendsRef = extendsRef;
    }

    /**
     * Gets if property name.
     * @return property name, may be null.
     */
    public final String getIf() {
            return ifCond;
    }
    /**
     * Sets the property name for the 'if' condition.
     *
     * The define will be ignored unless the property is defined.
     *
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") will throw an exception when
     * evaluated.
     *
     * @param propName
     *            property name
     */
    public final void setIf(String propName) {
            if (isReference()) {
                    throw tooManyAttributes();
            }
        ifCond = propName;
    }
    /**
     * Specifies that this element should behave as if the content of the
     * element with the matching id attribute was inserted at this location. If
     * specified, no other attributes should be specified.
     *
     */
    public void setRefid(Reference r) throws BuildException {
        super.setRefid(r);
    }
    /**
     * Gets if property name.
     * @return property name, may be null.
     */
    public final String getUnless() {
            return unlessCond;
    }
    /**
     * Set the property name for the 'unless' condition.
     *
     * If named property is set, the define will be ignored.
     *
     * The value of the property is insignificant, but values that would imply
     * misinterpretation ("false", "no") of the behavior will throw an
     * exception when evaluated.
     *
     * @param propName
     *            name of property
     */
    public final void setUnless(String propName) {
            if (isReference()) {
                    throw tooManyAttributes();
            }
        unlessCond = propName;
    }
    /**
     * Gets file version.
     * @return file version, may be null.
     *
     */
    public String getFileversion() {
            return fileVersion;
    }
    /**
     * Gets Product version.
     * @return product version, may be null
     */
    public String getProductversion() {
            return productVersion;
    }
    /**
     * Gets compatibility version.
     * @return compatibility version, may be null
     */
    public String getCompatibilityversion() {
            return compatibilityVersion;
    }
    /**
     * Gets file language, should be an IETF RFC 3066 identifier, for example, en-US.
     * @return language, may be null.
     */
    public String getLanguage() {
            return language;
    }

    /**
     * Gets comments.
     * @return comments, may be null.
     */
    public String getFilecomments() {
            return fileComments;
    }
    /**
     * Gets Company name.
     * @return company name, may be null.
     */
    public String getCompanyname() {
            return companyName;
    }
    /**
     * Gets Description.
     * @return description, may be null.
     */
    public String getFiledescription() {
            return fileDescription;
    }
    /**
     * Gets internal name.
     * @return internal name, may be null.
     */
    public String getInternalname() {
            return internalName;
    }
    /**
     * Gets legal copyright.
     * @return legal copyright, may be null.
     */
    public String getLegalcopyright() {
            return legalCopyright;
    }
    /**
     * Gets legal trademark.
     * @return legal trademark, may be null;
     */
    public String getLegaltrademarks() {
            return legalTrademarks;
    }
    /**
     * Gets original filename.
     * @return original filename, may be null.
     */
    public String getOriginalfilename() {
            return originalFilename;
    }
    /**
     * Gets private build.
     * @return private build, may be null.
     */
    public String getPrivatebuild() {
            return privateBuild;
    }
    /**
     * Gets prerelease.
     * @return prerelease, may be null.
     */
    public Boolean getPrerelease() {
            return prerelease;
    }
    /**
     * Gets patched.
     * @return patched, may be null.
     */
    public Boolean getPatched() {
            return patched;
    }
    /**
     * Gets product name.
     * @return product name, may be null.
     */
    public String getProductname() {
            return productName;
    }
    /**
     * Special build
     * @return special build, may be null.
     */
    public String getSpecialbuild() {
            return specialBuild;
    }

    /**
     * Sets file version.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setFileversion(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            fileVersion = value;
    }
    /**
     * Sets product version.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setProductversion(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            productVersion = value;
    }
    /**
     * Sets compatibility version.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setCompatibilityversion(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            compatibilityVersion = value;
    }
    /**
     * Sets language.
     * @param value new value, should be an IETF RFC 3066 language identifier.
     * @throws BuildException if specified with refid
     */
    public void setLanguage(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        language = value;
    }
    /**
     * Sets comments.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setFilecomments(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            fileComments = value;
    }

    /**
     * Sets file description.
     * @param value new value
     */
    public void setFiledescription(String value)  {
        if (isReference()) {
            throw tooManyAttributes();
        }
            fileDescription = value;
    }

    /**
     * Sets company name.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setCompanyname(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            companyName = value;
    }


    /**
     * Sets internal name.  Internal name will automatically be
     * specified from build step, only set this value if
     * intentionally overriding that value.
     *
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setInternalname(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            internalName = value;
    }

    /**
     * Sets legal copyright.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setLegalcopyright(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            legalCopyright = value;
    }
    /**
     * Sets legal trademark.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setLegaltrademarks(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            legalTrademarks = value;
    }
    /**
     * Sets original name.  Only set this value if
     * intentionally overriding the value from the build set.
     *
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setOriginalfilename(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            originalFilename = value;
    }
    /**
     * Sets private build.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setPrivatebuild(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        privateBuild = value;
    }
    /**
     * Sets prerelease.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setPrerelease(boolean value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (value) {
                prerelease = Boolean.TRUE;
        } else {
                prerelease = Boolean.FALSE;
        }
    }
    /**
     * Sets prerelease.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setPatched(boolean value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        if (value) {
                patched = Boolean.TRUE;
        } else {
                patched = Boolean.FALSE;
        }
    }
    /**
     * Sets product name.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setProductname(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
            productName= value;
    }
    /**
     * Sets private build.
     * @param value new value
     * @throws BuildException if specified with refid
     */
    public void setSpecialbuild(String value) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        specialBuild = value;
    }
}

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
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * to embed version information into executable images. This
 * behavior is new and subject to change.
 *
 * On the Microsoft Windows platform, a resource is generated and added
 * to the set of files to be compiled. A resource compiler must
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
   * 
   * @param stack
   *          list of version infos with most significant first.
   */
  private VersionInfo(final Vector<VersionInfo> stack) {
    VersionInfo source = null;
    for (int i = stack.size() - 1; i >= 0; i--) {
      source = stack.elementAt(i);
      if (source.getIf() != null) {
        this.ifCond = source.getIf();
      }
      if (source.getUnless() != null) {
        this.unlessCond = source.getUnless();
      }
      if (source.getFileversion() != null) {
        this.fileVersion = source.getFileversion();
      }
      if (source.getProductversion() != null) {
        this.productVersion = source.getProductversion();
      }
      if (source.getLanguage() != null) {
        this.language = source.getLanguage();
      }
      if (source.getFilecomments() != null) {
        this.fileComments = source.getFilecomments();
      }
      if (source.getCompanyname() != null) {
        this.companyName = source.getCompanyname();
      }
      if (source.getFiledescription() != null) {
        this.fileDescription = source.getFiledescription();
      }
      if (source.getInternalname() != null) {
        this.internalName = source.getInternalname();
      }
      if (source.getLegalcopyright() != null) {
        this.legalCopyright = source.getLegalcopyright();
      }
      if (source.getLegaltrademarks() != null) {
        this.legalTrademarks = source.getLegaltrademarks();
      }
      if (source.getOriginalfilename() != null) {
        this.originalFilename = source.getOriginalfilename();
      }
      if (source.getPrivatebuild() != null) {
        this.privateBuild = source.getPrivatebuild();
      }
      if (source.getProductname() != null) {
        this.productName = source.getProductname();
      }
      if (source.getSpecialbuild() != null) {
        this.specialBuild = source.getSpecialbuild();
      }
      if (source.getCompatibilityversion() != null) {
        this.compatibilityVersion = source.getCompatibilityversion();
      }
      if (source.getPrerelease() != null) {
        this.prerelease = source.getPrerelease();
      }
      if (source.getPatched() != null) {
        this.patched = source.getPatched();
      }
    }
    setProject(source.getProject());
  }

  /**
   * Methods is required for documentation generation, throws
   * exception if called.
   *
   * @throws org.apache.tools.ant.BuildException
   *           if called
   */
  public void execute() throws org.apache.tools.ant.BuildException {
    throw new org.apache.tools.ant.BuildException("Not an actual task, but looks like one for documentation purposes");
  }

  /**
   * Gets Company name.
   * 
   * @return company name, may be null.
   */
  public String getCompanyname() {
    return this.companyName;
  }

  /**
   * Gets compatibility version.
   * 
   * @return compatibility version, may be null
   */
  public String getCompatibilityversion() {
    return this.compatibilityVersion;
  }

  public Reference getExtends() {
    return this.extendsRef;
  }

  /**
   * Gets comments.
   * 
   * @return comments, may be null.
   */
  public String getFilecomments() {
    return this.fileComments;
  }

  /**
   * Gets Description.
   * 
   * @return description, may be null.
   */
  public String getFiledescription() {
    return this.fileDescription;
  }

  /**
   * Gets file version.
   * 
   * @return file version, may be null.
   *
   */
  public String getFileversion() {
    return this.fileVersion;
  }

  /**
   * Gets if property name.
   * 
   * @return property name, may be null.
   */
  public final String getIf() {
    return this.ifCond;
  }

  /**
   * Gets internal name.
   * 
   * @return internal name, may be null.
   */
  public String getInternalname() {
    return this.internalName;
  }

  /**
   * Gets file language, should be an IETF RFC 3066 identifier, for example,
   * en-US.
   * 
   * @return language, may be null.
   */
  public String getLanguage() {
    return this.language;
  }

  /**
   * Gets legal copyright.
   * 
   * @return legal copyright, may be null.
   */
  public String getLegalcopyright() {
    return this.legalCopyright;
  }

  /**
   * Gets legal trademark.
   * 
   * @return legal trademark, may be null;
   */
  public String getLegaltrademarks() {
    return this.legalTrademarks;
  }

  /**
   * Gets original filename.
   * 
   * @return original filename, may be null.
   */
  public String getOriginalfilename() {
    return this.originalFilename;
  }

  /**
   * Gets patched.
   * 
   * @return patched, may be null.
   */
  public Boolean getPatched() {
    return this.patched;
  }

  /**
   * Gets prerelease.
   * 
   * @return prerelease, may be null.
   */
  public Boolean getPrerelease() {
    return this.prerelease;
  }

  /**
   * Gets private build.
   * 
   * @return private build, may be null.
   */
  public String getPrivatebuild() {
    return this.privateBuild;
  }

  /**
   * Gets product name.
   * 
   * @return product name, may be null.
   */
  public String getProductname() {
    return this.productName;
  }

  /**
   * Gets Product version.
   * 
   * @return product version, may be null
   */
  public String getProductversion() {
    return this.productVersion;
  }

  /**
   * Special build
   * 
   * @return special build, may be null.
   */
  public String getSpecialbuild() {
    return this.specialBuild;
  }

  /**
   * Gets if property name.
   * 
   * @return property name, may be null.
   */
  public final String getUnless() {
    return this.unlessCond;
  }

  /**
   * Returns true if the define's if and unless conditions (if any) are
   * satisfied.
   *
   * @exception BuildException
   *              throws build exception if name is not set
   */
  public final boolean isActive() throws BuildException {
    return CUtil.isActive(getProject(), this.ifCond, this.unlessCond);
  }

  /**
   * Returns a VersionInfo that reflects any inherited version information.
   * 
   * @return merged version information.
   *         \
   */
  public VersionInfo merge() {
    if (isReference()) {
      final VersionInfo refVersion = (VersionInfo) getCheckedRef(VersionInfo.class, "VersionInfo");
      return refVersion.merge();
    }
    Reference currentRef = this.getExtends();
    if (currentRef == null) {
      return this;
    }
    final Vector<VersionInfo> stack = new Vector<>(5);
    stack.addElement(this);
    while (currentRef != null) {
      final Object obj = currentRef.getReferencedObject(getProject());
      if (obj instanceof VersionInfo) {
        VersionInfo current = (VersionInfo) obj;
        if (current.isReference()) {
          current = (VersionInfo) current.getCheckedRef(VersionInfo.class, "VersionInfo");
        }
        if (stack.contains(current)) {
          throw this.circularReference();
        }
        stack.addElement(current);
        currentRef = current.getExtends();
      } else {
        throw new BuildException("Referenced element " + currentRef.getRefId() + " is not a versioninfo.");
      }
    }
    return new VersionInfo(stack);
  }

  /**
   * Sets company name.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setCompanyname(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.companyName = value;
  }

  /**
   * Sets compatibility version.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setCompatibilityversion(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.compatibilityVersion = value;
  }

  /**
   * Specifies that this element extends the element with id attribute with a
   * matching value. The configuration will be constructed from the settings
   * of this element, element referenced by extends, and the containing cc
   * element.
   *
   * @param extendsRef
   *          Reference to the extended processor definition.
   * @throws BuildException
   *           if this processor definition is a reference
   */
  public void setExtends(final Reference extendsRef) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.extendsRef = extendsRef;
  }

  /**
   * Sets comments.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setFilecomments(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.fileComments = value;
  }

  /**
   * Sets file description.
   * 
   * @param value
   *          new value
   */
  public void setFiledescription(final String value) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.fileDescription = value;
  }

  /**
   * Sets file version.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setFileversion(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.fileVersion = value;
  }

  /**
   * Sets an id that can be used to reference this element.
   *
   * @param id
   *          id
   */
  public void setId(final String id) {
    //
    // this is actually accomplished by a different
    // mechanism, but we can document it
    //
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
   *          property name
   */
  public final void setIf(final String propName) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.ifCond = propName;
  }

  /**
   * Sets internal name. Internal name will automatically be
   * specified from build step, only set this value if
   * intentionally overriding that value.
   *
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setInternalname(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.internalName = value;
  }

  /**
   * Sets language.
   * 
   * @param value
   *          new value, should be an IETF RFC 3066 language identifier.
   * @throws BuildException
   *           if specified with refid
   */
  public void setLanguage(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.language = value;
  }

  /**
   * Sets legal copyright.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setLegalcopyright(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.legalCopyright = value;
  }

  /**
   * Sets legal trademark.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setLegaltrademarks(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.legalTrademarks = value;
  }

  /**
   * Sets original name. Only set this value if
   * intentionally overriding the value from the build set.
   *
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setOriginalfilename(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.originalFilename = value;
  }

  /**
   * Sets prerelease.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setPatched(final boolean value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    if (value) {
      this.patched = Boolean.TRUE;
    } else {
      this.patched = Boolean.FALSE;
    }
  }

  /**
   * Sets prerelease.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setPrerelease(final boolean value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    if (value) {
      this.prerelease = Boolean.TRUE;
    } else {
      this.prerelease = Boolean.FALSE;
    }
  }

  /**
   * Sets private build.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setPrivatebuild(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.privateBuild = value;
  }

  /**
   * Sets product name.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setProductname(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.productName = value;
  }

  /**
   * Sets product version.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setProductversion(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.productVersion = value;
  }

  /**
   * Specifies that this element should behave as if the content of the
   * element with the matching id attribute was inserted at this location. If
   * specified, no other attributes should be specified.
   *
   */
  @Override
  public void setRefid(final Reference r) throws BuildException {
    super.setRefid(r);
  }

  /**
   * Sets private build.
   * 
   * @param value
   *          new value
   * @throws BuildException
   *           if specified with refid
   */
  public void setSpecialbuild(final String value) throws BuildException {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.specialBuild = value;
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
   *          name of property
   */
  public final void setUnless(final String propName) {
    if (isReference()) {
      throw tooManyAttributes();
    }
    this.unlessCond = propName;
  }
}

package com.github.maven_nar.cpptasks.openwatcom;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for the OpenWatcom linker.
 *
 * @author Curt Arnold
 */
public final class OpenWatcomCLinker
    extends OpenWatcomLinker {
  /**
   * Dll linker.
   */
  private static final OpenWatcomCLinker DLL_LINKER =
      new OpenWatcomCLinker(".dll");
  /**
   * Exe linker.
   */
  private static final OpenWatcomCLinker INSTANCE =
      new OpenWatcomCLinker(".exe");
  /**
   * Get linker instance.
   * @return OpenWatcomCLinker linker
   */
  public static OpenWatcomCLinker getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param outputSuffix String output suffix.
   */
  private OpenWatcomCLinker(final String outputSuffix) {
    super("wcl386", outputSuffix);
  }

  /**
   * Get linker.
   * @param type LinkType link type
   * @return Linker linker
   */
  public Linker getLinker(final LinkType type) {
    if (type.isStaticLibrary()) {
      return OpenWatcomLibrarian.getInstance();
    }
    if (type.isSharedLibrary()) {
      return DLL_LINKER;
    }
    return INSTANCE;
  }
}

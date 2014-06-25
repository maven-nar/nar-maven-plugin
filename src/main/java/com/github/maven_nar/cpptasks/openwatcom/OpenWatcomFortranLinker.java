package com.github.maven_nar.cpptasks.openwatcom;

import com.github.maven_nar.cpptasks.compiler.LinkType;
import com.github.maven_nar.cpptasks.compiler.Linker;

/**
 * Adapter for the OpenWatcom Fortran linker.
 *
 * @author Curt Arnold
 */
public final class OpenWatcomFortranLinker
    extends OpenWatcomLinker {
  /**
   * Singleton for DLL linking.
   */
  private static final OpenWatcomFortranLinker DLL_LINKER = new
      OpenWatcomFortranLinker(".dll");
  /**
   * Singleton for executables.
   */
  private static final OpenWatcomFortranLinker INSTANCE = new
      OpenWatcomFortranLinker(".exe");
  /**
   * Get instance.
   * @return OpenWatcomFortranLinker linker
   */
  public static OpenWatcomFortranLinker getInstance() {
    return INSTANCE;
  }

  /**
   * Constructor.
   * @param outputSuffix String output suffix
   */
  private OpenWatcomFortranLinker(final String outputSuffix) {
    super("wfl386", outputSuffix);
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

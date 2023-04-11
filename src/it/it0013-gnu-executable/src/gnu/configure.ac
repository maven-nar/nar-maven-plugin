dnl Process this file with autoconf to produce a configure script
AC_INIT(it0013-gnu-executable, 1.0 Mark.Donszelmann@gmail.com)
AC_CONFIG_AUX_DIR(config)
AC_CONFIG_SRCDIR(src/Executable.c)
AC_CONFIG_MACRO_DIR([m4])

AM_INIT_AUTOMAKE

AC_PROG_CC

AC_ARG_WITH(pkginfo,
  AC_HELP_STRING([--with-pkginfo=PKG],
                 [Package information string]),
  [case "$withval" in
    (yes) AC_MSG_ERROR([package information not specified]) ;;
    (*) PKG_INFO="$withval" ;;
   esac])
AC_SUBST(PKG_INFO)

AC_OUTPUT(Makefile src/Makefile)


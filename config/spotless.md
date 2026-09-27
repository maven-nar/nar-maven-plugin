# Spotless code style

`spotless-pom.xml` is a standalone Maven configuration derived from all three
Eclipse inputs in this directory:

- `eclipse-code-formatter-profile.xml`: two-space indentation, 120-column Java
  lines, 80-column comments, braces on the same line, and the remaining formatter
  settings. Spotless reads this file directly.
- `eclipse-code-cleanup-profile.xml`: import cleanup, member sorting, and trailing
  whitespace removal, plus the Eclipse-only actions listed below.
- `eclipse-save-actions.png`: format **all lines**, organize imports, and apply
  additional actions. In particular, sort members while preserving fields, enum
  constants, and initializers.

## Run on files added on a branch

Use Python 3, Git, Maven, and a JDK 17 or newer (tested with JDK 17 and 21). From the
repository root:

```sh
python3 config/spotless.py apply --base origin/master
python3 config/spotless.py check --base origin/master
```

Set `JAVA_HOME` and `PATH` to the formatting JDK as needed. `--mvn` selects a Maven
executable. The formatter uses Spotless 3.10.2 and Eclipse JDT 4.26; its first run
may download their tooling dependencies. This does not change NAR's Java 7
source/target settings or add dependencies to the plugin.

The helper compares the working tree with the merge base of `HEAD` and `--base`.
It selects only added files, including staged additions; stage new files before
running it. Existing files modified on the branch are excluded. It formats each
selected file in full, matching the screenshot's **Format all lines** setting.

The helper temporarily copies the POM to the repository root, invokes Spotless
with an explicit file selection, and removes the temporary POM on exit. This
keeps all saved configuration in `config`, gives Maven the correct base directory,
and leaves the normal build independent of the formatter. Formatter output and
cache files go under `target/spotless`. Checks bypass the incremental cache.

Java files receive the Eclipse formatter and the cleanup steps below. The supplied
profiles contain no C, BeanShell, or XML syntax formatter settings, so those files
and the text map fixtures receive only trailing-whitespace removal and a final
newline. All selected text formats use UTF-8 and LF line endings.

## Cleanup mapping and limits

Spotless's Eclipse integration accepts a **formatter** profile, not an Eclipse
**cleanup** profile. Passing the cleanup XML as another formatter file would not
execute its actions. The configuration therefore maps the supported actions
explicitly:

| Eclipse cleanup/save action | Spotless configuration |
| --- | --- |
| Format source code; correct indentation | `eclipse`, using the existing formatter XML |
| Organize imports | `importOrder`: `java,javax,org,com`, other imports, static imports |
| Remove unused imports | `removeUnusedImports` |
| Sort members, excluding fields, enum constants, and initializers | `sortMembersEnabled=true`, `sortMembersDoNotSortFields=true` |
| Member category order | Eclipse default `T,SF,SI,SM,F,I,C,M`; visibility sorting disabled |
| Remove trailing whitespace on every line | `trimTrailingWhitespace` |
| Insert a missing final newline | `endWithNewline`, also requested by the formatter XML |

Neither the XML nor the screenshot specifies an import or member category order.
The import groups follow the repository's existing style. The member categories
use Spotless's documented Eclipse default.

The following enabled cleanup actions require Eclipse's cleanup engine and are
**not performed or checked by this Spotless configuration**:

- Qualify instance field accesses with `this` and qualify static accesses through
  instances or subtypes with the declaring class.
- Add blocks around control-statement bodies.
- Add `final` to eligible parameters, local variables, and private fields.
- Remove unnecessary parentheses and casts; convert eligible loops to enhanced
  `for` loops.
- Add missing `@Override`, interface-method `@Override`, and `@Deprecated`
  annotations; add missing methods and a default serial version UID.
- Remove unnecessary NLS tags.

The screenshot enables private-field `final` modifiers and indentation correction,
whereas the cleanup XML disables them. The screenshot is the intended save-action
setting for these differences. Formatting corrects indentation; adding `final`
still requires Eclipse. No text substitutions approximate binding-sensitive Java
cleanup operations.

The XML's child settings for removing unused private members, diamond conversion,
and lambda conversion have disabled parent switches (`remove_unused_private_members`,
`use_type_arguments`, and `convert_functional_interfaces`). Those actions stay
disabled, including lambda conversion that would break Java 7 compatibility.

See the [Spotless Maven documentation](https://github.com/diffplug/spotless/blob/main/plugin-maven/README.md#eclipse-jdt)
for the Eclipse formatter and member-sorting options.

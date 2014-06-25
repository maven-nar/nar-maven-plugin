package com.github.maven_nar.cpptasks.ide;


/**
 * Defines a comment to place in the generated project files.
 *
 */
public final class CommentDef {
    private String text;

    public CommentDef() {
		text = "";
    }


    public String getText() {
		return text;
    }
	public void addText(final String newText) {
		text += newText;
	}

    public String toString() {
        return text;
    }
}

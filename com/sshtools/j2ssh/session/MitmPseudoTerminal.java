/*
 * Created on Mar 2, 2003
 *
 */
package com.sshtools.j2ssh.session;

/**
 * @author David G&uuml;mbel<br>
 * created 02 March 2003<br>
 * Implementation of a <code>PseudoTerminal</code> for use in mitm attacks.
 * 
 */
public class MitmPseudoTerminal implements PseudoTerminal {

private int columns;
private int height;
private int width;
private int rows;
private String term = "xterm";
	
	/**
	 * Creates a new<code> MitmPseudoTerminal</code>.
	 * @param term
	 * @param width
	 * @param height
	 * @param rows
	 * @param columns
	 */
	public MitmPseudoTerminal(String term, int width, int height, int rows, int columns) {
		this.term = term;
		this.width = width;
		this.height = height;
		this.rows = rows;
		this.columns = columns;
		
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getColumns()
	 */
	public int getColumns() {
		return columns;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getEncodedTerminalModes()
	 */
	public String getEncodedTerminalModes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getHeight()
	 */
	public int getHeight() {
		return height;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getRows()
	 */
	public int getRows() {
		return rows;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getTerm()
	 */
	public String getTerm() {
		return term;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.PseudoTerminal#getWidth()
	 */
	public int getWidth() {
		return width;
	}

}

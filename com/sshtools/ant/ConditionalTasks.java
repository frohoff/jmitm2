/*
 * Sshtools - Java SSH2 API
 *
 * Copyright (C) 2002 Lee David Painter.
 *
 * Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.sshtools.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;


/**
 * A simple ant task container that executes the nested tasks only if a set of
 * conditions are met. This class currently provides execution of tasks on the
 * condition that certain files and/or directories exist.
 *
 * @author <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 * @version $Id: ConditionalTasks.java,v 1.2 2002/12/26 18:14:10 martianx Exp $
 */
public class ConditionalTasks
    extends Task
    implements TaskContainer {
    private ArrayList tasks = new ArrayList();
    private String dirs;
    private String files;
    private String name;

    /**
     * Creates a new ConditionalTasks object.
     */
    public ConditionalTasks() {
    }

    /**
     * Sets a comma delimted string of directories that will be verifed
     * before allowing the tasks to be performed. Either this parameter or
     * the files parameter must be provided.
     *
     * @param dirs a comma delimited string of directories that must exist
     */
    public void setDirs(String dirs) {
        this.dirs = dirs;
    }

    /**
     * Sets a comma delimted string of files that will be verifed
     * before allowing the tasks to be performed. Either this parameter or
     * the dirs parameter must be provided.
     *
     * @param files a comma delimited string of files that must exist
     */
    public void setFiles(String files) {
        this.files = files;
    }

    /**
     * Sets the name for these conditional tasks. This parameter is madatory
     *
     * @param name a descriptive name for the tasks
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Called by the ANT framework to add a nested task.
     *
     * @param task The task to be performed
     */
    public void addTask(Task task) {
        tasks.add(tasks.size(), task);
    }

    /**
     * Called by the ANT framework to execute the task.
     */
    public void execute() {
        if ((dirs==null) && (files==null)) {
            throw new BuildException("ConditionalTasks: You must supply at least one of either the files or dirs properties");
        }

        if (name==null) {
            throw new BuildException("ConditionalTasks: You must supply a name for these conditional tasks!");
        }

        log("Verifying conditions for " + name);

        if (dirs!=null) {
            StringTokenizer tokenizer = new StringTokenizer(dirs, ",");
            File f;

            while (tokenizer.hasMoreElements()) {
                String condition = (String) tokenizer.nextElement();
                f = new File(condition);

                if (!f.exists()) {
                    log("ConditionalTasks: Directory '" + condition
                        + "' does not exist; " + name
                        + " will not be performed");

                    return;
                }
            }
        }

        if (files!=null) {
            StringTokenizer tokenizer = new StringTokenizer(files, ",");
            File f;

            while (tokenizer.hasMoreElements()) {
                String condition = (String) tokenizer.nextElement();
                f = new File(condition);

                if (!f.exists()) {
                    log("ConditionalTasks: File '" + condition
                        + "' does not exist; " + name
                        + " will not be performed");

                    return;
                }
            }
        }

        System.out.println("Executing Conditional Tasks");

        Iterator it = tasks.iterator();
        Task task;

        while (it.hasNext()) {
            task = (Task) it.next();
            task.perform();
        }
    }
}

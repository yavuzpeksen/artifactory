/**
 * Copyright (c) 2007-2008 Sonatype, Inc. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
/*
 * Additional contributors:
 *    JFrog Ltd.
 */

package org.artifactory.repo.index.locator;

import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.sonatype.nexus.artifact.Gav;
import org.sonatype.nexus.artifact.GavCalculator;
import org.sonatype.nexus.index.locator.GavHelpedLocator;

import java.io.File;
import java.util.List;

/**
 * @author yoavl
 */
public class PomLocator implements GavHelpedLocator {

    public File locate(File source, GavCalculator gavCalculator, Gav gav) {
        //Get the pom name
        String artifactName = gav.getArtifactId() + "-" + gav.getVersion() + ".pom";
        JcrFile file = (JcrFile) source;
        List<JcrFsItem> children = file.getParentFolder().getItems();
        for (JcrFsItem child : children) {
            if (artifactName.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }
}
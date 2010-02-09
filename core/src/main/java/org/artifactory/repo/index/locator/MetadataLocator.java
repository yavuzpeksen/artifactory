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

import org.sonatype.nexus.index.locator.Locator;

import java.io.File;

/**
 * @author yoavl
 */
public class MetadataLocator implements Locator {

    public File locate(File source) {
        //We never locate metadata
        //return new File(source.getParentFile().getParentFile() - may throw NPE, "maven-metadata.xml") {
        return new File("", "maven-metadata.xml") {
            @Override
            public boolean exists() {
                return false;
            }
        };
    }
}
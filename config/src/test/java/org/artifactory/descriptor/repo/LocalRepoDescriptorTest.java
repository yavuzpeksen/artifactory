/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.descriptor.repo;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the LocalRepoDescriptor.
 *
 * @author Yossi Shaul
 */
@Test
public class LocalRepoDescriptorTest {

    public void defaultConstructor() {
        LocalRepoDescriptor localRepo = new LocalRepoDescriptor();

        assertNull(localRepo.getKey(), "Key should be null");
        assertNull(localRepo.getDescription(), "Description should be null");
        assertEquals(localRepo.getIncludesPattern(), "**/*",
                "Includes pattern should be **/*");
        assertNull(localRepo.getExcludesPattern(), "Excludes pattern should be null");
        assertEquals(localRepo.getMaxUniqueSnapshots(), 0,
                "Max unique snapshot should be 0 by default");
        assertEquals(localRepo.getSnapshotVersionBehavior(),
                SnapshotVersionBehavior.NONUNIQUE,
                "SnapshotVersionBehavior should be non-unique by default");
        assertFalse(localRepo.isSuppressPomConsistencyChecks(),
                "Default should not supress pom consistency checks");
        assertNotNull(localRepo.getPropertySets(), "Property sets list should not be null");
    }
}
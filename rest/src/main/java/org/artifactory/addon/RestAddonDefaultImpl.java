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

package org.artifactory.addon;

import org.artifactory.api.rest.artifact.FileList;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.resource.artifact.DownloadResource;
import org.artifactory.rest.resource.artifact.SyncResource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * Default implementation of the rest add-on
 *
 * @author Yoav Landman
 */
@Component
public class RestAddonDefaultImpl implements RestAddon {

    public boolean isDefault() {
        return true;
    }

    public MoveCopyResult copy(String path, String target, int dryRun) throws Exception {
        throw new MissingRestAddonException();
    }

    public MoveCopyResult move(String path, String target, int dryRun) throws Exception {
        throw new MissingRestAddonException();
    }

    public Response download(String path, DownloadResource.Content content, int mark,
            HttpServletResponse response) throws Exception {
        throw new MissingRestAddonException();
    }

    public Set<String> searchArtifactsByPattern(String pattern) {
        throw new MissingRestAddonException();
    }

    public MoveCopyResult moveOrCopyBuildItems(boolean move, String buildName, String buildNumber, String started,
            String to, int arts, int deps, StringList scopes, int dry) {
        throw new MissingRestAddonException();
    }

    public FileList getFileList(String uri, String path, int deep) {
        throw new MissingRestAddonException();
    }

    public Response replicate(String path, int progress, int mark, int deleteExisting, SyncResource.Overwrite overwrite,
            HttpServletResponse httpResponse) {
        throw new MissingRestAddonException();
    }
}

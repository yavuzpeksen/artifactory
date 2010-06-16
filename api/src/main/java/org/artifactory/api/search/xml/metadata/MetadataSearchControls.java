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

package org.artifactory.api.search.xml.metadata;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.search.SearchControlsBase;

/**
 * @author Yoav Landman
 */
public class MetadataSearchControls<T> extends SearchControlsBase {

    private String metadataName;
    private String path;
    private String value;
    private Class<? extends T> metadataObjectClass;

    /**
     * Default constructor
     */
    public MetadataSearchControls() {
    }

    /**
     * Copy constructor
     *
     * @param metadataSearchControls Controls to copy
     */
    public MetadataSearchControls(MetadataSearchControls metadataSearchControls) {
        this.metadataName = metadataSearchControls.metadataName;
        this.path = metadataSearchControls.path;
        this.value = metadataSearchControls.value;
        this.selectedRepoForSearch = metadataSearchControls.selectedRepoForSearch;
        //noinspection unchecked
        this.metadataObjectClass = metadataSearchControls.metadataObjectClass;
        setLimitSearchResults(metadataSearchControls.isLimitSearchResults());
    }

    public String getMetadataName() {
        return metadataName;
    }

    public void setMetadataName(String metadataName) {
        this.metadataName = metadataName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Class<? extends T> getMetadataObjectClass() {
        return metadataObjectClass;
    }

    public void setMetadataObjectClass(Class<T> metadataObjectClass) {
        this.metadataObjectClass = metadataObjectClass;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(metadataName);
    }
}
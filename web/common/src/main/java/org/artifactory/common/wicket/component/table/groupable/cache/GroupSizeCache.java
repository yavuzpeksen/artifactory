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

package org.artifactory.common.wicket.component.table.groupable.cache;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class GroupSizeCache implements Serializable {
    private List<GroupMetaData> indicesList;

    public GroupSizeCache(Iterator iterator, IChoiceRenderer renderer) {
        indicesList = new ArrayList<GroupMetaData>();
        int index = 0;
        int beginIndex = 0;
        String prevId = null;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            String id = renderer.getIdValue(o, index);
            if (prevId != null && !prevId.equals(id)) {
                indicesList.add(new GroupMetaData(beginIndex, index));
                beginIndex = index;
            }

            prevId = id;
            index++;
        }
        indicesList.add(new GroupMetaData(beginIndex, index));
    }

    public static GroupSizeCache getSizeCache(Iterator iterator, IChoiceRenderer renderer) {
        return new GroupSizeCache(iterator, renderer);
    }

    public int getGroupSize(int index) {
        int foundIndex = Collections.binarySearch(indicesList, new GroupMetaData(index, 0));
        if (foundIndex < 0) {
            // index is inside group, get group start index
            foundIndex = -2 - foundIndex;
        }
        GroupMetaData group = indicesList.get(foundIndex);
        return group.getSize();
    }
}

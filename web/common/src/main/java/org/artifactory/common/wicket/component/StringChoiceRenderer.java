/*
 * This file is part of Artifactory.
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

package org.artifactory.common.wicket.component;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

/**
 * @author Yoav Aharoni
 */
public class StringChoiceRenderer implements IChoiceRenderer {
    private static final StringChoiceRenderer INSTANCE = new StringChoiceRenderer();

    public Object getDisplayValue(Object object) {
        return object.toString();
    }

    public String getIdValue(Object object, int index) {
        return String.valueOf(index);
    }

    public static StringChoiceRenderer getInstance() {
        return INSTANCE;
    }
}
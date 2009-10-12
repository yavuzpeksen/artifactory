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

package org.artifactory.common.wicket.behavior.combobox;

import org.apache.wicket.Component;
import org.apache.wicket.markup.ComponentTag;
import org.artifactory.common.wicket.behavior.template.TemplateBehavior;
import org.artifactory.common.wicket.resources.dojo.DojoPackage;

/**
 * @author Yoav Aharoni
 */
public class ComboBoxBehavior extends TemplateBehavior {
    public ComboBoxBehavior() {
        getResourcePackage().dependsOn(new DojoPackage()).addJavaScript();
    }

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        super.onComponentTag(component, tag);
        assertTagName(tag, "select");
        tag.put("dojoType", "dijit.form.ComboBox");
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }
}
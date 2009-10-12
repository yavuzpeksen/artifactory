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

package org.artifactory.common.wicket.component.autocomplete;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.artifactory.common.wicket.behavior.CssClass;

import java.util.Iterator;

/**
 * @author Yoav Aharoni
 */
public abstract class ImprovedAutoCompleteTextField extends AutoCompleteTextField {
    public static final AutoCompleteSettings DEFAULT_SETTINGS =
            new AutoCompleteSettings().setShowListOnEmptyInput(true).setMaxHeightInPx(200);

    private static final ResourceReference AUTOCOMPLETE_JS = new JavascriptResourceReference(
            ImprovedAutoCompleteBehavior.class, "improved-autocomplete.js");

    public ImprovedAutoCompleteTextField(String id, IModel model, Class type, AutoCompleteSettings settings) {
        this(id, model, type, StringAutoCompleteRenderer.INSTANCE, settings);
    }

    public ImprovedAutoCompleteTextField(String id, IModel object, AutoCompleteSettings settings) {
        this(id, object, null, settings);
    }

    public ImprovedAutoCompleteTextField(String id, IModel object) {
        this(id, object, null, DEFAULT_SETTINGS);
    }

    public ImprovedAutoCompleteTextField(String id, AutoCompleteSettings settings) {
        this(id, null, settings);
    }

    public ImprovedAutoCompleteTextField(String id) {
        this(id, null, DEFAULT_SETTINGS);
    }

    public ImprovedAutoCompleteTextField(String id, IAutoCompleteRenderer renderer) {
        this(id, (IModel) null, renderer);
    }

    public ImprovedAutoCompleteTextField(String id, Class type, IAutoCompleteRenderer renderer) {
        this(id, null, type, renderer, DEFAULT_SETTINGS);
    }

    public ImprovedAutoCompleteTextField(String id, IModel model, IAutoCompleteRenderer renderer) {
        this(id, model, null, renderer, DEFAULT_SETTINGS);
    }

    public ImprovedAutoCompleteTextField(String id, IModel model, Class type,
            IAutoCompleteRenderer renderer, AutoCompleteSettings settings) {
        super(id, model, type, renderer, settings);

        add(new CssClass("text autocomplete"));
        add(new SimpleAttributeModifier("autocomplete", "off"));
    }

    @Override
    protected AutoCompleteBehavior newAutoCompleteBehavior(IAutoCompleteRenderer renderer,
            AutoCompleteSettings settings) {
        return new AutoCompleteBehavior(renderer, settings) {

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.renderJavascriptReference(AUTOCOMPLETE_JS);
            }

            @Override
            protected Iterator getChoices(String input) {
                return ImprovedAutoCompleteTextField.this.getChoices(input);
            }
        };
    }
}
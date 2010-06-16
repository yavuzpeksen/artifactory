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

package org.artifactory.webapp.wicket.page.config.license;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.form.Form;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

/**
 * Artifactory licensing information and installation.
 *
 * @author Yossi Shaul
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class LicensePage extends AuthenticatedPage {

    public LicensePage() {
        Form form = new Form("form");
        add(form);

        LicensePanel licensePanel = new LicensePanel("licensePanel");
        form.add(licensePanel);

        form.add(licensePanel.createSaveButton(form));
        form.add(createCancelButton());
    }

    /**
     * Creates a cancel button for the panel
     *
     * @return TitledAjaxLink - The cancel button
     */
    private TitledAjaxLink createCancelButton() {
        return new TitledAjaxLink("cancel", "Cancel") {
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(LicensePage.class);
            }
        };
    }

    @Override
    public String getPageName() {
        return "Artifactory Pro License";
    }
}
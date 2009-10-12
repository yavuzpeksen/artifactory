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

import org.apache.wicket.Page;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecuredPageLink extends BookmarkablePageLink {

    private final Class<? extends Page> pageClass;

    public SecuredPageLink(final String id, String caption, final Class<? extends Page> pageClass) {
        super(id, pageClass);
        if (caption != null) {
            setModel(new Model(caption));
        }
        this.pageClass = pageClass;
    }

    @Override
    public boolean isEnabled() {
        IAuthorizationStrategy authorizationStrategy = getSession().getAuthorizationStrategy();
        boolean authorized = authorizationStrategy.isInstantiationAuthorized(pageClass);
        return authorized && super.isEnabled() && isEnableAllowed();
    }
}
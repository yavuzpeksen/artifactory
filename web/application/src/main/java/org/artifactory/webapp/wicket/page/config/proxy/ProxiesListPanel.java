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

package org.artifactory.webapp.wicket.page.config.proxy;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.config.CentralConfigService;
import static org.artifactory.common.wicket.component.CreateUpdateAction.CREATE;
import static org.artifactory.common.wicket.component.CreateUpdateAction.UPDATE;
import org.artifactory.common.wicket.component.modal.panel.BaseModalPanel;
import org.artifactory.common.wicket.component.panel.list.ModalListPanel;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.page.config.SchemaHelpModel;

import java.util.List;

/**
 * @author Yossi Shaul
 */
public class ProxiesListPanel extends ModalListPanel<ProxyDescriptor> {
    @SpringBean
    private CentralConfigService centralConfigService;

    private MutableCentralConfigDescriptor mutableCentralConfig;

    public ProxiesListPanel(String id) {
        super(id);
        mutableCentralConfig = centralConfigService.getMutableDescriptor();
    }

    @Override
    public String getTitle() {
        return "Proxies";
    }

    @Override
    protected Component newToolbar(String id) {
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        SchemaHelpModel helpModel = new SchemaHelpModel(descriptor, "proxies");
        return new SchemaHelpBubble(id, helpModel);
    }

    @Override
    protected BaseModalPanel newCreateItemPanel() {
        return new ProxyCreateUpdatePanel(CREATE, new ProxyDescriptor(), this);
    }

    @Override
    protected BaseModalPanel newUpdateItemPanel(ProxyDescriptor itemObject) {
        return new ProxyCreateUpdatePanel(UPDATE, itemObject, this);
    }

    @Override
    protected String getDeleteConfirmationText(ProxyDescriptor itemObject) {
        String name = itemObject.getKey();
        return "Are you sure you wish to delete the proxy '" + name + "'?";
    }

    @Override
    protected void deleteItem(ProxyDescriptor itemObject, AjaxRequestTarget target) {
        mutableCentralConfig.removeProxy(itemObject.getKey());
        centralConfigService.saveEditedDescriptorAndReload(mutableCentralConfig);
    }

    @Override
    protected List<ProxyDescriptor> getList() {
        return mutableCentralConfig.getProxies();
    }

    @Override
    protected void addColumns(List<IColumn> columns) {
        columns.add(new PropertyColumn(new Model("Proxy Key"), "key", "key"));
        columns.add(new PropertyColumn(new Model("Host"), "host", "host"));
        columns.add(new PropertyColumn(new Model("Port"), "port", "port"));
    }

    MutableCentralConfigDescriptor getEditingDescriptor() {
        return mutableCentralConfig;
    }
}
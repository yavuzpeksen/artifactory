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

package org.artifactory.webapp.wicket.page.browse.treebrowser.action;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.ajax.ConfirmationAjaxCallDecorator;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.common.wicket.util.WicketUtils;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.webapp.wicket.page.browse.treebrowser.TreeBrowsePanel;
import org.artifactory.webapp.wicket.page.logs.SystemLogsPage;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.Iterator;
import java.util.List;

/**
 * This panel displays a list of local repositories the user can select to move a path to.
 *
 * @author Yossi Shaul
 */
public class MovePathPanel extends MoveBasePanel {

    @SpringBean
    private AuthorizationService authorizationService;

    @SpringBean
    private RepositoryService repoService;

    private final RepoPath pathToMove;
    private final Component componentToRefresh;
    private final TreeBrowsePanel browseRepoPanel;

    public MovePathPanel(String id, RepoPath pathToMove, Component componentToRefresh,
            TreeBrowsePanel browseRepoPanel) {
        super(id);
        this.pathToMove = pathToMove;
        this.componentToRefresh = componentToRefresh;
        this.browseRepoPanel = browseRepoPanel;
        init();
    }

    @Override
    protected TitledAjaxSubmitLink createSubmitButton(Form form, String wicketId) {
        return new TitledAjaxSubmitLink(wicketId, "Move", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                String targetRepoKey = getSelectedTargetRepository();

                MoveMultiStatusHolder status = repoService.move(pathToMove, targetRepoKey, false);

                if (!status.isError() && !status.hasWarnings()) {
                    getPage().info("Successfully moved '" + pathToMove + "'.");
                } else {
                    if (status.hasWarnings()) {
                        List<StatusEntry> warnings = status.getWarnings();
                        String logs;
                        if (authorizationService.isAdmin()) {
                            CharSequence systemLogsPage = WicketUtils.mountPathForPage(SystemLogsPage.class);
                            logs = "<a href=\"" + systemLogsPage + "\">log</a>";
                        } else {
                            logs = "log";
                        }
                        getPage().warn(warnings.size() + " warnings have been produced during the move. Please " +
                                "review the " + logs + " for further information.");
                    }
                    if (status.isError()) {
                        String message = status.getStatusMsg();
                        Throwable exception = status.getException();
                        if (exception != null) {
                            message = exception.getMessage();
                        }
                        getPage().error("Failed to move '" + pathToMove + "': " + message);
                    }
                }

                // colapse all tree nodes
                if (componentToRefresh instanceof Tree) {
                    // we collapse all since we don't know which path will eventually move
                    Tree tree = (Tree) componentToRefresh;
                    ITreeState treeState = tree.getTreeState();
                    treeState.collapseAll();
                    DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModelObject();
                    treeState.selectNode(((TreeNode) treeModel.getRoot()).getChildAt(0), true);
                }

                browseRepoPanel.removeNodePanel(target);
                target.addComponent(componentToRefresh);
                AjaxUtils.refreshFeedback(target);
                ModalHandler.closeCurrent(target);
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                // add confirmation dialog when clicked
                String message = String.format("Are you sure you wish to move '%s'?", pathToMove);
                return new ConfirmationAjaxCallDecorator(message);
            }
        };
    }

    @Override
    protected MoveMultiStatusHolder executeDryRun(String targetRepoKey) {
        return repoService.move(pathToMove, targetRepoKey, true);
    }

    @Override
    protected List<LocalRepoDescriptor> getDeployableLocalReposKeys() {
        return getDeployableLocalReposKeysExcludingSource(pathToMove.getRepoKey());
    }

    private List<LocalRepoDescriptor> getDeployableLocalReposKeysExcludingSource(String sourceRepoKey) {
        // only display repositories the user has deploy permission on
        List<LocalRepoDescriptor> localRepos = repoService.getDeployableRepoDescriptors();
        // remove source repository from the targets list
        Iterator<LocalRepoDescriptor> iter = localRepos.iterator();
        while (iter.hasNext()) {
            if (iter.next().getKey().equals(sourceRepoKey)) {
                iter.remove();
            }
        }
        return localRepos;
    }
}
package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.ivy;

import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.TextContentPanel;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.ivy.IvyService;
import org.artifactory.webapp.actionable.FileActionable;


/**
 * @author Eli Givoni
 */
public class XmlViewTabPanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private IvyService ivyService;

    public XmlViewTabPanel(String id, FileActionable repoItem, XmlTypes xmlType) {
        super(id);
        add(new CssClass("veiw-tab"));
        if (xmlType.equals(XmlTypes.IVY_XML)) {
            addDependencySection(repoItem);
        } else {
            WebMarkupContainer dumyContainer = new WebMarkupContainer("moduleBorder");
            dumyContainer.add(new WebMarkupContainer("moduleContent"));
            add(dumyContainer);
        }
        TitledBorder xmlBorder = xmlType.getTabBorder();
        add(xmlBorder);
        TextContentPanel content = new TextContentPanel("content", true);
        content.setContent(getContent(repoItem));
        xmlBorder.add(content);
    }

    private void addDependencySection(FileActionable item) {
        TitledBorder moduleBorder = new FieldSetBorder("moduleBorder");
        RepoPath repoPath = item.getRepoPath();
        StringBuilder sb = new StringBuilder();
        ModuleDescriptor descriptor = ivyService.parseIvyFile(repoPath);
        if (descriptor == null) {
            //TODO: [by ys] display message: Failed to parse file
            sb.append(" Failed to parse file");
        } else {
            buildModuleContent(sb, descriptor);
        }
        String content = sb.toString();
        TextContentPanel moduleContent = new TextContentPanel("moduleContent", true);
        moduleContent.setContent(content);
        moduleBorder.add(moduleContent);
        add(moduleBorder);
    }

    private void buildModuleContent(StringBuilder sb, ModuleDescriptor descriptor) {
        ModuleRevisionId moduleRevisionId = descriptor.getModuleRevisionId();
        ModuleId module = moduleRevisionId.getModuleId();

        sb.append("<dependency org=\"");
        sb.append(module.getOrganisation()).append("\" ");
        sb.append("name=\"");
        sb.append(module.getName()).append("\" ");
        sb.append("rev=\"");
        sb.append(moduleRevisionId.getRevision()).append("\" />");
    }


    private String getContent(FileActionable item) {
        FileInfo info = item.getFileInfo();
        return repoService.getTextFileContent(info);
    }

    public enum XmlTypes {
        IVY_XML("Ivy Module", "Ivy View"), GENERAL_XML("XML", "XML View");

        private String borderTitle;
        private String tabTitle;

        XmlTypes(String borderTitle, String tabTitle) {
            this.tabTitle = tabTitle;
            this.borderTitle = borderTitle;
        }

        public String getTabTitle() {
            return tabTitle;
        }

        public String getBorderTitle() {
            return borderTitle;
        }

        private TitledBorder getTabBorder() {
            return new FieldSetBorder("xmlBorder") {
                @Override
                public String getTitle() {
                    return getBorderTitle();
                }
            };
        }
    }
}

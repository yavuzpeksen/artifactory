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

package org.artifactory.search.metadata.pom;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchControls;
import org.artifactory.api.search.metadata.pom.PomSearchResult;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.FileInfoProxy;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.search.SearcherBase;
import org.artifactory.util.PathUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the POM search logic
 *
 * @author Noam Tenne
 */
public class PomSearcher extends SearcherBase<MetadataSearchControls, PomSearchResult> {
    private static final String FORWARD_SLASH = "/";

    /**
     * Searches for content within any pom in the repository
     *
     * @param controls The search controls
     * @return SearchResults<PomSearchResult> - Search results
     * @throws RepositoryException
     */
    @Override
    public SearchResults<PomSearchResult> doSearch(MetadataSearchControls controls) throws RepositoryException {
        QueryResult queryResult = doQuery(controls, null);
        List<PomSearchResult> results = new ArrayList<PomSearchResult>();
        RowIterator rows = queryResult.getRows();

        //Filter the results and if the search results are limited, stop when reached more than max results + 1
        while (rows.hasNext() && (!controls.isLimitSearchResults() || (results.size() < getMaxResults()))) {
            Row row = rows.nextRow();
            String path = row.getValue(JcrConstants.JCR_PATH).getString();
            String metadataNameFromPath = NamingUtils.getMetadataNameFromJcrPath(path);

            //Node path might not be an xml path (if user has searched for "/" in the path with a blank value) 
            int xmlNodeIndex = path.lastIndexOf("/" + JcrService.NODE_ARTIFACTORY_XML);
            if (xmlNodeIndex > 0) {
                path = path.substring(0, xmlNodeIndex);
            }
            Node artifactNode = (Node) getJcrService().getManagedSession().getItem(path);
            RepoPath repoPath = JcrPath.get().getRepoPath(artifactNode.getPath());
            FileInfoProxy fileInfo = new FileInfoProxy(repoPath);
            boolean canRead = getAuthService().canRead(fileInfo.getRepoPath());
            if (canRead) {
                PomSearchResult result = new PomSearchResult(fileInfo, metadataNameFromPath);
                results.add(result);
            }
        }
        return new SearchResults<PomSearchResult>(results, rows.getSize());
    }

    /**
     * Searches for POM's within a certain path
     *
     * @param controls The search controls
     * @return QueryResult - Search results
     * @throws RepositoryException
     */
    public QueryResult searchForDeployableUnits(PomSearchControls controls) throws RepositoryException {
        QueryResult queryResult = doQuery(controls, controls.getRepoPath());
        return queryResult;
    }

    /**
     * Performs a query for POM files. If the repo path is valid, it will search for poms in the given path. If the
     * search controls are valid, it will search for content within the poms according to the given params
     *
     * @param controls  The search controls - not mandatory
     * @param pathToPom - Path - not mandatory
     * @return QueryResult - Search results
     * @throws RepositoryException
     */
    private QueryResult doQuery(MetadataSearchControls controls, RepoPath pathToPom) throws RepositoryException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("/jcr:root").append(JcrPath.get().getRepoJcrRootPath()).append(FORWARD_SLASH);

        //Build path to POM (if valid)
        if ((pathToPom == null)) {
            //Repo path not valid - append to form a double slash
            queryBuilder.append(FORWARD_SLASH);
        } else {
            //Add repo key
            if (!StringUtils.isEmpty(pathToPom.getRepoKey())) {
                addElementToSb(queryBuilder, pathToPom.getRepoKey());
            }

            //Add path
            if (!StringUtils.isEmpty(pathToPom.getPath()) && PathUtils.hasText(pathToPom.getPath())) {
                String relativePath = ISO9075.encodePath(pathToPom.getPath());
                addElementToSb(queryBuilder, relativePath);
            }

            queryBuilder.append(FORWARD_SLASH);
        }

        //Search for any POM
        queryBuilder.append("element(*)[jcr:like(@").append(JcrHelper.PROP_ARTIFACTORY_NAME).append(",'%.pom')]");

        String xPath = controls.getPath();
        String exp = controls.getValue();

        //If search controls are valid - Search for content within the POM
        if ((!StringUtils.isEmpty(xPath)) && (!StringUtils.isEmpty(exp))) {

            xPath = xPath.replace("\\", "/");
            if ((xPath.length() > 1) && (xPath.startsWith("/"))) {
                xPath = xPath.substring(1);
            }
            if (controls.isExactMatch()) {
                exp = "'" + escapeToJcrLikeString(exp) + "'";
            } else {
                exp = "'%" + escapeToJcrLikeString(exp) + "%'";
            }

            queryBuilder.append(FORWARD_SLASH);
            addElementToSb(queryBuilder, JcrService.NODE_ARTIFACTORY_XML);
            addElementToSb(queryBuilder, xPath);
            queryBuilder.append(". [jcr:like(fn:lower-case(@jcr:xmlcharacters), ").append(exp.toLowerCase()).
                    append(")] order by jcr:score decending");
        }

        String query = queryBuilder.toString();

        QueryResult queryResult = getJcrService().executeXpathQuery(query);
        return queryResult;
    }

    /**
     * Appends the given String to the given StringBuilder, and adds a forward slash to the end
     *
     * @param sb      StringBuilder to add content to
     * @param element Content to add to StringBuilder
     */
    private void addElementToSb(StringBuilder sb, String element) {
        sb.append(element).append(FORWARD_SLASH);
    }
}
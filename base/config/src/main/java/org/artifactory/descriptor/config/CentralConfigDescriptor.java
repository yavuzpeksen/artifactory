/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.descriptor.config;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.addon.AddonSettings;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.cleanup.CleanupConfigDescriptor;
import org.artifactory.descriptor.external.ExternalProvidersDescriptor;
import org.artifactory.descriptor.gc.GcConfigDescriptor;
import org.artifactory.descriptor.index.IndexerDescriptor;
import org.artifactory.descriptor.mail.MailServerDescriptor;
import org.artifactory.descriptor.property.PropertySet;
import org.artifactory.descriptor.quota.QuotaConfigDescriptor;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Immutable interface for the central config.
 *
 * @author Yossi Shaul
 */
public interface CentralConfigDescriptor extends Descriptor {
    TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    Map<String, LocalRepoDescriptor> getLocalRepositoriesMap();

    Map<String, RemoteRepoDescriptor> getRemoteRepositoriesMap();

    Map<String, VirtualRepoDescriptor> getVirtualRepositoriesMap();

    List<ProxyDescriptor> getProxies();

    String getDateFormat();

    int getFileUploadMaxSizeMb();

    List<BackupDescriptor> getBackups();

    IndexerDescriptor getIndexer();

    String getServerName();

    @Nonnull
    SecurityDescriptor getSecurity();

    /**
     * @return true if the global offline mode is set.
     */
    boolean isOfflineMode();

    ProxyDescriptor getDefaultProxy();

    MailServerDescriptor getMailServer();

    List<PropertySet> getPropertySets();

    String getUrlBase();

    AddonSettings getAddons();

    String getLogo();

    String getFooter();

    List<RepoLayout> getRepoLayouts();

    RepoLayout getRepoLayout(String repoLayoutName);

    List<RemoteReplicationDescriptor> getRemoteReplications();

    List<LocalReplicationDescriptor> getLocalReplications();

    RemoteReplicationDescriptor getRemoteReplication(String replicatedRepoKey);

    LocalReplicationDescriptor getLocalReplication(String replicatedRepoKey);

    GcConfigDescriptor getGcConfig();

    /**
     * Normalizes the Artifactory's server URL set in the mail server config; falls back to the server URL set in the
     * general config if none is defined under the mail settings. For use within the contents of e-mails
     *
     * @return Artifactory server URL
     */
    String getServerUrlForEmail();

    CleanupConfigDescriptor getCleanupConfig();

    QuotaConfigDescriptor getQuotaConfig();

    Map<String, LocalReplicationDescriptor> getLocalReplicationsMap();

    ExternalProvidersDescriptor getExternalProvidersDescriptor();
}

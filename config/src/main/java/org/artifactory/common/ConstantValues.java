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

package org.artifactory.common;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author freds
 * @date Oct 10, 2008
 */
@SuppressWarnings({"EnumeratedConstantNamingConvention"})
public enum ConstantValues {
    test("runMode.test", FALSE), //Use and set only in specific itests - has serious performance implications
    qa("runMode.qa", FALSE),
    dev("runMode.dev", FALSE),
    artifactoryVersion("version", null),
    artifactoryRevision("revision", null),
    artifactoryTimestamp("timestamp", null),
    disabledAddons("addons.disabled", ""),
    springConfigDir("spring.configDir", null),
    jcrConfigDir("jcr.configDir", null),
    jcrFixConsistency("jcr.fixConsistency", FALSE),
    jcrAutoRemoveMissingBinaries("jcr.autoRemoveMissingBinaries", TRUE),
    jcrSessionPoolMaxSize("jcr.session.pool.maxSize", 30),
    versioningQueryIntervalSecs("versioningQueryIntervalSecs", Seconds.MINUTE * 12),
    logsViewRefreshRateSecs("logs.viewRefreshRateSecs", 10),
    locksTimeoutSecs("locks.timeoutSecs", 120),
    locksDebugTimeouts("locks.debugTimeouts", FALSE),
    taskCompletionLockTimeoutRetries("task.completionLockTimeoutRetries", 100),
    substituteRepoKeys("repo.key.subst.", null),
    repoCleanupIntervalHours("repo.cleanup.intervalHours", 1),
    repoConcurrentDownloadSyncTimeoutSecs("repo.concurrentDownloadSyncTimeoutSecs", Seconds.MINUTE * 15),
    artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts(
            "artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts", FALSE),
    fsItemCacheIdleTimeSecs("fsitem.cache.idleTimeSecs", Seconds.MINUTE * 20),
    searchMaxResults("search.maxResults", 500),
    searchUserQueryLimit("search.userQueryLimit", 1000),
    searchMaxFragments("search.content.maxFragments", 500),
    searchMaxFragmentsSize("search.content.maxFragmentsSize", 5000),
    searchArchiveMinQueryLength("search.archive.minQueryLength", 3),
    searchForceArchiveIndexing("search.content.forceArchiveIndexing", FALSE),
    searchPatternTimeoutSecs("search.pattern.timeoutSecs", 30),
    gcIntervalSecs("gc.intervalSecs", Seconds.HOUR * 6),
    gcBatchDeleteMaxSize("gc.batchDeleteMaxSize", 30),
    gcSleepBetweenNodesMillis("gc.sleepBetweenNodesMillis", 20),
    gcScanStartSleepingThresholdMillis("gc.scanStartSleepingThresholdMillis", 20000),
    gcScanSleepBetweenIterationsMillis("gc.scanSleepBetweenIterationsMillis", 200),
    trafficCollectionActive("traffic.collectionActive", FALSE),
    trafficCollectionIntervalSecs("traffic.collectionIntervalSecs", Seconds.MINUTE),
    trafficEntriesRetentionSecs("traffic.trafficEntriesRetentionSecs", Seconds.HOUR * 2),
    securityAuthenticationCacheIdleTimeSecs("security.authentication.cache.idleTimeSecs", Seconds.MINUTE * 5),
    userLastAccessUpdatesResolutionSecs("security.userLastAccessUpdatesResolutionSecs", Seconds.MINUTE),
    mvnCentralHostPattern("mvn.central.hostPattern", ".maven.org"),
    mvnCentralIndexerMaxQueryIntervalSecs("mvn.central.indexerMaxQueryIntervalSecs", Seconds.DAY),
    applicationContextClass("applicationContextClass", null),
    xmlAdditionalMimeTypeExtensions("xmlAdditionalMimeTypeExtensions", null),
    buildMaxFoldersToScanForDeletionWarnings("build.maxFoldersToScanForDeletionWarnings", 2),
    missingBuildChecksumCacheIdeTimeSecs("build.checksum.cache.idleTimeSecs", Seconds.MINUTE * 5),
    artifactoryUpdatesRefreshIntervalSecs("updates.refreshIntervalSecs", Seconds.HOUR * 4),
    artifactoryUpdatesUrl("updates.url", "http://service.jfrog.org/artifactory/updates"),
    uiSyntaxColoringMaxTextSizeBytes("ui.syntaxColoringMaxTextSizeBytes", 512000),
    artifactoryLicenseDir("licenseDir", null);

    public static final String SYS_PROP_PREFIX = "artifactory.";

    private final String propertyName;
    private final String defValue;

    ConstantValues(String propertyName, Object defValue) {
        this.propertyName = SYS_PROP_PREFIX + propertyName;
        this.defValue = defValue == null ? null : defValue.toString();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDefValue() {
        return defValue;
    }

    public String getString() {
        return ArtifactoryHome.get().getArtifactoryProperties().getProperty(propertyName, defValue);
    }

    public int getInt() {
        return (int) getLong();
    }

    public long getLong() {
        return ArtifactoryHome.get().getArtifactoryProperties().getLongProperty(propertyName, defValue);
    }

    public boolean getBoolean() {
        return ArtifactoryHome.get().getArtifactoryProperties().getBooleanProperty(propertyName, defValue);
    }

    private static class Seconds {
        private static final int MINUTE = 60;
        private static final int HOUR = MINUTE * 60;
        private static final int DAY = HOUR * 24;

    }
}
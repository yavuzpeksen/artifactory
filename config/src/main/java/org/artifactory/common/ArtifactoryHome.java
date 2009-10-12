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

package org.artifactory.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.version.ArtifactoryVersionReader;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.ConfigVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author yoavl
 */
public class ArtifactoryHome {
    public static final String SYS_PROP = "artifactory.home";
    public static final String SERVLET_CTX_ATTR = "artifactory.home.obj";
    private static final String ENV_VAR = "ARTIFACTORY_HOME";

    public static final String ARTIFACTORY_CONFIG_FILE = "artifactory.config.xml";
    public static final String ARTIFACTORY_STARTUP_CONFIG_FILE = "artifactory.config.startup.xml";
    public static final String ARTIFACTORY_CONFIG_IMPORT_FILE = "artifactory.config.import.xml";
    public static final String ARTIFACTORY_CONFIG_BOOTSTRAP_FILE = "artifactory.config.bootstrap.xml";
    public static final String ARTIFACTORY_SYSTEM_PROPERTIES_FILE = "artifactory.system.properties";
    public static final String ARTIFACTORY_PROPERTIES_FILE = "artifactory.properties";
    public static final String ARTIFACTORY_JCR_FILE = "repo.xml";
    public static final String LOGBACK_CONFIG_FILE_NAME = "logback.xml";

    private CompoundVersionDetails runningVersion;
    private CompoundVersionDetails originalStorageVersion;

    private final File homeDir;
    private ArtifactorySystemProperties artifactorySystemProperties;

    private File etcDir;
    private File dataDir;
    private File jcrRootDir;
    private File logDir;
    private File backupDir;
    private File tmpDir;
    private File tmpUploadsDir;

    public ArtifactoryHome() {
        this(new SimpleLog() {
            public void log(String message) {
                System.out.println(message);
            }
        });
    }

    public ArtifactoryHome(SimpleLog logger) {
        String homeDirPath = findArtifactoryHome(logger);
        homeDir = new File(homeDirPath);
        create();
    }

    public ArtifactoryHome(File homeDir) {
        if (homeDir == null) {
            throw new IllegalArgumentException("Home dir path cannot be null");
        }
        this.homeDir = homeDir;
        create();
    }

    public File getHomeDir() {
        return homeDir;
    }

    /**
     * @return The current running version
     */
    public CompoundVersionDetails getRunningVersionDetails() {
        return runningVersion;
    }

    /**
     * @return Original config version used when artifactory started. May be null if same as running version.
     */
    public CompoundVersionDetails getOriginalVersionDetails() {
        return originalStorageVersion;
    }

    public boolean startedFromDifferentVersion() {
        return (originalStorageVersion != null) && (!originalStorageVersion.isCurrent());
    }

    public File getDataDir() {
        return dataDir;
    }

    public File getJcrRootDir() {
        return jcrRootDir;
    }

    public File getEtcDir() {
        return etcDir;
    }

    public File getLogDir() {
        return logDir;
    }

    public File getBackupDir() {
        return backupDir;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public File getTmpUploadsDir() {
        return tmpUploadsDir;
    }

    public File getOrCreateSubDir(String subDirName) throws IOException {
        return getOrCreateSubDir(getHomeDir(), subDirName);
    }

    private File getOrCreateSubDir(File parent, String subDirName) throws IOException {
        File subDir = new File(parent, subDirName);
        FileUtils.forceMkdir(subDir);
        return subDir;
    }

    private void create() {
        try {
            // Create or find all the needed subfolders
            etcDir = getOrCreateSubDir("etc");
            dataDir = getOrCreateSubDir("data");
            jcrRootDir = dataDir;
            logDir = getOrCreateSubDir("logs");
            backupDir = getOrCreateSubDir("backup");
            File jettyWorkDir = getOrCreateSubDir("work");
            tmpDir = getOrCreateSubDir(dataDir, "tmp");
            tmpUploadsDir = getOrCreateSubDir(tmpDir, "artifactory-uploads");

            //Manage the artifactory.system.properties file under etc dir
            initAndLoadSystemPropertyFile();

            //Check the write access to all directories that need it
            checkWritableDirectory(dataDir);
            checkWritableDirectory(jcrRootDir);
            checkWritableDirectory(logDir);
            checkWritableDirectory(backupDir);
            checkWritableDirectory(jettyWorkDir);
            checkWritableDirectory(tmpDir);
            checkWritableDirectory(tmpUploadsDir);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not initialize artifactory main directory due to: " + e.getMessage(), e);
        }
    }

    private String findArtifactoryHome(SimpleLog logger) {
        logger.log("Determining " + SYS_PROP + "...");
        logger.log("Looking for '-D" + SYS_PROP + "=<path>' vm parameter...");
        String home = System.getProperty(SYS_PROP);
        if (home == null) {
            logger.log("Could not find vm parameter.");
            //Try the environment var
            logger.log("Looking for " + ENV_VAR + " environment variable...");
            home = System.getenv(ENV_VAR);
            if (home == null) {
                logger.log("Could not find environment variable.");
                home = new File(System.getProperty("user.home", "."), ".artifactory").getAbsolutePath();
                logger.log("Defaulting to '" + home + "'...");
            } else {
                logger.log("Found environment variable value: " + home + ".");
            }
        } else {
            logger.log("Found vm parameter value: " + home + ".");
        }

        home = home.replace('\\', '/');
        logger.log("Using artifactory.home at '" + home + "'.");
        return home;
    }

    /**
     * Chacks the existence of the logback configuration file under the etc directory. If the file doesn't exist this
     * method will extract a default one from the war.
     */
    public File getLogbackConfig() {
        File etcDir = new File(getHomeDir(), "etc");
        File logbackFile = new File(etcDir, LOGBACK_CONFIG_FILE_NAME);
        if (!logbackFile.exists()) {
            try {
                //Copy from default
                URL configUrl = ArtifactoryHome.class.getResource("/META-INF/default/" + LOGBACK_CONFIG_FILE_NAME);
                FileUtils.copyURLToFile(configUrl, logbackFile);
            } catch (IOException e) {
                // we don't have the logger configuration - use System.err
                System.err.printf("Could not create default %s into %s", LOGBACK_CONFIG_FILE_NAME, logbackFile);
                e.printStackTrace();
            }
        }
        return logbackFile;
    }

    /**
     * Returns the content of the artifactory.config.import.xml file
     *
     * @return Content of artifactory.config.import.xml if exists, null if not
     */
    public String getImportConfigXml() {
        File importConfigFile = new File(etcDir, ARTIFACTORY_CONFIG_IMPORT_FILE);
        if (importConfigFile.exists()) {
            try {
                String configContent = FileUtils.readFileToString(importConfigFile);
                if (StringUtils.isNotBlank(configContent)) {
                    File bootstrapConfigFile = new File(etcDir, ARTIFACTORY_CONFIG_BOOTSTRAP_FILE);
                    org.artifactory.util.FileUtils.switchFiles(importConfigFile, bootstrapConfigFile);
                    return configContent;
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read data from '" + importConfigFile.getAbsolutePath() +
                        "' file due to: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public String getBootstrapConfigXml() {
        File oldLocalConfig = new File(etcDir, ARTIFACTORY_CONFIG_FILE);
        File newBootstrapConfig = new File(etcDir, ARTIFACTORY_CONFIG_BOOTSTRAP_FILE);
        String result;
        if (newBootstrapConfig.exists()) {
            try {
                result = FileUtils.readFileToString(newBootstrapConfig);
            } catch (IOException e) {
                throw new RuntimeException("Could not read data from '" + newBootstrapConfig.getAbsolutePath() +
                        "' file due to: " + e.getMessage(), e);
            }
        } else if (oldLocalConfig.exists()) {
            try {
                result = FileUtils.readFileToString(oldLocalConfig);
            } catch (IOException e) {
                throw new RuntimeException("Could not read data from '" + newBootstrapConfig.getAbsolutePath() +
                        "' file due to: " + e.getMessage(), e);
            }
            // The new file is the one I want at the end, so the old is new and the new is old :)
            org.artifactory.util.FileUtils.switchFiles(oldLocalConfig, newBootstrapConfig);
        } else {
            String resPath = "/META-INF/default/" + ARTIFACTORY_CONFIG_FILE;
            InputStream is = ArtifactoryHome.class.getResourceAsStream(resPath);
            if (is == null) {
                throw new RuntimeException("Could read the default configuration from classpath at " + resPath);
            }
            try {
                result = IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Could not read data from '" + resPath +
                        "' file due to: " + e.getMessage(), e);
            }
        }
        return result;
    }

    public ArtifactorySystemProperties getArtifactoryProperties() {
        return artifactorySystemProperties;
    }

    /**
     * Missing Closure ;-)
     */
    public interface SimpleLog {
        public void log(String message);
    }

    public File getArtifactoryPropertiesFile() {
        return new File(dataDir, ARTIFACTORY_PROPERTIES_FILE);
    }

    private URL getDefaultArtifactoryPropertiesUrl() {
        return ArtifactoryHome.class.getResource("/META-INF/" + ARTIFACTORY_PROPERTIES_FILE);
    }

    public void writeBundledArtifactoryProperties() {
        File artifactoryPropertiesFile = getArtifactoryPropertiesFile();
        //Copy the artifactory.properties file into the data folder
        try {
            //Copy from default
            FileUtils.copyURLToFile(getDefaultArtifactoryPropertiesUrl(), artifactoryPropertiesFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy " + ARTIFACTORY_PROPERTIES_FILE + " to " +
                    artifactoryPropertiesFile.getAbsolutePath(), e);
        }
    }

    private CompoundVersionDetails readRunningArtifactoryVersion() {
        InputStream inputStream = ArtifactoryHome.class.getResourceAsStream("/META-INF/" + ARTIFACTORY_PROPERTIES_FILE);
        CompoundVersionDetails details = ArtifactoryVersionReader.read(inputStream);
        //Sanity check
        if (!details.isCurrent()) {
            throw new IllegalStateException("Running version is not the current version.");
        }
        return details;
    }

    /**
     * Copy the system properties file and set its data as system properties
     */
    public void initAndLoadSystemPropertyFile() {
        // Expose the properties inside artfactory.properties and artfactory.system.properties
        // as system properties, availale to ArtifactoryConstants
        File systemPropertiesFile = new File(etcDir, ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
        if (!systemPropertiesFile.exists()) {
            try {
                //Copy from default
                URL url = ArtifactoryHome.class.getResource("/META-INF/default/" + ARTIFACTORY_SYSTEM_PROPERTIES_FILE);
                FileUtils.copyURLToFile(url, systemPropertiesFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not create a default " +
                        ARTIFACTORY_SYSTEM_PROPERTIES_FILE + " at " + systemPropertiesFile.getAbsolutePath(), e);
            }
        }

        runningVersion = readRunningArtifactoryVersion();
        File artifactoryPropertiesFile = getArtifactoryPropertiesFile();
        if (artifactoryPropertiesFile.exists()) {
            CompoundVersionDetails storedStorageVersion = ArtifactoryVersionReader.read(artifactoryPropertiesFile);
            //Store the original version - may need to activate converters based on it
            originalStorageVersion = storedStorageVersion;
            if (!runningVersion.equals(storedStorageVersion)) {
                // the version written in the jar and the version read from the data directory are different
                // make sure the version from the data directory is supported by the current deployed artifactory
                ConfigVersion actualConfigVersion =
                        ConfigVersion.findCompatibleVersion(storedStorageVersion.getVersion());
                //No compatible version -> conversion needed, but supported only from v4 onward
                if (!actualConfigVersion.isCurrent()) {
                    String msg = "The storage version for (" + storedStorageVersion.getVersion().getValue() + ") " +
                            "is not up-to-date with the currently deployed Artifactory (" +
                            runningVersion.getVersion().getValue() + ")";
                    if (!actualConfigVersion.isAutoUpdateCapable()) {
                        //Cannot convert
                        msg += ": no automatic conversion is possible. Exiting now...";
                        throw new IllegalStateException(msg);
                    }
                }
            }
        } else {
            writeBundledArtifactoryProperties();
        }
        artifactorySystemProperties = new ArtifactorySystemProperties();
        artifactorySystemProperties.loadArtifactorySystemProperties(systemPropertiesFile, artifactoryPropertiesFile);
    }

    private static void checkWritableDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            throw new IllegalArgumentException("Directory '" + dir.getAbsolutePath() + "' is not writable!");
        }
    }
}
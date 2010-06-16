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

package org.artifactory.security.ldap;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LdapGroupAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.PathUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for the LDAP bind authenticator. Used to authenticate users against ldap and as a factory for the security
 * context and actual authenticator.
 *
 * @author Yossi Shaul
 */
@Reloadable(beanClass = InternalLdapAuthenticator.class, initAfter = InternalCentralConfigService.class)
@Component
public class ArtifactoryLdapAuthenticator implements InternalLdapAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryLdapAuthenticator.class);

    private static final String NO_LDAP_SERVICE_CONFIGURED = "No LDAP service configured";
    public static final String LDAP_SERVICE_MISCONFIGURED = "LDAP service misconfigured";

    @Autowired
    private CentralConfigService centralConfig;

    private Map<String, BindAuthenticator> authenticators;

    public void init() {
        try {
            authenticators = createBindAuthenticators();
            if (authenticators.isEmpty()) {
                authenticators = null;
            }
        } catch (Exception e) {
            log.error("Failed to create LDAP authenticator. Please verify and fix your LDAP settings.", e);
        }
        if (authenticators == null) {
            log.debug("LDAP service is disabled");
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        authenticators = null;
        init();
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public Map<String, BindAuthenticator> getAuthenticators() {
        return authenticators;
    }

    private Map<String, BindAuthenticator> createBindAuthenticators() {
        Map<String, BindAuthenticator> result = new LinkedHashMap<String, BindAuthenticator>();
        LdapGroupAddon groupAddon = ContextHelper.get().beanForType(AddonsManager.class).addonByType(
                LdapGroupAddon.class);
        List<LdapSetting> ldapSettings = groupAddon.getEnabledLdapSettings();
        for (LdapSetting ldapSetting : ldapSettings) {
            LdapContextSource contextSource = createSecurityContext(ldapSetting);
            ArtifactoryBindAuthenticator bindAuthenticator =
                    new ArtifactoryBindAuthenticator(contextSource, ldapSetting);
            result.put(ldapSetting.getKey(), bindAuthenticator);
        }
        return result;
    }

    static LdapContextSource createSecurityContext(LdapSetting ldapSetting) {
        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource(ldapSetting.getLdapUrl());
        // set default connection timeout to 5 seconds
        HashMap<String, String> env = new HashMap<String, String>();
        //TODO: [by yl] check how timeout is set on other jdks
        env.put("com.sun.jndi.ldap.connect.timeout", "10000");
        contextSource.setBaseEnvironmentProperties(env);
        SearchPattern searchPattern = ldapSetting.getSearch();
        if (searchPattern != null) {
            if (PathUtils.hasText(searchPattern.getManagerDn())) {
                contextSource.setUserDn(searchPattern.getManagerDn());
                contextSource.setPassword(searchPattern.getManagerPassword());
            } else {
                contextSource.setAnonymousReadOnly(true);
            }
        }

        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return contextSource;
    }

    public DirContextOperations authenticate(Authentication authentication) {
        //Spring expects an exception on failed authentication
        if (authenticators != null && centralConfig.getDescriptor().getSecurity().isLdapEnabled()) {
            RuntimeException authenticationException = null;
            for (BindAuthenticator authenticator : authenticators.values()) {
                DirContextOperations user = null;
                try {
                    user = authenticator.authenticate(authentication);
                } catch (RuntimeException e) {
                    authenticationException = e;
                }
                if (user != null) {
                    return user;
                }
            }
            if (authenticationException != null) {
                throw authenticationException;
            }
            throw new AuthenticationServiceException(LDAP_SERVICE_MISCONFIGURED);
        } else {
            throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);
        }
    }
}

package org.ovirt.engine.core.utils.ipa;

import static org.ovirt.engine.core.utils.kerberos.InstallerConstants.ERROR_PREFIX;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.ovirt.engine.core.ldap.LdapProviderType;
import org.ovirt.engine.core.utils.CLIParser;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

public class SimpleAuthenticationCheck {
    private static String INVALID_CREDENTIALS_ERROR_CODE = "49";
    private LdapProviderType ldapProviderType;

    public enum Arguments {
        domain,
        user,
        password,
        ldapProviderType
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out
                .println("SimpleAuthenticationCheck: -domain=<domains> -user=<user> -password=<password> -ldapProviderType=<ldapProviderType>");
    }

    private String getLdapUrl(String ldapServer) {
        return "ldap://" + ldapServer;
    }

    private boolean validate(CLIParser parser) {
        Arguments[] argsToValidate =
                { Arguments.domain, Arguments.user, Arguments.password };
        for (Arguments argument : argsToValidate) {
            if (!parser.hasArg(argument.name())) {
                System.out.println(argument.name() + " is required");
                return false;
            }
        }
        if (LdapProviderType.valueOf(parser.getArg(Arguments.ldapProviderType.name())) == null) {
            System.out.println(Arguments.ldapProviderType.name() + " must be one of ");
            for (LdapProviderType type : LdapProviderType.values()) {
                System.out.println(type.name());
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SimpleAuthenticationCheck util = new SimpleAuthenticationCheck();
        CLIParser parser = new CLIParser(args);
        if (!util.validate(parser)) {
            util.printUsage();
            System.exit(ReturnStatus.INPUT_VALIDATION_FAILURE.ordinal());
        }
        String username = parser.getArg(Arguments.user.name());
        String password = parser.getArg(Arguments.password.name());
        String domain = parser.getArg(Arguments.domain.name());
        LdapProviderType ldapProviderType = LdapProviderType.valueOf(parser.getArg(Arguments.ldapProviderType.name()));
        StringBuffer userGuid = new StringBuffer();

        ReturnStatus status =
                util.printUserGuid(domain, username, password, "localhost:389", userGuid, ldapProviderType);

        System.exit(status.ordinal());
    }

    public ReturnStatus printUserGuid(String domain,
            String username,
            String password,
            String ldapServerUrl,
            StringBuffer userGuid, LdapProviderType ldapProviderType) {

        LdapContextSource contextSource = getContextSource(domain, ldapProviderType, username, password, ldapServerUrl);
        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            System.err.println(ERROR_PREFIX + "Failed setting LDAP context for domain " + domain);
            return ReturnStatus.LDAP_CONTEXT_FAILURE;
        }

        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        String query = "";
        ContextMapper contextMapper;

        if (ldapProviderType.equals(LdapProviderType.ipa)) {
            query = "(&(objectClass=posixAccount)(objectClass=krbPrincipalAux)(uid=" + username + "))";
            contextMapper = new IPAUserContextMapper();
            // AD
        } else if (ldapProviderType.equals(LdapProviderType.activeDirectory)) {
            contextMapper = new ADUserContextMapper();
            // ITDS
        } else if (ldapProviderType.equals(LdapProviderType.itds)) {
            query = "(&(objectClass=person)(uid=" + username + "))";
            contextMapper = new ITDSUserContextMapper();
            // RHDS
        } else {
            query = "(&(objectClass=person)(uid=" + username + "))";
            contextMapper = new RHDSUserContextMapper();
        }

        try {
            List searchResult =
                    ldapTemplate.search("", query, contextMapper);
            if (searchResult == null) {
                System.err.println(ERROR_PREFIX + "Cannot query user " + username + " from domain " + domain);
                return ReturnStatus.CANNOT_QUERY_USER;
            } else {
                userGuid.append((String) searchResult.get(0));
                System.out.println("User guid is: " + userGuid.toString());
            }
        } catch (org.springframework.ldap.AuthenticationException authEx) {
            return authenticationReturnStatus(authEx, username, domain);
        } catch (Exception ex) {
            System.err.println(ERROR_PREFIX + "Cannot query user " + username + " from domain " + domain
                    + ", details: " + ex.getMessage());
            return ReturnStatus.CANNOT_QUERY_USER;
        }

        return ReturnStatus.OK;
    }

    /***
     * Returns the ReturnStatus according to the given AuthenticationException. Either INVALID_CREDENTIALS if this is
     * the case, or the general CANNOT_AUTHENTICATE_USER otherwise.
     *
     * @param authEx
     */
    private ReturnStatus authenticationReturnStatus(AuthenticationException authEx, String userName, String domain) {
        ReturnStatus returnStatus = ReturnStatus.CANNOT_AUTHENTICATE_USER;
        String authExMessage = authEx.getMessage();

        // Using contains() since the AuthenticationException does not have an error code property
        if (authExMessage != null && authExMessage.contains(INVALID_CREDENTIALS_ERROR_CODE)) {
            System.err.println(ERROR_PREFIX + "Invalid credentials for " + userName + " and domain " + domain
                    + ", details: " + authEx.getMessage());
            returnStatus = ReturnStatus.INVALID_CREDENTIALS;
        } else {
            System.err.println(ERROR_PREFIX + "Cannot authenticate user " + userName + " to domain " + domain
                    + ", details: " + authEx.getMessage());
        }
        return returnStatus;
    }

    private static String domainToDN(String domain) {

        String returnValue = "dc=" + domain.replaceAll("\\.", ",dc=");

        return returnValue;
    }

    private LdapContextSource getContextSource(String domain,
            LdapProviderType ldapProviderType,
            String username,
            String password,
            String ldapServer) {
        LdapContextSource context = new LdapContextSource();

        String ldapBaseDn = domainToDN(domain);
        StringBuilder ldapUserDn = new StringBuilder();

        if (ldapProviderType.equals(LdapProviderType.ipa)) {
            ldapUserDn.append("uid=").append(username).append(",cn=Users").append(",cn=Accounts,");
        } else if (ldapProviderType.equals(LdapProviderType.rhds)) {
            ldapUserDn.append("uid=").append(username).append(",ou=People");
        } else if (ldapProviderType.equals(LdapProviderType.itds)) {
            ldapUserDn.append("uid=").append(username);
        } else {
            ldapUserDn.append("CN=").append(username).append(",CN=Users,");
        }

        ldapUserDn.append(ldapBaseDn);

        context.setUrl(getLdapUrl(ldapServer));
        if (!ldapProviderType.equals(LdapProviderType.itds)) {
            context.setBase(ldapBaseDn);
        } else {
            context.setAnonymousReadOnly(true);
        }
        context.setUserDn(ldapUserDn.toString());
        context.setPassword(password);
        context.setReferral("follow");
        Map<String, String> baseEnvironmentProperties = new HashMap<String, String>();
        // objectGUID - for AD
        baseEnvironmentProperties.put("java.naming.ldap.attributes.binary", "objectGUID");
        context.setBaseEnvironmentProperties(baseEnvironmentProperties);
        return context;
    }

    private static DirContext getDirContext(String ldapServer) throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.SECURITY_AUTHENTICATION, "SIMPLE");
        env.put(Context.SECURITY_PRINCIPAL, "");
        env.put(Context.SECURITY_CREDENTIALS, "");
        env.put(Context.REFERRAL, "follow");

        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapServer.toString());

        return new InitialDirContext(env);
    }

}

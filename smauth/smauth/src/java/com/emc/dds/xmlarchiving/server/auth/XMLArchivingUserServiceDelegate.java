package com.emc.dds.xmlarchiving.server.auth;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.emc.documentum.xml.dds.exception.DDSException;
import com.emc.documentum.xml.dds.logging.LogCenter;
import com.emc.documentum.xml.dds.persistence.exception.UserNotFoundException;
import com.emc.documentum.xml.dds.service.exception.ServiceNotAvailableException;
import com.emc.documentum.xml.dds.user.User;
import com.emc.documentum.xml.dds.user.exception.BadPasswordException;
import com.emc.documentum.xml.dds.user.impl.UserServiceImpl;

public class XMLArchivingUserServiceDelegate extends UserServiceImpl {

	private Hashtable<String, XMLArchivingUserImpl> users = null;

	public XMLArchivingUserServiceDelegate() {
		super();
		users = new Hashtable<String, XMLArchivingUserImpl>();
	}

	/**
	 * @param id
	 *            user id
	 * @param password
	 *            user password
	 * @param roleList
	 *            list of roles the user has
	 * @param groupnNamePrefix
	 *            group name we should look for
	 */
	public void loginSiteMinderUser(String id, String password, String roleNames, String appRoleLists) {
		//roleNames= "abc=yes;xyz=;approle=;..."
		
		boolean hasRole = false;
		String roleName = null;
		for(String role : appRoleLists.split(";")){
			if(roleNames.contains(role+"=yes")){
				hasRole = true;
				roleName = role;
				break;
			}
		}
		if (!hasRole) {
			LogCenter.log("Could not find role associated with the application");
			return;
		}
		XMLArchivingUserImpl xmlArchivingUser = null;
		LogCenter.log("Creating XMLArchivingUserImpl instance with role = " + roleName + ", id = " + id);
		xmlArchivingUser = new XMLArchivingUserImpl(roleName, id);
		users.put(id, xmlArchivingUser);
	}

	@SuppressWarnings("unchecked")
	private String getLDAPRole(NamingEnumeration<SearchResult> results, String rolePrefix) throws NamingException {
		String groupNameInAD = null;
		String roleName = null;
		SearchResult result = null;
		boolean foundRole = false;

		// now extract out the group name that has our role prefix in it
		// first, upcase the role
		rolePrefix = rolePrefix.toUpperCase();

		// find the group list
		while (results.hasMoreElements() && !foundRole) {
			result = results.nextElement();

			// remember to uppercase all the attributes we check against
			Attributes attrs = result.getAttributes();
			Attribute attr = null;
			NamingEnumeration<?> attrEnum = attrs.getAll();
			NamingEnumeration<String> groupNames = null;
			while (attrEnum.hasMoreElements() && !foundRole) {
				attr = (Attribute) attrEnum.nextElement();
				if (attr.getID().equals("memberOf")) {
					LogCenter.log("Found memberOf attribute, now looking for role name");
					groupNames = (NamingEnumeration<String>) attr.getAll();
					while (groupNames.hasMoreElements() && !foundRole) {
						// sample role list CN=AD Content Technology LoB - CM
						// Practitioner (Employees
						// ONLY),CN=Users,DC=flatironssolutions,DC=com

						groupNameInAD = groupNames.nextElement();
						if (groupNameInAD.toUpperCase().contains(rolePrefix)) {
							String[] roleDomainSegments = groupNameInAD.split(",");

							// our role name should always be the first CN in
							// the list, but just to be safe, look at all of the
							// CN strings and double check that
							// our role prefix is in the string
							for (String domainName : roleDomainSegments) {
								if (domainName.contains("CN=") && domainName.toUpperCase().contains(rolePrefix)) {
									String cnPrefix = "CN=";
									// extract just the role name
									int groupNameOffset = domainName.indexOf(cnPrefix) + cnPrefix.length();
									roleName = domainName.substring(groupNameOffset, domainName.length());
									foundRole = true;
									LogCenter.log("Found roleName " + roleName + " for rolePrefix " + rolePrefix);
									break;

								}
							}
						}
					}

				}
			}
			if (!foundRole) {
				LogCenter.log("Warning! Could not find a role with prefix " + rolePrefix);
			}
			break;
		}
		return roleName;
	}

	boolean loginLDAPUser(String user, String password, String groupPrefixName) {
		boolean loginOK = false;
		String domainUser = user;
		String protocol = getLDAPProtocol();

		// Set up the environment for creating the initial context
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

		// look to see if we're using LDAP or LDAPS URL.

		// get the port #, in case we need to run on a non-standard port
		// String portNumber = getLDAPPort();
		String ldapURL = null;

		// build the URL, including the port # if specified in our config file.
		env.put(Context.PROVIDER_URL, protocol + "://" + getLDAPHost());

		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		env.put(Context.SECURITY_CREDENTIALS, password);
		String searchName = getSearchContext();
		// "dc=flatironssolutions,dc=com";
		String adUser = user;

		// strip off a domain name if one was specified by the user on input
		if (user.indexOf("\\") > -1) {
			int slashOffset = user.indexOf("\\");
			adUser = user.substring(slashOffset + 1, user.length());
		} else {
			// we probably have defined the domain name in our property file, so
			// look there for it as well
			String domainName = null;
			domainName = getDomainName();
			if (domainName != null) {
				domainUser = domainName + "\\" + user;
			}
		}
		env.put(Context.SECURITY_PRINCIPAL, domainUser);
		String filter = "(sAMAccountName=" + adUser + ")";
		String ATTRIBUTE_LIST[] = { "sAMAccountName", "memberOf" };
		SearchControls constraints = new SearchControls();
		constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

		constraints.setReturningAttributes(ATTRIBUTE_LIST);

		// Create the initial context
		try {
			LogCenter.log("looking up LDAP information for user " + domainUser + ", aduser " + adUser
					+ ", attribute list = \"sAMAccountName, memberOf\"" + ", searchName = " + searchName);
			DirContext ctx = new InitialDirContext(env);
			LogCenter.log("context lookup succeeded for user " + user + ", now looking for group info");
			NamingEnumeration<SearchResult> results = ctx.search(searchName, filter, constraints);
			String roleName = getLDAPRole(results, groupPrefixName);
			if (roleName == null) {
				LogCenter.log("could not find role for user " + user + ", using default role of '1'");
				roleName = "1";
			}
			// get the role id and create a login user object
			XMLArchivingUserImpl xmlArchivingUser = null;
			LogCenter
					.log("Creating XMLArchivingUserImpl instance after LDAP authentication and group lookup with role = "
							+ roleName + ", id = " + user);
			xmlArchivingUser = new XMLArchivingUserImpl(roleName, user);
			users.put(user, xmlArchivingUser);
			loginOK = true;

		} catch (NamingException e) {
			LogCenter.log("Error looking up ldap information, detailed message = " + e.getMessage());
		}

		return loginOK;
	}

	private String getDomainName() {
		String ldapDomainName = null;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.domainName")) {
				ldapDomainName = rb.getString(key);
				break;
			}
		}
		return ldapDomainName;
	}

	private String getSearchContext() {
		String ldapSearchContext = null;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.ldapContext")) {
				ldapSearchContext = rb.getString(key);
				break;
			}
		}
		return ldapSearchContext;
	}

	@Override
	public boolean loginUser(String id, String password)
			throws UserNotFoundException, BadPasswordException, ServiceNotAvailableException {
		boolean result = true;
		try {
			XMLArchivingUserImpl xmlArchivingUser = null;
			// ok, try for DDS user authentication
			result = super.loginUser(id, password);
			// and get the DDS user we just created so we can return it
			System.out.println("res" + result);
			if (result) {
				xmlArchivingUser = new XMLArchivingUserImpl(super.getUser(id));
				users.put(id, xmlArchivingUser);
			}
		} catch (UserNotFoundException de) {
			result = false;
		}
		LogCenter.log("In XMLArchivingUserServiceDelegate.login(), user = " + id + " login result = " + result);
		return result;
	}

	private String getLDAPHost() {
		String ldapPrincipal = null;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.ldapHost")) {
				ldapPrincipal = rb.getString(key);
				break;
			}
		}
		return ldapPrincipal;
	}

	private String getLDAPProtocol() {
		String protocolName = null;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.ldapProtocol")) {
				protocolName = rb.getString(key);
				break;
			}
		}
		return protocolName;
	}

	boolean isLDAPEnabled() {

		boolean isLDAP = false;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.useLDAP")) {
				isLDAP = rb.getString(key).toLowerCase().equals("true") ? true : false;
				break;
			}
		}
		return isLDAP;

	}

	@Override
	public boolean logoutUser(String id) throws UserNotFoundException, ServiceNotAvailableException {
		boolean result = true;
		try {
			result = super.logoutUser(id);

			// also remove the user from our map.
			users.remove(id);
		} catch (DDSException de) {
			result = false;
		}
		System.err.println("User: " + id + " logging out: " + result);
		return result;
	}

	@Override
	public boolean isLoggedIn(String sessionID) {
		boolean isLoggedIn = false;
		if (null != sessionID && sessionID.length() > 0) {
			isLoggedIn = true;
		}
		return isLoggedIn;
	}

	@Override
	public User getUser(final String id) {
		return users.get(id);
	}

}

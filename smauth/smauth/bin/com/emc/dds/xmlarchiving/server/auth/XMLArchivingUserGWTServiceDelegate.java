package com.emc.dds.xmlarchiving.server.auth;

import java.util.Enumeration;
import java.util.ResourceBundle;

import javax.servlet.http.HttpSession;

import com.emc.documentum.xml.dds.DDS;
import com.emc.documentum.xml.dds.application.Application;
import com.emc.documentum.xml.dds.gwt.server.application.UserServiceImpl;
import com.emc.documentum.xml.dds.logging.LogCenter;
import com.emc.documentum.xml.dds.service.DDSServiceType;
import com.emc.documentum.xml.dds.service.exception.ServiceNotAvailableException;
import com.emc.documentum.xml.dds.user.TokenService;
import com.emc.documentum.xml.dds.user.UserToken;

///
/**
 * @author Malik
 *
 */
public class XMLArchivingUserGWTServiceDelegate extends UserServiceImpl {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public XMLArchivingUserGWTServiceDelegate() {
		super();
	}

	private boolean isSiteMinderAuthenticated() {
		boolean isSiteMinder = false;

		// two steps here - one is to see if we have the property set to try for
		// SiteMinder integration. The other is to actually get the HTTP header
		String useSiteMinderFlag = null;

		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.useSiteMinder")) {
				useSiteMinderFlag = rb.getString(key);
				break;
			}
		}
		if (useSiteMinderFlag != null && useSiteMinderFlag.toUpperCase().contains("TRUE")) {
			isSiteMinder = getSiteMinderUserID() == null ? false : true;
			LogCenter.log("isSiteMinderAuthenticated = " + isSiteMinder);
		}
		return isSiteMinder;
	}

	private String getUserID(String headerName) {
		String headerValue = ""; 
		/*
		//hardcoding
		if(headerName.equalsIgnoreCase("HTTP_mail"))
			headerValue = "girida1@stgexcng.medtronic.com";
		*/	
		headerValue = this.getThreadLocalRequest().getHeader(headerName);
		headerValue = headerValue.contains("@")?headerValue.split("@")[0]:headerValue;
		LogCenter.log("header = " + headerName + ", header value = " + headerValue);
		return headerValue;
	}

	private String getSiteMinderUserID() {
		LogCenter.log("In XMLArchivingUserGWTServiceDelegate.getSiteMinderUserID, looking for session header HTTP_mail");
		String smUserID = getUserID("HTTP_mail");
		if (null == smUserID) {
			LogCenter.log("Did not find a SiteMinder user id in this HTTP session");
		} else {
			LogCenter.log("User has been authenticated through SiteMinder as user " + smUserID);
		}
		return smUserID;
	}

	private String getRolesHeaderString() {
		String appRoles = getGroupNames();
		StringBuffer sb = new StringBuffer();
		for(String role:appRoles.split(";")){
			sb.append(role).append("=").append(getRoleDetailFromHeader(role)).append(";");
		}
		return sb.toString();
	}

	private String getRoleDetailFromHeader(String headerName) {
		String headerValue = ""; 
		/*
		//hardcoding
		if(headerName.equalsIgnoreCase("HTTP_Archive-Admin-GS"))
			headerValue = "yes";
		*/
		headerValue = this.getThreadLocalRequest().getHeader(headerName);
		return headerValue;
	}
	
	@Override
	protected String getSession() {
		if (isSiteMinderAuthenticated()) {
			HttpSession session = this.getThreadLocalRequest().getSession();
			session.setMaxInactiveInterval(-1);
		}
		return super.getSession();
	}

	private String getGroupNames() {
		String groupNames = null;
		final ResourceBundle rb = ResourceBundle.getBundle("smauth");
		for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements();) {
			final String key = (String) keys.nextElement();
			if (key.equals("com.uhg.xmlarchiving.smauth.groupNames")) {
				groupNames = rb.getString(key);
				break;
			}
		}
		return groupNames;
	}

	@Override
	public boolean login(String id, String password) {
		boolean result = false;
		String siteMinderUser = null;
		LogCenter.log(" group names string = " + getGroupNames());
		LogCenter.log("In XMLArchivingUserGWTServiceDelegate.login()");
		XMLArchivingUserServiceDelegate ddsUserService = (XMLArchivingUserServiceDelegate) DDS.getApplication().getService("UserService");
		if (isSiteMinderAuthenticated()) {
			siteMinderUser = getSiteMinderUserID();
			ddsUserService.loginSiteMinderUser(siteMinderUser, password, getRolesHeaderString(), getGroupNames());
			initTokenService(siteMinderUser, "SiteMinder");
			LogCenter.log("User " + siteMinderUser + " was previously authenticated in SiteMinder.");
			result = true;
		} else {
			if (id != null && id.length() > 0) {
				LogCenter.log("User " + id + " was not authenticated in SiteMinder.");
				if (ddsUserService.isLDAPEnabled()) {
					result = ddsUserService.loginLDAPUser(id, password, getGroupNames());
					if (result) {
						initTokenService(id, "LDAP");
					}
				} else {
					LogCenter.log("Trying xDB login for user " + id);
					result = super.login(id, password);
				}
			}
		}
		String loginUser = id;
		if (isSiteMinderAuthenticated()) {
			loginUser = siteMinderUser;
		} else {
			if (id == null || id.length() == 0) {
				LogCenter.log("User id was empty and SiteMinder integration is not enabled so login request failed.");
			} else {
				LogCenter.log("User: " + loginUser + " logging in: " + result);
			}
		}
		return result;
	}

	private void initTokenService(String id, String authProtocol) {
		XMLArchivingUserServiceDelegate ddsUserService = (XMLArchivingUserServiceDelegate) DDS.getApplication().getService("UserService");
		XMLArchivingUserImpl xmlArchivingUser = (XMLArchivingUserImpl) ddsUserService.getUser(id);
		boolean foundUser = false;
		if (xmlArchivingUser != null) {
			LogCenter.log("Found XMLArchivingUser " + xmlArchivingUser.getId());
			foundUser = true;
		} else {
			LogCenter.log("Could not retrieve XMLArchivingUserImpl for " + authProtocol + " user " + id);
		}
		if (foundUser) {
			Application application = DDS.getApplication();
			TokenService tokenManager = (TokenService) application.getServiceManager().getService(DDSServiceType.TOKEN);
			UserToken userToken = null;
			try {
				userToken = tokenManager.createToken(application, xmlArchivingUser);
			} catch (ServiceNotAvailableException e) {
				e.printStackTrace();
			}
			String sessionId = getSession();
			LogCenter.log("found session " + sessionId);
			LogCenter.log("storing token " + userToken);
			tokenManager.getTokenToTokenMapping().storeTokenMapping(sessionId, userToken);
		}
	}
}

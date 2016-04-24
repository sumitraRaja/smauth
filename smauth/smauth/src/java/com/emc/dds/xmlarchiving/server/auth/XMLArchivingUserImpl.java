package com.emc.dds.xmlarchiving.server.auth;

import com.emc.documentum.xml.dds.DDS;
import com.emc.documentum.xml.dds.application.StoreManager;
import com.emc.documentum.xml.dds.exception.DDSException;
import com.emc.documentum.xml.dds.logging.LogCenter;
import com.emc.documentum.xml.dds.operation.exception.BeginFailedException;
import com.emc.documentum.xml.dds.operation.exception.CommitFailedException;
import com.emc.documentum.xml.dds.persistence.Session;
import com.emc.documentum.xml.dds.persistence.Store;
import com.emc.documentum.xml.dds.persistence.StoreUser;
import com.emc.documentum.xml.dds.persistence.exception.StoreSpecificException;
import com.emc.documentum.xml.dds.user.User;
import com.emc.documentum.xml.dds.xquery.XQueryExecutor;
import com.emc.documentum.xml.dds.xquery.XQueryResultHandler;
import com.xhive.query.interfaces.XhiveXQueryValueIf;
import com.xhive.util.interfaces.IterableIterator;

/**
 * @author Administrator
 *
 */
	public class XMLArchivingUserImpl  implements XMLArchivingUser 
	{
	 
	private User ddsUser = null;
	private boolean useDDSAuth = false;
	private String roleName = null;
	private String id;

	/**
	 * creates a new user. Also looks up what role the user is in and stores the role id in this object.
	 * @param theUser a DDS user that is stored when we're using DDS for authentication
	 */
	public XMLArchivingUserImpl(User theUser)
	{
		boolean wasStarted = true;
		ddsUser = theUser;
		id = ddsUser.getId();
		String appName = DDS.getApplication().getName();
		String roleQuery = "<result>\n" + " {\n" + "   let $userRoleId := doc('/APPLICATIONS/" + appName
			      + "/users/" + theUser.getId() +"')/com.emc.dds.xmlarchiving.client.rpc.LDMUser/roleId/text()\n return \n " 
			      + "doc('/APPLICATIONS/" + appName + "/roles')[/role/id = $userRoleId] }\n" + "</result>";

		useDDSAuth = true;


        Store store = DDS.getApplication().getMainStore();
        Session session = null;
        try {
			session = store.getSession(store.getDefaultStoreUser(), true);
			if (!session.isOpen())
			{
				session.begin();
				wasStarted = false;
			}
		} catch (StoreSpecificException e) {
			LogCenter.log("failed to create store session, detailed err = " + e.getMessage());
		} catch (BeginFailedException e) {
			LogCenter.log("failed to create store session, detailed err = " + e.getMessage());		}
        
        XQueryExecutor xqueryExec = store.getXQueryExecutor();
        try {
        	XhiveXQueryValueIf result = null;
        	@SuppressWarnings("unchecked")
			IterableIterator<? extends XhiveXQueryValueIf> results = (IterableIterator<? extends XhiveXQueryValueIf>)xqueryExec.execute(session, roleQuery, null);
			while (results.hasNext())
			{
				result = results.next();
				roleName = result.asString();
				LogCenter.log("Setting XMLArchivingUserImpl object with role " + roleName + " from xDB role definition");
				break;
			}
		} catch (DDSException e) {
			
		}
        finally
        {
        	try {
        		if (!wasStarted)
        		{
        			session.commit();
        		}
			} catch (CommitFailedException e) 
			{
				LogCenter.log("failed to commit store session, detailed err = " + e.getMessage());			
        	}
        }
	}
	/**
	 * 
	 */
	public XMLArchivingUserImpl(String roleID, String theId)
	{
		// store the role list for this user
		roleName = roleID;
		id = theId;
	}
	
     public void addStoreUser(String storeAlias, StoreUser storeUser) {
    	 throw new UnsupportedOperationException();
     }

     
     public boolean checkPassword(String input) {
    	 throw new UnsupportedOperationException();
     }

     
     public String getId() {
       return id;
     }


     public String getRoleId() {
       return roleName;
     }


     public StoreUser getStoreUser(String storeAlias) {
    	 StoreUser ddsStoreUser = null;
    	 if (useDDSAuth)
    	 {
    		 ddsStoreUser = ddsUser.getStoreUser(storeAlias);  
    	 }
    	 else
    	 {
    		 // use the default store user
             StoreManager storeManager = DDS.getApplication().getStoreManager();
             Store store = storeManager.getStore(storeAlias);
             ddsStoreUser = store.getDefaultStoreUser();
    	 }
    	 return ddsStoreUser;
     }


     public boolean isAdministrator() {
    	 throw new UnsupportedOperationException();
     }


     public void removeStoreUser(String storeAlias) {
    	 throw new UnsupportedOperationException();
     }


     public void setAdministrator(boolean administrator) {
    	 throw new UnsupportedOperationException();
     }


     public void setPassword(String password) {
    	 throw new UnsupportedOperationException();
     }
     

}

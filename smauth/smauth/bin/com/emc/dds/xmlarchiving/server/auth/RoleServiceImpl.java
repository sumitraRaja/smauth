/***************************************************************************************************
 * Copyright (c) 2012 EMC Corporation All Rights Reserved
 **************************************************************************************************/
package com.emc.dds.xmlarchiving.server.auth;


import com.emc.dds.xmlarchiving.client.rpc.auth.RoleService;
import com.emc.documentum.xml.dds.gwt.server.AbstractDDSService;
import com.emc.documentum.xml.dds.operation.exception.OperationFailedException;
import com.emc.documentum.xml.dds.user.User;



/**
 * Implementation of the GWT {@link RoleService}.
 */

public class RoleServiceImpl extends AbstractDDSService implements RoleService {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public String getRoleId() throws OperationFailedException {
	
    User user = this.getUserFromRequest();
    XMLArchivingUser xaUser = (XMLArchivingUser)user;
    String role = (xaUser.getRoleId());
    return role;
  }

public String getUserId() throws OperationFailedException {
	User user = this.getUserFromRequest();
	XMLArchivingUser xaUser = (XMLArchivingUser)user;
	String userID = (xaUser.getId());
	return userID;
}



}

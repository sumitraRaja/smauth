package com.emc.dds.xmlarchiving.client.rpc.auth;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("RoleService")
public interface RoleService extends RemoteService {

	String getRoleId() throws Exception;

	String getUserId() throws Exception;

}

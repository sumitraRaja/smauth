package com.emc.dds.xmlarchiving.client.rpc.auth;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface RoleServiceAsync {

	void getRoleId(AsyncCallback<String> callback);

	void getUserId(AsyncCallback<String> callback);

}

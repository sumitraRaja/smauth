package com.emc.dds.xmlarchiving.server.auth;

import com.emc.documentum.xml.dds.user.User;

public interface XMLArchivingUser extends User {

  String getRoleId();
}

/*******************************************************************************
	 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 *******************************************************************************/
package com.emc.dds.xmlarchiving.client;

import java.util.ArrayList;
import java.util.List;

import com.emc.dds.xmlarchiving.client.authorization.Restriction;
import com.emc.dds.xmlarchiving.client.authorization.Role;
import com.emc.dds.xmlarchiving.client.configuration.ApplicationSettings;
import com.emc.dds.xmlarchiving.client.i18n.Locale;
import com.emc.dds.xmlarchiving.client.i18n.Messages;
import com.emc.dds.xmlarchiving.client.rpc.auth.RoleService;
import com.emc.dds.xmlarchiving.client.rpc.auth.RoleServiceAsync;
import com.emc.dds.xmlarchiving.client.ui.LoginPanel;
import com.emc.dds.xmlarchiving.client.ui.MainClientBundle;
import com.emc.documentum.xml.dds.gwt.client.LogCenterFailureListener;
import com.emc.documentum.xml.dds.gwt.client.rpc.DDSServices;
import com.emc.documentum.xml.dds.gwt.client.rpc.LogCenterServiceAsync;
import com.emc.documentum.xml.dds.gwt.client.rpc.application.SerializableApplicationContext;
import com.emc.documentum.xml.dds.gwt.client.rpc.application.UserServiceAsync;
import com.emc.documentum.xml.dds.gwt.client.rpc.persistence.SerializableXQueryValue;
import com.emc.documentum.xml.dds.gwt.client.util.ApplicationContext;
import com.emc.documentum.xml.dds.gwt.client.util.DDSURI;
import com.emc.documentum.xml.dds.logging.LogCenter;
import com.emc.documentum.xml.gwt.client.Dialog;
import com.emc.documentum.xml.gwt.client.FailureHandler;
import com.emc.documentum.xml.gwt.client.FailureHandler.FailureListener;
import com.emc.documentum.xml.gwt.client.ui.DialogBox;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;


/**
 * Entry point class. First loads the application context object that stores all information about
 * locales and data sets. Then the template configuration documents are loaded. Based on this
 * information, the application is composed. WARNING: This class is experimental and may change in
 * future DDS releases.
 */
public class Main implements EntryPoint, FailureListener {

  private ApplicationSettings applicationSettings;

  private boolean userServiceConfigured;

  private Role role;

  private String userName;

  private Panel mainPanel;

  public String getUserName() {
	return userName;
  }
  
  public void setUserName(String userName) {
	this.userName = userName;
  }

  @Override
  public void onModuleLoad() {
	UserServiceAsync userService = DDSServices.getUserService();
    userService.isConfigured(new UserServiceConfigurationCallback());

    MainClientBundle.INSTANCE.bootstrap().ensureInjected();

    FailureHandler.addFailureListener(this);
  }

  /**
   * After the check for the UserService, either run with or without UserManagement.
   */
  private void onUserServiceConfigurationCheck() {
	
       if (userServiceConfigured) {
    	

		// try to login with no explicit creds. If we've already been
		// authenticated by SiteMinder (or some other SSO mechanism),
		// this will work and we'll have setup our user id and don't
		// need to display the login dialog
		UserServiceAsync service = DDSServices.getUserService();
		final Main mainWindow = this;
		service.login("", "", new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				System.out.println("On Failure");
				showLogin();
			}

			@Override
			public void onSuccess(Boolean result) {
				if (result.booleanValue()) {
					// ok, we're already logged in. Get the role we're in
					// and create the role object
					// set the role information here.
					final RoleServiceAsync roleService = (RoleServiceAsync) GWT
							.create(RoleService.class);
					roleService.getRoleId(new AsyncCallback<String>() {

						@Override
						public void onFailure(Throwable caught) {
							FailureHandler.handle(this, caught);
						}

						@Override
						public void onSuccess(String roleId) {
							getRole(roleId);
						}
					});
		
						roleService.getUserId(new AsyncCallback<String>() {

							@Override
							public void onFailure(Throwable caught) {
								FailureHandler.handle(this, caught);
								System.out.println("iN fAILURE");
							}

							@Override
							public void onSuccess(String userId) {
								System.out.println("User Id: " + userId);

								setUserName(userId);
							}
						});
					} else {
					// not logged in, show the login dialog
					showLogin();
				}
			}
		});
    } else {
      // set a default role that has no restrictions
      role = new Role(new ArrayList<Restriction>());
      DDSServices.getApplicationService().getApplicationContext(
          new RetrieveApplicationContextCallback());
    }
  }

  private void getRole(final String id) {
		final Messages messages = Locale.getMessages();
		String appName = GWT.getModuleName();

		String xquery = "<result>\n" + " {\n" + "   doc('/APPLICATIONS/"
				+ appName + "/roles')[/role/id = '" + id + "']\n" + " }\n"
				+ "</result>";

		DDSServices.getXQueryService().execute(null, xquery,
				new AsyncCallback<List<SerializableXQueryValue>>() {
					@Override
					public void onFailure(Throwable caught) {
						FailureHandler.handle(this, caught);
					}

					public void onSuccess(List<SerializableXQueryValue> result) {
						if (result.size() > 0) {
							String value = result.get(0).asString();
							role = new RoleLoader().getRole(value);
							DDSServices
									.getApplicationService()
									.getApplicationContext(
											new RetrieveApplicationContextCallback());
						}
					}
				});

	}
  public void onLogout() {
    // this.mainPanel.setVisible(false);
    logRequest("Application", "logout", userName, "true");
    showLogin();
  }

  LogCenterFailureListener loggerListener = new LogCenterFailureListener();

  public void logRequest(String appName, String loginType, String currentUserName,
      String successfulLogin) {
    // build up the string we'll put in the audit log
    StringBuilder auditLogEntry = new StringBuilder();
    auditLogEntry.append("app : '");
    auditLogEntry.append(appName);
    auditLogEntry.append("', IRM_CODE : E10, user : ");
    auditLogEntry.append(currentUserName);
    auditLogEntry.append(", searchConfiguration : '");
    auditLogEntry.append(loginType);
    auditLogEntry.append("', fields : ");
    auditLogEntry.append("<data><successfulLogin>");
    auditLogEntry.append(successfulLogin);
    auditLogEntry.append("</successfulLogin></data>");

    LogCenterServiceAsync logger = DDSServices.getLogCenterService();
    logger.log(auditLogEntry.toString(), loggerListener);
  }

  /**
   * When the User has been created, show the Login Panel.
   */
  private void showLogin() {
    RootPanel rootPanel = RootPanel.get();
    rootPanel.addStyleName("login");
    rootPanel.clear();
    final LoginPanel loginPanel = new LoginPanel(this);
    rootPanel.add(loginPanel);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {

      @Override
      public void execute() {
        loginPanel.setFocus(true);
      }
    });
  }

  /**
   * When the User has logged in, retrieve the Application Context.
   */
  public void onLoginSuccess(String userNameResult, Role roleResult) {
    role = roleResult;
    userName = userNameResult;
    RootPanel rootPanel = RootPanel.get();
    rootPanel.removeStyleName("login");
    rootPanel.clear();
    DDSServices.getApplicationService().getApplicationContext(
        new RetrieveApplicationContextCallback());
  }

  public ApplicationSettings getApplicationSettings2() {
    return applicationSettings;
  }

  public void setApplicationSettings(ApplicationSettings applicationSettings) {
    this.applicationSettings = applicationSettings;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onFailure(AsyncCallback sender, final Throwable caught) {
    if (caught != null) {
      if (caught.getMessage() != null && caught.getMessage().equals("Session timed out")) {
        onLogout();
      } else {
        final DialogBox confirm =
            Dialog.confirm("An unexpected error occurred", "", "Full Details");
        confirm.addCloseHandler(new CloseHandler<PopupPanel>() {

          @Override
          public void onClose(CloseEvent<PopupPanel> event) {
            if (!confirm.isCanceled()) {
              final StringBuilder builder = new StringBuilder();
              String headMessage = caught.getMessage();
              if (headMessage == null) {
                headMessage = Locale.getErrors().unexpectedException(caught.getClass().getName());
              }
              builder.append(headMessage);
              for (StackTraceElement s : caught.getStackTrace()) {
                builder.append('\n');
                builder.append(s.toString());
              }
              Dialog.alert(builder.toString());
            }
          }
        });
      }
    }
  }

  private class RetrieveApplicationContextCallback implements
      AsyncCallback<SerializableApplicationContext> {

    @Override
    public void onFailure(Throwable caught) {
      FailureHandler.handle(this, caught);
    }

    @Override
    public void onSuccess(SerializableApplicationContext result) {
      ApplicationContext applicationContext = new ApplicationContext(result);
      // Main.this.applicationSettings = new ApplicationSettings(applicationContext);
      applicationSettings = new ApplicationSettings(applicationContext);

      applicationSettings.setRole(role);
      applicationSettings.setUserName(userName);
      applicationSettings.setUserServiceConfigured(userServiceConfigured);

      DDSURI templateContentURI = new DDSURI("template/template-content.xml");
      templateContentURI.setAttribute(DDSURI.ATTRIBUTE_DOMAIN, DDSURI.DOMAIN_RESOURCE);
      DDSURI templatePanesURI = new DDSURI("template/template-panes.xml");
      templatePanesURI.setAttribute(DDSURI.ATTRIBUTE_DOMAIN, DDSURI.DOMAIN_RESOURCE);

      List<String> config = new ArrayList<String>();
      config.add(templateContentURI.toString());
      config.add(templatePanesURI.toString());

      DDSServices.getXMLPersistenceService().getContentsAsString(config,
          new RetrieveContentConfigurationCallback());
    }
  }

  private class RetrieveContentConfigurationCallback implements AsyncCallback<List<String>> {

    @Override
    public void onFailure(Throwable caught) {
      FailureHandler.handle(this, caught);
    }

    @Override
    public void onSuccess(List<String> result) {
      mainPanel = new ConfigurationLoader(result).load(Main.this);
    }
  }

  private class UserServiceConfigurationCallback implements AsyncCallback<Boolean> {

    @Override
    public void onFailure(Throwable caught) {
      FailureHandler.handle(this, caught);
    }

    @Override
    public void onSuccess(Boolean result) {
      if (result) {
        userServiceConfigured = true;
      } else {
        userServiceConfigured = false;
      }
      System.out.println("userServiceConfigured : " + userServiceConfigured);
      onUserServiceConfigurationCheck();
    }
  }
}

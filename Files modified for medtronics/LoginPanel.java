/*******************************************************************************
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 *******************************************************************************/
package com.emc.dds.xmlarchiving.client.ui;

import java.util.List;

import com.emc.dds.xmlarchiving.client.Main;
import com.emc.dds.xmlarchiving.client.RoleLoader;
import com.emc.dds.xmlarchiving.client.authorization.Role;
import com.emc.dds.xmlarchiving.client.i18n.Labels;
import com.emc.dds.xmlarchiving.client.i18n.Locale;
import com.emc.dds.xmlarchiving.client.i18n.Messages;
import com.emc.dds.xmlarchiving.client.rpc.auth.RoleService;
import com.emc.dds.xmlarchiving.client.rpc.auth.RoleServiceAsync;
import com.emc.dds.xmlarchiving.client.ui.image.MainImageBundle;
import com.emc.documentum.xml.dds.gwt.client.LogCenterFailureListener;
import com.emc.documentum.xml.dds.gwt.client.rpc.DDSServices;
import com.emc.documentum.xml.dds.gwt.client.rpc.LogCenterServiceAsync;
import com.emc.documentum.xml.dds.gwt.client.rpc.application.UserServiceAsync;
import com.emc.documentum.xml.dds.gwt.client.rpc.persistence.SerializableXQueryValue;
import com.emc.documentum.xml.gwt.client.Dialog;
import com.emc.documentum.xml.gwt.client.FailureHandler;
import com.emc.documentum.xml.gwt.client.ui.Button;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Login panel. This panel displays the login dialog.
 */
public class LoginPanel extends Composite implements ClickHandler, KeyDownHandler {

	private final RoleServiceAsync roleService = (RoleServiceAsync) GWT.create(RoleService.class);

	private TextBox tbUser = new TextBox();
	private PasswordTextBox tbPassword = new PasswordTextBox();
	private String userName;
	private Main main;

	/**
	 * Creates a new instance.
	 *
	 * @param main
	 *            {@link Main} instance
	 */
	public LoginPanel(Main main) {
		this.main = main;

		VerticalPanel mainPanel = new VerticalPanel();
		initWidget(mainPanel);

		Widget loginWidget = createLoginWidget();

		FlowPanel container = new FlowPanel();
		container.addStyleName("login-form-container");
		container.add(MainImageBundle.INSTANCE.infoarchive().createImage());
		container.add(new SimplePanel(loginWidget));
		loginWidget.getParent().addStyleName("form-signin");
		Label copyrightLabel = new Label(Locale.getLabels().copyrightEMC());
		copyrightLabel.addStyleName("text-center");
		copyrightLabel.addStyleName("muted");
		container.add(copyrightLabel);

		mainPanel.add(container);

		mainPanel.setCellVerticalAlignment(container, HasVerticalAlignment.ALIGN_MIDDLE);
		mainPanel.setCellHorizontalAlignment(container, HasHorizontalAlignment.ALIGN_CENTER);
		mainPanel.addStyleName("container");
	}

	private Widget createLoginWidget() {
		VerticalPanel panel = new VerticalPanel();
		Labels labels = Locale.getLabels();
		FlowPanel loginPanel = new FlowPanel();

		Label heading = new Label(Locale.getLabels().pleaseSignIn());
		heading.addStyleName("form-signin-heading");
		loginPanel.add(heading);

		tbUser.getElement().setAttribute("placeholder", labels.username());
		tbPassword.getElement().setAttribute("placeholder", labels.password());
		loginPanel.add(tbUser);
		loginPanel.add(tbPassword);

		Button button = new Button(labels.signIn(), this);
		button.setStylePrimaryName("btn");
		button.addStyleDependentName("large");
		button.addStyleDependentName("primary");
		loginPanel.add(button);

		panel.add(loginPanel);

		tbUser.addKeyDownHandler(this);
		tbPassword.addKeyDownHandler(this);
		return panel;
	}

	private void getRoleId() {
		roleService.getRoleId(new AsyncCallback<String>() {
			@Override
			public void onFailure(Throwable caught) {
				FailureHandler.handle(this, caught);
			}

			@Override
			public void onSuccess(String roleId) {
				if ("useDDSAuth".equalsIgnoreCase(roleId)) {
					getDDSUserRoleId();
				} else {
					getRole(roleId);
				}
			}
		});
	}

	private void getDDSUserRoleId() {
		final Messages messages = Locale.getMessages();
		String appName = GWT.getModuleName();
		String xquery = "<result>\n" + " {\n" + "   let $userRoleId := doc('/APPLICATIONS/" + appName + "/users/"
				+ this.userName + "')/com.emc.dds.xmlarchiving.client.rpc.LDMUser/roleId/text()\n return \n "
				+ "doc('/APPLICATIONS/" + appName + "/roles')[/role/id = $userRoleId] }\n" + "</result>";

		DDSServices.getXQueryService().execute(null, xquery, new AsyncCallback<List<SerializableXQueryValue>>() {

			@Override
			public void onFailure(Throwable caught) {
				Dialog.alert(messages.loginFailed());
			}

			@Override
			public void onSuccess(List<SerializableXQueryValue> result) {
				if (result.size() > 0) {
					String value = result.get(0).asString();
					Role role = new RoleLoader().getRole(value);
					if (role != null) {
						LoginPanel.this.main.onLoginSuccess(LoginPanel.this.userName, role);
					} else {
						Dialog.alert(messages.roleNotFoundError(value));
					}
				}
			}
		});
	}

	private void getRole(final String id) {
		final Messages messages = Locale.getMessages();
		String appName = GWT.getModuleName();
		String xquery = "<result>\n" + " {\n" + "   doc('/APPLICATIONS/" + appName + "/roles')[/role/id = '" + id
				+ "']\n" + " }\n" + "</result>";

		DDSServices.getXQueryService().execute(null, xquery, new AsyncCallback<List<SerializableXQueryValue>>() {

			@Override
			public void onFailure(Throwable caught) {
				FailureHandler.handle(this, caught);
			}

			@Override
			public void onSuccess(List<SerializableXQueryValue> result) {
				if (result.size() > 0) {
					String value = result.get(0).asString();
					Role role = new RoleLoader().getRole(value);
					if (role != null) {
						main.onLoginSuccess(userName, role);
					} else {
						Dialog.alert(messages.roleNotFoundError(id));
					}
				}
			}
		});

	}

	public void setFocus(boolean value) {
		tbUser.setFocus(value);
	}

	@Override
	public void onClick(ClickEvent event) {
		submit();
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		int nativeKeyCode = event.getNativeKeyCode();
		if (nativeKeyCode == KeyCodes.KEY_ENTER || nativeKeyCode == ' ') {
			submit();
		}
	}

	private void submit() {
		final Messages messages = Locale.getMessages();
		userName = tbUser.getText();
		String password = tbPassword.getText();
		final String loginName = userName;

		UserServiceAsync service = DDSServices.getUserService();
		service.login(userName, password, new AsyncCallback<Boolean>() {

			@Override
			public void onFailure(Throwable caught) {
				logRequest("Application", "login", loginName, "false");
				FailureHandler.handle(this, caught);
			}

			@Override
			public void onSuccess(Boolean result) {
				if (result.booleanValue()) {
					logRequest("Application", "login", loginName, "true");
					getRoleId();
				} else {
					logRequest("Application", "login", loginName, "false");
					Dialog.alert(messages.loginFailed());
				}
			}
		});
	}

	LogCenterFailureListener loggerListener = new LogCenterFailureListener();

	public void logRequest(String appName, String loginType, String currentUserName, String successfulLogin) {
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
}

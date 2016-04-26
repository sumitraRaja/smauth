/***************************************************************************************************
 * Copyright (c) 2008, 2011 EMC Corporation All Rights Reserved
 **************************************************************************************************/
package com.emc.dds.xmlarchiving.client.ui;

import com.emc.dds.xmlarchiving.client.Main;
import com.emc.dds.xmlarchiving.client.i18n.Locale;
import com.emc.dds.xmlarchiving.client.ui.image.MainImageBundle;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Login panel. This panel displays the login dialog.
 */
public class LoginAccessDenied extends Composite {

	/**
	 * Creates a new instance.
	 *
	 * @param main
	 *            {@link Main} instance
	 */
	
	private Main main;
	
	public LoginAccessDenied() {
		this.main = main;
		VerticalPanel mainPanel = new VerticalPanel();
		initWidget(mainPanel);

		Widget loginWidget = createLoginWidget();

		FlowPanel container = new FlowPanel();
		container.addStyleName("login-form-container");
		container.add(MainImageBundle.INSTANCE.infoarchive().createImage());
		container.add(new SimplePanel(loginWidget));
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
		HTML label = new HTML("<center><h4><p>Access Denied</p></h4><p>You do not have required access to access this application.<br>Please contact helpdesk to raise request</p></center>");
		panel.add(label);
		panel.setCellVerticalAlignment(label, HasVerticalAlignment.ALIGN_MIDDLE);
		panel.setCellHorizontalAlignment(label, HasHorizontalAlignment.ALIGN_CENTER);
		return panel;
	}
}

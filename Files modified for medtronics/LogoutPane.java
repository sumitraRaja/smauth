/*******************************************************************************
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 *******************************************************************************/
package com.emc.dds.xmlarchiving.client.ui;

import com.emc.dds.xmlarchiving.client.Main;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * Header for the application. WARNING: This class is experimental and may change in future DDS
 * releases.
 */
public class LogoutPane extends Pane {

  public LogoutPane(final Main main) {
    HorizontalPanel headerPanel = new HorizontalPanel();
    String uname = main.getUserName();
    Label loggedInLabel =
        new Label("Currently logged in as " + uname + "\u00a0\u00a0");
    HTML label = new HTML("<a>Logout</a>");
    headerPanel.add(loggedInLabel);
    headerPanel.add(label);
    headerPanel.addStyleName(getPaneStyle());
    initWidget(headerPanel);
    label.addClickHandler(new ClickHandler() {

      @Override
      public void onClick(ClickEvent event) {
        main.onLogout();
      }
    });
  }

  @Override
  public String getPaneName() {
    return LOGOUT_PANE_NAME;
  }

  @Override
  public int getPaneType() {
    return LOGOUT_PANE;
  }
}

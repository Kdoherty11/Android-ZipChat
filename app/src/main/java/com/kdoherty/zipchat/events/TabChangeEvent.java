package com.kdoherty.zipchat.events;


public class TabChangeEvent {

    private String tabTitle;

    public TabChangeEvent(String tabTitle) {
        this.tabTitle = tabTitle;
    }

    public String getTabTitle() {
        return tabTitle;
    }
}

package com.kdoherty.zipchat.events;


public class TabChangeEvent {

    private String mTabTitle;

    public TabChangeEvent(String tabTitle) {
        this.mTabTitle = tabTitle;
    }

    public String getTabTitle() {
        return mTabTitle;
    }
}

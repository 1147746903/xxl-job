package com.xxl.job.core.biz.model;

import java.io.Serializable;

/**
 * wqe添加
 * Created by xuxueli on 2017-05-10 20:22:42
 */
public class RegistryGroupParam implements Serializable {
    private static final long serialVersionUID = 42L;

    private String appname;
    private String title;
    private String addressType;

    public RegistryGroupParam(){}

    public RegistryGroupParam(String appname, String title, String addressType) {
        this.appname = appname;
        this.title = title;
        this.addressType = addressType;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    @Override
    public String toString() {
        return "RegistryGroupParam{" +
                "appname='" + appname + '\'' +
                ", title='" + title + '\'' +
                ", addressType='" + addressType + '\'' +
                '}';
    }
}

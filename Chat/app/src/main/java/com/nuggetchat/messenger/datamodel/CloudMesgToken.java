package com.nuggetchat.messenger.datamodel;

public class CloudMesgToken {
    private String version;
    private String deviceToken;

    public CloudMesgToken() {}

    public CloudMesgToken(String version, String token) {
        setVersion(version);
        setDeviceToken(token);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}

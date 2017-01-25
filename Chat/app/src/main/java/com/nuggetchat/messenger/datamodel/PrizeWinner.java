package com.nuggetchat.messenger.datamodel;

public class PrizeWinner {
    String date;
    String email;
    String address;

    public PrizeWinner(String date, String email, String address) {
        this.date = date;
        this.email = email;
        this.address = address;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

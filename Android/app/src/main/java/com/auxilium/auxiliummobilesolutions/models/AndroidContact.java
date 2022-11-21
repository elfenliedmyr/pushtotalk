package com.auxilium.auxiliummobilesolutions.models;

public class AndroidContact {

    private String name;
    private String phone;
    private String email;

    public AndroidContact(){}

    public AndroidContact(
            String name,
            String number,
            String email
    ){
        this.name = name;
        this.phone = number;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return phone;
    }

    public void setNumber(String number) {
        this.phone = number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

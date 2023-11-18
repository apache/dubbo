package org.apache.dubbo.remoting.http12.message.codec;

public class User {

    private String username;

    private String location;

    public User(String username, String location) {
        this.username = username;
        this.location = location;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

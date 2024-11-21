package org.apache.dubbo.springboot.demo.servlet;

public class Live {

    private boolean alive;
    private byte[] raw;

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }
}

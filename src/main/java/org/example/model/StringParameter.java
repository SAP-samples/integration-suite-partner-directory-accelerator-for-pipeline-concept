package org.example.model;

public class StringParameter {
    private String pid;
    private String id;
    private String value;

    public StringParameter(String pid, String id, String value) {
        this.pid = pid;
        this.id = id;
        this.value = value;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

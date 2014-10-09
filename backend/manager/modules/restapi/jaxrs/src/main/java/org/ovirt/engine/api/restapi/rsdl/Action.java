package org.ovirt.engine.api.restapi.rsdl;

public class Action {

    public Action() {
        super();
    }

    public Action(String name) {
        super();
        this.name = name;
        this.request = new Request();
    }

    private String name;
    private Request request;


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Request getRequest() {
        return request;
    }
    public void setRequest(Request request) {
        this.request = request;
    }
}

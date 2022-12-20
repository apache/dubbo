package org.apache.dubbo.common.model.person;

import java.util.HashMap;

public class PersonMap extends HashMap<String, String> {

    private static final String ID = "1";
    private static final String NAME = "hand";

    String personId;
    String personName;

    public String getPersonId() {
        return get(ID);
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonName() {
        return get(NAME);
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }
}


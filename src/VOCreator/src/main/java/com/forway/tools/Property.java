package com.forway.tools;

public class Property {
    private String name;
    private String codeName;
    private String shortName;
    private String type;
    private String genericsType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.codeName = name.substring(0,1).toUpperCase()+name.substring(1);
    }

    public String getGenericsType() {
        return genericsType;
    }

    public void setGenericsType(String genericsType) {
        this.genericsType = genericsType;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
            return "Property{" +
                    "name='" + name + '\'' +
                    ", shortName='" + shortName + '\'' +
                    ", type='" + type + '\'' +
                    '}';
    }
}

package com.forway.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by WuJianjun on 15/1/12.
 */
public class Entity {
    private String className;
    private String superClassName;
    private String packageName;
    private List<Property> properties;
    private Set<String> importClassName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Set<String> getImportClassName() {
        return importClassName;
    }
    public void addImportClassName(String importClassName) {
        if (this.importClassName == null) {
            setImportClassName(new HashSet<String>());
        }
        getImportClassName().add(importClassName);
    }
    public void setImportClassName(Set<String> importClassName) {
        this.importClassName = importClassName;
    }

    public void addProperties(Property property) {
        if (properties == null) {
            setProperties(new ArrayList<Property>());
        }
        getProperties().add(property);
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "className='" + className + '\'' +
                ", superClassName='" + superClassName + '\'' +
                ", properties=" + properties +
                '}';
    }
}


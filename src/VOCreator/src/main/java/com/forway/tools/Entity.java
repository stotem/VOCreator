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
    private Class superClass;
    private String packageName;
    private List<Property> properties;
    private Set<ConstProperty> constants;
    private Set<ConstProperty> constantString;
    private Set<String> importClassName;

    public String getPackageName() {
        return packageName;
    }

    public Class getSuperClass() {
        return superClass;
    }

    public void setSuperClass(Class superClass) {
        this.superClass = superClass;
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

    public Set<ConstProperty> getConstants() {
        return constants;
    }

    public void setConstants(Set<ConstProperty> constants) {
        this.constants = constants;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Set<ConstProperty> getConstantString() {
        return constantString;
    }

    public void setConstantString(Set<ConstProperty> constantString) {
        this.constantString = constantString;
    }

    public void addConstantString(ConstProperty property) {
        if (constantString == null) {
            setConstantString(new HashSet<ConstProperty>());
        }
        getConstantString().add(property);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entity entity = (Entity) o;

        if (!className.equals(entity.className)) return false;
        return packageName.equals(entity.packageName);

    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + packageName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "className='" + className + '\'' +
                ", superClassName='" + superClassName + '\'' +
                ", properties=" + properties +
                '}';
    }

    public void addConstants(ConstProperty property) {
        if (constants == null) {
            setConstants(new HashSet<ConstProperty>());
        }
        getConstants().add(property);
    }
}


package com.forway.tools;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.nutz.json.JsonField;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by WuJianjun on 15/1/12.
 */
public class Creator {

    public static final Properties properties = new Properties();

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, FileNotFoundException {
        if (args == null || args.length==0) {
            args = new String[]{"config.ini"};
        }
        loadConfig(args[0]);
        // 获取包下所有的类
        String scanPackage = properties.getProperty("VO.ScanPackage","");
        if (scanPackage.trim().length() <= 0) {
            System.err.println("[ERROR] scanPackage not found");
            return;
        }
        Set<Class> allClass = ClassScaner.scan(scanPackage.split(","));
        List<Entity> entities = new Vector<>();
        for (Class clas : allClass) {
            if (clas.isInterface()) {
                continue;
            }
            // 获取类属性
            setAllClass(entities, clas);
        }
        // 剔除重复属性
        for (Entity entity : entities) {
            Entity superEntity = new Entity();
            superEntity.setClassName(entity.getSuperClass().getSimpleName());
            superEntity.setPackageName(entity.getSuperClass().getPackage().getName());
            if (entity.getProperties() != null && !entity.getProperties().isEmpty()) {
                for (Property property : entity.getProperties()) {
                    removePropertyFromSuperClass(entities, superEntity, property);
                }
            }
        }
        // 调用模板生成文件
        outputFiles(entities);

        System.out.println("==========成功生成"+entities.size()+"个文件===============");
    }

    private static void loadConfig(String path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(path);
        if (inputStream == null) {
            throw new RuntimeException("[ERROR] not found config.ini");
        }
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void outputFiles(List<Entity> entities) {
        //处理中文问题
        Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
        Velocity.setProperty(Velocity.OUTPUT_ENCODING,"UTF-8");
        Velocity.init();

        VelocityContext root = new VelocityContext();
        String prefix = properties.getProperty("VO.Prefix", "");
        String suffix = properties.getProperty("VO.Suffix", "");
        String outDic = properties.getProperty("VO.Output", "creator/");
        outDic = outDic.endsWith("/") ? outDic:outDic+"/";
        root.put("prefix", prefix);
        root.put("suffix", suffix);
        File outFile = new File(outDic);
        if (!outFile.exists()) {
            outFile.mkdirs();
        }
        for (Entity entity : entities) {
            root.remove("entity");
            System.out.println("------------生成"+entity.getClassName()+"类--------------");
            String dic = getPackageDir(entity.getPackageName());
            File file = new File(dic);
            if (!file.exists()) {
                file.mkdirs();
            }
            root.put("entity", entity);
            out(root, "Template_h.vm", dic + prefix + entity.getClassName() + suffix + ".h");
            out(root, "Template_m.vm", dic + prefix + entity.getClassName() + suffix + ".m");
        }

        if (properties.getProperty("VO.CreateBasic","true").trim().equalsIgnoreCase("true")) {
            System.out.println("------------生成Base基础类--------------");
            out(root, "BaseVO_h.vm", outDic + prefix + "Base" + suffix + ".h");
            out(root, "BaseVO_m.vm", outDic + prefix + "Base" + suffix + ".m");
        }
    }

    public static String getPackageDir(String pageName) {
        String scanPackage = properties.getProperty("VO.ScanPackage");
        String[] scanPkgs = scanPackage.split(",");
        for (String scanPkg : scanPkgs) {
            pageName = pageName.replaceAll(scanPkg, "");
        }
        String outDic = properties.getProperty("VO.Output","creator/");
        outDic = outDic.endsWith("/") ? outDic:outDic+"/";
        if (pageName.trim().length() <= 0) {
            return outDic;
        }
        pageName = pageName.replaceAll("\\.","/");
        pageName = pageName.endsWith("/") ? pageName:pageName+"/";
        return outDic + pageName;
    }

    private static void out(VelocityContext root,String templateFile,String outFile) {
        Writer writer = null;

        Template template = null;
        // 生成m文件
        try {
            template = Velocity.getTemplate(templateFile);
            writer = new PrintWriter(new FileOutputStream(new File(outFile)));
            template.merge(root, writer);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void setAllClass(List<Entity> entities, Class clas) throws IllegalAccessException, InstantiationException {
        if (clas.isEnum() || Exception.class.isAssignableFrom(clas)) {
            System.out.println("-- skip class - [" + clas + "]");
            return;
        }
        Object obj = null;
        try {
            obj = clas.newInstance();
        }catch(Exception e) {
        }

        Entity entity = new Entity();
        entity.setClassName(clas.getSimpleName());
        entity.setPackageName(clas.getPackage().getName());
        entity.setSuperClass(clas.getSuperclass());
        entity.setSuperClassName(clas.getSuperclass().getSimpleName());
        Field[] declaredFields = clas.getDeclaredFields();

        for (Field declaredField : declaredFields) {
            getAllProperty(entities, obj,declaredField, entity);
        }
        entities.add(entity);
    }

    private static void getAllProperty(List<Entity> entities, Object obj, Field declaredField, Entity entity) {
        if (declaredField.getName().equalsIgnoreCase("serialVersionUID")) {
            return;
        }

        // 如果为枚举
        Class<?> fieldType = declaredField.getType();
        if (fieldType.isEnum() || Exception.class.isAssignableFrom(fieldType)) {
            System.out.println("-- skip property - [" + declaredField.getName() + "] - "+fieldType);
            return;
        }

        int modifiers = declaredField.getModifiers();
        if (Modifier.isPrivate(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
            System.out.println("-- skip constant - [" + declaredField.getName() + "]");
            return;
        }
        if (obj != null && Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)
                && (int.class.isAssignableFrom(fieldType) || Integer.class.isAssignableFrom(fieldType)
                || float.class.isAssignableFrom(fieldType) || Float.class.isAssignableFrom(fieldType)
                || double.class.isAssignableFrom(fieldType) || Double.class.isAssignableFrom(fieldType)
                || long.class.isAssignableFrom(fieldType) || Long.class.isAssignableFrom(fieldType) || String.class.isAssignableFrom(fieldType)
                || boolean.class.isAssignableFrom(fieldType) || Boolean.class.isAssignableFrom(fieldType))) {
            try {
                Object value = declaredField.get(obj);
                if (value == null) {
                    return;
                }

                ConstProperty property = new ConstProperty();
                property.setName(declaredField.getName());
                if (boolean.class.isAssignableFrom(fieldType) || Boolean.class.isAssignableFrom(fieldType)) {
                    property.setValue((boolean)value ? "1":"0");
                } else {
                    property.setValue(value.toString().trim());
                }
                if (String.class.isAssignableFrom(fieldType)) {
                    entity.addConstantString(property);
                }
                else {
                    entity.addConstants(property);
                }
            } catch (IllegalAccessException e) {
                System.err.println("[WARN] not found value of const field "+entity.getClassName()+"."+declaredField.getName());
            }
            return;
        }

        Property property = new Property();
        property.setName(declaredField.getName());
        property.setType(fieldType.getSimpleName());

        if (List.class.isAssignableFrom(fieldType) || Set.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
            Type[] types = ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments();
            if (types.length > 0) {
                Type argsType = types[types.length-1];
                // 如果泛型为?,则按int处理

                Class argsClass = null;
                if (argsType instanceof Class) {
                    argsClass = (Class)argsType;
                }
                else {
                    argsClass = Integer.class;
                    System.err.println("[WARN] entity "+entity.getClassName()+"."+property.getName()+" type error."+argsType);
                }
                property.setGenericsType(argsClass.getSimpleName());

                if (!argsClass.isPrimitive() && !fieldType.isArray() && !argsClass.equals(Integer.class) && !argsClass.equals(String.class)
                        && !argsClass.equals(Long.class) && !argsClass.equals(Timestamp.class)
                        && !argsClass.equals(Date.class) && !argsClass.equals(Map.class)
                        && !argsClass.equals(Boolean.class) && !argsClass.equals(boolean.class)
                        && !argsClass.equals(Byte.class) && !argsClass.equals(byte.class)&& !argsClass.equals(Object.class)) {
                    entity.addImportClassName(argsClass.getSimpleName());
                }
            }
        }
        else if (!fieldType.isPrimitive() && !fieldType.isArray() && !Integer.class.equals(fieldType) && !String.class.equals(fieldType)
                && !Long.class.equals(fieldType) && !Date.class.equals(fieldType) && !Float.class.equals(fieldType)
                && !Double.class.equals(fieldType)&& !Timestamp.class.equals(fieldType) && !Short.class.equals(fieldType)
                && !fieldType.equals(Boolean.class) && !fieldType.equals(boolean.class)
                && !fieldType.equals(Byte.class) && !fieldType.equals(byte.class)&& !fieldType.equals(Object.class)) {
            entity.addImportClassName(fieldType.getSimpleName());
        }
        JsonField jsonField = declaredField.getAnnotation(JsonField.class);
        property.setShortName(jsonField != null ? jsonField.value() : property.getName());

        // Check SuperClass Property
        Entity superEntity = new Entity();
        superEntity.setClassName(entity.getSuperClass().getSimpleName());
        superEntity.setPackageName(entity.getSuperClass().getPackage().getName());

        removePropertyFromSuperClass(entities, superEntity, property);
        entity.addProperties(property);
    }

    public static void removePropertyFromSuperClass(List<Entity> entities, Entity entity, Property property) {
        int idx = entities.indexOf(entity);
        if (idx == -1) {
            return;
        }
        entity = entities.get(idx);
        List<Property> properties = entity.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        if (properties.contains(property)) {
            properties.remove(property);
        }

        if (entity.getSuperClass() != null && !entity.getSuperClass().equals(Object.class)) {
            Entity superEntity = new Entity();
            superEntity.setClassName(entity.getSuperClass().getSimpleName());
            superEntity.setPackageName(entity.getSuperClass().getPackage().getName());
            removePropertyFromSuperClass(entities, superEntity, property);
        }
    }
}
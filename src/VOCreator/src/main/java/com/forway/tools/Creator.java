package com.forway.tools;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.nutz.json.JsonField;
import sun.reflect.generics.reflectiveObjects.WildcardTypeImpl;

import java.io.*;
import java.lang.reflect.Field;
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
        String scanPackage = properties.getProperty("VO.scanPackage","");
        if (scanPackage.trim().length() <= 0) {
            System.err.println("[ERROR] scanPackage not found");
            return;
        }
        Set<Class> allClass = ClassScaner.scan(scanPackage.split(","));
        Set<Entity> entities = new HashSet<>();
        for (Class clas : allClass) {
            if (clas.isInterface()) {
                continue;
            }
            // 获取类属性
            Entity entity = getAllClass(clas);
            entities.add(entity);
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

    private static void outputFiles(Set<Entity> entities) {
        //处理中文问题
        Velocity.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
        Velocity.setProperty(Velocity.OUTPUT_ENCODING,"UTF-8");
        Velocity.init();

        VelocityContext root = new VelocityContext();
        String prefix = properties.getProperty("VO.Prefix", "");
        String suffix = properties.getProperty("VO.Suffix", "");
        String outDic = properties.getProperty("VO.Output","creator/");
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
            out(root, "h_template.vm", dic + prefix + entity.getClassName() + suffix + ".h");
            out(root, "m_template.vm", dic + prefix + entity.getClassName() + suffix + ".m");
        }

        if (properties.getProperty("VO.CreateBasic","true").trim().equalsIgnoreCase("true")) {
            System.out.println("------------生成Base基础类--------------");
            out(root, "h_BaseVO.vm", outDic + prefix + "Base" + suffix + ".h");
            out(root, "m_BaseVO.vm", outDic + prefix + "Base" + suffix + ".m");
            out(root, "h_RequestVO.vm", outDic + prefix + "Request" + suffix + ".h");
            out(root, "m_RequestVO.vm", outDic + prefix + "Request" + suffix + ".m");
            out(root, "h_ResponseVO.vm", outDic + prefix + "Response" + suffix + ".h");
            out(root, "m_ResponseVO.vm", outDic + prefix + "Response" + suffix + ".m");
            out(root, "h_TokenVO.vm", outDic + prefix + "Token" + suffix + ".h");
            out(root, "m_TokenVO.vm", outDic + prefix + "Token" + suffix + ".m");
        }
    }

    public static String getPackageDir(String pageName) {
        String scanPackage = properties.getProperty("VO.scanPackage");
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

    private static Entity getAllClass(Class clas) throws IllegalAccessException, InstantiationException {
        Entity entity = new Entity();
        entity.setClassName(clas.getSimpleName());
        entity.setPackageName(clas.getPackage().getName());
        entity.setSuperClassName(clas.getSuperclass().getSimpleName());
        Field[] declaredFields = clas.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            getAllProperty(declaredField, entity);
        }
        return entity;

    }

    private static void getAllProperty(Field declaredField, Entity entity) {
        if (declaredField.getName().indexOf("_") > -1) {
            return;
        }
        if (declaredField.getName().equalsIgnoreCase("serialVersionUID")) {
            return;
        }
        Property property = new Property();
        property.setName(declaredField.getName());
        property.setType(declaredField.getType().getSimpleName());

        if (declaredField.getType().isAssignableFrom(List.class) || declaredField.getType().isAssignableFrom(Set.class) || declaredField.getType().equals(Map.class)) {
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

                if (!argsClass.isPrimitive() && !argsClass.equals(Integer.class) && !argsClass.equals(String.class)
                        && !argsClass.equals(Long.class) && !argsClass.equals(Timestamp.class) && !argsClass.equals(Date.class) && !argsClass.equals(Map.class)){
                    entity.addImportClassName(argsClass.getSimpleName());
                }
            }
        }
        else if (!declaredField.getType().isPrimitive() && !declaredField.getType().equals(Integer.class) && !declaredField.getType().equals(String.class)
                && !declaredField.getType().equals(Long.class) && !declaredField.getType().equals(Date.class) && !declaredField.getType().equals(Float.class)
                && !declaredField.getType().equals(Double.class)&& !declaredField.getType().equals(Timestamp.class) && !declaredField.getType().equals(Short.class)){
            entity.addImportClassName(declaredField.getType().getSimpleName());
        }
        JsonField jsonField = declaredField.getAnnotation(JsonField.class);
        property.setShortName(jsonField != null ? jsonField.value() : property.getName());

        entity.addProperties(property);
    }
}
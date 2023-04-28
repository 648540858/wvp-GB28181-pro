package com.genersoft.iot.vmp.jt1078.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

public class ClassUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);


    public static Object getBean(Class<?> clazz) {
        if (clazz != null) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                logger.error("ClassUtil:找不到指定的类", ex);
            }
        }
        return null;
    }


    public static Object getBean(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (Exception ex) {
            logger.error("ClassUtil:找不到指定的类");
        }
        if (clazz != null) {
            try {
                //获取声明的构造器--》创建实例
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                logger.error("ClassUtil:找不到指定的类", ex);
            }
        }
        return null;
    }


    /**
     * 获取包下所有带注解的class
     *
     * @param packageName     包名
     * @param annotationClass 注解类型
     * @return list
     */
    public static List<Class<?>> getClassList(String packageName, Class<? extends Annotation> annotationClass) {
        List<Class<?>> classList = getClassList(packageName);
        classList.removeIf(next -> !next.isAnnotationPresent(annotationClass));
        return classList;
    }

    public static List<Class<?>> getClassList(String... packageName) {
        List<Class<?>> classList = new LinkedList<>();
        for (String s : packageName) {
            List<Class<?>> c = getClassList(s);
            classList.addAll(c);
        }
        return classList;
    }

    public static List<Class<?>> getClassList(String packageName) {
        List<Class<?>> classList = new LinkedList<>();
        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resourcePatternResolver.getResources(packageName.replace(".", "/") + "/**/*.class");
            for (Resource resource : resources) {
                String url = resource.getURL().toString();

                String[] split = url.split(packageName.replace(".", "/"));
                String s = split[split.length - 1];
                String className = s.replace("/", ".");
                className = className.substring(0, className.lastIndexOf("."));
                doAddClass(classList, packageName + className);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return classList;
    }

    private static void doAddClass(List<Class<?>> classList, String className) {
        Class<?> cls = loadClass(className, false);
        classList.add(cls);
    }

    public static Class<?> loadClass(String className, boolean isInitialized) {
        Class<?> cls;
        try {
            cls = Class.forName(className, isInitialized, getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return cls;
    }


    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}

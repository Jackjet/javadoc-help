package com.genx.javadoc;

import com.genx.javadoc.filter.IJavaDocFilter;
import com.genx.javadoc.parse.JavaDocSpringMvcControllerParser;
import com.genx.javadoc.utils.FileUtil;
import com.sun.deploy.util.StringUtils;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/3/9 16:02
 */
public class JavaDocReader {

    private static RootDoc root = null;

    public static class Doclet {
        public static LanguageVersion languageVersion() {
            return LanguageVersion.JAVA_1_5;
        }

        public static boolean start(RootDoc root) {
            JavaDocReader.root = root;
            return true;
        }
    }
    public synchronized static void read(File sourceDir, File classesDir, File libDir, IJavaDocFilter javaDocFilter){
        javadocExecute(sourceDir, classesDir, libDir);
        JavaDocSpringMvcControllerParser javaDocSpringMvcControllerParser = new JavaDocSpringMvcControllerParser(root, javaDocFilter );
        javaDocSpringMvcControllerParser.parse();
    }

    private static void javadocExecute(File sourceDir, File classesDir, File libDir){
        List<String> classPathes = new ArrayList(1024);
//        classPathes.add("D:\\Program Files\\Java\\jdk1.8.0\\lib/tools.jar");
        classPathes.add(classesDir.getAbsolutePath());
        Collection<File> jarList = FileUtil.listFiles(libDir, pathname -> pathname.getName().toLowerCase().endsWith(".jar"), false);
        for (File jar : jarList) {
            classPathes.add(jar.getAbsolutePath());
        }

        List<String> commandList = new ArrayList<>(1024);
        commandList.add("-doclet");
        commandList.add(Doclet.class.getName());
        commandList.add("-encoding");
        commandList.add("utf-8");
        commandList.add("-classpath");
        commandList.add(StringUtils.join(classPathes, ";"));

        Collection<File> list = FileUtil.listFiles(sourceDir, file ->
                file.isDirectory() || file.getName().toLowerCase().endsWith(".java"), false);

        if (list.size() == 0) {
            throw new IllegalArgumentException("list is null");
        }

        for (File file : list) {
            commandList.add(file.getAbsolutePath());
        }
        com.sun.tools.javadoc.Main.execute(commandList.toArray(new String[commandList.size()]));
    }
}
package org.genx.javadoc;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.genx.javadoc.bean.JavaDoc;
import org.genx.javadoc.bean.rest.RestApiDoc;
import org.genx.javadoc.helper.RestApiBuilder;
import org.genx.javadoc.utils.FileUtil;
import org.genx.javadoc.utils.ZipUtil;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 *
 * @author: genx
 * @date: 2019/3/11 22:51
 */
@Mojo(name = "javaDoc", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class JavaDocMojo extends AbstractMojo {


    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Contains the full list of projects in the reactor.
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution mojo;

    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;


    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File outputDirectory;


    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compilePath;

    @Parameter(property = "scope")
    private String scope;


    @Parameter(property = "sourceDir")
    private String sourceDir;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File sourceDirectory;


    /**
     * The dependency tree builder to use.
     */
//    @Component(hint = "default")
//    private DependencyGraphBuilder dependencyGraphBuilder;

    /**
     * 导入外部源码文件夹
     */
    @Parameter(property = "importSourceDirs")
    private List<File> importSourceDirs;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        System.out.println("JavaDocMojo start");

        System.out.println("sourceDir=" + sourceDir);

        System.out.println("compilePath:");
        for (String s : compilePath) {
            System.out.println(s);
        }
        System.out.println("##############");

//        try {
//            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
//            buildingRequest.setProject(project);
//            ArtifactFilter artifactFilter = createResolvingArtifactFilter();
//            // non-verbose mode use dependency graph component, which gives consistent results with Maven version
//            // running
//            DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifactFilter, reactorProjects);
//            //获取依赖树结构
//            System.out.println(JSONObject.toJSONString(rootNode));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


        if (sourceDirectory == null || !sourceDirectory.exists()) {
            throw new MojoFailureException("sourceDirectory error : " + sourceDirectory);
        }

        if (outputDirectory == null || !outputDirectory.exists()) {
            throw new MojoFailureException("outputDirectory error : " + outputDirectory);
        }

        if (target == null || !target.exists()) {
            throw new MojoFailureException("target error : " + target);
        }

        File file = null;
        if (StringUtils.isNotBlank(sourceDir)) {
            file = new File(sourceDir);
        }

        if (file == null || !file.exists()) {
            file = sourceDirectory;
        }

        List<File> sourceDirs = new ArrayList<>(16);
        sourceDirs.add(file);

        System.out.println("当前源码文件夹:" + file.getAbsolutePath());

        if (importSourceDirs != null) {
            System.out.println("引入外部源码文件夹:");
            for (File importSourceDir : importSourceDirs) {
                System.out.println(importSourceDir);
                if (importSourceDir.exists()) {
                    sourceDirs.add(importSourceDir);
                }
            }
        }

        JavaDoc javaDocVO = JavaDocReader.read(sourceDirs, compilePath);

        File docDir = new File(target.getAbsolutePath() + "/docs");
        docDir.mkdirs();

        File file1 = new File(target.getAbsolutePath() + "/docs/javadoc.json");
        FileUtil.writeFile(file1, JSON.toJSONString(javaDocVO.getClassDocs().values()));
//
//        File file2 = new File(target.getAbsolutePath() + "/docs/javadoc.js");
//        FileUtil.writeFile(file2, "var javadoc = " + json + ";");

        RestApiDoc restApiDoc = new RestApiBuilder()
                .analysisClassDocs(javaDocVO).build();


        File file3 = new File(target.getAbsolutePath() + "/docs/restApiData.js");
        FileUtil.writeFile(file3, "var restApiData = " + JSON.toJSONString(restApiDoc) + ";");


        copyHtml(docDir);
    }

    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;

        // filter scope
        if (scope != null) {
            getLog().debug("+ Resolving dependency tree for scope '" + scope + "'");

            filter = new ScopeArtifactFilter(scope);
        } else {
            filter = null;
        }

        return filter;
    }

    private void copyHtml(File dir) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:html.zip");
        if (resource.exists()) {
            try {
                ZipUtil.unzip(resource.getInputStream(), dir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("未找到 classpath:html.zip");
        }
    }
}

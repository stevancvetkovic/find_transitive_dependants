package tech.simpledevops.maven.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.lang.Exception;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Component;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.eclipse.aether.RepositorySystemSession;

@Mojo(name = "get-transitive-deps", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GetTransitiveDependentsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    @Component
    private ProjectBuilder mavenProjectBuilder;

    private List<Model> allModels = new ArrayList<>();
    private Collection<MavenProject> allMavenProjects = new HashSet<MavenProject>();

    private Model readPomFile(File basedir) throws Exception {
        Model model = new Model();
        MavenXpp3Reader reader = new MavenXpp3Reader();

        File pom = new File(basedir, "pom.xml");
        BufferedReader in = new BufferedReader(new FileReader(pom));
        model = reader.read(in);
        model.setPomFile(pom);

        return model;
    }

    private void recursivelyFindAllModules(File baseDir) throws Exception {
        Model pomFile = readPomFile(baseDir);
        allModels.add(pomFile);

        for (String module : pomFile.getModules()) {
            File modulePath = new File(baseDir, module);
            recursivelyFindAllModules(modulePath);
        }
    }

    private void findAllMavenProjects() throws Exception {
        recursivelyFindAllModules(project.getBasedir());
        for (Model module : allModels) allMavenProjects.add(getMavenProject(module.getPomFile()));
    }

    private MavenProject getMavenProject(File pomFile) throws ProjectBuildingException {
        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest().setRepositorySession(repoSession);
        ProjectBuildingResult build = mavenProjectBuilder.build(pomFile, projectBuildingRequest);

        return build.getProject();
    }

    private List<MavenProject> findTransitiveDependants(MavenProject myProject) throws Exception {
        ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(allMavenProjects);
        return graph.getDownstreamProjects(myProject, true);
    }

    private void printErrorMessage(Exception e) {
        System.out.println(e.getMessage());
    }

    public void execute() throws MojoExecutionException {
        try {
            findAllMavenProjects();

            for (MavenProject project : allMavenProjects) {
                System.out.println("Transitive dependants of " + project.getArtifactId());
                List<MavenProject> transitiveDependants = findTransitiveDependants(project);

                for (MavenProject dependentProject : transitiveDependants) System.out.println("\t" + dependentProject.getArtifactId());
                System.out.println("\n");
            }
        } catch (Exception e) {
            printErrorMessage(e);
        }
    }
}

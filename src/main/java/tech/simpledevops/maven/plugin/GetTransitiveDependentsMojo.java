package tech.simpledevops.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.graph.DefaultProjectDependencyGraph;

import java.io.File;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.LocalRepository;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;

@Mojo(name = "get-transitive-deps", requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GetTransitiveDependentsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Component
    private ProjectBuilder mavenProjectBuilder;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> projectRepos;

    @Parameter(defaultValue = "${project.remotePluginRepositories}")
    private List<RemoteRepository> pluginRepos;

    public List<Model> findAllModules(MavenProject baseProject) {
        List<Model> modelList = new ArrayList<>();
        recursiveFindModules(baseProject.getBasedir(), modelList);
        return modelList;
    }

    private void recursiveFindModules(File baseDir, List<Model> modelList) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        File pom = new File(baseDir, "pom.xml");

        Model model = new Model();

        try {
            BufferedReader in = new BufferedReader(new FileReader(pom));
            model = reader.read(in);
            model.setPomFile(pom);
            //System.out.println("Found maven module: " + pom.getParentFile().getAbsolutePath());
        } catch (Exception e) {
            //System.out.println("Failed to load POM: " + pom.getAbsolutePath());
        }

        modelList.add(model);

        Set<String> modulePaths = new LinkedHashSet<>();
        modulePaths.addAll(model.getModules());

        for (String modulePath : modulePaths) {
            File moduleDir = new File(baseDir, modulePath);
            recursiveFindModules(moduleDir, modelList);
        }
    }

    private MavenProject getMavenProject(File pomFile) throws ProjectBuildingException {
        DefaultRepositorySystemSession sessionRepo = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
        sessionRepo.setLocalRepositoryManager( repoSystem.newLocalRepositoryManager( sessionRepo, localRepo ) );
        repoSystem.newResolutionRepositories(sessionRepo, projectRepos);
        repoSession = sessionRepo;
        ProjectBuildingResult build = mavenProjectBuilder.build(pomFile, session.getProjectBuildingRequest().setRepositorySession(repoSession));
        return build.getProject();
    }

    public void execute() throws MojoExecutionException {
        Collection<MavenProject> allProjects = new HashSet<MavenProject>();

        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(new File(session.getCurrentProject().getBasedir() + "/pom.xml"));
        List<Model> mavenModels = findAllModules(mavenProject);

        for (Model module : mavenModels) {
            try {
                allProjects.add(getMavenProject(module.getPomFile()));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        try {
            ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(allProjects);

            for (MavenProject project : allProjects) {
                List<MavenProject> transitiveDeps = graph.getDownstreamProjects(project, true);

                System.out.println("Transitive dependants of " + project.getArtifactId());
                for (MavenProject childProj : transitiveDeps) {
                    System.out.println("\t" + childProj.getArtifactId());
                }
                System.out.println("\n");
            }
        } catch (Exception e) {}
    }
}

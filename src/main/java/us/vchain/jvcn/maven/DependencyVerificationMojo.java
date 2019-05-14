package us.vchain.jvcn.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import us.vchain.jvcn.JVCN;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_SOURCES;
import static us.vchain.jvcn.maven.MavenUtils.asBoolean;
import static us.vchain.jvcn.maven.MavenUtils.transitiveClosure;

@Mojo(name = "audit", defaultPhase = PROCESS_SOURCES)
public class DependencyVerificationMojo extends AbstractMojo {
    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "failOnError", defaultValue = "true")
    private String failOnError;

    @Parameter(property = "transitive", defaultValue = "false")
    private String transitive;

    @Parameter(property = "requiredSigner")
    private String requiredSigner;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            final JVCN jvcn = new JVCN.Builder().build();
            final ArtifactVerifier artifactVerifier = new ArtifactVerifier(getLog(), requiredSigner, jvcn);
            final ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
            final ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);
            buildingRequest.setProject(project);
            final DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
            final LinkedHashSet<DependencyNode> dependencyNodes = new LinkedHashSet<>(rootNode.getChildren());
            final Set<DependencyNode> dependencies = asBoolean(transitive) ? transitiveClosure(dependencyNodes) : dependencyNodes;
            final Long failures = artifactVerifier.verify(dependencies);
            if (failures > 0 && asBoolean(failOnError)) {
                throw new MojoExecutionException(failures + " dependencies could not be verified");
            }
        } catch (final DependencyGraphBuilderException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

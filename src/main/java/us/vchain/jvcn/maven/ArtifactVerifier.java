package us.vchain.jvcn.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import us.vchain.jvcn.Asset;
import us.vchain.jvcn.JVCN;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static us.vchain.jvcn.maven.MavenUtils.newSet;
import static us.vchain.jvcn.maven.MavenUtils.toLinkedMap;

class ArtifactVerifier {
    private final Log log;

    private final String requiredSigner;

    private final JVCN jvcn;

    ArtifactVerifier(final Log log, final String requiredSigner, final JVCN jvcn) {
        this.log = log;
        this.requiredSigner = requiredSigner;
        this.jvcn = jvcn;
    }

    Long verify(final Set<DependencyNode> dependencyNodes) {
        final Set<String> requiredSigners = getRequiredSigners();
        log.info("Verifying " + dependencyNodes.size() + " artifact dependencies" +
            (requiredSigners.isEmpty() ? "" : " signed by: " + requiredSigners));
        return verify(dependencyNodes, requiredSigners).entrySet().stream()
            .filter(entry -> !verifyAsset(entry.getKey(), entry.getValue()))
            .count();
    }

    private Set<String> getRequiredSigners() {
        return requiredSigner == null || requiredSigner.isEmpty()
            ? emptySet()
            : newSet(requiredSigner.split(",")).stream().map(String::trim).collect(toSet());
    }

    private Map<Artifact, Optional<Asset>> verify(final Set<DependencyNode> dependencyNodes,
                                                  final Set<String> requiredSigners) {
        return dependencyNodes.parallelStream()
            .collect(toLinkedMap(
                DependencyNode::getArtifact,
                dependencyNode -> verify(dependencyNode, requiredSigners)));
    }

    private Optional<Asset> verify(final DependencyNode dependencyNode,
                                   final Set<String> requiredSigners) {
        return requiredSigners.isEmpty()
            ? jvcn.verify(dependencyNode.getArtifact().getFile())
            : jvcn.verify(dependencyNode.getArtifact().getFile(), requiredSigners);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Boolean verifyAsset(final Artifact artifact, final Optional<Asset> asset) {
        if (asset.isPresent()) {
            final String publisher = asset.get().getPublisher() == null
                ? asset.get().getSigner()
                : asset.get().getPublisher();
            switch (asset.get().getStatus()) {
                case TRUSTED:
                    log.info(format("Dependency %s: %s:%s:%s - signed by: %s (%s)",
                        asset.get().getStatus().toString().toLowerCase(),
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        publisher,
                        asset.get().getLevel()));
                    return true;
                case UNTRUSTED: // fall-through
                case UNKNOWN: // fall-through
                case UNSUPPORTED:
                    log.warn(format("Dependency %s: %s:%s:%s - signed by: %s (%s)",
                        asset.get().getStatus().toString().toLowerCase(),
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        publisher,
                        asset.get().getLevel()));
                    return false;
                default:
                    throw new IllegalStateException("Unknown status: " + asset.get().getStatus());
            }
        } else {
            log.warn("Dependency unknown: "
                + artifact.getGroupId()
                + ":" + artifact.getArtifactId()
                + ":" + artifact.getVersion());
            return false;
        }
    }
}

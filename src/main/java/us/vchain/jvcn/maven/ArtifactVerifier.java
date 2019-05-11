package us.vchain.jvcn.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import us.vchain.jvcn.Asset;
import us.vchain.jvcn.JVCN;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static us.vchain.jvcn.maven.MavenUtils.toLinkedMap;

class ArtifactVerifier {
    private final Log log;

    private final String requiredSigner;

    private final JVCN jvcn;

    ArtifactVerifier(final Log log,
                     final String requiredSigner) {
        this.log = log;
        this.requiredSigner = requiredSigner;
        jvcn = new JVCN.Builder().build();
    }

    int verify(final Set<DependencyNode> dependencyNodes) {
        int failures = 0;
        log.info("Verifying " + dependencyNodes.size() + " artifact dependencies" +
            (requiredSigner == null || requiredSigner.isEmpty() ? "" : " signed by: " + requiredSigner));
        final Map<Artifact, Optional<Asset>> assetMap = verifyAll(dependencyNodes);
        for (final Entry<Artifact, Optional<Asset>> entry : assetMap.entrySet()) {
            final Artifact artifact = entry.getKey();
            final Optional<Asset> asset = entry.getValue();
            if (asset.isPresent()) {
                switch (asset.get().getStatus()) {
                    case TRUSTED:
                        log.info(format("Dependency trusted: %s:%s:%s - signed by: %s (%s)",
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion(),
                            asset.get().getSigner(),
                            asset.get().getLevel()));
                        break;
                    case UNTRUSTED: // fall-through
                    case UNKNOWN: // fall-through
                    case UNSUPPORTED:
                        log.warn(format("Dependency %s: %s:%s:%s - signed by: %s (%s)",
                            asset.get().getStatus().toString().toLowerCase(),
                            artifact.getGroupId(),
                            artifact.getArtifactId(),
                            artifact.getVersion(),
                            asset.get().getSigner(),
                            asset.get().getLevel()));
                        failures++;
                        break;
                    default:
                        throw new IllegalStateException("Unknown status: " + asset.get().getStatus());
                }
            } else {
                log.warn("Dependency unknown: "
                    + artifact.getGroupId()
                    + ":" + artifact.getArtifactId()
                    + ":" + artifact.getVersion());
                failures++;
            }
        }
        return failures;
    }

    private Map<Artifact, Optional<Asset>> verifyAll(final Set<DependencyNode> dependencyNodes) {
        return dependencyNodes.parallelStream()
            .collect(toLinkedMap(
                DependencyNode::getArtifact,
                d -> requiredSigner == null || requiredSigner.isEmpty()
                    ? jvcn.verify(d.getArtifact().getFile())
                    : jvcn.verify(d.getArtifact().getFile(), requiredSigner)));
    }
}

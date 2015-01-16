/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.cartographer.discover.post.patch;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.cartographer.discover.DiscoveryContextConstants.POM_VIEW_CTX_KEY;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributionPomPatcher
    implements DepgraphPatcher
{

    private static final String[] PATHS =
        {
            "/project[packaging/text()=\"pom\"]/build/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/executions/execution/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/executions/execution/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/build/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/profiles/profile/build/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/executions/execution/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/profiles/profile/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/executions/execution/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/profiles/profile/build/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/configuration[appendAssemblyId/text()=\"false\"]",
            "/project[packaging/text()=\"pom\"]/profiles/profile/build/pluginManagement/plugins/plugin[artifactId/text()=\"maven-assembly-plugin\"]/configuration[appendAssemblyId/text()=\"false\"]" };

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void patch( final DiscoveryResult orig, final List<? extends Location> locations,
                       final Map<String, Object> context )
    {
        final DiscoveryResult result = orig;
        final ProjectVersionRef ref = result.getSelectedRef();
        try
        {
            final MavenPomView pomView = (MavenPomView) context.get( POM_VIEW_CTX_KEY );

            // TODO: find a way to detect an assembly/distro pom, and turn deps from provided scope to compile scope.
            final String assemblyOnPomProjectPath = join( PATHS, "|" );

            if ( pomView.resolveXPathToNode( assemblyOnPomProjectPath, false ) == null )
            {
                return;
            }

            logger.debug( "Detected pom-packaging project with an assembly that produces artifacts without classifiers..."
                + "Need to flip provided-scope deps to compile scope here." );

            for ( final ProjectRelationship<?> rel : result.getAcceptedRelationships() )
            {
                if ( !rel.isManaged() && rel instanceof DependencyRelationship
                    && ( (DependencyRelationship) rel ).getScope() == DependencyScope.provided )
                {
                    // flip provided scope to compile scope...is this dangerous??
                    // if the project has packaging == pom and a series of assemblies, it's likely to be a distro project, and not meant for consumption via maven
                    // also, if something like type == zip is used for a dependency, IIRC transitive deps are not traversed during the build.
                    // so this SHOULD be safe.
                    final DependencyRelationship dep = (DependencyRelationship) rel;

                    logger.debug( "Fixing scope for: {}", dep );

                    result.removeDiscoveredRelationship( dep );
                    final Set<ProjectRef> excludes = dep.getExcludes();
                    final ProjectRef[] excludedRefs =
                        excludes == null ? new ProjectRef[0] : excludes.toArray( new ProjectRef[excludes.size()] );

                    final DependencyRelationship replacement =
                        new DependencyRelationship( dep.getSources(), dep.getPomLocation(), ref,
                                                    dep.getTargetArtifact(), DependencyScope.embedded, dep.getIndex(),
                                                    false, excludedRefs );

                    result.addDiscoveredRelationship( replacement );
                }
            }
        }
        catch ( final GalleyMavenException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
        catch ( final InvalidRefException e )
        {
            logger.error( String.format( "Failed to build/query MavenPomView for: %s from: %s. Reason: %s", ref,
                                         locations, e.getMessage() ), e );
        }
    }

    @Override
    public String getId()
    {
        return "dist-pom";
    }

}

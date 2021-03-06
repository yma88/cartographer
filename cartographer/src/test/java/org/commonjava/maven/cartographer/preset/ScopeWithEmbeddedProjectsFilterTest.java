/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.cartographer.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.DEPENDENCY;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;
import static org.commonjava.maven.atlas.ident.DependencyScope.compile;
import static org.commonjava.maven.atlas.ident.DependencyScope.embedded;
import static org.commonjava.maven.atlas.ident.DependencyScope.provided;
import static org.commonjava.maven.atlas.ident.DependencyScope.runtime;
import static org.commonjava.maven.atlas.ident.DependencyScope.test;
import static org.commonjava.cartographer.testutil.PresetAssertions.assertConcreteAcceptance;
import static org.commonjava.cartographer.testutil.PresetAssertions.assertRejectsAllManaged;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import org.commonjava.cartographer.graph.preset.ScopeWithEmbeddedProjectsFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.rel.*;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.junit.Before;
import org.junit.Test;

public class ScopeWithEmbeddedProjectsFilterTest
{

    private ScopeWithEmbeddedProjectsFilter filter;

    private URI from;

    private ProjectVersionRef src;

    private ProjectVersionRef root;

    private ArtifactRef tgt;

    @Before
    public void setup()
        throws Exception
    {
        filter = new ScopeWithEmbeddedProjectsFilter( DependencyScope.runtime, false );
        from = new URI( "test:source" );
        root = new SimpleProjectVersionRef( "group", "root", "1" );
        src = new SimpleProjectVersionRef( "group.id", "artifact-id", "1.0" );
        tgt = new SimpleArtifactRef( "other.group", "other-artifact", "2.0", "jar", null );
    }

    @Test
    public void initialInstanceAcceptsRuntimeAndCompileDependencies_AndBom_AndParent()
        throws Exception
    {
        assertConcreteAcceptance( filter, from, src, tgt,
                                  new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ),
                                  DEPENDENCY, PARENT, BOM );
    }

    @Test
    public void initialInstanceRejectsAllManagedRelationships()
    {
        assertRejectsAllManaged( filter, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTraversingPlugin()
        throws Exception
    {
        final PluginRelationship plugin = new SimplePluginRelationship( from, root, src, 0, false, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( plugin );
        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTraversingExtension()
        throws Exception
    {
        final ExtensionRelationship plugin = new SimpleExtensionRelationship( from, root, src, 0, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( plugin );
        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>() );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterTestDependency()
        throws Exception
    {
        final DependencyRelationship dep =
            new SimpleDependencyRelationship( from, root, new SimpleArtifactRef( src, "jar", null ), test, 0, false, false, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>(), BOM, PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptNothingAfterProvidedDependency()
        throws Exception
    {
        final DependencyRelationship dep =
            new SimpleDependencyRelationship( from, root, new SimpleArtifactRef( src, "jar", null ), provided, 0, false, false, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt, new HashSet<DependencyScope>(), BOM, PARENT );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAndBomsAfterTraversingRuntimeDependency()
        throws Exception
    {
        final DependencyRelationship dep =
            new SimpleDependencyRelationship( from, root, new SimpleArtifactRef( src, "jar", null ), runtime, 0, false, false, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt,
                                  new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ),
                                  DEPENDENCY, PARENT, BOM );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAndBomsAfterTraversingCompileDependency()
        throws Exception
    {
        final DependencyRelationship dep =
            new SimpleDependencyRelationship( from, root, new SimpleArtifactRef( src, "jar", null ), compile, 0, false, false, false );

        final ProjectRelationshipFilter child = filter.getChildFilter( dep );

        assertConcreteAcceptance( child, from, src, tgt,
                                  new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ),
                                  DEPENDENCY, PARENT, BOM );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptAllEmbeddedAndRuntimeAndCompileDependenciesWithParentsAndBomsAfterTraversingParent()
        throws Exception
    {
        final ParentRelationship parent = new SimpleParentRelationship( from, src, root );

        final ProjectRelationshipFilter child = filter.getChildFilter( parent );

        assertConcreteAcceptance( child, from, src, tgt,
                                  new HashSet<DependencyScope>( Arrays.asList( embedded, runtime, compile ) ),
                                  DEPENDENCY, PARENT, BOM );

        assertRejectsAllManaged( child, from, src, tgt );
    }

    @Test
    public void acceptParentAndBomAfterTraversingParent()
        throws Exception
    {
        final ProjectVersionRef parentRef = new SimpleProjectVersionRef( "group.id", "intermediate-parent", "2" );
        final ParentRelationship parent = new SimpleParentRelationship( from, src, parentRef );

        final ProjectRelationshipFilter child = filter.getChildFilter( parent );

        final ParentRelationship gparent = new SimpleParentRelationship( from, parentRef, root );
        assertThat( child.accept( gparent ), equalTo( true ) );

        final BomRelationship bom = new SimpleBomRelationship( from, parentRef, root, 0, false, false );
        assertThat( child.accept( bom ), equalTo( true ) );
    }

}

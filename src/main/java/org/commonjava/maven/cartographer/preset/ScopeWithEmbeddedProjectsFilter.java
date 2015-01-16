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
package org.commonjava.maven.cartographer.preset;

import static org.commonjava.maven.atlas.graph.rel.RelationshipType.BOM;
import static org.commonjava.maven.atlas.graph.rel.RelationshipType.PARENT;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.maven.atlas.graph.filter.DependencyFilter;
import org.commonjava.maven.atlas.graph.filter.NoneFilter;
import org.commonjava.maven.atlas.graph.filter.OrFilter;
import org.commonjava.maven.atlas.graph.filter.ProjectRelationshipFilter;
import org.commonjava.maven.atlas.graph.filter.StructuralRelationshipsFilter;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ScopeTransitivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopeWithEmbeddedProjectsFilter
    implements ProjectRelationshipFilter
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final long serialVersionUID = 1L;

    private static final boolean DEFAULT_CHILD_ACCEPTBOMS = false;

    private static final boolean DEFAULT_CHILD_ACCEPTMANAGED = false;

    private final ProjectRelationshipFilter filter;

    private final boolean acceptBOMs;

    private final boolean acceptManaged;

    private DependencyScope scope;

    private transient String longId;

    private transient String shortId;

    public ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final boolean acceptManaged )
    {
        this.acceptBOMs = true;
        this.scope = scope == null ? DependencyScope.runtime : scope;
        this.acceptManaged = acceptManaged;
        this.filter =
            new OrFilter( new DependencyFilter( this.scope, ScopeTransitivity.maven, false, true, true, null ),
                          new DependencyFilter( DependencyScope.embedded, ScopeTransitivity.maven, false, true, true,
                                                null ) );
    }

    private ScopeWithEmbeddedProjectsFilter( final DependencyScope scope, final ProjectRelationshipFilter childFilter )
    {
        this.acceptBOMs = DEFAULT_CHILD_ACCEPTBOMS;
        this.acceptManaged = DEFAULT_CHILD_ACCEPTMANAGED;
        this.filter =
            childFilter == null ? new OrFilter( new DependencyFilter( scope, ScopeTransitivity.maven, false, true,
                                                                      true, null ),
                                                new DependencyFilter( DependencyScope.embedded,
                                                                      ScopeTransitivity.maven, false, true, true, null ) )
                            : childFilter;
    }

    @Override
    public boolean accept( final ProjectRelationship<?> rel )
    {
        boolean result;
        if ( rel.getType() == PARENT )
        {
            result = true;
        }
        else if ( rel.getType() == BOM )
        {
            result = acceptBOMs;
        }
        else if ( !acceptManaged && rel.isManaged() )
        {
            result = false;
        }
        else
        {
            result = filter.accept( rel );
        }

        logger.debug( "{} {}", ( result ? "IN" : "OUT" ), rel );

        //        if ( !result )
        //        {
        //            logger.info( "[FILT-STOP]: {}", rel );
        //        }

        return result;
    }

    @Override
    public ProjectRelationshipFilter getChildFilter( final ProjectRelationship<?> lastRelationship )
    {
        switch ( lastRelationship.getType() )
        {
            case BOM:
                return StructuralRelationshipsFilter.INSTANCE;
            case PARENT:
            {
                return this;
            }
            case EXTENSION:
            case PLUGIN:
            case PLUGIN_DEP:
            {
                logger.debug( "[FILT-OFFx1]: {}", lastRelationship );
                return NoneFilter.INSTANCE;
                //                if ( filter == NoneFilter.INSTANCE )
                //                {
                //                    return this;
                //                }
                //
                //                //                logger.info( "getChildFilter({})", lastRelationship );
                //                return new ScopeWithEmbeddedProjectsFilter( scope, NoneFilter.INSTANCE );
            }
            default:
            {
                //                logger.info( "getChildFilter({})", lastRelationship );

                final DependencyRelationship dr = (DependencyRelationship) lastRelationship;
                if ( DependencyScope.test == dr.getScope() || DependencyScope.provided == dr.getScope() )
                {
                    if ( acceptBOMs == DEFAULT_CHILD_ACCEPTBOMS && acceptManaged == DEFAULT_CHILD_ACCEPTMANAGED
                            && filter == NoneFilter.INSTANCE )
                    {
                        return this;
                    }

                    logger.debug( "[FILT-OFFx2]: {}", lastRelationship );
                    return new ScopeWithEmbeddedProjectsFilter( scope, NoneFilter.INSTANCE );
                }

                final ProjectRelationshipFilter nextFilter = filter.getChildFilter( lastRelationship );
                if ( acceptBOMs == DEFAULT_CHILD_ACCEPTBOMS && acceptManaged == DEFAULT_CHILD_ACCEPTMANAGED
                        && nextFilter == filter )
                {
                    return this;
                }

                return new ScopeWithEmbeddedProjectsFilter( scope, nextFilter );
            }
        }
    }

    @Override
    public String getLongId()
    {
        if ( longId == null )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "Scope-Plus-Embedded(sub-filter:" );

            if ( filter == null )
            {
                sb.append( "none" );
            }
            else
            {
                sb.append( filter.getLongId() );
            }

            sb.append( ",acceptManaged:" )
              .append( acceptManaged )
              .append( ",acceptBOMs:" )
              .append( acceptBOMs )
              .append( ")" );

            longId = sb.toString();
        }

        return longId;
    }

    @Override
    public String toString()
    {
        return getLongId();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( acceptBOMs ? 727 : 733 );
        result = prime * result + ( acceptManaged ? 1231 : 1237 );
        result = prime * result + filter.hashCode();
        result = prime * result + ( ( scope == null ) ? 0 : scope.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ScopeWithEmbeddedProjectsFilter other = (ScopeWithEmbeddedProjectsFilter) obj;
        if ( acceptBOMs != other.acceptBOMs )
        {
            return false;
        }
        if ( acceptManaged != other.acceptManaged )
        {
            return false;
        }
        if ( !filter.equals( other.filter ) )
        {
            return false;
        }
        if ( scope != other.scope )
        {
            return false;
        }
        return true;
    }

    @Override
    public String getCondensedId()
    {
        if ( shortId == null )
        {
            shortId = DigestUtils.shaHex( getLongId() );
        }

        return shortId;
    }

    @Override
    public boolean includeManagedRelationships()
    {
        return acceptManaged;
    }

    @Override
    public boolean includeConcreteRelationships()
    {
        return true;
    }

    @Override
    public Set<RelationshipType> getAllowedTypes()
    {
        final Set<RelationshipType> types = new HashSet<>();
        types.add( PARENT );
        if ( acceptBOMs )
        {
            types.add( BOM );
        }

        types.addAll( filter.getAllowedTypes() );

        return types;
    }

}

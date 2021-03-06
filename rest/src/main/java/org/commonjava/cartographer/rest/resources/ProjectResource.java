/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.rest.resources;

import org.commonjava.cartographer.request.ProjectGraphRelationshipsRequest;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.rest.CartoRESTException;
import org.commonjava.cartographer.rest.ctl.ProjectController;
import org.commonjava.cartographer.result.MappedProjectRelationshipsResult;
import org.commonjava.cartographer.result.MappedProjectResult;
import org.commonjava.cartographer.result.ProjectListResult;
import org.commonjava.propulsor.deploy.resteasy.RestResources;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static org.commonjava.cartographer.rest.util.ResponseUtils.throwError;
import static org.commonjava.propulsor.deploy.undertow.util.StandardApplicationContent.*;

@Path( "/api/depgraph/project" )
@Consumes( { application_json } )
@Produces( { application_json } )
public class ProjectResource
        implements RestResources
{
    @Inject
    private ProjectController controller;

    @Path( "/list" )
    @POST
    public ProjectListResult list( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.list( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        // not used; throwError will supercede with WebApplicationException
        return null;
    }

    @Path( "/parents" )
    @POST
    public MappedProjectResult parentOf( final ProjectGraphRequest recipe )
    {
        try
        {
            return controller.parentOf( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/relationships" )
    @POST
    public MappedProjectRelationshipsResult dependenciesOf( final ProjectGraphRelationshipsRequest recipe )
    {
        try
        {
            return controller.relationshipsDeclaredBy( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/targeting" )
    @POST
    public MappedProjectRelationshipsResult relationshipsTargeting( final ProjectGraphRelationshipsRequest recipe )
    {
        try
        {
            return controller.relationshipsTargeting( recipe );
        }
        catch ( final CartoRESTException e )
        {
            throwError( e );
        }

        return null;
    }

}

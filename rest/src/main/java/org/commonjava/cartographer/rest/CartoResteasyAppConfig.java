/**
 * Copyright (C) 2016 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.cartographer.rest;

import org.commonjava.propulsor.deploy.resteasy.ResteasyAppConfig;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;

import static java.util.Arrays.asList;

@ApplicationScoped
public class CartoResteasyAppConfig
        implements ResteasyAppConfig
{

    @Override
    public List<String> getJaxRsMappings()
    {
        // Just map everything for now...until we have some static content to serve that needs the UI servlet from the
        // propulsor-undertow module.
        return asList( "/*" );
    }
}

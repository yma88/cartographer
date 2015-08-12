package org.commonjava.maven.cartographer.ftest;

import org.apache.maven.model.Model;
import org.commonjava.maven.cartographer.request.PomRequest;
import org.junit.Test;

/**
 * TCK test class checking that a plugin dependency declared in a project is included when running generatečPOM()
 * method. The dependency graph looks like this:
 * <pre>
 *   +----------+
 *   | consumer |----+
 *   +----------+    |
 *        |          |
 *        | uses     |declares plugin dependency
 *        V          |
 *   +----------+    |    +-------+
 *   |  plugin  |----+--->|  dep  |
 *   +----------+         +-------+
 * </pre>
 *
 * The {@code consumer} is used as the request root artifact. Used preset is "build-requires", which results in usage of
 * {@link org.commonjava.maven.cartographer.preset.BuildRequirementProjectsFilter}. Consumer pom, plugin's maven-plugin
 * and dep jar are expected to be in the result.
 */
public class PluginDepDownloadTest
    extends AbstractCartographerTCK
{

    private static final String PROJECT = "plugin-dep";

    @Test
    public void run()
        throws Exception
    {
        final String dto = "pom.json";
        final String depsTxt = "deps.txt";
        final String repoResource = "/repo/org/foo/consumer/1/consumer-1.pom";
        final int repoResourceTrim = 5;
        final String alias = "test";

        aliasRepo( alias, repoResource, repoResourceTrim );

        final PomRequest recipe = readRecipe( dto, PomRequest.class );

        final Model pom = carto.getRenderer()
                               .generatePOM( recipe );

        assertPomDeps( pom, false, depsTxt );
    }

    @Override
    protected String getTestDir()
    {
        return PROJECT;
    }

}
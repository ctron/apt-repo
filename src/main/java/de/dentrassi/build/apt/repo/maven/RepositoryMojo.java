/*
 * Copyright 2014 Jens Reimann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.dentrassi.build.apt.repo.maven;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import de.dentrassi.build.apt.repo.AptWriter;
import de.dentrassi.build.apt.repo.Component;
import de.dentrassi.build.apt.repo.Configuration;
import de.dentrassi.build.apt.repo.Distribution;

/**
 * Create an APT repository structure.
 * <p>
 * It takes all <code>.deb</code> files and creates one APT repository out of
 * it.
 * </p>
 * <p>
 * At the moment the plugin itself can only create a repository with one
 * distribution and one component.
 * </p>
 * 
 * @author Jens Reimann
 */
@Mojo ( name = "apt", requiresProject = false, threadSafe = false )
public class RepositoryMojo extends AbstractMojo
{

    /**
     * The source directory
     */
    @Parameter ( required = true )
    private File sourceDirectory;

    /**
     * The output directory
     */
    @Parameter ( required = true, defaultValue = "${project.build.directory}/apt" )
    private File outputDirectory;

    /**
     * The supported architectures
     * <p>
     * File that are
     * <q>all</q> will be registered with all these architectures.
     * </p>
     */
    @Parameter ( required = false )
    private Set<String> architectures = new HashSet<String> ( Arrays.asList ( "i386", "amd64" ) );

    /**
     * The name of the distribution
     */
    @Parameter ( required = true, defaultValue = "devel" )
    private String distributionName;

    /**
     * The label of the distribution
     */
    @Parameter
    private String distributionLabel;

    /**
     * The name of the component
     */
    @Parameter ( required = true, defaultValue = "main" )
    private String componentName;

    /**
     * The label of the component
     */
    @Parameter
    private String componentLabel;

    /**
     * The origin of the repository
     */
    @Parameter
    private String origin;

    /**
     * The description of the repository
     */
    @Parameter ( defaultValue = "${project.description}" )
    private String description;

    public void setArchitectures ( final Set<String> architectures )
    {
        this.architectures = architectures;
    }

    @Override
    public void execute () throws MojoExecutionException, MojoFailureException
    {
        final Configuration configuration = new Configuration ();

        configuration.setSourceFolder ( this.sourceDirectory );
        configuration.setTargetFolder ( this.outputDirectory );
        configuration.setArchitectures ( this.architectures );

        final Distribution dist = new Distribution ();
        dist.setName ( this.distributionName );
        dist.setOrigin ( this.origin );
        dist.setLabel ( this.distributionLabel );
        dist.setDescription ( this.description );

        final Component comp = new Component ();
        comp.setName ( this.componentName );
        comp.setLabel ( this.componentLabel );
        dist.addComponent ( comp );

        configuration.addDistribution ( dist );

        final AptWriter writer = new AptWriter ( configuration, new MojoConsole ( getLog () ) );
        try
        {
            writer.build ();
        }
        catch ( final Exception e )
        {
            throw new MojoExecutionException ( "Failed to create APT repository", e );
        }
    }

}

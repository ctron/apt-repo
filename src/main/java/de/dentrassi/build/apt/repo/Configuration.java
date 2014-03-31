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
package de.dentrassi.build.apt.repo;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An APT repository configuration
 * 
 * @author Jens Reimann
 */
public class Configuration
{
    private File sourceFolder;

    private File targetFolder;

    private final Set<Distribution> distributions = new HashSet<Distribution> ();

    private Set<String> architectures = new HashSet<String> ( Arrays.asList ( "i386", "amd64" ) );

    public Configuration ()
    {
    }

    public Configuration ( final Configuration other )
    {
        this.sourceFolder = other.sourceFolder;
        this.targetFolder = other.targetFolder;
        for ( final Distribution dist : other.distributions )
        {
            this.distributions.add ( new Distribution ( dist ) );
        }
        this.architectures = new HashSet<String> ( other.architectures );
    }

    public void validate () throws IllegalStateException
    {
        if ( this.architectures == null || this.architectures.isEmpty () )
        {
            throw new IllegalStateException ( "Architectures must be set" );
        }

        for ( final String arch : this.architectures )
        {
            Names.validate ( "architecture", arch );
        }
    }

    @Override
    protected Configuration clone ()
    {
        return new Configuration ( this );
    }

    public File getSourceFolder ()
    {
        return this.sourceFolder;
    }

    public void setSourceFolder ( final File sourceFolder )
    {
        this.sourceFolder = sourceFolder;
    }

    public File getTargetFolder ()
    {
        return this.targetFolder;
    }

    public void setTargetFolder ( final File targetFolder )
    {
        this.targetFolder = targetFolder;
    }

    public void setArchitectures ( final Set<String> architectures )
    {
        this.architectures = architectures;
    }

    public Set<String> getArchitectures ()
    {
        return this.architectures;
    }

    public Set<Distribution> getDistributions ()
    {
        return this.distributions;
    }

    /**
     * Add a new component to the distribution. <br/>
     * The component is copied and cannot be altered after adding
     * 
     * @param dist
     *            distribution to add
     */
    public void addDistribution ( final Distribution dist )
    {
        this.distributions.add ( new Distribution ( dist ) );
    }
}

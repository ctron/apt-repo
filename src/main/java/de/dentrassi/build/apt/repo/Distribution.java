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

import java.util.HashSet;
import java.util.Set;

/**
 * An APT distribution description
 * 
 * @author Jens Reimann
 */
public class Distribution
{

    private String name = "devel";

    private String label = "Development";

    private String origin = "Unknown";

    private String description;

    private final Set<Component> components = new HashSet<Component> ();

    public Distribution ()
    {
    }

    public Distribution ( final Distribution other )
    {
        this.name = other.name;
        this.label = other.label;
        this.origin = other.origin;
        this.description = other.description;
        for ( final Component comp : other.components )
        {
            addComponent ( comp );
        }
    }

    public void setDescription ( final String description )
    {
        this.description = description;
    }

    public String getDescription ()
    {
        return this.description;
    }

    public void setOrigin ( final String origin )
    {
        this.origin = origin;
    }

    public String getOrigin ()
    {
        return this.origin;
    }

    public void setLabel ( final String label )
    {
        this.label = label;
    }

    public String getLabel ()
    {
        return this.label;
    }

    public void setName ( final String name )
    {
        Names.validate ( "name", name );
        this.name = name;
    }

    public String getName ()
    {
        return this.name;
    }

    public Set<Component> getComponents ()
    {
        return this.components;
    }

    /**
     * Add a new component to the distribution. <br/>
     * The component is copied and cannot be altered after adding
     * 
     * @param component
     *            the component to add
     */
    public void addComponent ( final Component component )
    {
        final Component newComp = new Component ( component );
        newComp.setDistribution ( this );
        this.components.add ( newComp );
    }

    @Override
    public String toString ()
    {
        return this.name;
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( this.name == null ? 0 : this.name.hashCode () );
        return result;
    }

    @Override
    public boolean equals ( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass () != obj.getClass () )
        {
            return false;
        }
        final Distribution other = (Distribution)obj;
        if ( this.name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !this.name.equals ( other.name ) )
        {
            return false;
        }
        return true;
    }

}

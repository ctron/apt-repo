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

/**
 * An APT repository component
 * 
 * @author Jens Reimann
 */
public class Component
{
    private String name = "main";

    private String label = "Main component";

    private Distribution distribution;

    public Component ()
    {
    }

    public Component ( final Component other )
    {
        this.name = other.name;
        this.label = other.label;
    }

    public void setLabel ( final String label )
    {
        this.label = label;
    }

    public String getLabel ()
    {
        return this.label;
    }

    void setDistribution ( final Distribution distribution )
    {
        this.distribution = distribution;
    }

    public Distribution getDistribution ()
    {
        return this.distribution;
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
        result = prime * result + ( this.distribution == null ? 0 : this.distribution.hashCode () );
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
        final Component other = (Component)obj;
        if ( this.distribution == null )
        {
            if ( other.distribution != null )
            {
                return false;
            }
        }
        else if ( !this.distribution.equals ( other.distribution ) )
        {
            return false;
        }
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

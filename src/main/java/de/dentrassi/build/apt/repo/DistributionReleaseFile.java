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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.vafer.jdeb.debian.ControlField;
import org.vafer.jdeb.debian.ControlFile;

/**
 * Release file
 * 
 * @author Jens Reimann
 */
public final class DistributionReleaseFile extends ControlFile
{

    private static final ControlField[] FIELDS = {
            new ControlField ( "Origin" ),
            new ControlField ( "Label" ),
            new ControlField ( "Codename", true ),
            new ControlField ( "Date", true ),
            new ControlField ( "Components", true ),
            new ControlField ( "Description", true, ControlField.Type.MULTILINE ),
            new ControlField ( "MD5Sum", true, ControlField.Type.MULTILINE ),
            new ControlField ( "SHA1", true, ControlField.Type.MULTILINE ),
            new ControlField ( "SHA256", true, ControlField.Type.MULTILINE )
    };

    public DistributionReleaseFile ()
    {
    }

    public DistributionReleaseFile ( final String input ) throws IOException, ParseException
    {
        parse ( input );
    }

    public DistributionReleaseFile ( final InputStream input ) throws IOException, ParseException
    {
        parse ( input );
    }

    @Override
    protected ControlField[] getFields ()
    {
        return FIELDS;
    }

    @Override
    protected char getUserDefinedFieldLetter ()
    {
        return 'R';
    }
}

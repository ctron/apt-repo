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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.vafer.jdeb.Console;
import org.vafer.jdeb.debian.BinaryPackageControlFile;

/**
 * An APT repository writer
 * <p>
 * This class takes all files from the source directory and converts it to an
 * APT repository in another directory. The target directory should be empty or
 * not existing since it will overwrite everything with the name state from the
 * source directory.
 * </p>
 * <p>
 * Here is what this class can do:
 * <ul>
 * <li>Copy all source files to a "pool"</li>
 * <li>Extract the metadata and write Packages files</li>
 * <li>Create Release files for components and distributions</li>
 * <li>Create checksum for all files</li>
 * </ul>
 * </p>
 * <p>
 * At the moment this class is still missing some functionality:
 * <ul>
 * <li>Signing is not implemented</li>
 * <li>Compression of index files is not implemented</li>
 * <li>And maybe a few other things</li>
 * </ul>
 * </p>
 * 
 * @author Jens Reimann
 */
public class AptWriter
{

    private final Configuration configuration;

    private File pool;

    private File dists;

    private interface Digester
    {
        public MessageDigest create ();

        public String getName ();
    }

    private static class SimpleDigester implements Digester
    {

        private final String name;

        private final String javaName;

        public SimpleDigester ( final String name, String javaName )
        {
            this.name = name;
            this.javaName = javaName;
        }

        @Override
        public String getName ()
        {
            return this.name;
        }

        @Override
        public MessageDigest create ()
        {
            try
            {
                return MessageDigest.getInstance ( this.javaName );
            }
            catch ( final Exception e )
            {
                throw new RuntimeException ( e );
            }
        }

    }

    private final List<Digester> digestersRelease = new LinkedList<AptWriter.Digester> ();

    private final List<Digester> digestersPackage = new LinkedList<AptWriter.Digester> ();

    private static final DateFormat DF;

    private final Map<Component, Map<String, List<BinaryPackagePackagesFile>>> files = new HashMap<Component, Map<String, List<BinaryPackagePackagesFile>>> ();

    private final Console console;

    static
    {
        DF = new SimpleDateFormat ( "EEE, dd MMM YYYY HH:mm:ss z", Locale.US );
        DF.setTimeZone ( TimeZone.getTimeZone ( "UTC" ) );
    }

    public AptWriter ( final Configuration configuration, final Console console )
    {
        this.console = console;
        this.configuration = configuration.clone ();

        this.digestersRelease.add ( new SimpleDigester ( "MD5Sum", "MD5" ) );
        this.digestersRelease.add ( new SimpleDigester ( "SHA1", "SHA-1" ) );
        this.digestersRelease.add ( new SimpleDigester ( "SHA256", "SHA-256" ) );

        this.digestersPackage.add ( new SimpleDigester ( "MD5sum", "MD5" ) ); // yes, this is really the difference
        this.digestersPackage.add ( new SimpleDigester ( "SHA1", "SHA-1" ) );
        this.digestersPackage.add ( new SimpleDigester ( "SHA256", "SHA-256" ) );
    }

    public void build () throws Exception
    {
        if ( this.configuration.getTargetFolder ().exists () )
        {
            throw new IllegalStateException ( "The target path must not exists: " + this.configuration.getTargetFolder () );
        }

        if ( !this.configuration.getSourceFolder ().isDirectory () )
        {
            throw new IllegalStateException ( "The source path must exists and must be a directory: " + this.configuration.getTargetFolder () );
        }

        this.configuration.validate ();

        this.configuration.getTargetFolder ().mkdirs ();

        this.pool = new File ( this.configuration.getTargetFolder (), "pool" );
        this.dists = new File ( this.configuration.getTargetFolder (), "dists" );

        this.pool.mkdirs ();
        this.dists.mkdirs ();

        final FileFilter debFilter = new AndFileFilter ( //
        Arrays.asList ( //
                CanReadFileFilter.CAN_READ, //
                FileFileFilter.INSTANCE, //
                new SuffixFileFilter ( ".deb" ) //
            ) //
        );

        for ( final File packageFile : this.configuration.getSourceFolder ().listFiles ( debFilter ) )
        {
            processPackageFile ( packageFile );
        }

        writePackageLists ();
    }

    private void writePackageLists () throws IOException
    {
        for ( final Distribution dist : this.configuration.getDistributions () )
        {
            for ( final Component comp : dist.getComponents () )
            {
                final Map<String, List<BinaryPackagePackagesFile>> fileList = this.files.get ( comp );
                for ( final Map.Entry<String, List<BinaryPackagePackagesFile>> entry : fileList.entrySet () )
                {
                    writePackageList ( dist, comp, entry.getKey (), entry.getValue () );
                }
            }
            writeRelease ( dist );
        }
    }

    private void writeRelease ( final Distribution dist ) throws IOException
    {
        final File dir = new File ( this.dists, dist.getName () );

        final DistributionReleaseFile rf = new DistributionReleaseFile ();
        rf.set ( "Codename", dist.getName () );
        rf.set ( "Origin", dist.getOrigin () );
        rf.set ( "Label", dist.getLabel () );
        rf.set ( "Description", dist.getDescription () );
        rf.set ( "Components", join ( dist.getComponents () ) );
        rf.set ( "Architectures", join ( this.configuration.getArchitectures () ) );
        rf.set ( "Date", DF.format ( new Date () ) );

        for ( final Digester d : this.digestersRelease )
        {
            rf.set ( d.getName (), digestPackageLists ( rf, d, dist ) );
        }

        try ( FileOutputStream os = new FileOutputStream ( new File ( dir, "Release" ) ) )
        {
            os.write(rf.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private String digestPackageLists ( final DistributionReleaseFile rf, final Digester d, final Distribution dist ) throws IOException
    {

        final StringWriter sw = new StringWriter ();
        final PrintWriter pw = new PrintWriter ( sw );

        final File distDir = new File ( this.dists, dist.getName () ).getCanonicalFile ();

        pw.println (); // start with a newline

        for ( final Component comp : dist.getComponents () )
        {
            for ( final String arch : this.configuration.getArchitectures () )
            {
                File dir = new File ( this.dists, dist.getName () );
                dir = new File ( dir, comp.getName () );
                dir = new File ( dir, "binary-" + arch );

                digestPackageList ( pw, d, distDir, new File ( dir, "Packages" ).getCanonicalFile () );
                digestPackageList ( pw, d, distDir, new File ( dir, "Packages.gz" ).getCanonicalFile () );
                digestPackageList ( pw, d, distDir, new File ( dir, "Release" ).getCanonicalFile () );
            }
        }

        pw.close ();

        return sw.toString ();
    }

    private void digestPackageList ( final PrintWriter pw, final Digester d, final File distDir, final File file ) throws IOException
    {
        if ( !file.exists () )
        {
            return;
        }

        final String relativeDir = file.getAbsolutePath ().substring ( distDir.getAbsolutePath ().length () + 1 ); // +1 for the leading/trailing slash

        final long size = file.length ();
        pw.format ( " %s %20s %s", digest ( file, d.create () ), size, relativeDir );
        pw.println ();
    }

    private String join ( final Collection<?> items )
    {
        if ( items == null )
        {
            return null;
        }

        final StringBuilder sb = new StringBuilder ();

        boolean first = true;
        for ( final Object item : items )
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                sb.append ( ' ' );
            }
            sb.append ( item );
        }

        return sb.toString ();
    }

    private void writePackageList ( final Distribution distribution, final Component component, final String architecture, final List<BinaryPackagePackagesFile> files ) throws IOException
    {
        File dir = new File ( this.dists, distribution.getName () );

        dir = new File ( dir, component.getName () );
        dir = new File ( dir, "binary-" + architecture );
        dir.mkdirs ();

        // Packages

        final File packagesFile = new File ( dir, "Packages" );

        this.console.info ( "Writing: " + packagesFile );

        try ( final PrintStream ps1 = new PrintStream ( packagesFile ) )
        {
            for ( final BinaryPackagePackagesFile cf : files )
            {
                ps1.println ( cf.toString () );
            }
        }

        compressFile ( packagesFile );

        // Release

        final File releaseFile = new File ( dir, "Release" );

        this.console.info ( "Writing: " + releaseFile );

        final ComponentReleaseFile crf = new ComponentReleaseFile ();
        crf.set ( "Component", component.getName () );
        crf.set ( "Architecture", architecture );
        crf.set ( "Label", component.getLabel () );
        crf.set ( "Origin", component.getDistribution ().getOrigin () );

        try ( final FileOutputStream os = new FileOutputStream ( releaseFile ) )
        {
            os.write ( crf.toString ().getBytes ( "UTF-8" ) );
        }
    }

    private void compressFile ( final File packagesFile ) throws IOException
    {
        this.console.debug ( "Compressing: " + packagesFile );

        final File compressedFile = new File ( packagesFile.getAbsolutePath () + ".gz" );
        try ( final OutputStream os = new GZIPOutputStream ( new FileOutputStream ( compressedFile ) ) )
        {
            try ( final InputStream is = new FileInputStream ( packagesFile ) )
            {
                IOUtils.copy ( is, os );
            }
        }
    }

    protected void processPackageFile ( final File packageFile ) throws Exception
    {
        final BinaryPackagePackagesFile cf = readArtifact ( packageFile );

        final Component component = findComponent ( cf );
        if ( component == null )
        {
            return; // skip
        }

        this.console.debug ( "Processing: " + cf );

        copyArtifact ( component, packageFile, cf );

        final String arch = cf.get ( "Architecture" );
        if ( "all".equals ( arch ) )
        {
            for ( final String ae : this.configuration.getArchitectures () )
            {
                registerPackage ( component, ae, cf );
            }
        }
        else
        {
            if ( this.configuration.getArchitectures ().contains ( arch ) )
            {
                registerPackage ( component, arch, cf );
            }
        }
    }

    /**
     * Get the component that this package is assigned to
     * <p>
     * Note: This method is called twice at the moment. It must return the same
     * result for the same package data.
     * </p>
     * 
     * @param cf
     *            the package file data, may be <code>null</code>
     * @return the component or <code>null</code> if the package should be
     *         ignored
     */
    protected Component findComponent ( final BinaryPackagePackagesFile cf )
    {
        if ( cf == null )
        {
            return null;
        }

        // at the moment we allow only one distribution and one component
        // you may override this behavior right here
        return this.configuration.getDistributions ().iterator ().next ().getComponents ().iterator ().next ();
    }

    private void registerPackage ( final Component component, final String architecture, final BinaryPackagePackagesFile cf )
    {
        Map<String, List<BinaryPackagePackagesFile>> fileList = this.files.get ( component );
        if ( fileList == null )
        {
            fileList = new HashMap<String, List<BinaryPackagePackagesFile>> ();
            this.files.put ( component, fileList );
        }

        List<BinaryPackagePackagesFile> arch = fileList.get ( architecture );
        if ( arch == null )
        {
            arch = new LinkedList<BinaryPackagePackagesFile> ();
            fileList.put ( architecture, arch );
        }
        arch.add ( cf );
    }

    private BinaryPackagePackagesFile readArtifact ( final File packageFile ) throws Exception
    {
        try ( final ArArchiveInputStream in = new ArArchiveInputStream ( new FileInputStream ( packageFile ) ) )
        {
            ArchiveEntry ar;
            while ( ( ar = in.getNextEntry () ) != null )
            {
                if ( !ar.getName ().equals ( "control.tar.gz" ) )
                {
                    continue;
                }

                try ( final TarArchiveInputStream inputStream = new TarArchiveInputStream ( new GZIPInputStream ( in ) ) )
                {

                    TarArchiveEntry te;
                    while ( ( te = inputStream.getNextTarEntry () ) != null )
                    {
                        if ( !te.getName ().equals ( "./control" ) )
                        {
                            continue;
                        }
                        return convert ( new BinaryPackageControlFile ( inputStream ), packageFile );
                    }

                }
            }
        }
        return null;
    }

    private BinaryPackagePackagesFile convert ( final BinaryPackageControlFile cf, final File packageFile ) throws Exception
    {
        final BinaryPackagePackagesFile pf = new BinaryPackagePackagesFile ( cf.toString () );

        for ( final Digester d : this.digestersPackage )
        {
            pf.set ( d.getName (), digest ( packageFile, d.create () ) );
        }

        final Component component = findComponent ( pf );
        if ( component == null )
        {
            return null;
        }

        final File targetFile = makeTargetFile ( component, packageFile, cf.get ( "Package" ) );
        final String filename = targetFile.toString ().substring ( this.configuration.getTargetFolder ().toString ().length () + 1 );

        pf.set ( "Filename", filename );
        pf.set ( "Size", "" + packageFile.length () );

        return pf;
    }

    public static String digest ( final File file, final MessageDigest digest ) throws IOException
    {
        try ( final InputStream in = new FileInputStream ( file ) )
        {
            final byte[] buffer = new byte[4096];
            int rc;
            while ( ( rc = in.read ( buffer ) ) > 0 )
            {
                digest.update ( buffer, 0, rc );
            }
            final byte[] dv = digest.digest ();
            final StringBuilder sb = new StringBuilder ();
            for ( final byte b : dv )
            {
                sb.append ( String.format ( "%02x", b ) );
            }
            return sb.toString ();
        }
    }

    private void copyArtifact ( final Component component, final File packageFile, final BinaryPackagePackagesFile cf ) throws IOException
    {
        final String name = cf.get ( "Package" );

        final File targetFile = makeTargetFile ( component, packageFile, name );
        this.console.info ( "Copy artifact: " + targetFile );
        targetFile.mkdirs ();
        Files.copy ( packageFile.toPath (), targetFile.toPath (), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING );
    }

    private File makeTargetFile ( final Component component, final File packageFile, final String packageName )
    {
        File targetFile = new File ( this.pool, component.getName () );
        targetFile = new File ( targetFile, packageName.substring ( 0, 1 ) );
        targetFile = new File ( targetFile, packageName );
        targetFile = new File ( targetFile, packageFile.getName () );
        return targetFile.getAbsoluteFile ();
    }
}

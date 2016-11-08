/*
 * Copyright 2016 Imola Informatica S.P.A..
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
package it.imolinfo.maven.plugins.jboss.fuse.utils;

import it.imolinfo.maven.plugins.jboss.fuse.AbstractGoal;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class ArchiveManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGoal.class);
    
    private ArchiveManager() {
    }
    
    /**
     * Estrae un archivio sulla base dell'estensione (supportato zip, tar.gz, tgz, tar.bz2
     * @param archivePath Percorso all'archivio
     * @param destDirectory Directory di destinazione dove estrarre i files
     * @throws MojoExecutionException 
     */
    public static void extract(String archivePath, String destDirectory) throws MojoExecutionException {
        final AbstractUnArchiver abstractUnArchiver; 
        Pattern p = Pattern.compile(".*\\.([^.]+)$");
        Matcher m = p.matcher(archivePath.toLowerCase());
        if (m.find()) {        
            String ext = m.group(1);
            LOG.info("Check extension support for {} of {}", ext, archivePath);
            
            switch (ext.toLowerCase()) {
                case "zip":
                    abstractUnArchiver = new ZipUnArchiver();
                    break;
                case "bz2":
                    abstractUnArchiver = new TarBZip2UnArchiver();
                    break;
                case "tgz":
                case "gz":
                    abstractUnArchiver = new TarGZipUnArchiver();
                    break;
                default:
                    throw new MojoExecutionException(String.format("Unknown extension %s", ext));
            }
        }
        else {
            throw new MojoExecutionException("Cannot detect archive type");
        }
        abstractUnArchiver.setSourceFile(new File(archivePath));
        LOG.info(abstractUnArchiver.getSourceFile().getAbsolutePath());
        File destination = new File(destDirectory);
        destination.mkdirs();        
        abstractUnArchiver.setDestDirectory(destination);
        abstractUnArchiver.extract();
    }
    
}

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

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.archiver.tar.TarUnArchiver;

/**
 *
 * @author ale
 */
public class ArchiveExtractor {
    
    private ArchiveExtractor() {
    }
    
    /**
     * Estrae un archivio sulla base dell'estensione (supportato zip, tar.gz, tgz, tar.bz2
     * @param archivePath Percorso all'archivio
     * @param destDirectory Directory di destinazione dove estrarre i files
     * @throws MojoExecutionException 
     */
    public static void extract(String archivePath, String destDirectory) throws MojoExecutionException {
        if (archivePath.toLowerCase().endsWith(".zip")) {
            UnzipUtility.unzip(archivePath, destDirectory);
        }
        else {
            untar(archivePath, destDirectory);
        }
    }
    
    
    
    private static void untar(String sourceFile, String destDirectory) {
        final TarUnArchiver ua = sourceFile.toLowerCase().endsWith("gz") ? new TarGZipUnArchiver() : new TarBZip2UnArchiver();
        ua.setSourceFile(new File(sourceFile));
        File destination = new File(destDirectory);
        destination.mkdirs();
        ua.setDestDirectory(destination);
        ua.extract();
    }
    
}

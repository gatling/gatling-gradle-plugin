package io.gatling.gradle

import org.apache.tools.zip.UnixStat
import org.apache.tools.zip.Zip64Mode
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.GradleException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.DefaultFileTreeElement
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.api.tasks.util.PatternSet

import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId


class GatlingEnterpriseCopyAction implements CopyAction {
    /**
     * DOS epoch used as the default time for ZIP entries when file timestamps are not preserved (for reproducible ZIP
     * files). Uses the system default time zone because that's what {@link ZipEntry#setTime(long)} expects.
     */
    private static final long ZIP_ENTRY_DEFAULT_TIME =
        LocalDateTime.of(1980, Month.JANUARY, 1, 0, 0)
            .toInstant(ZoneId.systemDefault().offset)
            .toEpochMilli()

    private final File archiveFile
    private final PatternSet patternSet
    private final String encoding
    private final boolean preserveFileTimestamps
    private final ZipEntryCompression entryCompression
    private final boolean allowZip64Mode

    GatlingEnterpriseCopyAction(
        File archiveFile,
        String encoding,
        PatternSet patternSet,
        boolean preserveFileTimestamps,
        ZipEntryCompression entryCompression,
        boolean allowZip64Mode
    ) {
        this.archiveFile = archiveFile
        this.patternSet = patternSet
        this.encoding = encoding
        this.preserveFileTimestamps = preserveFileTimestamps
        this.entryCompression = entryCompression
        this.allowZip64Mode = allowZip64Mode
    }

    @Override
    WorkResult execute(CopyActionProcessingStream stream) {
        try {
            new ZipOutputStream(archiveFile).withCloseable { outputStream ->
                outputStream.setUseZip64(allowZip64Mode ? Zip64Mode.AsNeeded : Zip64Mode.Never)
                if (entryCompression == ZipEntryCompression.STORED) {
                    outputStream.setMethod(ZipEntry.STORED) // otherwise DEFLATED method is already the default
                }
                if (encoding != null) {
                    outputStream.setEncoding(encoding)
                }
                stream.process(new ProcessingStreamAction(outputStream, patternSet))
            }
        } catch (Exception e) {
            throw new GradleException("Could not create ZIP '${archiveFile.toString()}'", e)
        }

        return WorkResults.didWork(true)
    }

    private long getArchiveTimeFor(long timestamp) {
        return preserveFileTimestamps ? timestamp : ZIP_ENTRY_DEFAULT_TIME
    }

    private class ProcessingStreamAction implements CopyActionProcessingStreamAction {

        private final ZipOutputStream outputStream
        private final PatternSet patternSet

        private final Set<String> visitedPaths = new HashSet<String>()

        ProcessingStreamAction(ZipOutputStream outputStream, PatternSet patternSet) {
            this.outputStream = outputStream
            this.patternSet = patternSet
        }

        @Override
        void processFile(FileCopyDetailsInternal details) {
            if (details.directory) {
                visitDir(details)
            } else {
                visitFile(details)
            }
        }

        /** @return true if not already visited */
        private boolean recordVisit(RelativePath path) {
            return visitedPaths.add(path.pathString)
        }

        private void visitDir(FileCopyDetails dirDetails) {
            try {
                // Trailing slash in ZIP entry name indicates that entry is a directory
                String entryName = dirDetails.relativePath.pathString + '/'
                ZipEntry entry = new ZipEntry(entryName)
                entry.setTime(getArchiveTimeFor(dirDetails.lastModified))
                entry.unixMode = (UnixStat.DIR_FLAG | dirDetails.mode)
                outputStream.putNextEntry(entry)
                outputStream.closeEntry()
                recordVisit(dirDetails.relativePath)
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add directory %s to archive '%s'.", dirDetails, archiveFile), e)
            }
        }

        private void visitFile(FileCopyDetails fileDetails) {
            if (!isArchive(fileDetails)) {
                try {
                    String entryName = dirDetails.relativePath.pathString
                    ZipEntry entry = new ZipEntry(entryName)
                    entry.setTime(getArchiveTimeFor(fileDetails.lastModified))
                    entry.unixMode = (UnixStat.FILE_FLAG | fileDetails.mode)
                    outputStream.putNextEntry(entry)
                    fileDetails.copyTo(outputStream)
                    outputStream.closeEntry()
                    recordVisit(fileDetails.relativePath)
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add file %s to archive '%s'.", fileDetails, archiveFile), e)
                }
            } else {
                try {
                    processArchive(fileDetails)
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not merge archive %s into archive '%s'.", fileDetails, archiveFile), e)
                }
            }
        }

        private static boolean isArchive(FileCopyDetails fileDetails) {
            return fileDetails.relativePath.pathString.endsWith('.jar')
        }

        private void processArchive(FileCopyDetails fileDetails) {
            new ZipFile(fileDetails.file).withCloseable {archive ->
                Spec<FileTreeElement> patternSpec = patternSet.getAsSpec()
                archive.entries.each { entry ->
                    if (!entry.directory) {
                        RelativePath path = new RelativePath(!entry.directory, entry.name.split('/'))
                        if (patternSpec.isSatisfiedBy(new DefaultFileTreeElement(null, path, null, null)) && recordVisit(path)) {
                            copyArchiveFile(archive, entry, path)
                        }
                    }
                }
            }
        }

        private void copyArchiveFile(ZipFile sourceArchive, ZipEntry sourceEntry, RelativePath archiveFilePath) {
            ZipEntry targetEntry = new ZipEntry(sourceEntry.name)
            targetEntry.setTime(getArchiveTimeFor(sourceEntry.time))
            addParentDirectories(archiveFilePath)
            outputStream.putNextEntry(targetEntry)
            sourceArchive.getInputStream(sourceEntry).withCloseable { is ->
                byte[] buf = new byte[8192]
                int length
                while ((length = is.read(buf)) > 0) {
                    outputStream.write(buf, 0, length)
                }
            }
            outputStream.closeEntry()
        }

        private void addParentDirectories(RelativePath archiveDirPath) {
            if (archiveDirPath != null && archiveDirPath.segments.length > 0) {
                addParentDirectories(archiveDirPath.parent)
                if (!archiveDirPath.file && recordVisit(archiveDirPath)) {
                    ZipEntry entry = new ZipEntry(archiveDirPath.segments.join('/') + '/')
                    entry.setTime(getArchiveTimeFor(entry.time))
                    outputStream.putNextEntry(entry)
                    outputStream.closeEntry()
                }
            }
        }
    }
}

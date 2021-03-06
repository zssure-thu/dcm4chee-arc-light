package org.dcm4chee.arc.storage;

import org.dcm4chee.arc.conf.StorageDescriptor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jul 2015
 */
public interface Storage extends Closeable {
    StorageDescriptor getStorageDescriptor();

    WriteContext createWriteContext();

    ReadContext createReadContext();

    boolean isAccessable();

    OutputStream openOutputStream(WriteContext ctx) throws IOException;

    void commitStorage(WriteContext ctx) throws IOException;

    void revokeStorage(WriteContext ctx) throws IOException;

    void deleteObject(String storagePath) throws IOException;

    InputStream openInputStream(ReadContext ctx) throws IOException;
}

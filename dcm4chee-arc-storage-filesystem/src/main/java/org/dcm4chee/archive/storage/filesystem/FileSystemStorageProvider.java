package org.dcm4chee.archive.storage.filesystem;

import org.dcm4chee.archive.conf.StorageDescriptor;
import org.dcm4chee.archive.storage.Storage;
import org.dcm4chee.archive.storage.StorageProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @since Jul 2015
 */
@ApplicationScoped
@Named("file")
class FileSystemStorageProvider implements StorageProvider {
    @Override
    public Storage openStorage(StorageDescriptor descriptor) {
        return new FileSystemStorage(descriptor);
    }
}

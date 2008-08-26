/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.interceptor.Interceptors;

import ome.api.IRepositoryInfo;
import ome.api.RawFileStore;
import ome.api.ServiceInterface;
import ome.conditions.ApiUsageException;
import ome.conditions.ResourceError;
import ome.io.nio.FileBuffer;
import ome.io.nio.OriginalFilesService;
import ome.model.core.OriginalFile;
import ome.services.util.OmeroAroundInvoke;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.annotation.ejb.RemoteBinding;
import org.jboss.annotation.ejb.RemoteBindings;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raw file gateway which provides access to the OMERO file repository.
 * 
 * @author Chris Allan &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:callan@blackcat.ca">callan@blackcat.ca</a>
 * @version 3.0 <small> (<b>Internal version:</b> $Revision$ $Date:
 *          2005/06/08 15:21:59 $) </small>
 * @since OMERO3.0
 */
@TransactionManagement(TransactionManagementType.BEAN)
@Transactional(readOnly = true)
@Stateful
@Remote(RawFileStore.class)
@RemoteBindings({
    @RemoteBinding(jndiBinding = "omero/remote/ome.api.RawFileStore"),
    @RemoteBinding(jndiBinding = "omero/secure/ome.api.RawFileStore",
		   clientBindUrl="sslsocket://0.0.0.0:3843")
})
@Local(RawFileStore.class)
@LocalBinding(jndiBinding = "omero/local/ome.api.RawFileStore")
@Interceptors( { OmeroAroundInvoke.class })
public class RawFileBean extends AbstractStatefulBean implements RawFileStore {
    /**
     * 
     */
    private static final long serialVersionUID = -450924529925301925L;
    
    /** The logger for this particular class */
    private static Log log = LogFactory.getLog(RawPixelsBean.class);

    /** The id of the original files instance. */
    private Long id;

    /** Only set after a passivated bean is activated. */
    private transient Long reset = null;
    
    /** The original file this service is currently working on. */
    private transient OriginalFile file;

    /** The file buffer for the service's original file. */
    private transient FileBuffer buffer;
    
    /** ROMIO I/O service for files. */
    private transient OriginalFilesService ioService;

    /** the disk space checking service */
    private transient IRepositoryInfo iRepositoryInfo;

    /** is file service checking for disk overflow */
    private transient boolean diskSpaceChecking;
    
    /**
     * default constructor
     */
    public RawFileBean() {}
    
    /**
     * overriden to allow Spring to set boolean
     * @param checking
     */
    public RawFileBean(boolean checking) {
    	this.diskSpaceChecking = checking;
    }
    public Class<? extends ServiceInterface> getServiceInterface() {
        return RawFileStore.class;
    }

    /**
     * I/O service (OriginalFilesService) Bean injector.
     * 
     * @param ioService
     *            an <code>OriginalFileService</code>.
     */
    public final void setOriginalFilesService(OriginalFilesService ioService) {
        getBeanHelper().throwIfAlreadySet(this.ioService, ioService);
        this.ioService = ioService;
    }
    
    /**
     * Disk Space Usage service Bean injector
     * @param iRepositoryInfo
     *   		  	an <code>IRepositoryInfo</code>
     */
    public final void setIRepositoryInfo(IRepositoryInfo iRepositoryInfo) {
        getBeanHelper().throwIfAlreadySet(this.iRepositoryInfo, iRepositoryInfo);
        this.iRepositoryInfo = iRepositoryInfo;
    }
    
    @PostConstruct
    @PostActivate
    public void create() {
        selfConfigure();
        if (id != null) {
            reset = id;
            id = null;
        }
    }

    @PrePassivate
    @PreDestroy
    @RolesAllowed("user")
    public void destroy() {
        // id is the only thing passivated.
        ioService = null;
        file = null;
        closeFileBuffer();
        buffer = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ome.api.StatefulServiceInterface#close()
     */
    @RolesAllowed("user")
    @Remove
    @Transactional(readOnly = true)
    public void close() {
        closeFileBuffer();
    }
    
    /**
     * Close the active file buffer, cleaning up any potential messes left by
     * the file buffer itself.
     */
    private void closeFileBuffer()
    {
		try
		{
			if (buffer != null)
				buffer.close();
		}
		catch (IOException e)
		{
            if (log.isDebugEnabled()) {
                log.debug("Buffer could not be closed successfully.", e);
            }
			throw new ResourceError(
					e.getMessage() + " Please check server log.");
		}
    }

    /*
     * (non-Javadoc)
     * 
     * @see ome.api.RawFileStore#setFileId(long)
     */
    @RolesAllowed("user")
    @Transactional(readOnly = true)
    public void setFileId(long fileId) {
        if (id == null || id.longValue() != fileId) {
            id = new Long(fileId);
            file = null;
            closeFileBuffer();
            buffer = null;

            file = iQuery.get(OriginalFile.class, id);
            buffer = ioService.getFileBuffer(file);
        }
    }
    
    private synchronized void errorIfNotLoaded() {
        // If we're not loaded because of passivation, then load.
        if (reset != null) {
            id = null;
            setFileId(reset.longValue());
            reset = null;
        }
        if (buffer == null) {
            throw new ApiUsageException(
                    "This RawFileStore has not been properly initialized.\n"
                            + "Please set the file id before executing any other methods.\n");
        }
    }
    
    /* (non-Javadoc)
     * @see ome.api.RawFileStore#exists()
     */
    @RolesAllowed("user")
    public boolean exists() {
        errorIfNotLoaded();
        return ioService.exists(file);
    }

    @RolesAllowed("user")
    public byte[] read(long position, int length) {
        errorIfNotLoaded();
        byte[] rawBuf = new byte[length];
        ByteBuffer buf = ByteBuffer.wrap(rawBuf);

        try {
            buffer.read(buf, position);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Buffer could not be read.", e);
            }
            throw new ResourceError(e.getMessage());
        }
        return rawBuf;
    }

    @RolesAllowed("user")
    public void write(byte[] buf, long position, int length) {
        errorIfNotLoaded();
        ByteBuffer nioBuffer = MappedByteBuffer.wrap(buf);
        nioBuffer.limit(length);

        if (diskSpaceChecking) {
        	iRepositoryInfo.sanityCheckRepository();
        }
        
        try {
            buffer.write(nioBuffer, position);
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Buffer write did not occur.", e);
            }
            throw new ResourceError(e.getMessage());
        }
    }

    /**
     * getter disk overflow checking
     * @return
     */
	public boolean isDiskSpaceChecking() {
		return diskSpaceChecking;
	}

	/**
	 * setter disk overflow checking
	 * @param diskSpaceChecking
	 *   a <code>boolean</code>
	 */
	public void setDiskSpaceChecking(boolean diskSpaceChecking) {
		this.diskSpaceChecking = diskSpaceChecking;
	}
}

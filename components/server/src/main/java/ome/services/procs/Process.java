/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.procs;

/**
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta2
 */
public interface Process {

    Processor processor();

    boolean isActive();

    void finish();

    void cancel();

    void registerCallback(ProcessCallback cb);

    void unregisterCallback(ProcessCallback cb);

}

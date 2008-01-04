/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.util;

import ome.model.IObject;
import ome.model.internal.Details;
import ome.model.meta.Event;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.DateBridge;

/**
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * 
 */
public class DetailsFieldBridge implements FieldBridge {

    // TODO add combined_fields to constants
    public final static String COMBINED = "combined_fields";

    public final static DateBridge dateBridge = new DateBridge(
            Resolution.MINUTE);

    public void set(String name, Object value, Document document,
            Field.Store store, Field.Index index, Float boost) {

        IObject object = (IObject) value;
        Details details = object.getDetails();

        Experimenter e = details.getOwner();
        String omename = e.getOmeName();
        String firstName = e.getFirstName();
        String lastName = e.getLastName();

        ExperimenterGroup g = details.getGroup();
        String groupName = g.getName();

        add(document, "owner", omename, store, index, boost);
        add(document, "firstname", firstName, store, index, boost);
        add(document, "lastName", lastName, store, index, boost);
        add(document, "group", groupName, store, index, boost);

        Event creationEvent = details.getCreationEvent();
        Event updateEvent = details.getUpdateEvent();

        String creation = dateBridge.objectToString(creationEvent.getTime());
        String update = dateBridge.objectToString(updateEvent.getTime());

        add(document, "creation", creation, store, index, boost);
        add(document, "update", update, store, index, boost);
    }

    protected void add(Document d, String field, String value,
            Field.Store store, Field.Index index, Float boost) {

        Field f = new Field(field, value, store, index);
        if (boost != null) {
            f.setBoost(boost);
        }
        d.add(f);

        f = new Field(COMBINED, value, store, index);
        if (boost != null) {
            f.setBoost(boost);
        }
        d.add(f);
    }

}
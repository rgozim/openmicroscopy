 /*
 * org.openmicroscopy.shoola.agents.editor.model.params.SingleParam
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.editor.model.params;

//Java imports

import java.util.HashMap;

//Third-party libraries

//Application-internal dependencies

/** 
 * This is a Parameter that has a single value attribute, PARAM_VALUE.
 * Can be used as the data object for any parameter that has a single
 * value, Eg. Text-Line, Text-Box,
 * and a single default attribute DEFAULT_VALUE.
 * 
 * @author  William Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:will@lifesci.dundee.ac.uk">will@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class TextParam 
	extends AbstractParam {

	public static final String PARAM_VALUE = "value";
	
	public static final String DEFAULT_VALUE = "default-value";
	
	/**
	 * A parameter defined as a short text string. 
	 * Equivalent to the "TextField" of Beta 3.0
	 */
	public static final String 		TEXT_LINE_PARAM = "TEXT";
	
	/**
	 * This defines a parameter that is a longer piece of text.
	 * Equivalent to the "TextBox" of Beta 3.0
	 */
	public static final String 		TEXT_BOX_PARAM = "TEXTBOX";
	
	/**
	 * Creates an instance. 
	 * 
	 * @param fieldType		The String defining the field type
	 */
	public TextParam(String fieldType) 
	{
		super(fieldType);
	}
	
	/**
	 * Returns a single attribute name that identifies the default value
	 * 
	 * @see AbstractParam#getDefaultAttributes()
	 */
	public String[] getDefaultAttributes() 
	{
		return new String [] {DEFAULT_VALUE};
	}

	/**
	 * This field is filled if the value isn't null, and 
	 * is not an empty string. 
	 * 
	 * @see AbstractParam#isParamFilled()
	 */
	public boolean isParamFilled() {
		String textValue = getParamValue();
		
		return (textValue != null && textValue.length() > 0);
	}
	
	/**
	 * Returns the value of the parameter. 
	 * 
	 * @see Object#toString()
	 */
	public String toString() {
		
		String text = super.toString();
		
		String value = getParamValue();
		if (value != null) {
			text = value;
		}
		
		return text;
	}
	
	/**
	 * Implemented as specified by the {@link IParam} interface. 
	 */
	public String getParamValue() 
	{
		if (getValueAt(0) == null) 		return null;
		else return getValueAt(0) + "";
	}
}

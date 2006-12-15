/*
 * ome.security.JBossLoginModule
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.security;

//Java imports
import javax.security.auth.login.LoginException;

//Third-party libraries
import org.jboss.security.auth.spi.DatabaseServerLoginModule;

//Application-internal dependencies

/** 
 * configured in jboss-login.xml to add logic to the JBoss authentication 
 * procedure.
 * 
 * Specifically, we override {@link #validatePassword(String, String)} here
 * in order to interpret empty string passwords as "open", i.e. any password
 * will be accepted. This eases entry into the system in that passwords can
 * be initially ignored.
 * 
 * @author  Josh Moore &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 
 * <small>
 * (<b>Internal version:</b> $Rev$ $Date$)
 * </small>
 * @since 1.0
 */
public class JBossLoginModule extends DatabaseServerLoginModule 
{

	/** overrides password creation for testing purposes */
	@Override
	protected String createPasswordHash(String arg0, String arg1, String arg2) 
	throws LoginException {
		String retVal = super.createPasswordHash(arg0, arg1, arg2);
		return retVal;
	}

	/** overrides the standard behavior of returning false (bad match) for 
	 * all differing passwords. Here, we allow stored passwords to be empty
	 * which signifies that anyone can use the account, regardless of password.
	 */
	@Override
	protected boolean validatePassword(
			String inputPassword, String expectedPassword) {
		
		if ( null!=expectedPassword && 
				expectedPassword.trim().length()<=0 ) 
		{
			return true;
		}
		return super.validatePassword(
				inputPassword == null ? null : inputPassword.trim(), 
				expectedPassword == null ? null : expectedPassword.trim());
	}
	
}

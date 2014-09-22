/*
 *   $Id$
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.server.utests.sec;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ome.api.ILdap;
import ome.conditions.ApiUsageException;
import ome.logic.LdapImpl;
import ome.model.meta.Experimenter;
import ome.security.auth.ConfigurablePasswordProvider;
import ome.security.auth.FilePasswordProvider;
import ome.security.auth.JdbcPasswordProvider;
import ome.security.auth.LdapConfig;
import ome.security.auth.LdapPasswordProvider;
import ome.security.auth.PasswordChangeException;
import ome.security.auth.PasswordProvider;
import ome.security.auth.PasswordProviders;
import ome.security.auth.PasswordUtil;
import ome.security.auth.PasswordUtility;
import ome.system.Roles;
import ome.util.SqlAction;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.springframework.util.ResourceUtils;
import org.testng.annotations.Test;

@Test
public class PasswordTest extends MockObjectTestCase {

    private final static Long GUEST_ID = new Roles().getGuestId();

    static File file = null;
    static {
        try {
            file = ResourceUtils.getFile("classpath:ome/server/utests/sec/"
                    + "PasswordTest_FilePasswordProvider.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Concrete implementation of {@link ConfigurablePasswordProvider}'s default
     * actions.
     */
    static class PP extends ConfigurablePasswordProvider {
        PP() {
            super(null);
        }
    }

    PasswordProvider provider;

    Mock mockSql, mockLdap;

    SqlAction sql;

    LdapImpl ldap;

    AtomicBoolean validPassword = new AtomicBoolean();

    AtomicReference<String> currentDn = new AtomicReference<String>();

    AtomicReference<Experimenter> createdUser = new AtomicReference<Experimenter>();

    protected void initJdbc() {
        mockSql = mock(SqlAction.class);
        sql = (SqlAction) mockSql.proxy();
    }

    protected void initLdap(boolean setting) {
        initJdbc();
        mockLdap = mock(ILdap.class);
        // ldap = (ILdap) mockLdap.proxy();
        ldap = new LdapImpl(null, null, null,
                new LdapConfig(setting, "", "", "", "", "", false, ""), null, sql) {
            @Override
            public String findDN(String username) {
                return currentDn.get();
            }
            @Override
            public boolean validatePassword(String dn, String password) {
                return validPassword.get();
            }
            @Override
            public Experimenter createUser(String username,
                    String password, boolean checkPassword) {
                return createdUser.get();
            }
        };
        mockLdap.expects(atLeastOnce()).method("getSetting").will(
                returnValue(setting));
    }

    // CONFIGURABLE

    /**
     * By default, the base class should return False ("Password rejected")
     */
    public void testConfigurableDefaultsReturnsFalse() {
        provider = new PP();
        assertFalse(provider.checkPassword("", "", false));
        assertFalse(provider.hasPassword(""));
    }

    /**
     * By default, the base class should return False ("Password rejected") and
     * throw a {@link PasswordChangeException} ("Can't change")
     */
    @Test(expectedExceptions = PasswordChangeException.class)
    public void testConfigurableDefaultsThrows() throws Exception {
        provider = new PP();
        provider.changePassword("", "");
    }

    // FILE

    public void testFileDefaults() throws Exception {
        provider = new FilePasswordProvider(new PasswordUtil(sql), file);
        assertTrue(provider.hasPassword("test"));
        assertTrue(provider.checkPassword("test", "test", false));
        assertFalse(provider.checkPassword("unknown", "anything", false));
    }

    public void testFilesDontIgnoreUnknownReturnsNull() throws Exception {
        provider = new FilePasswordProvider(null, file, true);
        assertFalse(provider.hasPassword("unknown"));
        assertNull(provider.checkPassword("unknown", "anything", false));
    }

    @Test(expectedExceptions = PasswordChangeException.class)
    public void testFilesThrowsOnChange() throws Exception {
        provider = new FilePasswordProvider(null, file, true);
        provider.changePassword("test", "something new");
    }

    // JDBC

    public void tesJdbcDefaults() throws Exception {
        initJdbc();
        provider = new JdbcPasswordProvider(new PasswordUtil(sql));

        userIdReturns1();
        provider.hasPassword("test");

        userIdReturnsNull();
        provider.hasPassword("unknown");

        String encoded = ((PasswordUtility) provider).encodePassword("test");
        getPasswordHash(encoded);
        userIdReturns1();
        assertTrue(provider.checkPassword("test", "test", false));

        getPasswordHash(encoded);
        userIdReturns1();
        assertFalse(provider.checkPassword("test", "GARBAGE", false));
    }

    public void tesJdbcIgnoreUnknownReturnsFalse() throws Exception {
        initJdbc();
        userIdReturnsNull();
        provider = new JdbcPasswordProvider(new PasswordUtil(sql));
        assertFalse(provider.checkPassword("unknown", "anything", false));
    }

    public void tesJdbcDontIgnoreUnknownReturnsNull() throws Exception {
        initJdbc();
        userIdReturnsNull();
        provider = new JdbcPasswordProvider(new PasswordUtil(sql), true);
        assertNull(provider.checkPassword("unknown", "anything", false));
    }

    public void testJdbcChangesPassword() throws Exception {
        initJdbc();
        userIdReturns1();
        mockSql.expects(once()).method("setUserPassword").will(returnValue(true));
        provider = new JdbcPasswordProvider(new PasswordUtil(sql));
        provider.changePassword("a", "b");
    }

    @Test(expectedExceptions = PasswordChangeException.class)
    public void testJdbcThrowsOnBadUsername() throws Exception {
        initJdbc();
        userIdReturnsNull();
        provider = new JdbcPasswordProvider(new PasswordUtil(sql));
        provider.changePassword("a", "b");
    }

    // LDAP

    public void tesLdapDefaults() throws Exception {
        initLdap(true);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap);

        String encoded = ((PasswordUtility) provider).encodePassword("test");
        getPasswordHash(encoded);
        getDn("dn");
        userIdReturns1();
        validateLdapPassword(true);
        assertTrue(provider.checkPassword("test", "test", false));

        getPasswordHash(encoded);
        userIdReturns1();
        validateLdapPassword(false);
        getDn("dn");
        assertFalse(provider.checkPassword("test", "GARBAGE", false));

        userIdReturnsNull();
        assertFalse(provider.hasPassword("unknown"));

        userIdReturns1();
        getPasswordHash(null);
        validateLdapPassword(false);
        getDn(null);
        assertFalse(provider.hasPassword("no-dn"));

        getPasswordHash("dn");
        getDn("dn");
        userIdReturns1();
        assertTrue(provider.hasPassword("dn"));
    }

    public void tesLdapIgnoreUnknownCreatesFailsReturnsFalse() throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUser(false);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, true);
        assertNull(provider.checkPassword("unknown", "anything", false));
    }

    public void tesLdapIgnoreUnknownCreatesSucceedsReturnsTrue()
            throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUser(true);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, true);
        assertTrue(provider.checkPassword("unknown", "anything", false));
    }

    public void tesLdapIgnoreUnknownCreatesThrows() throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUserAndThrows();
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, true);
        assertNull(provider.checkPassword("unknown", "anything", false));
    }

    public void tesLdapDontIgnoreUnknownCreatesFailsReturnsFalse()
            throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUser(false);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, false);
        assertFalse(provider.checkPassword("unknown", "anything", false));
    }

    public void tesLdapDontIgnoreUnknownCreatesSucceedsReturnsTrue()
            throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUser(true);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, false);
        assertTrue(provider.checkPassword("unknown", "anything", false));
    }

    public void tesLdapDontIgnoreUnknownCreatesThrows() throws Exception {
        initLdap(true);
        userIdReturnsNull();
        ldapCreatesUserAndThrows();
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap, false);
        assertFalse(provider.checkPassword("unknown", "anything", false));
    }

    @Test(expectedExceptions = PasswordChangeException.class)
    public void testLdapChangesPasswordThrows() throws Exception {
        initLdap(true);
        provider = new LdapPasswordProvider(new PasswordUtil(sql), ldap);
        provider.changePassword("a", "b");
    }

    /**
     * Straight-forward stub to allow easy composite testing.
     */
    static class Stub implements PasswordProvider {
        Boolean check = null;
        boolean exception = true;
        boolean hasPasswordCalled = false;
        boolean changePasswordCalled = false;
        boolean checkPasswordCalled = false;

        public Stub() {

        }

        public Stub(Boolean check, boolean exc) {
            this.check = check;
            this.exception = exc;
        }

        public void changePassword(String user, String password)
                throws PasswordChangeException {
            changePasswordCalled = true;
            if (exception) {
                throw new PasswordChangeException("");
            }
        }

        public boolean hasPassword(String user) {
            hasPasswordCalled = true;
            return check == null ? false : check.booleanValue();
        }

        public Boolean checkPassword(String user, String password, boolean readOnly) {
            checkPasswordCalled = true;
            return check;
        }

        void assertChangePasswordCalled() {
            if (!changePasswordCalled) {
                fail();
            }
        }


        void assertChangePasswordNotCalled() {
            if (changePasswordCalled) {
                fail();
            }
        }

        void assertCheckPasswordCalled() {
            if (!checkPasswordCalled) {
                fail();
            }
        }

        void assertCheckPasswordNotCalled() {
            if (checkPasswordCalled) {
                fail();
            }
        }

        void assertHasPasswordCalled() {
            if (!hasPasswordCalled) {
                fail();
            }
        }

        void assertHasPasswordNotCalled() {
            if (hasPasswordCalled) {
                fail();
            }
        }

    }

    // COMPOSITE LDAP FIRST THEN JDBC (standard)

    public void testChainedUnknownPropogatesToSecondStub() throws Exception {
        Stub s1 = new Stub();
        Stub s2 = new Stub();
        provider = new PasswordProviders(s1, s2);
        assertNull(provider.checkPassword("known", "password", false));
        s1.assertCheckPasswordCalled();
        s2.assertCheckPasswordCalled();
    }

    public void testChainedUnknownPropogatesToSecondStubWhichFails() throws Exception {
        Stub s1 = new Stub();
        Stub s2 = new Stub(false, false);
        provider = new PasswordProviders(s1, s2);
        assertFalse(provider.checkPassword("known", "password", false));
        s1.assertCheckPasswordCalled();
        s2.assertCheckPasswordCalled();
    }

    public void testChainedKnownPropogatesToSecondStubWhichSucceeds() throws Exception {
        Stub s1 = new Stub();
        Stub s2 = new Stub(true, false);
        provider = new PasswordProviders(s1, s2);
        assertTrue(provider.checkPassword("known", "password", false));
        s1.assertCheckPasswordCalled();
        s2.assertCheckPasswordCalled();
    }

    public void testChainedKnownDoesntPropagate() throws Exception {
        Stub s1 = new Stub(true, false);
        Stub s2 = new Stub(true, false);
        provider = new PasswordProviders(s1, s2);
        assertTrue(provider.checkPassword("known", "password", false));
        s1.assertCheckPasswordCalled();
        s2.assertCheckPasswordNotCalled();
    }

    public void testChainedUnknownDoesntPropagate() throws Exception {
        Stub s1 = new Stub(false, false);
        Stub s2 = new Stub(false, false);
        provider = new PasswordProviders(s1, s2);
        assertFalse(provider.checkPassword("unknown", "password", false));
        s1.assertCheckPasswordCalled();
        s2.assertCheckPasswordNotCalled();
    }

    public void testChainedFirstChangePassword() throws Exception {
        Stub s1 = new Stub(true, false);
        Stub s2 = new Stub(true, false);
        provider = new PasswordProviders(s1, s2);
        provider.changePassword("","");
        s1.assertHasPasswordCalled();
        s1.assertChangePasswordCalled();
        s2.assertHasPasswordNotCalled();
        s2.assertChangePasswordNotCalled();
    }

    public void testChainedSecondChangePassword() throws Exception {
        Stub s1 = new Stub(false, false);
        Stub s2 = new Stub(true, false);
        provider = new PasswordProviders(s1, s2);
        provider.changePassword("","");
        s1.assertHasPasswordCalled();
        s1.assertChangePasswordNotCalled();
        s2.assertHasPasswordCalled();
        s2.assertChangePasswordCalled();
    }

    public void testChainedNoneChangePassword() throws Exception {
        Stub s1 = new Stub(false, false);
        Stub s2 = new Stub(false, false);
        provider = new PasswordProviders(s1, s2);
        assertChangeThrows();
        s1.assertHasPasswordCalled();
        s1.assertChangePasswordNotCalled();
        s2.assertHasPasswordCalled();
        s2.assertChangePasswordNotCalled();
    }

    public void testChainedFirstWontChangePassword() throws Exception {
        Stub s1 = new Stub(true, true);
        Stub s2 = new Stub(false, false);
        provider = new PasswordProviders(s1, s2);
        assertChangeThrows();
        s1.assertHasPasswordCalled();
        s2.assertHasPasswordNotCalled();
    }

    public void testChainedSecondWontChangePassword() throws Exception {
        Stub s1 = new Stub(false, true);
        Stub s2 = new Stub(true, true);
        provider = new PasswordProviders(s1, s2);
        assertChangeThrows();
        s1.assertHasPasswordCalled();
        s2.assertHasPasswordCalled();
    }

    // ~ Helpers
    // =========================================================================

    private void getPasswordHash(String value) {
        mockSql.expects(once()).method("getPasswordHash").will(returnValue(value));
    }

    private void getDn(String value) {
        mockSql.expects(once()).method("dnForUser").will(returnValue(value));
        currentDn.set(value);
    }

    final static String good = "ążćę";
    final static String bad = "????";
    final static String badHash = "6U8L+rjJh6dDe6ThaXwcwA==";
    final static String goodHash = "iIoEyIOGsGsDhWZMYNBTKQ==";

    public void testLatin1Encoding() {
        Charset latin1 = StandardCharsets.ISO_8859_1;
        byte[] badBytes = bad.getBytes(latin1);
        byte[] goodBytes = good.getBytes(latin1);
        assertTrue(Arrays.equals(badBytes, goodBytes));
        assertEquals(bad, new String(good.getBytes(latin1)));
        PasswordUtil util = new PasswordUtil(sql, latin1);
        assertEquals(badHash, util.passwordDigest(bad));
        assertEquals(badHash, util.passwordDigest(good));
    }

    public void testUtf8Encoding() {
        Charset utf8 = StandardCharsets.UTF_8;
        assertEquals(good, new String(good.getBytes(utf8)));
        PasswordUtil util = new PasswordUtil(sql, utf8);
        assertEquals(badHash, util.passwordDigest(bad));
        assertEquals(goodHash, util.passwordDigest(good));
        assertFalse(goodHash.equals(badHash));
    }

    public void testIsPasswordRequiredWithoutStrictSetting() {
        PasswordUtil util = new PasswordUtil(sql, false);
        assertFalse(util.isPasswordRequired(null));
        assertFalse(util.isPasswordRequired(456l));
        assertFalse(util.isPasswordRequired(GUEST_ID));
    }

    public void testIsPasswordRequiredWithStrictSetting() {
        PasswordUtil util = new PasswordUtil(sql, true);
        assertTrue(util.isPasswordRequired(null));
        assertTrue(util.isPasswordRequired(456l));
        assertFalse(util.isPasswordRequired(GUEST_ID));
    }

    // ~ Helpers
    // =========================================================================

    private void userIdReturnsNull() {
        mockSql.expects(once()).method("getUserId").will(returnValue(null));
    }

    private void userIdReturns1() {
        mockSql.expects(once()).method("getUserId").will(returnValue(new Long(1)));
    }

    private void ldapCreatesUser(boolean andReturns) {
        Experimenter e = andReturns ? new Experimenter() : null;
        createdUser.set(e);
        mockLdap.expects(once()).method("createUser").will(
                returnValue(andReturns));
    }

    private void ldapCreatesUserAndThrows() {
        createdUser.set(null);
        mockLdap.expects(once()).method("createUser").will(
                throwException(new ApiUsageException("")));
    }

    private void validateLdapPassword(boolean v) {
        validPassword.set(v);
    }

    private void assertChangeThrows() {
        try {
            provider.changePassword("", "");
            fail("must throw");
        } catch (PasswordChangeException pce) {
            // good.
        }
    }
}

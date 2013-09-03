package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import org.jboss.as.test.integration.management.rbac.JmxClientUtil;
import org.jboss.as.test.integration.management.rbac.JmxMgmtClient;
import org.jboss.as.test.integration.management.rbac.MgmtClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

/**
 * @author jcechace
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(UserRolesMappingServerSetupTask.StandardUsersSetup.class)
public class JmxMgmtStandardRolesBasicTestCase extends AbstractMgmtStandardRolesBasicTestCase{
    private static final String JMX_EXAMPLE_DS = "subsystem=datasources,data-source=ExampleDS";
    private static final String JMX_HTTP_BINDING =
            "socket-binding-group=standard-sockets,socket-binding=http";

    @Override
    protected MgmtClient createClient(String userName) throws Exception {
        return JmxMgmtClient.create(getManagementClient().getRemoteJMXURL(),
                userName,
                STD_PASSWORD,
                getDomain());
    }

    protected String getDomain() {
        return "jboss.as";
    }

    @Test @Override
    public void testMonitor() throws Exception {
        MgmtClient client = getClientForUser(MONITOR_USER);
        whoami(client, MONITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.UNAUTHORIZED);
        checkAttributeAccessInfo(client, true, false);
        checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Test @Override
    public void testOperator() throws Exception {
        MgmtClient client = getClientForUser(OPERATOR_USER);
        whoami(client, OPERATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.SUCCESS);
        checkAttributeAccessInfo(client, true, false);
        checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Test @Override
    public void testMaintainer() throws Exception {
        MgmtClient client = getClientForUser(MAINTAINER_USER);
        whoami(client, MAINTAINER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.SUCCESS);
        checkAttributeAccessInfo(client, true, true);
        checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Test @Override
    public void testDeployer() throws Exception {
        MgmtClient client = getClientForUser(DEPLOYER_USER);
        whoami(client, DEPLOYER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.UNAUTHORIZED);
        checkAttributeAccessInfo(client, true, false);
        checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Test @Override
    public void testAdministrator() throws Exception {
        MgmtClient client = getClientForUser(ADMINISTRATOR_USER);
        whoami(client, ADMINISTRATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        checkSensitiveAttributeDirectly(client, Outcome.SUCCESS);
        runGC(client, Outcome.SUCCESS);
        checkAttributeAccessInfo(client, true, true);
        checkSensitiveAttributeAccessInfo(client, true, true);
    }

    @Test
    public void testAuditor() throws Exception {
        MgmtClient client = getClientForUser(AUDITOR_USER);
        whoami(client, AUDITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        checkSensitiveAttributeDirectly(client, Outcome.SUCCESS);
        runGC(client, Outcome.UNAUTHORIZED);
        checkAttributeAccessInfo(client, true, false);
        checkSensitiveAttributeAccessInfo(client, false, false);
    }

    @Test
    public void testSuperUser() throws Exception {
        MgmtClient client = getClientForUser(SUPERUSER_USER);
        whoami(client, SUPERUSER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        checkSensitiveAttributeDirectly(client, Outcome.SUCCESS);
        runGC(client, Outcome.SUCCESS);
        checkAttributeAccessInfo(client, true, true);
        checkSensitiveAttributeAccessInfo(client, true, true);
    }

    private static void checkAttributeAccessInfo(MgmtClient client,
                                                 boolean read, boolean write) throws Exception {
        JmxMgmtClient jmxClient = (JmxMgmtClient) client;
        readAttributeAccessInfo(jmxClient, JMX_HTTP_BINDING,
                JmxClientUtil.toCammelCase(PORT), read, write);
    }

    private static void checkSensitiveAttributeAccessInfo(MgmtClient client,
                                                         boolean read, boolean write) throws Exception {
        JmxMgmtClient jmxClient = (JmxMgmtClient) client;
        readAttributeAccessInfo(jmxClient, JMX_EXAMPLE_DS, PASSWORD, read, write);
    }

    private static void readAttributeAccessInfo(JmxMgmtClient client, String address, String attribute,
                                                boolean read, boolean write) throws Exception {
        ObjectName objectName = client.objectName(address);
        MBeanInfo mBeanInfo = client.getConnection().getMBeanInfo(objectName);
        for (MBeanAttributeInfo attrInfo : mBeanInfo.getAttributes()) {
            if (attrInfo.getName().equals(attribute)) {
                Assert.assertEquals(read, attrInfo.isReadable());
                Assert.assertEquals(write, attrInfo.isWritable());
                return;
            }
        }
        fail("Attribute " + attribute + " not found on " + address);
    }


}

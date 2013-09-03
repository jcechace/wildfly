package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BYTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PASSWORD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USERNAME;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.ADMINISTRATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.AUDITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.DEPLOYER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MAINTAINER_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.MONITOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.OPERATOR_USER;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.SUPERUSER_USER;
import static org.junit.Assert.assertEquals;
import org.jboss.as.test.integration.management.rbac.MgmtClient;
import org.jboss.as.test.integration.management.rbac.NativeMgmtClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jcechace
 */
public abstract class AbstractMgmtStandardRolesBasicTestCase extends AbstractMgmtInterfaceRbacTest {
    private static final Map<String, MgmtClient> clients = new HashMap<String, MgmtClient>();

    protected static final String DEPLOYMENT_1 = "deployment=war-example.war";
    protected static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    protected static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    protected static final String MANAGEMENT_REALM = "core-service=management/security-realm=ManagementRealm";
    protected static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    protected static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    protected static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    protected static final String TEST_PATH = "path=rbac.test";


    public MgmtClient getClientForUser(String userName) throws Exception {
        MgmtClient result = clients.get(userName);
        if (result == null) {
            result = createClient(userName);
            clients.put(userName, result);
        }
        return result;
    }

    protected MgmtClient createClient(String userName) throws Exception {
        return NativeMgmtClient.create(getManagementClient().getMgmtAddress(),
                getManagementClient().getMgmtPort(),
                userName,
                STD_PASSWORD);
    }

    public static void removeClientForUser(String userName) throws IOException {
        MgmtClient client = clients.remove(userName);
        if (client != null) {
            client.close();
        }
    }


    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        // Tired of fighting Intellij to get it to pick up a file to include in the war so I can debug, I resort to...
        final String html = "<html><body>Hello</body></html>";
        war.addAsWebResource(new Asset() {
            @Override
            public InputStream openStream() {
                try {
                    return new ByteArrayInputStream(html.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "index.html");
        return war;
    }

    @After
    public void tearDown() throws IOException {
        AssertionError assertionError = null;
        try {
            removeResource(DEPLOYMENT_2);
        } catch (AssertionError e) {
            assertionError = e;
        } finally {
            removeResource(TEST_PATH);
        }


        if (assertionError != null) {
            throw assertionError;
        }
    }

    @Test
    public void testMonitor() throws Exception {
        MgmtClient client = getClientForUser(MONITOR_USER);
        whoami(client, MONITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testOperator() throws Exception {
        MgmtClient client = getClientForUser(OPERATOR_USER);
        whoami(client, OPERATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testMaintainer() throws Exception {
        MgmtClient client = getClientForUser(MAINTAINER_USER);
        whoami(client, MAINTAINER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    @Test
    public void testDeployer() throws Exception {
        MgmtClient client = getClientForUser(DEPLOYER_USER);
        whoami(client, DEPLOYER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        checkSensitiveAttributeDirectly(client, Outcome.UNAUTHORIZED);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testAdministrator() throws Exception {
        MgmtClient client = getClientForUser(ADMINISTRATOR_USER);
        whoami(client, ADMINISTRATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        checkSensitiveAttributeDirectly(client, Outcome.SUCCESS);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
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
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
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
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    protected static void checkStandardReads(MgmtClient client) throws IOException {
        readResource(client, null, Outcome.SUCCESS);
        readResource(client, DEPLOYMENT_1, Outcome.SUCCESS);
        readResource(client, HTTP_BINDING, Outcome.SUCCESS);
    }

    protected static ModelNode readResource(MgmtClient client, String address,
                                          Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        return client.executeForOutcome(op, expectedOutcome);
    }

    protected static ModelNode readAttribute(MgmtClient client, String address, String attribute,
                                      Outcome expectedOutcome) {
        ModelNode op = createOpNode(address, READ_ATTRIBUTE_OPERATION);
        op.get(NAME).set(attribute);
        return client.executeForOutcome(op, expectedOutcome);
    }

    protected static void checkSensitiveAttribute(MgmtClient client, boolean expectSuccess) throws IOException {
        ModelNode attrValue = readResource(client, EXAMPLE_DS, Outcome.SUCCESS).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    protected static void checkSensitiveAttributeDirectly(MgmtClient client,
                                                        Outcome expectedOutcome) throws IOException {
        ModelNode attrValue = readAttribute(client, EXAMPLE_DS, PASSWORD, expectedOutcome).get(RESULT);
        ModelNode correct = new ModelNode();
        if (expectedOutcome.equals(Outcome.SUCCESS)) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    protected static void runGC(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(MEMORY_MBEAN, "gc");
        client.executeForOutcome(op, expectedOutcome);
    }

    protected static void addDeployment2(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        client.executeForOutcome(op, expectedOutcome);
    }

    protected static void addPath(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(TEST_PATH, ADD);
        op.get(PATH).set("/");
        client.executeForOutcome(op, expectedOutcome);
    }

    protected static void whoami(MgmtClient client, String expectedUsername) {
        ModelNode op = createOpNode(null, "whoami");
        ModelNode result = client.executeForOutcome(op, Outcome.SUCCESS);
        String returnedUsername = result.get(RESULT, "identity", USERNAME).asString();
        assertEquals(expectedUsername, returnedUsername);
    }

    private void removeResource(String address) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        ModelNode result = getManagementClient().getControllerClient().execute(op);
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            op = createOpNode(address, REMOVE);
            result = getManagementClient().getControllerClient().execute(op);
            assertEquals(result.asString(), SUCCESS, result.get(OUTCOME).asString());
        }
    }

}

package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.arquillian.container.test.api.Deployment;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import org.jboss.as.test.integration.management.rbac.MgmtClient;
import org.jboss.as.test.integration.management.rbac.NativeMgmtClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import static org.jboss.as.test.integration.management.rbac.RbacUtil.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import static org.junit.Assert.assertEquals;
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

    private static final String DEPLOYMENT_1 = "deployment=war-example.war";
    private static final String DEPLOYMENT_2 = "deployment=rbac.txt";
    private static final byte[] DEPLOYMENT_2_CONTENT = "CONTENT".getBytes(Charset.defaultCharset());
    private static final String MANAGEMENT_REALM = "core-service=management/security-realm=ManagementRealm";
    private static final String HTTP_BINDING = "socket-binding-group=standard-sockets/socket-binding=http";
    private static final String MEMORY_MBEAN = "core-service=platform-mbean/type=memory";
    private static final String EXAMPLE_DS = "subsystem=datasources/data-source=ExampleDS";
    private static final String TEST_PATH = "path=rbac.test";


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
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testOperator() throws Exception {
        MgmtClient client = getClientForUser(OPERATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testMaintainer() throws Exception {
        MgmtClient client = getClientForUser(MAINTAINER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    @Test
    public void testDeployer() throws Exception {
        MgmtClient client = getClientForUser(DEPLOYER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.HIDDEN);
        checkSensitiveAttribute(client, false);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testAdministrator() throws Exception {
        MgmtClient client = getClientForUser(ADMINISTRATOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    @Test
    public void testAuditor() throws Exception {
        MgmtClient client = getClientForUser(AUDITOR_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.UNAUTHORIZED);
        addDeployment2(client, Outcome.UNAUTHORIZED);
        addPath(client, Outcome.UNAUTHORIZED);
    }

    @Test
    public void testSuperUser() throws Exception {
        MgmtClient client = getClientForUser(SUPERUSER_USER);
        checkStandardReads(client);
        readResource(client, MANAGEMENT_REALM, Outcome.SUCCESS);
        checkSensitiveAttribute(client, true);
        runGC(client, Outcome.SUCCESS);
        addDeployment2(client, Outcome.SUCCESS);
        addPath(client, Outcome.SUCCESS);
    }

    private static void checkStandardReads(MgmtClient client) throws IOException {
        readResource(client, null, Outcome.SUCCESS);
        readResource(client, DEPLOYMENT_1, Outcome.SUCCESS);
        readResource(client, HTTP_BINDING, Outcome.SUCCESS);
    }

    private static ModelNode readResource(MgmtClient client, String address, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(address, READ_RESOURCE_OPERATION);
        return client.executeForOutcome(op, expectedOutcome);
    }

    private static void checkSensitiveAttribute(MgmtClient client, boolean expectSuccess) throws IOException {
        ModelNode attrValue = readResource(client, EXAMPLE_DS, Outcome.SUCCESS).get(RESULT, PASSWORD);
        ModelNode correct = new ModelNode();
        if (expectSuccess) {
            correct.set("sa");
        }
        assertEquals(correct, attrValue);
    }

    private static void runGC(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(MEMORY_MBEAN, "gc");
        client.executeForOutcome(op, expectedOutcome);
    }

    private static void addDeployment2(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(DEPLOYMENT_2, ADD);
        op.get(ENABLED).set(false);
        ModelNode content = op.get(CONTENT).add();
        content.get(BYTES).set(DEPLOYMENT_2_CONTENT);

        client.executeForOutcome(op, expectedOutcome);
    }

    private static void addPath(MgmtClient client, Outcome expectedOutcome) throws IOException {
        ModelNode op = createOpNode(TEST_PATH, ADD);
        op.get(PATH).set("/");
        client.executeForOutcome(op, expectedOutcome);
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

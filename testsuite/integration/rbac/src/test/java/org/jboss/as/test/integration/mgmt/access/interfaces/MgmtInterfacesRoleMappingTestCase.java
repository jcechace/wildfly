package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import org.jboss.as.test.integration.management.rbac.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

/**
 * @author jcechace
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(UserRolesMappingServerSetupTask.StandardUsersSetup.class)
public class MgmtInterfacesRoleMappingTestCase extends AbstractMgmtInterfaceRbacTest {
    private static final String ROLE_MAPPING_ADDRESS_BASE = "core-service=management/access=authorization/role-mapping=";
    private static final String TEST_ROLE_MAPPING =  ROLE_MAPPING_ADDRESS_BASE +  "TestRoleMapping";
    private static final String TEST_ROLE_MAPPING_2 = ROLE_MAPPING_ADDRESS_BASE + "TestRoleMapping2";
    private static final String ROLE_INCLUSION_USER =  TEST_ROLE_MAPPING_2 + "/include=user-";
    private static final String TEST_USER =  "testUser";
    private static final String TEST_USER_2= "testUser2";

    private MgmtClient client;



    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "war-example.war");
        war.addAsWebResource("index.html");
        return war;
    }

    @Before
    public void setup() throws IOException {
        addIfNotExists(TEST_ROLE_MAPPING_2,
                getManagementClient().getControllerClient());
        addIfNotExists(ROLE_INCLUSION_USER + TEST_USER_2,
                getManagementClient().getControllerClient(),
                "name=" + TEST_USER_2, "type=user");
    }

    @After
    public void teardown() throws IOException {
        removeIfExists(TEST_ROLE_MAPPING,
                getManagementClient().getControllerClient());
        removeIfExists(ROLE_INCLUSION_USER + TEST_USER,
                getManagementClient().getControllerClient());
    }

    @Test
    public void httpClientTest() throws IOException {
        client = HttpMgmtClient.create(
                getManagementClient().getMgmtAddress(),
                getManagementClient().getMgmtPort(),
                RbacUtil.SUPERUSER_USER,
                STD_PASSWORD
        );
        addRoleMapping(client);
        removeUserInclusion(client);
        addUserInclusion(client);
        removeRoleMapping(client);
    }

    @Test
    public void cliClientTest() throws IOException {
        client = CliMgmtClient.create(
                getManagementClient().getMgmtAddress(),
                getManagementClient().getMgmtPort(),
                RbacUtil.SUPERUSER_USER,
                STD_PASSWORD
        );
        addRoleMapping(client);
        removeUserInclusion(client);
        addUserInclusion(client);
        removeRoleMapping(client);
    }


    public void addRoleMapping(MgmtClient client) throws IOException {
        ModelNode op = createOpNode(TEST_ROLE_MAPPING, ADD);
        System.out.println("Trying to add role mapping " + TEST_ROLE_MAPPING);
        client.executeForOutcome(op, Outcome.SUCCESS);
        checkIfExists(TEST_ROLE_MAPPING, true, getManagementClient().getControllerClient());
    }

    public void removeRoleMapping(MgmtClient client) throws IOException {
        ModelNode op = createOpNode(TEST_ROLE_MAPPING_2, REMOVE);
        System.out.println("Trying to remove role mapping " + TEST_ROLE_MAPPING);
        client.executeForOutcome(op, Outcome.SUCCESS);
        checkIfExists(TEST_ROLE_MAPPING_2, false, getManagementClient().getControllerClient());
    }

    public void addUserInclusion(MgmtClient client) throws IOException {
        String address = ROLE_INCLUSION_USER + TEST_USER;
        ModelNode op = createOpNode(address, ADD);
        setAttributes(op, "name=" + TEST_USER, "type=user");
        System.out.println("Trying to add user inclusion " + address);
        client.executeForOutcome(op, Outcome.SUCCESS);
        checkIfExists(address, true, getManagementClient().getControllerClient());
    }

    public void removeUserInclusion(MgmtClient client) throws IOException {
        String address = ROLE_INCLUSION_USER + TEST_USER_2;
        ModelNode op = createOpNode(address, REMOVE);
        System.out.println("Trying to remove user inclusion " + address);
        client.executeForOutcome(op, Outcome.SUCCESS);
        checkIfExists(address, false, getManagementClient().getControllerClient());
    }
}

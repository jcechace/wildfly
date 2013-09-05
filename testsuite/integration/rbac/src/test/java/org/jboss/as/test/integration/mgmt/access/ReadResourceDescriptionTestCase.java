package org.jboss.as.test.integration.mgmt.access;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
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
public class ReadResourceDescriptionTestCase extends AbstractRbacTestCase {
    private static final String TEST_DS = "subsystem=datasources/data-source=TestDS";

    @ContainerResource
    private ManagementClient managementClient;

    @After
    public void cleanUp() throws IOException {
        removeResource(TEST_DS);
    }

    @Test
    public void testMonitor() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.MONITOR_ROLE);
    }

    @Test
    public void testOperator() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.OPERATOR_ROLE);
    }

    @Test
    public void testMaintainer() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.MAINTAINER_ROLE);
    }

    @Test
    public void testDeployer() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.DEPLOYER_ROLE);
    }

    @Test
    public void testAdministrator() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.ADMINISTRATOR_ROLE);
    }

    @Test
    public void testAuditor() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.AUDITOR_ROLE);
    }

    @Test
    public void testSuperUser() throws Exception {
        addOperationWithSensitiveAttribute(RbacUtil.SUPERUSER_ROLE);
    }


    public void addOperationWithSensitiveAttribute(String role) throws IOException {
        ModelNode op = createOpNodeForRole(TEST_DS, ADD, role);
        op.get("connection-url").set("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        op.get("jndi-name").set("java:jboss/datasources/TestDS");
        op.get("driver-name").set("h2");

        boolean canExecute = canExecuteOperation(TEST_DS, ADD, role);

        RbacUtil.executeOperation(managementClient.getControllerClient(), op,
                canExecute ? Outcome.SUCCESS : Outcome.UNAUTHORIZED);
    }

    private boolean canExecuteOperation(String path, String opName, String role)
            throws IOException {
        ModelNode accessInfo = readAcccessControl(path, role);
        return accessInfo.get(RESULT)
                .get(ACCESS_CONTROL).get(DEFAULT)
                .get(OPERATIONS).get(opName)
                .get(EXECUTE).asBoolean();
    }

    private ModelNode readAcccessControl(String path, String role) throws IOException {
        ModelNode op = createOpNodeForRole(path , READ_RESOURCE_DESCRIPTION_OPERATION, role);
        System.out.println(op);
        op.get(OPERATIONS).set(true);
        op.get(ACCESS_CONTROL).set("trim-descriptions");
        return RbacUtil.executeOperation(managementClient.getControllerClient(),
                op, Outcome.SUCCESS);
    }

    private ModelNode createOpNodeForRole(String path, String opName, String... roles) {
        ModelNode op = ModelUtil.createOpNode(path, opName);
        RbacUtil.addRoleHeader(op, roles);
        return op;
    }


}

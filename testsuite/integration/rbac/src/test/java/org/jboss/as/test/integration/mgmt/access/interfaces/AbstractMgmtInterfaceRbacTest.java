package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import org.jboss.as.test.integration.management.rbac.MgmtClient;
import org.jboss.as.test.integration.management.rbac.NativeMgmtClient;
import org.jboss.as.test.integration.management.rbac.Outcome;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jcechace
 */
public class AbstractMgmtInterfaceRbacTest {
    public static final String STD_PASSWORD = "t3stSu!tePassword";

    @ContainerResource
    private ManagementClient managementClient;

    public ManagementClient getManagementClient() {
        return managementClient;
    }



    public void addIfNotExists(String address, ModelControllerClient client, String... attrs) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        if (FAILED.equals(client.execute(readOp).get(OUTCOME).asString())) {
            System.out.println("Creating resource " + address);
            ModelNode addOp = createOpNode(address, ADD);
            setAttributes(addOp, attrs);
            RbacUtil.executeOperation(client, addOp, Outcome.SUCCESS);
        }
        checkIfExists(address, true, client);
    }

    public void removeIfExists(String address, ModelControllerClient client) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        if (SUCCESS.equals(client.execute(readOp).get(OUTCOME).asString())) {
            System.out.println("Removing resource " + address);
            ModelNode removeOp = createOpNode(address, REMOVE);
            RbacUtil.executeOperation(client, removeOp, Outcome.SUCCESS);
        }
        checkIfExists(address, false, client);
    }

    public void checkIfExists(String address, boolean shouldExist, ModelControllerClient client) throws IOException {
        ModelNode readOp = createOpNode(address, READ_RESOURCE_OPERATION);
        ModelNode result = client.execute(readOp);
        String expected = shouldExist ? SUCCESS : FAILED;
        System.out.println("Validating existence of " + address);
        Assert.assertEquals(expected, result.get(OUTCOME).asString());
        System.out.println("OK - "  + (shouldExist ? "exists" : "not found"));
    }

    public void setAttributes(ModelNode op, String... attrs) {
        for(String attr : attrs) {
            String[] parts = attr.split("=");
            op.get(parts[0]).set(parts[1]);
        }
    }
}

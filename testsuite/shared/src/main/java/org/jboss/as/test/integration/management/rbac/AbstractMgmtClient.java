package org.jboss.as.test.integration.management.rbac;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import org.jboss.dmr.ModelNode;
import static org.junit.Assert.fail;

/**
 * @author jcechace
 */
public  abstract class AbstractMgmtClient implements MgmtClient {
    @Override
    public ModelNode executeForOutcome(ModelNode operation, Outcome expectedOutcome) {
        ModelNode result = execute(operation);
        String outcome = result.get(OUTCOME).asString();
        switch (expectedOutcome) {
            case SUCCESS:
                if (!SUCCESS.equals(outcome)) {
                    System.out.println("Failed: " + operation);
                    System.out.print("Result: " + result);
                    fail(result.get(FAILURE_DESCRIPTION).asString());
                }
                break;
            case UNAUTHORIZED:
                if (!FAILED.equals(outcome)) {
                    fail("Didn't fail: " + result.asString());
                }
                if (!result.get(FAILURE_DESCRIPTION).asString().contains("13456")) {
                    fail("Incorrect failure type: " + result.asString());
                }
                break;
            case HIDDEN:
                if (!FAILED.equals(outcome)) {
                    fail("Didn't fail: " + result.asString());
                }
                if (!result.get(FAILURE_DESCRIPTION).asString().contains("14807")) {
                    fail("Incorrect failure type: " + result.asString());
                }
                break;
            default:
                throw new IllegalStateException();
        }
        return result;
    }
}

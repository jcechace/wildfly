package org.jboss.as.test.integration.mgmt.access.interfaces;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.rbac.HttpMgmtClient;
import org.jboss.as.test.integration.management.rbac.MgmtClient;
import org.jboss.as.test.integration.management.rbac.UserRolesMappingServerSetupTask;
import org.junit.runner.RunWith;

/**
 * @author jcechace
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(UserRolesMappingServerSetupTask.StandardUsersSetup.class)
public class HttpMgmtStandardRolesBasicTestCase extends AbstractMgmtStandardRolesBasicTestCase {

    @Override
    protected MgmtClient createClient(String userName) throws Exception {
        return HttpMgmtClient.create(getManagementClient().getMgmtAddress(),
                getManagementClient().getMgmtPort(),
                userName,
                STD_PASSWORD);
    }

}

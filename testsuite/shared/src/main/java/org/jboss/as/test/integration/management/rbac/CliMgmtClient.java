package org.jboss.as.test.integration.management.rbac;

import org.jboss.as.cli.scriptsupport.CLI;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

import java.util.Iterator;

/**
 * @author jcechace
 */
public class CliMgmtClient extends AbstractMgmtClient implements MgmtClient {

    private CLI client;

    protected CliMgmtClient(CLI client) {
        this.client = client;
    }

    @Override
    public ModelNode execute(ModelNode operation) {
        String command = createCommand(operation);
        System.out.println("# Executing command: " + command);
        return client.cmd(command).getResponse();
    }

    private static String createCommand(ModelNode operation) {
        StringBuilder command = new StringBuilder();
        if(operation.has(ClientConstants.OP_ADDR)) {
            ModelNode address = operation.get(ClientConstants.OP_ADDR);
            for (ModelNode key : address.asList()) {
                Property segment = key.asProperty();
                command.append("/" + segment.getName() + "=" + segment.getValue());
            }
            operation.remove(ClientConstants.OP_ADDR);
        }

        if (operation.has(ClientConstants.OP)) {
            ModelNode op = operation.get(ClientConstants.OP);
            command.append(":" + op.asString());
            operation.remove(ClientConstants.OP);
        }

        if (operation.has(ClientConstants.OPERATION_HEADERS)) {
            throw new UnsupportedOperationException(ClientConstants.OPERATION_HEADERS + " are not" +
                    "supported");
        }

        StringBuilder attrs = new StringBuilder();
        Iterator<String> keys = operation.keys().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            ModelNode value = operation.get(key);
            if (value.getType() != ModelType.OBJECT) {
                attrs.append(key + "=" + value.asString());
            }
            if (keys.hasNext()) {
                attrs.append(",");
            }
        }
        command.append("(" + attrs + ")");
        return command.toString();
    }

    @Override
    public void close() {
        client.disconnect();
    }

    public static MgmtClient create(String host, int port, final String username, final String password) {
        CLI client = CLI.newInstance();
        client.connect(host, port, username, password.toCharArray());
        return new CliMgmtClient(client);
    }
}
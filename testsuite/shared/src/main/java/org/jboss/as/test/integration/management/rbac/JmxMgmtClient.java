package org.jboss.as.test.integration.management.rbac;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_CONTROL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author jcechace
 */
public class JmxMgmtClient extends AbstractMgmtClient implements MgmtClient {

    private final JMXConnector jmxConnector;
    private final String domain;

    protected JmxMgmtClient(JMXConnector jmxConnector, String domain) {
        this.jmxConnector = jmxConnector;
        this.domain = domain;
    }

    public void close() {
        try {
            jmxConnector.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public String getDomain() {
        return domain;
    }


    @Override
    public ModelNode execute(ModelNode operation) {
        try {
            ObjectName  objectName = objectName(operation);
            return doExecute(objectName, operation);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode doExecute(ObjectName objectName,ModelNode op) {
        String opName =  op.get(OP).asString();
        if (READ_ATTRIBUTE_OPERATION.equals(opName)) {
            String name = JmxClientUtil.toCammelCase(op.get(NAME).asString());
            return getAttribute(objectName, name);
        } else if (WRITE_ATTRIBUTE_OPERATION.equals(opName)) {
            String name = JmxClientUtil.toCammelCase(op.get(NAME).asString());
            Object value = object(op.get(VALUE));
            return setAttribute(objectName, name, value);
        } else if (ADD.equals(opName)) {
            throw new UnsupportedOperationException("Operations with parameters are not supported");
        } else if (READ_RESOURCE_OPERATION.equals(opName)) {
           return getInfo(objectName);
        } else {
           String name = JmxClientUtil.toCammelCase(op.get(OP).asString());
           return invoke(objectName, name);
        }
    }

    public ModelNode getAttribute(ObjectName objectName, String name) {
        MBeanServerConnection connection = getConnection();

        Object result = null;
        JMException exception = null;
        try {
            System.out.println("Reading attribute " + name  + " of " + objectName);
            result = connection.getAttribute(objectName, name);
        } catch (JMException e) {
            exception = e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ModelNode modelResult = modelNodeResult(result, exception);
        System.out.println(modelResult.asString());
        return modelResult;
    }

    public ModelNode setAttribute(ObjectName objectName,
                             String name, Object value) {
        MBeanServerConnection connection = getConnection();

        Attribute attribute = new Attribute(name, value);
        JMException exception = null;
        try {
            System.out.println("Setting attribute " + name  + "=" + value + " of " + objectName);
            connection.setAttribute(objectName, attribute);
        } catch (JMException e) {
            exception = e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return modelNodeResult(null, exception);
    }

    public ModelNode invoke(ObjectName objectName, String name) {
        MBeanServerConnection connection = getConnection();

        Object result = null;
        JMException exception = null;
        try {
            System.out.println("Executing operation " + name  + " on " + objectName);
            result = connection.invoke(objectName, name, null, null);
        } catch (JMException e) {
            exception = e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ModelNode modelResult = modelNodeResult(result, exception);
        System.out.println(modelResult.asString());
        return modelResult;
    }


    public ModelNode getInfo(ObjectName objectName) {
        MBeanServerConnection connection = getConnection();
        ModelNode attributes = null;
        ModelNode headers = null;
        JMException exception = null;
        try {
            System.out.println("Reading MBeanInfo of " + objectName);
            MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
            MBeanAttributeInfo[] attributeInfos = mBeanInfo.getAttributes();
            ModelNode[] data = modelNodeAttributesInfo(attributeInfos, objectName);
            attributes = data[0];
            headers = data[1];
        } catch (JMException e) {
            exception = e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ModelNode result = modelNodeResult(attributes, exception, headers);
        System.out.println(result.asString());
        return result;
    }

    private ModelNode[] modelNodeAttributesInfo(MBeanAttributeInfo[] attributeInfos,
                                              ObjectName objectName) throws IOException {
        MBeanServerConnection connection = getConnection();
        ModelNode attributes = new ModelNode();
        ModelNode filtered = new ModelNode().setEmptyList();
        ModelNode headers = null;

        for (MBeanAttributeInfo attribute : attributeInfos) {
            String attributeName = attribute.getName();
            try {
                Object attributeValue = connection.getAttribute(objectName, attributeName);
                try {
                    attributes.get(JmxClientUtil.toDashCase(attributeName)).set(JmxClientUtil.modelNode(attributeValue));
                } catch (UnsupportedOperationException e) {}
            } catch (JMException e) {
                // TODO: MBeanAttributeInfo#isReadable is currently broken!
                filtered.add(JmxClientUtil.toDashCase(attributeName));
            }
        }
        if (!filtered.asList().isEmpty()) {
            headers = new ModelNode();
            headers.get(ACCESS_CONTROL, "filtered-attributes").set(filtered);
        }
        return new ModelNode[] {attributes, headers};
    }

    private ModelNode modelNodeResult(Object result, JMException exception) {
        return modelNodeResult(result, exception, null);
    }

    private ModelNode modelNodeResult(Object result, JMException exception, ModelNode headers) {
        ModelNode root = new ModelNode();
        if (exception == null) {
            root.get(OUTCOME).set(SUCCESS);
            if (result != null){
                root.get(RESULT).set(JmxClientUtil.modelNode(result));
            }
        } else {
            String message;
            if (exception instanceof InstanceNotFoundException) {
                message = ControllerMessages.MESSAGES
                        .managementResourceNotFound(PathAddress.EMPTY_ADDRESS)
                        .getMessage();
            }
            message = JmxClientUtil.rawString(exception.getMessage());
            root.get(OUTCOME).set(FAILED);
            root.get(FAILURE_DESCRIPTION).set(message);
            if (result != null) {
                root.get(RESULT).set(JmxClientUtil.modelNode(result));
            }
        }

        if (headers != null) {
            root.get(RESPONSE_HEADERS).set(headers);
        }
        return root;
    }

    private static Object object(ModelNode node) {
        switch (node.getType()) {
            case BIG_DECIMAL:   return node.asBigDecimal();
            case BIG_INTEGER:   return node.asBigInteger();
            case BOOLEAN:       return node.asBoolean();
            case BYTES:         return node.asBytes();
            case DOUBLE:        return node.asDouble();
            case EXPRESSION:    return node.asExpression();
            case INT:           return node.asInt();
            case LIST:          return node.asList();
            case LONG:          return node.asLong();
            case PROPERTY:      return node.asProperty();
            case STRING:        return node.asString();
            case UNDEFINED:     return null;
            default:    throw new UnsupportedOperationException
                    ("Can't convert '" + node.getType() + "' to object");
        }
    }

    public ObjectName objectName(String name) throws MalformedObjectNameException {
        return new ObjectName(getDomain() + ":" + name);
    }

    public ObjectName objectName(ModelNode operation) throws MalformedObjectNameException {
        StringBuilder builder = new StringBuilder();
        String opName = operation.get(OP).asString();
        if(operation.has(OP_ADDR)) {
            ModelNode address = operation.get(OP_ADDR);
            Iterator<ModelNode> it =address.asList().iterator();
            while (it.hasNext()) {
                Property segment = it.next().asProperty();
                if (opName.equals(ADD) && !it.hasNext()) {
                    continue;
                }
                builder.append(segment.getName() + "=" + segment.getValue().asString() + ",");
            }
        }
        if (builder.toString().isEmpty()) {
            builder.append("management-root=server,");
        }

        builder.deleteCharAt(builder.length() - 1);
        return objectName(builder.toString());
    }



    public MBeanServerConnection getConnection() {
        MBeanServerConnection mBeanServerConnection = null;
        try {
            mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mBeanServerConnection;
    }

    public static JmxMgmtClient create(JMXServiceURL url, final String username,
                                       final String password, final String domain) {
        try {
            Map<String, String[]> env = new HashMap<String, String[]>();
            env.put(JMXConnector.CREDENTIALS, new String[]{username, password});
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url, env);
            return new JmxMgmtClient(jmxConnector, domain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

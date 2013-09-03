package org.jboss.as.test.integration.management.rbac;

import org.jboss.dmr.ModelNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jcechace
 */
public class JmxClientUtil {
    public static String rawString(String message) {
        String raw = removeQuotes(message);
        // This is need as StringModelValue#toString() returns escaped output
        return removeEscapes(raw);
    }

    private static String removeQuotes(String string) {
        if (string.startsWith("\"") && string.endsWith("\"")){
            string  = string.substring(1, string.length() - 1);
        }
        return string;
    }

    private static String  removeEscapes(String string) {
        Pattern pattern = Pattern.compile("\\\\(.)");
        Matcher matcher = pattern.matcher(string);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String followup = matcher.group();
            matcher.appendReplacement(result, followup);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String toDashCase(String string) {
        String regex = "([a-z])([A-Z])";
        String replacement = "$1-$2";
        return string.replaceAll(regex, replacement).toLowerCase();
    }

    public static String toCammelCase(String str) {
        Pattern pattern = Pattern.compile("-([a-z])");
        Matcher matcher = pattern.matcher(str);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String upperCaseLetter = matcher.group(1).toUpperCase();
            matcher.appendReplacement(result, upperCaseLetter);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static ModelNode modelNode(Object obj) {
        if (obj == null) {
            return new ModelNode();
        } else if (obj instanceof ModelNode) {
            return ((ModelNode) obj);
        } else if (obj instanceof BigDecimal) {
            return new ModelNode((BigDecimal) obj);
        } else if (obj instanceof BigInteger) {
            return new ModelNode((BigInteger) obj);
        } else if (obj instanceof Boolean) {
            return new ModelNode((Boolean) obj);
        } else if (obj instanceof byte[]) {
            return new ModelNode((byte[]) obj);
        } else if (obj instanceof Double) {
            return new ModelNode((Double) obj);
        } else if (obj instanceof Integer) {
            return new ModelNode((Integer) obj);
        } else if (obj instanceof Long) {
            return new ModelNode((Long) obj);
        } else if (obj instanceof String) {
            return nodeFromString((String) obj);
        } else {
            throw new UnsupportedOperationException("Can't convert '" + obj.getClass() + "' to ModelNode: " + obj);
        }
    }

    private static ModelNode nodeFromString(String string) {
        ModelNode result;
        try {
            result =  ModelNode.fromString(string);
        } catch (Exception e) {
            try {
                result = ModelNode.fromJSONString(string);
            } catch (Exception e1) {
                result = new ModelNode(string);
            }
        }
        return result;
    }

}

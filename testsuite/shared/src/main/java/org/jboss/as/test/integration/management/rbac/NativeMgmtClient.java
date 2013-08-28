/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.management.rbac;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class NativeMgmtClient extends AbstractMgmtClient implements MgmtClient {
    private static final Map<String, String> SASL_OPTIONS = Collections.singletonMap("SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");

    private final ModelControllerClient client;

    public NativeMgmtClient(ModelControllerClient client) {
        this.client = client;
    }

    @Override
    public ModelNode execute(ModelNode operation) {
        try {
            return client.execute(operation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MgmtClient create(String host, int port) {
        try {
            ModelControllerClient client = ModelControllerClient.Factory.create(host, port);
            return new NativeMgmtClient(client);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static MgmtClient create(String host, int port, final String username, final String password) {
        try {
            ModelControllerClient client = ModelControllerClient.Factory.create(host, port, new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback callback : callbacks) {
                        if (callback instanceof NameCallback) {
                            ((NameCallback) callback).setName(username);
                            System.out.println("set user " + username);
                        } else if (callback instanceof PasswordCallback) {
                            ((PasswordCallback) callback).setPassword(password.toCharArray());
                            System.out.println("set password " + password);
                        } else if (callback instanceof RealmCallback) {
                            RealmCallback rcb = (RealmCallback) callback;
                            rcb.setText(rcb.getDefaultText());
                        } else {
                            throw new UnsupportedCallbackException(callback);
                        }
                    }
                }
            }, SASL_OPTIONS);
            return new NativeMgmtClient(client);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
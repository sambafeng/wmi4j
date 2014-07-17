/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wmi4j;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.*;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 * <p>
 * You can use the methods of the {@link SWbemLocator} object to obtain an SWbemServices object that represents
 * a connection to a namespace on either a local computer or a remote host computer.
 * You can then use the methods of the {@link org.wmi4j.SWbemServices} object to access WMI.
 * </p>
 * Created by chenlichao on 14-7-17.
 */
public class SWbemLocator {

    private static final Logger logger = LoggerFactory.getLogger(SWbemLocator.class);

    private static final String WMI_CLSID = "76A6415B-CB41-11d1-8B02-00600806D9B6";

    private JISession session;

    private SWbemServices services;

    private String server;
    private String username;
    private String password;
    private String namespace;

    /**
     *
     * @param server Computer name or ip to which you are connecting.
     * @param username User name to use to connect. The string can be in the form of either a user name or a Domain\Username.
     * @param password String that specifies the password to use when attempting to connect.
     * @param namespace String that specifies the namespace to which you log on.
     *                  <p><strong>Note: Optional parameter, if you set it to null, then use the default namespace root\CIMv2</strong></p>
     */
    public SWbemLocator(String server, String username, String password, String namespace) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.namespace = namespace;
    }

    /**
     * <p>The ConnectServer method of the {@link org.wmi4j.SWbemLocator} object connects to the namespace on the computer
     * that is specified in the strServer parameter. The target computer can be either local or remote,
     * but it must have WMI installed. For examples and a comparison with the moniker type of connection,
     * see Creating a WMI Script.</p>
     *
     * <p>Starting with Windows Vista, SWbemLocator.connectServer
     * can connect with computers running IPv6 using an IPv6 address in the strServer parameter.
     * For more information, see IPv6 and IPv4 Support in WMI.</p>
     *
     *<p><strong>Windows Server 2003 and Windows XP:  You only can use SWbemLocator.connectServer
     * to connect to computers with IPv4 addresses.</strong></p>
     *
     * @param locale String that specifies the localization code. If you want to use the current locale, set to null.
     *               If not null, this parameter must be a string that indicates the desired locale where information must be retrieved.
     *               For Microsoft locale identifiers, the format of the string is "MS_xxxx",
     *               where xxxx is a string in the hexadecimal form that indicates the LCID.
     *               For example, American English would appear as "MS_409".
     * @param authority This parameter is optional. However, if it is specified, only Kerberos or NTLMDomain can be used.
     * @param securityFlag Used to pass flag values to connectServer method.
     *                     <ul>
     *                          <li><strong>0(0x0): </strong>Causes the server to return only after the connection to the server is established.
     *                          This could cause your program to stop responding indefinitely if the connection cannot be established.</li>
     *                          <li><strong>128(0x80): </strong>Causes the server to return in 2 minutes or less.
     *                          Use this flag to prevent your program from ceasing to respond indefinitely
     *                          if the connection cannot be established.</li>
     *                     </ul>
     * @param objwbemNamedValueSet Typically, this is undefined.
     * @return If successful, WMI returns an {@link org.wmi4j.SWbemServices} object that is bound to the namespace
     *              that is specified in namespace parameter on the computer that is specified in server parameter.
     * @throws WMIException Failed to connect to the server.
     * @throws UnknownHostException
     */
    public SWbemServices connectServer(String locale, String authority, Integer securityFlag,
                                       SWbemNamedValueSet objwbemNamedValueSet) throws WMIException, UnknownHostException {
        String hostPath = "\\\\" + server + "\\" + namespace;
        logger.info("Connect to {} ...", hostPath);
        try {
            // Initialize Session
            try {
                JISystem.setInBuiltLogHandler(false);
                JISystem.getLogger().setLevel(Level.FINEST);
            } catch (Exception e) {
                logger.warn("Exception occurred when disable integrated log.");
            }
            String userDomain = "";
            String user = username;
            if(username.contains("\\")) {
                String[] du = username.split("\\\\");
                if(du.length != 2) {
                    throw new IllegalArgumentException("Invalid username: " + username);
                }
                userDomain = du[0];
                user = du[1];
            }
            session = JISession.createSession(userDomain, user, password);
            session.useSessionSecurity(true);
            session.setGlobalSocketTimeout(5000);

            //Obtain WbemScripting.SWbemLocator object
            JIComServer comStub = new JIComServer(JIClsid.valueOf(WMI_CLSID), server, session);
            IJIComObject unknown = comStub.createInstance();
            IJIComObject wbemLocatorObj = unknown.queryInterface(WMI_CLSID);
            IJIDispatch wbemLocatorDispatch =  (IJIDispatch) JIObjectFactory.narrowObject(wbemLocatorObj.queryInterface(IJIDispatch.IID));

            // Call WbemScripting.SWbemLocator.ConnectServer method，obtain SWbemServices object
            JIVariant[] results = wbemLocatorDispatch.callMethodA("ConnectServer", new Object[]{
                    JIVariant.OPTIONAL_PARAM(), namespace, JIVariant.OPTIONAL_PARAM(), JIVariant.OPTIONAL_PARAM(),
                    (locale == null) ? JIVariant.OPTIONAL_PARAM() : new JIString(locale),
                    (authority == null) ? JIVariant.OPTIONAL_PARAM() : new JIString(authority),
                    (securityFlag == null) ? 0 : securityFlag, JIVariant.OPTIONAL_PARAM()
            });
            IJIDispatch wbemServiceDispatch = (IJIDispatch)JIObjectFactory.narrowObject(results[0].getObjectAsComObject());

            services = new SWbemServices(wbemServiceDispatch);
        } catch (JIException e) {
            throw new WMIException(e.getErrorCode(), e.getMessage(), e.getCause());
        }
        return services;
    }

    /**
     * Obtain {@link org.wmi4j.SWbemServices} object;
     * @return {@link org.wmi4j.SWbemServices} object
     * @exception IllegalStateException If call before connect to the server.
     */
    public SWbemServices getSWbemServices() {
        if(services == null) {
            throw new IllegalStateException("Please connect to the server first.");
        }
        return services;
    }

    /**
     * Disconnect from server and release resources
     * @throws WMIException
     */
    public void disconnect() throws WMIException {
        try {
            JISession.destroySession(session);
        } catch (JIException e) {
            throw new WMIException(e);
        }
    }
}
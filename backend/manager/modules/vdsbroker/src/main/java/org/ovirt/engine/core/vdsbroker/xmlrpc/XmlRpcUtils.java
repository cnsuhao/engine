package org.ovirt.engine.core.vdsbroker.xmlrpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcCommonsTransport;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.utils.ThreadLocalParamsContainer;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.ssl.AuthSSLProtocolSocketFactory;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.vdsbroker.vdsbroker.FutureCall;

public class XmlRpcUtils {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static Log log = LogFactory.getLog(XmlRpcUtils.class);
    static {
        if (Config.<Boolean> GetValue(ConfigValues.UseSecureConnectionWithServers)) {
            URL keystoreUrl;
            try {
                keystoreUrl = new URL("file://" + Config.resolveKeyStorePath());
                String keystorePassword = Config.<String> GetValue(ConfigValues.keystorePass);
                URL truststoreUrl = new URL("file://" + Config.resolveTrustStorePath());
                String truststorePassword = Config.<String> GetValue(ConfigValues.TruststorePass);

                // registering the https protocol with a socket factory that
                // provides client authentication.
                ProtocolSocketFactory factory = new AuthSSLProtocolSocketFactory(keystoreUrl, keystorePassword,
                        truststoreUrl, truststorePassword);
                Protocol clientAuthHTTPS = new Protocol("https", factory, 54321);
                Protocol.registerProtocol("https", clientAuthHTTPS);
            } catch (Exception e) {
                log.fatal("Failed to init AuthSSLProtocolSocketFactory. SSL connections will not work", e);
            }
        }
    }

    /**
     * wrapper for the apache xmlrpc client factory. gets the xmlrpc connection parameters and an interface to implement
     * and returns an instance which implements the given interface.
     * @param <T>
     *            the type of the instance for the interface
     * @param hostName
     *            - the host to connect to
     * @param port
     *            - the port to connect to
     * @param clientTimeOut
     *            - the time out for the connection
     * @param type
     *            - the instance type of the interface for this connection
     * @param isSecure
     *            - if a connection should be https or http
     * @return an instance of the given type.
     */
    public static <T> Pair<T, HttpClient> getConnection(String hostName, int port, int clientTimeOut,
            Class<T> type, boolean isSecure) {
        URL serverUrl;
        String prefix;
        if (isSecure) {
            prefix = HTTPS;
        } else {
            prefix = HTTP;
        }
        try {
            serverUrl = new URL(prefix + hostName + ":" + port);
        } catch (MalformedURLException mfue) {
            log.error("failed to forme the xml-rpc url", mfue);
            return null;
        }
        return getHttpConnection(serverUrl, clientTimeOut, type);
    }

    public static void shutDownConnection(HttpClient httpClient) {
        if (httpClient != null && httpClient.getHttpConnectionManager() != null) {
            ((MultiThreadedHttpConnectionManager) (httpClient).getHttpConnectionManager()).shutdown();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Pair<T, HttpClient> getHttpConnection(URL serverUrl, int clientTimeOut,
            Class<T> type) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(serverUrl);
        config.setConnectionTimeout(clientTimeOut);
        config.setReplyTimeout(clientTimeOut);
        XmlRpcClient xmlRpcClient = new XmlRpcClient();
        xmlRpcClient.setConfig(config);

        XmlRpcCommonsTransportFactory transportFactory = new CustomXmlRpcCommonsTransportFactory(xmlRpcClient);
        HttpClient httpclient = new HttpClient(new MultiThreadedHttpConnectionManager());
        transportFactory.setHttpClient(httpclient);
        xmlRpcClient.setTransportFactory(transportFactory);

        ClientFactory clientFactory = new ClientFactory(xmlRpcClient);
        T connector = (T) clientFactory.newInstance(Thread.currentThread().getContextClassLoader(), type, null);
        T asyncConnector = (T) AsyncProxy.newInstance(connector, clientTimeOut);

        Pair<T, HttpClient> returnValue =
                new Pair<T, HttpClient>(asyncConnector, httpclient);

        return returnValue;
    }

    private static class AsyncProxy implements InvocationHandler {

        private Object obj;
        private long timeoutInMilisec;

        public static Object newInstance(Object obj, long timeoutInMilisec) {
            return Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    obj.getClass().getInterfaces(),
                    new AsyncProxy(obj, timeoutInMilisec));
        }

        private AsyncProxy(Object obj, long timeoutInMilisec) {
            this.obj = obj;
            this.timeoutInMilisec = timeoutInMilisec;
        }

        @Override
        public Object invoke(Object proxy, final Method m, final Object[] args)
                throws Throwable {
            Object result;
            FutureTask<Object> future;
            FutureCall annotation = m.getAnnotation(FutureCall.class);
            if (annotation != null) {
                future = new FutureTask<Object>(createCallable(getMethod(m, annotation, proxy), args));
                ThreadPoolUtil.execute(future);
                return future;
            } else {
                future = new FutureTask<Object>(createCallable(m, args));
                ThreadPoolUtil.execute(future);
                try {
                    result = future.get(timeoutInMilisec, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    if (e instanceof TimeoutException) {
                        future.cancel(true);
                    }
                    throw new UndeclaredThrowableException(e);
                }

                return result;
            }
        }

        private Method getMethod(Method m, FutureCall annotation, Object proxy)
                throws SecurityException, NoSuchMethodException {
            if (!annotation.delegeteTo().isEmpty()) {
                return proxy.getClass().getDeclaredMethod(annotation.delegeteTo(), m.getParameterTypes());
            }
            return m;
        }

        private Callable<Object> createCallable(final Method m, final Object[] args) {
            return new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        return m.invoke(obj, args);
                    } catch (Exception e) {
                        throw e;
                    }
                }
            };
        }

    }

    /**
     * Designed to extend the request header with a value specifying the correlation ID to propagate to VDSM.
     */
    private static class CustomXmlRpcCommonsTransport extends XmlRpcCommonsTransport {

        private static final String FLOW_ID_HEADER_NAME = "FlowID";

        public CustomXmlRpcCommonsTransport(XmlRpcCommonsTransportFactory pFactory) {
            super(pFactory);
        }

        @Override
        protected void initHttpHeaders(XmlRpcRequest pRequest) throws XmlRpcClientException {
            super.initHttpHeaders(pRequest);

            String correlationId = ThreadLocalParamsContainer.getCorrelationId();
            if (StringUtils.isNotBlank(correlationId)) {
                method.setRequestHeader(FLOW_ID_HEADER_NAME, correlationId);
            }
        }
    };

    /**
     * Designed to to override the factory with a customized transport.
     */
    private static class CustomXmlRpcCommonsTransportFactory extends XmlRpcCommonsTransportFactory {

        public CustomXmlRpcCommonsTransportFactory(XmlRpcClient pClient) {
            super(pClient);
        }

        @Override
        public XmlRpcTransport getTransport() {
            return new CustomXmlRpcCommonsTransport(this);
        }

    };

}

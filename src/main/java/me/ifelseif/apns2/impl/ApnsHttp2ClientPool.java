package me.ifelseif.apns2.impl;

import me.ifelseif.apns2.ApnsHttp2Client;
import me.ifelseif.apns2.model.Apns2Config;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by linmingchang on 16/12/29.
 */
public final class ApnsHttp2ClientPool implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ApnsHttp2ClientPool.class);
    private final BlockingDeque<ApnsHttp2Client> pool;
    private static int CLIENT_ID_SEQ = 1;

    private ApnsHttp2ClientPool(Apns2Config config) {
        SslContextFactory sslContextFactory = ApnsHttp2ClientPool.getSslContextFactory(config.getPassword(), config.getKey());
        pool = new LinkedBlockingDeque<>(config.getPoolSize());
        for (int i = 0; i < config.getPoolSize(); i++) {
            String clientName = "production-" + CLIENT_ID_SEQ++;
            ApnsHttp2ClientImpl client = new ApnsHttp2ClientImpl(clientName, sslContextFactory, config.getConnectTimeout(), config.getPushTimeout(), config.getTopic(), config.getPushRetryTimes(), config.getApnsExpiration(), config.getApnsPriority(), config.getHeartbeatInterval());
            pool.add(client);
        }
    }

    public ApnsHttp2Client borrowClient() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            log.error("borrow client error", e);
        }
        return null;
    }

    public void returnClient(ApnsHttp2Client client) {
        if (client != null) {
            pool.add(client);
        }
    }

    @Override
    public void close() {
        while (!pool.isEmpty()) {
            try {
                pool.take().stop();
            } catch (InterruptedException e) {
                log.error("stop client error", e);
            }
        }
    }


    private static SslContextFactory getSslContextFactory(String password, InputStream key) {
        SslContextFactory sslContextFactory1 = new SslContextFactory(true);
        try {
            //init KeyStore
            final char[] pwdChars = password.toCharArray();
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(key, pwdChars);
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, pwdChars);
            //init TrustManager
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            //init SSLContext
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            sslContextFactory1.setSslContext(sslContext);
            sslContextFactory1.start();
        } catch (Exception e) {
            log.error("create sslContextFactory error.", e);
        }
        return sslContextFactory1;
    }

    public static ApnsHttp2ClientPool newClientPool(Apns2Config config) {
        return new ApnsHttp2ClientPool(config);
    }
}

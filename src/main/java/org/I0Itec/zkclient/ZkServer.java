package org.I0Itec.zkclient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.NIOServerCnxn.Factory;

public class ZkServer {

    private final static Logger LOG = Logger.getLogger(ZkServer.class);

    public static final int DEFAULT_PORT = 2181;
    private static final int DEFAULT_TICK_TIME = 5000;

    private String _dataDir;
    private String _logDir;

    private IDefaultNameSpace _defaultNameSpace;

    private ZooKeeperServer _zk;
    private Factory _nioFactory;
    private ZkClient _zkClient;
    private int _port;

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace) {
        this(dataDir, logDir, defaultNameSpace, DEFAULT_PORT);
    }

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace, int port) {
        _dataDir = dataDir;
        _logDir = logDir;
        _defaultNameSpace = defaultNameSpace;
        _port = port;
    }

    @PostConstruct
    public void start() throws InterruptedException, IOException, KeeperException {
        final String[] localHostNames = NetworkUtil.getLocalHostNames();
        String names = "";
        for (int i = 0; i < localHostNames.length; i++) {
            final String name = localHostNames[i];
            names += " " + name;
            if (i + 1 != localHostNames.length) {
                names += ",";
            }
        }
        LOG.info("Starting ZkServer on: [" + names + "] ...");
        startZooKeeperServer();
        _zkClient = new ZkClient("localhost:" + _port, 10000);
        _defaultNameSpace.createDefaultNameSpace(_zkClient);
    }

 
    private void startZooKeeperServer() throws InterruptedException {
        final String[] localhostHostNames = NetworkUtil.getLocalHostNames();
        final String servers = "localhost:" + _port;
        // check if this server needs to start a _client server.
        int pos = -1;
        LOG.debug("check if hostNames " + servers + " is in list: " + Arrays.asList(localhostHostNames));
        if ((pos = NetworkUtil.hostNamesInList(servers, localhostHostNames)) != -1) {
            // yes this server needs to start a zookeeper server
            final String[] hosts = servers.split(",");
            final String[] hostSplitted = hosts[pos].split(":");
            int port = _port;
            if (hostSplitted.length > 1) {
                port = Integer.parseInt(hostSplitted[1]);
            }
            // check if this machine is already something running..
            if (NetworkUtil.isPortFree(port)) {
                final int tickTime = DEFAULT_TICK_TIME;
                final File dataDir = new File(_dataDir);
                final File dataLogDir = new File(_logDir);
                dataDir.mkdirs();
                dataLogDir.mkdirs();

                if (hosts.length > 1) {
                    // multiple zk servers
                    LOG.info("Start distributed zookeeper server...");
                    throw new IllegalArgumentException("Unable to start distributed zookeeper server");
                }
                // single zk server
                LOG.info("Start single zookeeper server...");
                startSingleZkServer(tickTime, dataDir, dataLogDir, port);
                LOG.info("data dir: " + dataDir.getAbsolutePath());
                LOG.info("data log dir: " + dataLogDir.getAbsolutePath());
            } else {
                throw new IllegalStateException("Zookeeper port " + port + " was already in use. Running in single machine mode?");
            }
        }
    }

    private void startSingleZkServer(final int tickTime, final File dataDir, final File dataLogDir, final int port) throws InterruptedException {
        try {
            _zk = new ZooKeeperServer(dataDir, dataLogDir, tickTime);
            _nioFactory = new NIOServerCnxn.Factory(port);
            _nioFactory.startup(_zk);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to start single ZooKeeper server.", e);
        }
    }

    public void join() throws InterruptedException {
        if (_nioFactory != null) {
            _nioFactory.join();
        }
    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down server...");
        try {
            _zkClient.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (_nioFactory != null) {
            _nioFactory.shutdown();
            _nioFactory = null;
        }
        if (_zk != null) {
            _zk.shutdown();
            _zk = null;
        }
    }

    public ZkClient getZkClient() {
        return _zkClient;
    }
}

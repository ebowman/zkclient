package org.I0Itec.zkclient;

import org.apache.zookeeper.KeeperException;

public interface IDefaultNameSpace {

    /**
     * Creates a set of default folder structure within a zookeeper .
     * 
     * @throws InterruptedException
     * @throws KeeperException
     */

    public void createDefaultNameSpace(ZkClient zkClient) throws KeeperException, InterruptedException;

}

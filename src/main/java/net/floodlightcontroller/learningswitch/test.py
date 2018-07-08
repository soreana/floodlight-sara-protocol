#!/usr/bin/python

from mininet.topo import Topo

from mininet.cli import CLI
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel

from mininet.node import RemoteController

REMOTE_CONTROLLER_IP = "192.168.56.1"

if __name__ == '__main__':
    # Tell mininet to print useful information
    setLogLevel('info')

    net = Mininet(autoStaticArp=True)
    net.addController('c0',
                      controller=RemoteController,
                      ip=REMOTE_CONTROLLER_IP,
                      port=6653)

    s1 = net.addSwitch('s1', protocols='OpenFlow13')
    s2 = net.addSwitch('s2', protocols='OpenFlow13')
    s3 = net.addSwitch('s3', protocols='OpenFlow13')
    h1 = net.addHost('h1')
    h2 = net.addHost('h2')
    h3 = net.addHost('h3')

    net.addLink(h1, s1)
    net.addLink(h2, s2)
    net.addLink(h3, s3)
    net.addLink(s1, s2)
    net.addLink(s2, s3)

    h1.intf('h1-eth0').setMAC('aa:aa:aa:aa:aa:01')
    h2.intf('h2-eth0').setMAC('aa:aa:aa:aa:aa:02')
    h3.intf('h3-eth0').setMAC('aa:aa:aa:aa:aa:03')

    s1.intf('s1-eth1').setMAC('00:00:00:00:11:01')
    s1.intf('s1-eth2').setMAC('00:00:00:00:11:02')

    s2.intf('s2-eth1').setMAC('00:00:00:00:22:01')
    s2.intf('s2-eth2').setMAC('00:00:00:00:22:02')
    
    s3.intf('s3-eth1').setMAC('00:00:00:00:33:01')
    s3.intf('s3-eth2').setMAC('00:00:00:00:33:02')

    net.start()
    CLI(net)
    net.stop()

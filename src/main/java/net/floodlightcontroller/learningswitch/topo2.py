from mininet.topo import Topo


class MyTopo(Topo):
    def __init__(self):
        Topo.__init__(self)
        h1 = self.addHost("h1")
        h2 = self.addHost("h2")
        s1 = self.addSwitch("s1")
        s2 = self.addSwitch("s2")
        s3 = self.addSwitch("s3")
        s4 = self.addSwitch("s4")
        s5 = self.addSwitch("s5")

        self.addLink(h1, s1)
        self.addLink(s1, s2, delay='40ms')
        self.addLink(s1, s3, delay='30ms')
        self.addLink(s1, s4, delay='10ms')
        self.addLink(s2, s5, delay='20ms')
        self.addLink(s3, s4, delay='15ms')
        self.addLink(s3, s5, delay='20ms')
        self.addLink(s4, s5, delay='30ms')
        self.addLink(s5, h2)


topos = {'mytopo': (lambda: MyTopo())}

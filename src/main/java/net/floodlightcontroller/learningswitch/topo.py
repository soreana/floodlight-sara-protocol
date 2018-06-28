from mininet.net import Mininet
from mininet.topo import Topo
from mininet.node import Controller,RemoteController,OVSSwitch
from mininet.node import CPULimitedHost, Host, Node
from mininet.node import OVSKernelSwitch, UserSwitch
from mininet.log import setLogLevel,info
from mininet.cli import CLI
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from functools import partial

NUMHOSTS=2
NUMSWITCHES=5
class SingleSwitchTopo(Topo):
	def build(self):
		#for h in range(NUMHOSTS):
		#    host = self.addHost('h%s' % (h + 1))
		#for s in range(NUMSWITCHES):
		#    switch = self.addSwitch('s%s' % (s + 1))

		h1 = self.addHost('h1')
		h2 = self.addHost('h2')
		s1 = self.addSwitch('s1')
		s2 = self.addSwitch('s2')
		s3 = self.addSwitch('s3')
		s4 = self.addSwitch('s4')
		s5 = self.addSwitch('s5')
		print( '*** Add links\n')

		self.addLink( s1, s2, bw=10, delay='40ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s2, s5, bw=10, delay='20ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s1, s3, bw=10, delay='30ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s3, s5, bw=10, delay='20ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s1, s4, bw=10, delay='10ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s3, s4, bw=10, delay='15ms', loss=2, max_queue_size=1000, use_htb=True)
		self.addLink( s4, s5, bw=10, delay='30ms', loss=2, max_queue_size=1000, use_htb=True)

def simpleTest():
	"Create and test a simple network"
	topo = SingleSwitchTopo()
	net = Mininet( topo=topo, controller=partial( RemoteController, ip='127.0.0.1', port=6633 ), link=TCLink )
	net.start()
	print "Dumping host connections"
	dumpNodeConnections(net.hosts)
	print "Testing network connectivity"
	net.pingAll()
	net.stop()

if __name__ == '__main__':
	# Tell mininet to print useful information
	setLogLevel('info')
	simpleTest()

topos = { 'mytopo': ( lambda: SingleSwitchTopo() ) }

#sudo mn --custom topo.py --topo=mytopo --controller=remote,ip=127.0.0.1,port=6653 --switch ovsk,protocols=OpenFlow13 --link tc

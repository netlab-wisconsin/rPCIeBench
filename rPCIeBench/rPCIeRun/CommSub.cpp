#include <QDMAController.h>
#include "mmio.hpp"
#include "dma.hpp"
#include "tests.hpp"
#include "cstring"
#include <unistd.h>
#include <stdio.h>
#include <chrono>
#include <thread>
#include <vector>
#include <functional>
#include <algorithm>
using namespace std;

typedef std::chrono::high_resolution_clock Clock;

int main(){
	//init(0x67, 1024*1024*1024);
	init(0x6c, 1024*1024*1024);
	init(0x69, 1024*1024*1024);
	init(0x6f, 1024*1024*1024);
	init(0x70, 1024*1024*1024);

	//init_freelist(0x67);
	init_freelist(0x6c);
	init_freelist(0x69);
	init_freelist(0x6f);
	init_freelist(0x70);

	volatile uint32_t * bar;
	volatile uint32_t * sta;
	uint32_t tag;

	// bar = (volatile uint32_t*)getLiteAddr(0x67);
	// sta = (volatile uint32_t*)getLiteAddr(0x67)+512;
	// writeConfig(0x1408/4, 0, 0x67);
	// tag = readConfig(0x140c/4, 0x67);
	// bar[80] = tag;
	char pci;
	
	pci = 0x6c;
	bar = (volatile uint32_t*)getLiteAddr(pci);
	sta = (volatile uint32_t*)getLiteAddr(pci)+512;
	writeConfig(0x1408/4, 0, pci);
	tag = readConfig(0x140c/4, pci);
	bar[80] = tag;

	pci = 0x69;
	bar = (volatile uint32_t*)getLiteAddr(pci);
	sta = (volatile uint32_t*)getLiteAddr(pci)+512;
	writeConfig(0x1408/4, 0, pci);
	tag = readConfig(0x140c/4, pci);
	bar[80] = tag;

	pci = 0x6f;
	bar = (volatile uint32_t*)getLiteAddr(pci);
	sta = (volatile uint32_t*)getLiteAddr(pci)+512;
	writeConfig(0x1408/4, 0, pci);
	tag = readConfig(0x140c/4, pci);
	bar[80] = tag;

	pci = 0x70;
	bar = (volatile uint32_t*)getLiteAddr(pci);
	sta = (volatile uint32_t*)getLiteAddr(pci)+512;
	writeConfig(0x1408/4, 0, pci);
	tag = readConfig(0x140c/4, pci);
	bar[80] = tag;

	unsigned int * p1 = (unsigned int *)hpalloc(0x69, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p1[i] = -1;
	}
	unsigned int * s1 = (unsigned int *)hpalloc(0x69, 262144);
	for(int i=0;i<262144/4;i++){
		s1[i] = 0;
	}
	h2d(0x69, p1, 1024*1024*1024, 0x0000000040000000, s1, 0);
	while(s1[0]!=-1)
		{__sync_synchronize();}
	s1[0] = 0;
	unsigned int * p2 = (unsigned int *)hpalloc(0x6f, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p2[i] = -1;
	}
	unsigned int * s2 = (unsigned int *)hpalloc(0x6f, 262144);
	for(int i=0;i<262144/4;i++){
		s2[i] = 0;
	}
	h2d(0x6f, p2, 1024*1024*1024, 0x0000000040000000, s2, 0);
	while(s2[0]!=-1)
		{__sync_synchronize();}
	s2[0] = 0;
	unsigned int * p3 = (unsigned int *)hpalloc(0x70, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p3[i] = -1;
	}
	unsigned int * s3 = (unsigned int *)hpalloc(0x70, 262144);
	for(int i=0;i<262144/4;i++){
		s3[i] = 0;
	}
	h2d(0x70, p3, 1024*1024*1024, 0x0000000040000000, s3, 0);
	while(s3[0]!=-1)
		{__sync_synchronize();}
	s3[0] = 0;
	unsigned int * p4 = (unsigned int *)hpalloc(0x6c, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p4[i] = -1;
	}
	unsigned int * s4 = (unsigned int *)hpalloc(0x6c, 262144);
	for(int i=0;i<262144/4;i++){
		s4[i] = 0;
	}
	h2d(0x6c, p4, 1024*1024*1024, 0x0000000040000000, s4, 0);
	while(s4[0]!=-1)
		{__sync_synchronize();}
	s4[0] = 0;

	// MMIO_test(0x69);
	// h2d_DMA_latency(0x69);
	// d2h_DMA_latency(0x69);
	// h2d_DMA_bandwidth(0x69);
	// d2h_DMA_bandwidth(0xaf);
	// p2p_read_latency(0x69, 0x6c);
	// p2p_write_latency(0x69, 0x6c);
	// p2p_read_bandwidth(0x69, 0x6c);
	// p2p_write_bandwidth(0x69, 0x6c);
	// h2d_latency_throughput(0x6f);
	// d2h_latency_throughput(0x6f);
	// p2p_read_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false);
	// p2p_write_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false);
	// h2d_bandwidth_partition(0x6f);
	// d2h_bandwidth_partition(unsigned char pci_bus);
	// p2p_read_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2);
	// p2p_write_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2);
	


	return 0;
}

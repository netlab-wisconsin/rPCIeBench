#include "dma.hpp"
#include <stdio.h>
#include <unistd.h>
#include <cmath>
#include <chrono>
#include <QDMAController.h>

typedef std::chrono::high_resolution_clock Clock;

void mmio_write(volatile __m512i* bridge, __m512i* data, uint32_t length){
	for(int i=0;i<length/64;i++){
		_mm512_stream_si512 ((__m512i *)(bridge+i), data[i]);
	}
	return ;
}
void mmio_read(volatile __m512i* bridge, __m512i* data, uint32_t length){
	for(int i=0;i<length/64;i++){
		data[i] = _mm512_stream_load_si512 ((__m512i *)(bridge+i));
	}
	return ;
}

void p2p_read(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
{
	long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2)) + bias;
	h2d(pci_bus1, (unsigned int *)vaddr, length, addr, s, q_num, pkt_size);
	return;
}
void p2p_write(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
{
	long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2)) + bias;
	d2h(pci_bus1, (unsigned int *)vaddr, length, addr, s, q_num, pkt_size);
	return;
}

void h2d(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
{

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	uint32_t h2c_length;
	if (length % 64 > 0)
		h2c_length = length - length % 64 + 64;
	else
		h2c_length = length;

	bar[20] = 1;
	bar[50] = (uint32_t)((unsigned long)p>>32);
	bar[51] = (uint32_t)((unsigned long)p);
	bar[52] = (uint32_t)((unsigned long)addr>>32);
	bar[53] = (uint32_t)((unsigned long)addr);
	bar[54] = h2c_length;
	bar[55] = pkt_size;
	bar[56] = 1 << q_num;
	bar[57] = (uint32_t)((unsigned long)s>>32);
	bar[58] = (uint32_t)((unsigned long)s);

	bar[59] = 1;	//start
	bar[59] = 0;
}

void d2h(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size){

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;
	
	uint32_t c2h_length;
	if (length % 64 > 0)
		c2h_length = length - length % 64 + 64;
	else
		c2h_length = length;
	
	bar[20] = 1;
	bar[70] = (uint32_t)((unsigned long)p>>32);
	bar[71] = (uint32_t)((unsigned long)p);
	bar[72] = (uint32_t)((unsigned long)addr>>32);
	bar[73] = (uint32_t)((unsigned long)addr);
	bar[74] = c2h_length;
	bar[75] = pkt_size;
	bar[76] = 1 << q_num;
	bar[77] = (uint32_t)((unsigned long)s>>32);
	bar[78] = (uint32_t)((unsigned long)s);

	uint32_t tag;
	writeConfig(0x1408/4, 0, pci_bus);
	tag = readConfig(0x140c/4, pci_bus);
	bar[80] = tag;

	bar[79] = 1;	//start
	bar[79] = 0;
}

#ifndef dma_hpp
#define dma_hpp

#include <QDMAController.h>
#include <immintrin.h>
#include <iostream>
#include <pthread.h>
using namespace std;

void mmio_write(volatile __m512i* bridge, __m512i* data, uint32_t length);
void mmio_read(volatile __m512i* bridge, __m512i* data, uint32_t length);
void p2p_read(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 32768);
void p2p_write(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 4096);
void h2d(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 32768);
void d2h(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 4096);

#endif
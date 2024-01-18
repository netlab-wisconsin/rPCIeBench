#include <QDMAController.h>
#include <iostream>

void MMIO_test(unsigned char pci_bus);
void h2d_DMA_latency(unsigned char pci_bus);
void d2h_DMA_latency(unsigned char pci_bus);
void h2d_DMA_bandwidth(unsigned char pci_bus);
void d2h_DMA_bandwidth(unsigned char pci_bus);
void p2p_read_latency(unsigned char pci_bus1, unsigned char pci_bus2);
void p2p_write_latency(unsigned char pci_bus1, unsigned char pci_bus2);
void p2p_read_bandwidth(unsigned char pci_bus1, unsigned char pci_bus2);
void p2p_write_bandwidth(unsigned char pci_bus1, unsigned char pci_bus2);
void h2d_latency_throughput(unsigned char pci_bus, bool hetero=false);
void d2h_latency_throughput(unsigned char pci_bus, bool hetero=false);
void p2p_read_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false);
void p2p_write_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false);
void h2d_bandwidth_partition(unsigned char pci_bus);
void d2h_bandwidth_partition(unsigned char pci_bus);
void p2p_read_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2);
void p2p_write_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2);


using namespace std;
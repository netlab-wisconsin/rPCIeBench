# Driver and Initialization

To run an rPCIeBench program on an FPGA device, the device must be initialized.

```c
void init(unsigned char pci_bus, size_t bridge_bar_size)
```

This functions initialize FPGA device with pci bus id pci_bus, and mapped bridge_bar_size memory for the AXI bridge.
The pci_bus will be added to device_list, and the 3 BAR of FPGA device will be memory mapped.

We have a native memory management for all host physical memory and FPGA HBM. To use it, invoke:

```c
void init_freelist(unsigned char pci_bus)
```
This function will first initialize the huge page memory and mmio memory, then write the TLB into FPGA and create a freelist for the huge page memory.
This function automatically invokes init_tlb(pci_bus)

## Other funtions in QDMAController lib 

(You may use if you need, but they are automatically invoked at initialization):

```c
void init_tlb(unsigned char pci_bus)
```
This function memory maps the huge page allocated for the FPGA and gets the AXI Bridge address of all FPGAs in the device_list except itself. 

Then it will write the virtual address and physical address of huge pages into the FPGA’s TLB through AXI-Lite BAR.

After huge page address is done writting, the AXI Bridge virtual and physical address will be written into the FPGA’s TLB.

The AXI Bridge virtual address is continuous with huge page virtual address. But these virtual address must not be visited. The translation of an FPGA’s bridge address to another FPGA’s virtual address is saved, and can be get through function p2p_paddr_translation()

```c
void* hpalloc(unsigned char pci_bus, size_t size)
```
This function allocates a memory buffer from the freelist of an FPGA device in its huge page memory. It returns the pointer to that buffer.

```c
void hpfree(unsigned char pci_bus, void* p)
```
This function frees an existing buffer from the freelist of an FPGA.

```c
void init_hbmaddr(unsigned char pci_bus)
```
This function initializes the freelist of HBM space of an FPGA.

```c
unsigned long hbm_getaddr(unsigned char pci_bus, size_t size)
```
This function allocates a memory buffer from the freelist of HBM space of an FPGA. It returns the address of that buffer.

Here the address is the address of HBM inside the FPGA.


```c
void hbm_free(unsigned char pci_bus, unsigned long addr)
```
This function frees an existing buffer from the freelist of HBM space of an FPGA.


```c
void long int p2p_paddr_translation (unsigned char pci_bus1, unsigned char pci_bus2, long int pci_baddr)
```
This function provides a translation from pci_bus1 FPGA’s AXI Bridge Virtual address to pci_bus2 FPGA’s virtual address in its TLB. So that pci_bus2 FPGA can use this address for DMA.

# Basic Primitives

```c
void h2d(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
```
This triggers a DMA write from host memory to FPGA HBM.

pci_bus: pci bus id of FPGA

p: a pointer to a huge page memory buffer, created by hpalloc()

length: length of data for DMA

addr: address of HBM target

s: a pointer to the write back memory address which can be used for polling, created by hpalloc()

q_num: the number of queue for this command

pkt_size: the packet size of this flow in our FPGA logics, which means, after sending this number of bytes, this flow will switch to another flow in a round-robin multiplexing manner

length must be multiples of 64Bytes

addr must be multiples of 32Bytes

```c
void d2h(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
```

This triggers a DMA read from FPGA HBM to host memory.

pci_bus: pci bus id of FPGA


p: a pointer to a huge page memory buffer, created by hpalloc()

length: length of data for DMA

addr: address of HBM target

s: a pointer to the write back memory address which can be used for polling, created by hpalloc()

q_num: the number of queue for this command

pkt_size: the packet size of this flow in our FPGA logics, which means, after sending this number of bytes, this flow will switch to another flow in a round-robin multiplexing manner

length must be multiples of 64Bytes

addr must be multiples of 32Bytes

```c
void p2p_read(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 32768);
```

This triggers a DMA read from pci_bus1 FPGA HBM to pci_bus2 FPGA HBM.

pci_bus1: the principal FPGA’s pci bus id, this FPGA launches DMA request

bias: the addr shift of subordinate FPGA’s HBM

pci_bus2: the subordinate FPGA’s pci bus id

length: length of data for DMA

addr: address of HBM target of pci_bus1 FPGA

s: a pointer to the write back memory address which can be used for polling, created by hpalloc()

q_num: the number of queue for this command

pkt_size: the packet size of this flow in our FPGA logics, which means, after sending this number of bytes, this flow will switch to another flow in a round-robin multiplexing manner

length must be multiples of 64Bytes

addr must be multiples of 32Bytes

Data moves from pci_bus2 FPGA’s HBM address: BAR’s basic AXI address + bias to pci_bus1 FPGA’s HBM address: addr


```c
void p2p_write(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size)
```

This triggers a DMA write from pci_bus1 FPGA HBM to pci_bus2 FPGA HBM.

pci_bus1: the principal FPGA’s pci bus id, this FPGA launches DMA request

bias: the addr shift of subordinate FPGA’s HBM

pci_bus2: the subordinate FPGA’s pci bus id

length: length of data for DMA

addr: address of HBM target of pci_bus1 FPGA

s: a pointer to the write back memory address which can be used for polling, created by hpalloc()

q_num: the number of queue for this command

pkt_size: the packet size of this flow in our FPGA logics, which means, after sending this number of bytes, this flow will switch to another flow in a round-robin multiplexing manner

length must be multiples of 64Bytes

addr must be multiples of 32Bytes

Data moves from pci_bus1 FPGA’s HBM address: addr to pci_bus2 FPGA’s HBM address: BAR’s basic AXI address + bias


```c
void mmio_write(volatile __m512i* bridge, __m512i* data, uint32_t length)
```

This triggers an MMIO data write to FPGA's BAR address, which connects to its HBM via AXI bridge.

bridge: the pointer pointing to the BAR's virtual address of AXI bar, got by (volatile __m512i*)getBridgeAddr()

data: a __m512i* pointer pointing to the data to write

length: the lenght of data for MMIO

length must be multiples of 64 bytes

The HBM address to write is BAR’s basic AXI address


```c
void mmio_read(volatile __m512i* bridge, __m512i* data, uint32_t length)
```

This triggers an MMIO data read from FPGA's BAR address, which connects to its HBM via AXI bridge.

bridge: the pointer pointing to the BAR's virtual address of AXI bar, got by (volatile __m512i*)getBridgeAddr()

data: a __m512i* pointer pointing to the data to write

length: the lenght of data for MMIO

length must be multiples of 64 bytes

The HBM address to read is BAR’s basic AXI address

# Benchmark Tests

1. MMIO_read latency test

This test measures the latency of MMIO read from host side. To measure different granularities, double, __m128i, __m256i, __m512i data structures are used.

2. host-to-device and device-to-host DMA latency test

In these two tests, h2d and d2h latency are measured. The arguments are first passed to the FPGA, when counting time, only the last trigger is involved to minimize the overheads.

3. host-to-device and device-to-host bandwidth test

In these two tests, h2d and d2h bandwidth are measured.

4. FPGA peer-to-peer read latency test

Like the h2d latency test, only the target address changes to the memory mapped address of HBM of the second FPGA.

5. FPGA peer-to-peer write latency test

To measure the full data transfer latency, the second FPGA will write back the completion. So a module is put in the HBM interface of the second FPGA to measure data transfer. This is achieved by writing all-one bits to the FPGA. (the monitor module in second FPGA counts input bits that are all-one in data(31,0))

6. FPGA peer-to-peer read/write bandwidth test

In these two tests, we need not to poll from the second FPGA. We can only poll from the first FPGA because the error is negligible for bandwidth.

7. latency-throughput performance for h2d/d2h/FPGA read/FPGA write

In these tests, background flows are first generated, then we test the latency of the victim flows.

7. bandwidth partition on one path for h2d/d2h/FPGA read/FPGA write

In these tests, we launch several flows together, and measure one of the flows.

8. Current flows in reverse direction on one path (h2d+d2h / FPGA read+write)

First create a flow in revers direction, then we measure the latency or bandwidth.

9. Host-FPGA flow + Host-FPGA flow
10. Host-FPGA flow + FPGA-FPGA flow
11. FPGA-FPGA flow + FPGA-FPGA flow

Short for HF-HF, HF-FF, FF-FF. These are similar, create two or multiple flows and measure one of them. Or you can take turn to measure each of them.

You can refer to the example funtions provided and create your own tests.

























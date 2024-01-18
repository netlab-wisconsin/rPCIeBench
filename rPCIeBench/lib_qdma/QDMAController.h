#ifndef _QDMACONTROLLER_H_
#define _QDMACONTROLLER_H_

#include <iostream>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <immintrin.h>
#include <rc4ml.h>
#include <map>

struct Bars{
	volatile uint32_t *config_bar;
	volatile uint32_t *lite_bar;
	volatile __m512i *bridge_bar;
};
struct MemBuf{
	MemBuf* prev;
	MemBuf* next;
	void* start;
	void* end;
	uint64_t length;
};
struct HBMBuf{
	HBMBuf* prev;
	HBMBuf* next;
	unsigned long start;
	unsigned long end;
	uint64_t length;
};
struct MemFL{
	MemBuf o1fls;  // 4KB
	MemBuf o1fl[1024];
	MemBuf o2fls;  // 16KB
	MemBuf o2fl[1024];
	MemBuf o3fls;  // 64KB
	MemBuf o3fl[1024];
	MemBuf o4fls;  // 256KB
	MemBuf o4fl[1024];
	MemBuf o5fls;  // 1MB
	MemBuf* o5fl = new MemBuf();
	std::map<void*,uint64_t> o5flu;
	void *base,*hpstart,*hpend;
};
long int p2p_paddr_translation(unsigned char pci_bus1, unsigned char pci_bus2, long int pci_baddr);
void init_hbmaddr(unsigned char pci_bus);
unsigned long hbm_getaddr(unsigned char pci_bus, size_t size);
void hbm_free(unsigned char pci_bus, unsigned long addr);
void init_freelist(unsigned char pci_bus);
void* o1fl_alloc(unsigned char pci_bus);
void* o2fl_alloc(unsigned char pci_bus);
void* o3fl_alloc(unsigned char pci_bus);
void* o4fl_alloc(unsigned char pci_bus);
void* o5fl_alloc(unsigned char pci_bus, size_t size);
void o1fl_free(unsigned char pci_bus, void* ps);
void o2fl_free(unsigned char pci_bus, void* ps);
void o3fl_free(unsigned char pci_bus, void* ps);
void o4fl_free(unsigned char pci_bus, void* ps);
void o5fl_free(unsigned char pci_bus, void* ps);
void* hpalloc(unsigned char pci_bus, size_t size);
void hpfree(unsigned char pci_bus, void* p);
void o1fl_print(unsigned char pci_bus);
void o2fl_print(unsigned char pci_bus);
void o3fl_print(unsigned char pci_bus);
void o4fl_print(unsigned char pci_bus);
void o5fl_print(unsigned char pci_bus);
void o5flu_print(unsigned char pci_bus);
void hbmlist_print(unsigned char pci_bus);
void hbmu_print();
void init(unsigned char pci_bus, size_t bridge_bar_size=1*1024*1024*1024);
void writeConfig(uint32_t index,uint32_t value,unsigned char pci_bus=0);
uint32_t readConfig(uint32_t index,unsigned char pci_bus=0);
void writeReg(uint32_t index,uint32_t value,unsigned char pci_bus=0);
uint32_t readReg(uint32_t index,unsigned char pci_bus=0);
void* qdma_alloc(size_t size, unsigned char pci_bus=0, int num=0, bool print_addr=0, bool erase_tlb=1);
void* init_tlb(unsigned char pci_bus);
void writeBridge(uint32_t index, uint64_t* value,unsigned char pci_bus=0);
void readBridge(uint32_t index, uint64_t* value,unsigned char pci_bus=0);
void* getBridgeAddr(unsigned char pci_bus=0);
void* getLiteAddr(unsigned char pci_bus=0);
#define get_syspath_bar_mmap(s, bus,dev,func,bar) \
	snprintf(s, sizeof(s), \
		"/sys/bus/pci/devices/0000:%02x:%02x.%x/resource%u", \
		bus, dev, func, bar)

typedef struct{
	int npages;
	unsigned long* vaddr;
	unsigned long* paddr;
}tlb;

void resetCounters(unsigned char pci_bus=0);
void printCounters(unsigned char pci_bus=0);

#endif
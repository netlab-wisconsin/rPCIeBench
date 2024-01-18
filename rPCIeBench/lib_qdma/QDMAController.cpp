#include "QDMAController.h"
#include <map>
#include <cmath>
#include <errno.h>
#include <string.h>
using namespace std;

map<unsigned char,Bars> device_list;
int num_device=0;
unsigned char default_pci_bus=0;
map<pair<unsigned char,unsigned char>, long int> pci_paddr_tran;
map<unsigned char, MemFL*> DFL;
map<unsigned char, HBMBuf*> hbmlist;
map<pair<unsigned char, unsigned long>, uint64_t> hbmu;

void init_freelist(unsigned char pci_bus)
{
	MemFL* MFL = new MemFL();
	DFL[pci_bus] = MFL;
	(*MFL).base = init_tlb(pci_bus);

	unsigned int* p;
	p = (unsigned int*)(*MFL).base + 4096*1024/sizeof(int); //status buffer 4MB
	(*MFL).hpstart = p;

	(*MFL).o1fls.next = &(*MFL).o1fl[0];
	(*MFL).o1fl[0].prev = &(*MFL).o1fls;
	(*MFL).o1fls.start = (void*)p;
	(*MFL).o1fls.end = (void*)(p+4096*1024/sizeof(int));
	(*MFL).o1fls.length = 4096*1024;
	for (int i=0; i<1023; i++)
	{
		(*MFL).o1fl[i].next = &(*MFL).o1fl[i+1];
		(*MFL).o1fl[i+1].prev = &(*MFL).o1fl[i];
		(*MFL).o1fl[i].start = (void*)p;
		(*MFL).o1fl[i].end = (void*)(p+4096/sizeof(int));
		(*MFL).o1fl[i].length = 4096;
		p = p + 4096/sizeof(int);
	}
	(*MFL).o1fl[1023].next = &(*MFL).o1fls;
	(*MFL).o1fls.prev = &(*MFL).o1fl[1023];
	(*MFL).o1fl[1023].start = (void*)p;
	(*MFL).o1fl[1023].end = (void*)(p+4096/sizeof(int));
	(*MFL).o1fl[1023].length = 4096;
	p = p + 4096/sizeof(int);

	(*MFL).o2fls.next = &(*MFL).o2fl[0];
	(*MFL).o2fl[0].prev = &(*MFL).o2fls;
	(*MFL).o2fls.start = (void*)p;
	(*MFL).o2fls.end = (void*)(p+16384*1024/sizeof(int));
	(*MFL).o2fls.length = 16384*1024;
	for (int i=0; i<1023; i++)
	{
		(*MFL).o2fl[i].next = &(*MFL).o2fl[i+1];
		(*MFL).o2fl[i+1].prev = &(*MFL).o2fl[i];
		(*MFL).o2fl[i].start = (void*)p;
		(*MFL).o2fl[i].end = (void*)(p+16384/sizeof(int));
		(*MFL).o2fl[i].length = 16384;
		p = p + 16384/sizeof(int);
	}
	(*MFL).o2fl[1023].next = &(*MFL).o2fls;
	(*MFL).o2fls.prev = &(*MFL).o2fl[1023];
	(*MFL).o2fl[1023].start = (void*)p;
	(*MFL).o2fl[1023].end = (void*)(p+16384/sizeof(int));
	(*MFL).o2fl[1023].length = 16384;
	p = p + 16384/sizeof(int);

	(*MFL).o3fls.next = &(*MFL).o3fl[0];
	(*MFL).o3fl[0].prev = &(*MFL).o3fls;
	(*MFL).o3fls.start = (void*)p;
	(*MFL).o3fls.end = (void*)(p+64*1024*1024/sizeof(int));
	(*MFL).o3fls.length = 64*1024*1024;
	for (int i=0; i<1023; i++)
	{
		(*MFL).o3fl[i].next = &(*MFL).o3fl[i+1];
		(*MFL).o3fl[i+1].prev = &(*MFL).o3fl[i];
		(*MFL).o3fl[i].start = (void*)p;
		(*MFL).o3fl[i].end = (void*)(p+64*1024/sizeof(int));
		(*MFL).o3fl[i].length = 64*1024;
		p = p + 64*1024/sizeof(int);
	}
	(*MFL).o3fl[1023].next = &(*MFL).o3fls;
	(*MFL).o3fls.prev = &(*MFL).o3fl[1023];
	(*MFL).o3fl[1023].start = (void*)p;
	(*MFL).o3fl[1023].end = (void*)(p+64*1024/sizeof(int));
	(*MFL).o3fl[1023].length = 64*1024;
	p = p + 64*1024/sizeof(int);

	(*MFL).o4fls.next = &(*MFL).o4fl[0];
	(*MFL).o4fl[0].prev = &(*MFL).o4fls;
	(*MFL).o4fls.start = (void*)p;
	(*MFL).o4fls.end = (void*)(p+256*1024*1024/sizeof(int));
	(*MFL).o4fls.length = 256*1024*1024;
	for (int i=0; i<1023; i++)
	{
		(*MFL).o4fl[i].next = &(*MFL).o4fl[i+1];
		(*MFL).o4fl[i+1].prev = &(*MFL).o4fl[i];
		(*MFL).o4fl[i].start = (void*)p;
		(*MFL).o4fl[i].end = (void*)(p+256*1024/sizeof(int));
		(*MFL).o4fl[i].length = 256*1024;
		p = p + 256*1024/sizeof(int);
	}
	(*MFL).o4fl[1023].next = &(*MFL).o4fls;
	(*MFL).o4fls.prev = &(*MFL).o4fl[1023];
	(*MFL).o4fl[1023].start = (void*)p;
	(*MFL).o4fl[1023].end = (void*)(p+256*1024/sizeof(int));
	(*MFL).o4fl[1023].length = 256*1024;
	p = p + 256*1024/sizeof(int);

	(*MFL).o5fls.next = (*MFL).o5fl;
	(*MFL).o5fls.start = (void*)p;
	(*MFL).o5fls.end = (void*)p;
	(*(*MFL).o5fl).next = NULL;
	(*(*MFL).o5fl).start = (void*)p;
	(*(*MFL).o5fl).end = (void*)(p+16819159040/sizeof(int));
	(*MFL).hpend = (void*)(p+16819159040/sizeof(int));
	(*(*MFL).o5fl).length = 16819159040;

	printf("Memory inits successfully for device 0x%.2x.\n", (unsigned char)pci_bus);
}

void* o1fl_alloc(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o1fls;
	if ((*p).next==&(*DFL[pci_bus]).o1fls)
		return NULL;
	else{
		p = (*p).next;
		(*DFL[pci_bus]).o1fls.next = (*p).next;
		(*(*p).next).prev = &(*DFL[pci_bus]).o1fls;
		return (*p).start;
	}
}
void o1fl_free(unsigned char pci_bus, void* ps){
	int idx = ((unsigned int*)ps - (unsigned int*)((*DFL[pci_bus]).o1fls.start))*sizeof(int)/4096;
	MemBuf* p = (*DFL[pci_bus]).o1fls.prev;
	(*p).next = &(*DFL[pci_bus]).o1fl[idx];
	(*DFL[pci_bus]).o1fl[idx].next = &(*DFL[pci_bus]).o1fls;
	(*DFL[pci_bus]).o1fls.prev = &(*DFL[pci_bus]).o1fl[idx];
	(*DFL[pci_bus]).o1fl[idx].prev = p;
}
void* o2fl_alloc(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o2fls;
	if ((*p).next==&(*DFL[pci_bus]).o2fls)
		return NULL;
	else{
		p = (*p).next;
		(*DFL[pci_bus]).o2fls.next = (*p).next;
		(*(*p).next).prev = &(*DFL[pci_bus]).o2fls;
		return (*p).start;
	}
}
void o2fl_free(unsigned char pci_bus, void* ps){
	int idx = ((unsigned int*)ps - (unsigned int*)((*DFL[pci_bus]).o2fls.start))*sizeof(int)/16384;
	MemBuf* p = (*DFL[pci_bus]).o2fls.prev;
	(*p).next = &(*DFL[pci_bus]).o2fl[idx];
	(*DFL[pci_bus]).o2fl[idx].next = &(*DFL[pci_bus]).o2fls;
	(*DFL[pci_bus]).o2fls.prev = &(*DFL[pci_bus]).o2fl[idx];
	(*DFL[pci_bus]).o2fl[idx].prev = p;
}
void* o3fl_alloc(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o3fls;
	if ((*p).next==&(*DFL[pci_bus]).o3fls)
		return NULL;
	else{
		p = (*p).next;
		(*DFL[pci_bus]).o3fls.next = (*p).next;
		(*(*p).next).prev = &(*DFL[pci_bus]).o3fls;
		return (*p).start;
	}
}
void o3fl_free(unsigned char pci_bus, void* ps){
	int idx = ((unsigned int*)ps - (unsigned int*)((*DFL[pci_bus]).o3fls.start))*sizeof(int)/64/1024;
	MemBuf* p = (*DFL[pci_bus]).o3fls.prev;
	(*p).next = &(*DFL[pci_bus]).o3fl[idx];
	(*DFL[pci_bus]).o3fl[idx].next = &(*DFL[pci_bus]).o3fls;
	(*DFL[pci_bus]).o3fls.prev = &(*DFL[pci_bus]).o3fl[idx];
	(*DFL[pci_bus]).o3fl[idx].prev = p;
}
void* o4fl_alloc(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o4fls;
	if ((*p).next==&(*DFL[pci_bus]).o4fls)
		return NULL;
	else{
		p = (*p).next;
		(*DFL[pci_bus]).o4fls.next = (*p).next;
		(*(*p).next).prev = &(*DFL[pci_bus]).o4fls;
		return (*p).start;
	}
}
void o4fl_free(unsigned char pci_bus, void* ps){
	int idx = ((unsigned int*)ps - (unsigned int*)((*DFL[pci_bus]).o4fls.start))*sizeof(int)/256/1024;
	MemBuf* p = (*DFL[pci_bus]).o4fls.prev;
	(*p).next = &(*DFL[pci_bus]).o4fl[idx];
	(*DFL[pci_bus]).o4fl[idx].next = &(*DFL[pci_bus]).o4fls;
	(*DFL[pci_bus]).o4fls.prev = &(*DFL[pci_bus]).o4fl[idx];
	(*DFL[pci_bus]).o4fl[idx].prev = p;
}

void* o5fl_alloc(unsigned char pci_bus, size_t size){
	MemBuf* p = &(*DFL[pci_bus]).o5fls;
	int flag = 0;
	while ((*p).next!=NULL)
	{
		MemBuf* tmp = p;
		p = (*p).next;
		if ((*p).length == size)
		{
			(*tmp).next = (*p).next;
			void* ps = (*p).start;
			(*DFL[pci_bus]).o5flu[ps] = (uint64_t)size;
			free(p);
			return ps;
		}
		else if ((*p).length > size){
			uint64_t s = ceil((double)size/1024/1024)*1024*1024;
			void* ps = (*p).start;
			(*DFL[pci_bus]).o5flu[ps] = s;
			(*p).start = (unsigned int*)ps + s/sizeof(int);
			(*p).length = (*p).length-s;
			return ps;
		}
	}
	printf("Fails to allocate Huge Page Memory!\n");
	return NULL;
}
void o5fl_free(unsigned char pci_bus, void* ps){
	MemBuf* p1 = &(*DFL[pci_bus]).o5fls;
	MemBuf* p2 = (*p1).next;
	uint64_t s = (*DFL[pci_bus]).o5flu[ps];
	(*DFL[pci_bus]).o5flu.erase(ps);
	void* pe = (void*)((unsigned int*)ps + s/sizeof(int));
	while ((unsigned int*)ps > (unsigned int*)(*p2).start){
		p1=(*p1).next;
		p2=(*p1).next;
	}
	if (p1 == &(*DFL[pci_bus]).o5fls)
	{
		//printf("p1 == &o5fls\n");
		if (pe == (*p2).start)
		{
			//printf("pe == (*p2).start\n");
			(*p2).start = ps;
			(*p2).length = (*p2).length + s;
		}
		else
		{
			//printf("pe != (*p2).start\n");
			MemBuf* newfl = new MemBuf();
			(*newfl).start = ps;
			(*newfl).end = pe;
			(*newfl).length = s;
			(*newfl).next = p2;
			(*DFL[pci_bus]).o5fls.next = newfl;
		}
	}
	else
	{
		//printf("p1 != &o5fls\n");
		if (ps == (*p1).end && pe == (*p2).start)
		{
			//printf("ps == (*p1).end && pe == (*p2).start\n");
			(*p1).end = (*p2).end;
			(*p1).length = (*p1).length + s + (*p2).length;
			(*p1).next = (*p2).next;
			free(p2);
		}
		else if (ps == (*p1).end)
		{
			//printf("ps == (*p1).end\n");
			(*p1).end = pe;
			(*p1).length = (*p1).length + s;
		}
		else if (pe == (*p2).start)
		{
			//printf("pe == (*p2).start\n");
			(*p2).start = ps;
			(*p2).length = (*p2).length + s;
		}
		else
		{
			//printf("else\n");
			MemBuf* newfl = new MemBuf();
			(*newfl).start = ps;
			(*newfl).end = pe;
			(*newfl).length = s;
			(*p1).next = newfl;
			(*newfl).next = p2;
		}
	}
		
}

void* hpalloc(unsigned char pci_bus, size_t size){
	void* res = NULL;
	if (size <= 4096)
		{res = o1fl_alloc(pci_bus);}
	else if(size <= 16384)
		{res = o2fl_alloc(pci_bus);}
	else if(size <= 65536)
		{res = o3fl_alloc(pci_bus);}
	else if(size <= 262144)
		{res = o4fl_alloc(pci_bus);}
	if (res == NULL)
		{res = o5fl_alloc(pci_bus, uint64_t(size));}
	if (res == NULL)
		{printf("huge page memory allocation fails, please check if allocated memory is too large or you need to re-organized the buffer fragments.\n");}
	return res;
}
void hpfree(unsigned char pci_bus, void* p){
	if ((unsigned int*)p < (unsigned int*)(*DFL[pci_bus]).o1fls.start | (unsigned int*)p>=(*DFL[pci_bus]).hpend)
		{printf("Pointer not in huge page space! Unable to free this memory. \n");}
	else if ((*DFL[pci_bus]).o5flu.find(p) != (*DFL[pci_bus]).o5flu.end())
		{o5fl_free(pci_bus, p);}
	else if ((unsigned int*)p < (unsigned int*)(*DFL[pci_bus]).o2fls.start)
		{o1fl_free(pci_bus, p);}
	else if ((unsigned int*)p < (unsigned int*)(*DFL[pci_bus]).o3fls.start)
		{o2fl_free(pci_bus, p);}
	else if ((unsigned int*)p < (unsigned int*)(*DFL[pci_bus]).o4fls.start)
		{o3fl_free(pci_bus, p);}
	else if ((unsigned int*)p < (unsigned int*)(*DFL[pci_bus]).o5fls.start)
		{o4fl_free(pci_bus, p);}
	else
		{printf("Something is wrong with huge page memory management, this buffer is not freed. \n");}
}

void init_hbmaddr(unsigned char pci_bus){
	HBMBuf* newhbms = new HBMBuf();
	(*newhbms).start = 0x0000000010000000;
	(*newhbms).end = 0x0000000010000000;
	HBMBuf* newhbm = new HBMBuf();
	(*newhbms).next = newhbm;
	(*newhbm).start = 0x0000000010000000;
	(*newhbm).end = 0x0000000400000000;
	(*newhbm).length = 0x00000003F0000000;
	(*newhbm).next = NULL;
	hbmlist[pci_bus] = newhbms;
}
unsigned long hbm_getaddr(unsigned char pci_bus, size_t size){
	HBMBuf* p = hbmlist[pci_bus];
	int flag = 0;
	while ((*p).next!=NULL)
	{
		HBMBuf* tmp = p;
		p = (*p).next;
		if ((*p).length == size)
		{
			(*tmp).next = (*p).next;
			unsigned long addr = (*p).start;
			pair<unsigned char, unsigned long> _pair(pci_bus, addr);
			hbmu[_pair] = (uint64_t)size;
			free(p);
			return addr;
		}
		else if ((*p).length > size){
			uint64_t s = ceil((double)size/1024/1024)*1024*1024;
			unsigned long addr = (*p).start;
			pair<unsigned char, unsigned long> _pair(pci_bus, addr);
			hbmu[_pair] = s;
			(*p).start = addr + s;
			(*p).length = (*p).length - s;
			return addr;
		}
	}
	printf("Fails to get free HBM Memory Region!\n");
	return 4294967295;
}
void hbm_free(unsigned char pci_bus, unsigned long addr){
	HBMBuf* p1 = hbmlist[pci_bus];
	HBMBuf* p2 = (*p1).next;
	pair<unsigned char, unsigned long> _pair(pci_bus, addr);
	uint64_t s = hbmu[_pair];
	hbmu.erase(_pair);
	unsigned long addre = addr + s;
	while (addr > (*p2).start){
		p1=(*p1).next;
		p2=(*p1).next;
	}
	if (p1 == hbmlist[pci_bus])
	{
		//printf("p1 == hbmlist[pci_bus]\n");
		if (addre == (*p2).start)
		{
			//printf("addre == (*p2).start\n");
			(*p2).start = addr;
			(*p2).length = (*p2).length + s;
		}
		else
		{
			//printf("addre != (*p2).start\n");
			HBMBuf* newfl = new HBMBuf();
			(*newfl).start = addr;
			(*newfl).end = addre;
			(*newfl).length = s;
			(*newfl).next = p2;
			(*hbmlist[pci_bus]).next = newfl;
		}
	}
	else
	{
		//printf("p1 != hbmlist[pci_bus]\n");
		if (addr == (*p1).end && addre == (*p2).start)
		{
			//printf("ps == (*p1).end && pe == (*p2).start\n");
			(*p1).end = (*p2).end;
			(*p1).length = (*p1).length + s + (*p2).length;
			(*p1).next = (*p2).next;
			free(p2);
		}
		else if (addr == (*p1).end)
		{
			//printf("ps == (*p1).end\n");
			(*p1).end = addre;
			(*p1).length = (*p1).length + s;
		}
		else if (addre == (*p2).start)
		{
			//printf("pe == (*p2).start\n");
			(*p2).start = addr;
			(*p2).length = (*p2).length + s;
		}
		else
		{
			//printf("else\n");
			HBMBuf* newfl = new HBMBuf();
			(*newfl).start = addr;
			(*newfl).end = addre;
			(*newfl).length = s;
			(*p1).next = newfl;
			(*newfl).next = p2;
		}
	}
}

void o1fl_print(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o1fls;
	printf("o1list: %p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	while ((*p).next!=&(*DFL[pci_bus]).o1fls){
		p = (*p).next;
		printf("%p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void o2fl_print(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o2fls;
	printf("o2list: %p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	while ((*p).next!=&(*DFL[pci_bus]).o2fls){
		p = (*p).next;
		printf("%p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void o3fl_print(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o3fls;
	printf("o3list: %p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	while ((*p).next!=&(*DFL[pci_bus]).o3fls){
		p = (*p).next;
		printf("%p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void o4fl_print(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o4fls;
	printf("o4list: %p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	while ((*p).next!=&(*DFL[pci_bus]).o4fls){
		p = (*p).next;
		printf("%p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void o5fl_print(unsigned char pci_bus){
	MemBuf* p = &(*DFL[pci_bus]).o5fls;
	printf("o5list: %p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	while ((*p).next!=NULL){
		p = (*p).next;
		printf("%p, %p, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void o5flu_print(unsigned char pci_bus){
	printf("o5flu:\n");
	for (std::map<void*,uint64_t>::iterator it=(*DFL[pci_bus]).o5flu.begin(); it!=(*DFL[pci_bus]).o5flu.end(); ++it){
		printf("%p, %ld\n", it->first, it->second);
	}
}
void hbmlist_print(unsigned char pci_bus){
	HBMBuf* p = hbmlist[pci_bus];
	printf("hbmlist %d: %ld, %ld, %ld\n", pci_bus, (*p).start, (*p).end, (*p).length);
	while ((*p).next!=NULL){
		p = (*p).next;
		printf("%ld, %ld, %ld\n", (*p).start, (*p).end, (*p).length);
	}
}
void hbmu_print(){
	printf("hbm:\n");
	for (auto it = hbmu.begin(); it != hbmu.end(); ++it){
		printf("%d, %ld, %ld \n", it->first.first, it->first.second, it->second);
	}
}

unsigned char get_pci_bus(unsigned char pci_bus){
	if(pci_bus==0){
		pci_bus=default_pci_bus;
	}
	if(device_list.count(pci_bus)==0){
		printf("Device %d has not been initialized\n",pci_bus);
		exit(1);
	}
	return pci_bus;
}
void init(unsigned char pci_bus, size_t bridge_bar_size){
	printf("Init pci dev: 0x%x\n",pci_bus);
	if(device_list.count(pci_bus) != 0){
		printf("device 0x%x has already been initialized!\n",pci_bus);
		exit(1);
	}else{
		if(device_list.size() == 0){
			default_pci_bus = pci_bus;
		}
		Bars t;
		device_list[pci_bus] = t;
	}
	char fname[256];
	int fd;
	unsigned char pci_dev 	=	0;
	unsigned char dev_func	=	0;

	//axi-lite
	get_syspath_bar_mmap(fname, pci_bus, pci_dev,dev_func, 2);//lite bar is 2
	fd = open(fname, O_RDWR);
	if (fd < 0){
		printf("Open lite error, maybe need sudo or you can check whether if %s exists\n",fname);
		exit(1);
	}
	device_list[pci_bus].lite_bar =(uint32_t*) mmap(NULL, 4*1024, PROT_WRITE, MAP_SHARED, fd, 0);
	if(device_list[pci_bus].lite_bar == MAP_FAILED){
		printf("MMAP lite bar error, please check fpga lite bar size in vivado\n");
		exit(1);
	}
	//axi-bridge
	get_syspath_bar_mmap(fname, pci_bus, pci_dev,dev_func, 4);//bridge bar is 4
	fd = open(fname, O_RDWR);
	if (fd < 0){
		printf("Open bridge error, maybe need sudo or you can check whether if %s exists\n",fname);
		exit(1);
	}
	device_list[pci_bus].bridge_bar =(__m512i*) mmap(NULL, bridge_bar_size, PROT_WRITE, MAP_SHARED|MAP_LOCKED , fd, 0);
	if(device_list[pci_bus].bridge_bar == MAP_FAILED){
		printf("MMAP bridge bar error, please check fpga bridge bar size in vivado\n");
		exit(1);
	}
	//config bar
	get_syspath_bar_mmap(fname, pci_bus, pci_dev,dev_func, 0);//config bar is 0
	fd = open(fname, O_RDWR);
	if (fd < 0){
		printf("Open config error, maybe need sudo or you can check whether if %s exists\n",fname);
		exit(1);
	}
	device_list[pci_bus].config_bar = (uint32_t *)mmap(NULL, 256*1024, PROT_WRITE, MAP_SHARED, fd, 0);
	if(device_list[pci_bus].config_bar == MAP_FAILED){
		printf("MMAP config bar error, please check fpga config bar size in vivado\n");
		exit(1);
	}
}

void* qdma_alloc(size_t size, unsigned char pci_bus, int num, bool print_addr, bool erase_tlb){
	pci_bus = get_pci_bus(pci_bus);
	int fd,hfd;
	void* huge_base;
	struct huge_mem hm;
	if ((fd = open("/dev/rc4ml_dev",O_RDWR)) == -1) {
		printf("[ERROR] on open /dev/rc4ml_dev, maybe you need to add 'sudo', or insmod\n");
		exit(1);
   	}
	char path[30];
	sprintf(path,"/media/huge/hfd_%x_%d",pci_bus, num);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	huge_base = mmap(0, size, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	printf("huge pages base vaddr:%p\n", huge_base);
	hm.vaddr = (unsigned long)huge_base;
	hm.size = size;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	struct huge_mapping map;
	map.nhpages = size/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}

	tlb* page_table = (tlb*)calloc(1,sizeof(tlb));
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)huge_base + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}

	for(int i=0;i<page_table->npages;i++){
		if(print_addr){
			printf("%lx %lx\n",page_table->vaddr[i],page_table->paddr[i]);
		}
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= (i==0) & erase_tlb;
		if ((i==0) & erase_tlb == 1)
		// printf("is base at %lx %lx\n",page_table->vaddr[i],page_table->paddr[i]);
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	return huge_base;
}

void* init_tlb(unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	int fd,hfd;
	void* file1;
	void* file2;
	void* file3;
	void* file4;
	void* file5;
	void* file6;
	void* file7;
	void* file8;
	struct huge_mem hm;
	if ((fd = open("/dev/rc4ml_dev",O_RDWR)) == -1) {
		printf("[ERROR] on open /dev/rc4ml_dev, maybe you need to add 'sudo', or insmod\n");
		exit(1);
   	}
	char path[128];
	sprintf(path,"/media/huge/hfd_%x_1",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file1 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file1);
	sprintf(path,"/media/huge/hfd_%x_2",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file2 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file2);
	sprintf(path,"/media/huge/hfd_%x_3",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file3 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file3);
	sprintf(path,"/media/huge/hfd_%x_4",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file4 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file4);

	sprintf(path,"/media/huge/hfd_%x_5",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file5 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file5);
	sprintf(path,"/media/huge/hfd_%x_6",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file6 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file6);
	sprintf(path,"/media/huge/hfd_%x_7",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file7 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file7);
	sprintf(path,"/media/huge/hfd_%x_8",pci_bus);
	if ((hfd = open(path, O_CREAT | O_RDWR | O_SYNC, 0755)) == -1) {
		printf("[ERROR] on open %s, maybe you need to add 'sudo'\n",path);
		exit(1);
   	}
	file8 = mmap(0, 2L*1024*1024*1024, PROT_READ | PROT_WRITE, MAP_SHARED, hfd, 0);
	// printf("huge pages base vaddr:%p\n", file8);

	int error = 0;
	if ((unsigned int*)file1!=(unsigned int*)file2+536870912)
		error = 1;
	if ((unsigned int*)file2!=(unsigned int*)file3+536870912)
		error = 1;
	if ((unsigned int*)file3!=(unsigned int*)file4+536870912)
		error = 1;
	if ((unsigned int*)file4!=(unsigned int*)file5+536870912)
		error = 1;
	if ((unsigned int*)file5!=(unsigned int*)file6+536870912)
		error = 1;
	if ((unsigned int*)file6!=(unsigned int*)file7+536870912)
		error = 1;
	if ((unsigned int*)file7!=(unsigned int*)file8+536870912)
		error = 1;
	if (error)
		{
			printf("Fails to allocate 16GB continuous huge pages. Please check your system and modify the function hpalloc() and init_tlb()!\n");
			printf("errno = %d, %s \n", errno, strerror(errno)); 
		}
	else
		printf("Huge Pages init successfully.\n");

	hm.vaddr = (unsigned long)file8;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	struct huge_mapping map;
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	tlb* page_table = (tlb*)calloc(1,sizeof(tlb));
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file8 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= (i==0);
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file7;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file7 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file6;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file6 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file5;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file5 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file4;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file4 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file3;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file3 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file2;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file2 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
	}
	hm.vaddr = (unsigned long)file1;
	hm.size = 2L*1024*1024*1024;
	if(ioctl(fd, HUGE_MAPPING_SET, &hm) == -1){
		printf("IOCTL SET failed.\n");
	}
	map.nhpages = 2L*1024*1024*1024/(2*1024*1024);
	map.phy_addr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	if (ioctl(fd, HUGE_MAPPING_GET, &map) == -1) {
    	printf("IOCTL GET failed.\n");
   	}
	page_table->npages = map.nhpages;
	page_table->vaddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	page_table->paddr = (unsigned long*) calloc(map.nhpages, sizeof(unsigned long*));
	for(int i=0;i<page_table->npages;i++){
		page_table->vaddr[i] = (unsigned long)file1 + ((unsigned long)i)*2*1024*1024;
		page_table->paddr[i] = map.phy_addr[i];
	}
	for(int i=0;i<page_table->npages;i++){
		device_list[pci_bus].lite_bar[8]	= (uint32_t)(page_table->vaddr[i]);
		device_list[pci_bus].lite_bar[9]	= (uint32_t)((page_table->vaddr[i])>>32);
		device_list[pci_bus].lite_bar[10]	= (uint32_t)(page_table->paddr[i]);
		device_list[pci_bus].lite_bar[11]	= (uint32_t)((page_table->paddr[i])>>32);
		device_list[pci_bus].lite_bar[12]	= 0;
		device_list[pci_bus].lite_bar[13]	= 1;
		device_list[pci_bus].lite_bar[13]	= 0;
		// printf("%p %p \n", (unsigned int*)(page_table->vaddr[i]), (unsigned int*)(page_table->paddr[i]));
	}
	long int pci_vaddr = (long int)file1 + 2L*1024*1024*1024;
	for (auto it = device_list.begin(); it != device_list.end(); ++it){
		if (pci_bus != it->first)
			{
				long int pci_baddr = (long int)it->second.bridge_bar;
				sprintf(path,"/sys/bus/pci/devices/0000:%x:00.0/resource",(unsigned char)it->first);
				FILE* pci_resource = fopen(path, "r");
				char tmp_pci_paddr[19];
				char* fseek_return;
				fseek(pci_resource, 228, SEEK_SET);
				fseek_return = fgets(tmp_pci_paddr, 19, pci_resource);
				long int pci_paddr = strtol(tmp_pci_paddr, nullptr, 0);
				printf("pci_resource: %s vaddr: %.16lx \n", tmp_pci_paddr, pci_vaddr);

				pair<char,char> _pair(it->first, pci_bus);
				pci_paddr_tran[_pair] = pci_vaddr - pci_baddr;
				// printf("%p %p \n", (unsigned int*)pci_vaddr, (unsigned int*)pci_paddr);
				for(int i=0;i<512;i++){
					device_list[pci_bus].lite_bar[8]	= (uint32_t)(pci_vaddr);
					device_list[pci_bus].lite_bar[9]	= (uint32_t)(pci_vaddr>>32);
					device_list[pci_bus].lite_bar[10]	= (uint32_t)(pci_paddr);
					device_list[pci_bus].lite_bar[11]	= (uint32_t)(pci_paddr>>32);
					device_list[pci_bus].lite_bar[12]	= 0;
					device_list[pci_bus].lite_bar[13]	= 1;
					device_list[pci_bus].lite_bar[13]	= 0;
					// printf("%p %p \n", (unsigned int*)pci_vaddr, (unsigned int*)pci_paddr);
					pci_vaddr = pci_vaddr + 2097152;
					pci_paddr = pci_paddr + 2097152;
				}
			}
		}

	printf("TLB written into device 0x%.2x.\n", (unsigned int)pci_bus);
	return file8;
}

long int p2p_paddr_translation(unsigned char pci_bus1, unsigned char pci_bus2, long int pci_baddr)
{
	pair<char,char> _pair(pci_bus1,pci_bus2);
	long int paddr = pci_baddr + pci_paddr_tran[_pair];
	return paddr;
}

void writeConfig(uint32_t index,uint32_t value, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	device_list[pci_bus].config_bar[index] = value;
}
uint32_t readConfig(uint32_t index, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	return device_list[pci_bus].config_bar[index];
}

void writeReg(uint32_t index,uint32_t value, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	device_list[pci_bus].lite_bar[index] = value;
}
uint32_t readReg(uint32_t index, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	return device_list[pci_bus].lite_bar[index];
}

void writeBridge(uint32_t index, uint64_t* value, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	device_list[pci_bus].bridge_bar[index] = _mm512_set_epi64(value[7],value[6],value[5],value[4],value[3],value[2],value[1],value[0]);
}

void readBridge(uint32_t index, uint64_t* value, unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	_mm512_store_epi64(value,device_list[pci_bus].bridge_bar[index]);
}


void* getBridgeAddr(unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	return (void*)(device_list[pci_bus].bridge_bar);
}

void* getLiteAddr(unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	return (void*)(device_list[pci_bus].lite_bar);
}

void resetCounters(unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	device_list[pci_bus].lite_bar[14] = 1;
	device_list[pci_bus].lite_bar[14] = 0;
}

void printCounters(unsigned char pci_bus){
	pci_bus = get_pci_bus(pci_bus);
	volatile uint32_t *axi_lite = device_list[pci_bus].lite_bar;
 volatile uint32_t *bar = axi_lite + 512;
//Report width 32:
printf("tlb.io.tlb_miss_count                                       : %u\n", bar[300]);
printf("io.c2h_cmd.[fire]                                           : %u\n", bar[301]);
printf("io.h2c_cmd.[fire]                                           : %u\n", bar[302]);
printf("io.c2h_data.[fire]                                          : %u\n", bar[303]);
printf("io.h2c_data.[fire]                                          : %u\n", bar[304]);
printf("fifo_c2h_cmd.io.out.[fire]                                  : %u\n", bar[305]);
printf("fifo_h2c_cmd.io.out.[fire]                                  : %u\n", bar[306]);
printf("fifo_c2h_data.io.out.[fire]                                 : %u\n", bar[307]);
printf("fifo_h2c_data.io.in.[fire]                                  : %u\n", bar[308]);
printf("read_data_latency[high]                                     : %u\n", bar[309]);
printf("read_data_latency[low]                                      : %u\n", bar[310]);

//Report width 1:
printf("fifo_c2h_cmd.io.out.valid                                   : %u\n", (bar[311] >> 0) & 1);
printf("fifo_c2h_cmd.io.out.ready                                   : %u\n", (bar[311] >> 1) & 1);
printf("fifo_h2c_cmd.io.out.valid                                   : %u\n", (bar[311] >> 2) & 1);
printf("fifo_h2c_cmd.io.out.ready                                   : %u\n", (bar[311] >> 3) & 1);
printf("fifo_c2h_data.io.out.valid                                  : %u\n", (bar[311] >> 4) & 1);
printf("fifo_c2h_data.io.out.ready                                  : %u\n", (bar[311] >> 5) & 1);
printf("fifo_h2c_data.io.in.valid                                   : %u\n", (bar[311] >> 6) & 1);
printf("fifo_h2c_data.io.in.ready                                   : %u\n", (bar[311] >> 7) & 1);
printf("boundary_split.io.cmd_in.valid                              : %u\n", (bar[311] >> 8) & 1);
printf("boundary_split.io.cmd_in.ready                              : %u\n", (bar[311] >> 9) & 1);
printf("boundary_split.io.data_in.valid                             : %u\n", (bar[311] >> 10) & 1);
printf("boundary_split.io.data_in.ready                             : %u\n", (bar[311] >> 11) & 1);
  
 
	cout<<endl<<"QDMA debug info:"<<endl<<endl;
	printf("bar 1: 0x%x 	tlb miss count\n",	axi_lite[512+1]);

	cout<<endl<<"C2H CMD fire()"<<endl;
	printf("bar 2: 0x%x       io.c2h_cmd\n",	axi_lite[512+2]);
	printf("bar 3: 0x%x       check_c2h.io.out\n",	axi_lite[512+3]);
	printf("bar 5: 0x%x       tlb.io.c2h_out\n",	axi_lite[512+4]);
	printf("bar 4: 0x%x       boundary_split.io.cmd_out\n",	axi_lite[512+5]);
	printf("bar 6: 0x%x       fifo_c2h_cmd.io.out\n",	axi_lite[512+6]);

	cout<<endl<<"H2C CMD fire()"<<endl;
	printf("bar 7: 0x%x       io.h2c_cmd\n",	axi_lite[512+7]);
	printf("bar 8: 0x%x       check_h2c.io.out\n",	axi_lite[512+8]);
	printf("bar 9: 0x%x       tlb.io.h2c_out\n",	axi_lite[512+9]);
	printf("bar 10: 0x%x      fifo_h2c_cmd.io.out\n",	axi_lite[512+10]);

	cout<<endl<<"C2H DATA fire()"<<endl;
	printf("bar 11: 0x%x      io.c2h_data\n",	axi_lite[512+11]);
	printf("bar 12: 0x%x      boundary_split.io.data_out\n",	axi_lite[512+12]);
	printf("bar 13: 0x%x      fifo_c2h_data.io.out\n",	axi_lite[512+13]);

	cout<<endl<<"H2C DATA fire()"<<endl;
	printf("bar 14: 0x%x      io.h2c_data\n",	axi_lite[512+14]);
	printf("bar 15: 0x%x      fifo_h2c_data.io.in\n",	axi_lite[512+15]);

	//reporter
	cout<<endl;
	cout<<((axi_lite[512+16]>>0) & 1) << " Report 0:boundary check state===sIDLE"<<endl;
	cout<<((axi_lite[512+16]>>1) & 1) << " Report 1:boundary check state===sIDLE"<<endl;
	cout<<((axi_lite[512+16]>>2) & 1) << " Report 2:boundary split state===sIDLE"<<endl;
	cout<<endl;
	cout<<((axi_lite[512+16]>>3) & 1) << " Report 3:fifo_c2h_cmd.io.out.valid"<<endl;
	cout<<((axi_lite[512+16]>>4) & 1) << " Report 4:fifo_c2h_cmd.io.out.ready"<<endl;
	cout<<endl;
	cout<<((axi_lite[512+16]>>5) & 1) << " Report 5:fifo_h2c_cmd.io.out.valid"<<endl;
	cout<<((axi_lite[512+16]>>6) & 1) << " Report 6:fifo_h2c_cmd.io.out.ready"<<endl;
	cout<<endl;
	cout<<((axi_lite[512+16]>>7) & 1) << " Report 7:fifo_c2h_data.io.out.valid"<<endl;
	cout<<((axi_lite[512+16]>>8) & 1) << " Report 8:fifo_c2h_data.io.out.ready"<<endl;
	cout<<endl;
	cout<<((axi_lite[512+16]>>9) & 1)  << " Report 9:fifo_h2c_data.io.in.valid"<<endl;
	cout<<((axi_lite[512+16]>>10) & 1)  << " Report 10:fifo_h2c_data.io.in.ready"<<endl;
}
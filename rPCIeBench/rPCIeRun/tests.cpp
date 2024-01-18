#include <QDMAController.h>
#include "mmio.hpp"
#include "dma.hpp"
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

void MMIO_test(unsigned char pci_bus)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();
	volatile __m512i* bridge = (volatile __m512i*)getBridgeAddr(pci_bus);

	__m512i data1[256];
	__m512i data2[256];

    int idata3[8] = {1,2,3,4,5,6,7,8};
    __m256i data3 = _mm256_loadu_si256((__m256i *)&idata3[0]);
	__m256i data4;

	int idata5[4] = {9,10,11,12};
    __m128i data5 = _mm_loadu_si128((__m128i *)&idata5[0]);
	__m128i data6;

	double data7 = 12345;
	double data8;

	int size=16*1024;
	int times=10000;
	long total = 0;
	printf("\n");

    // 8 bytes
	for (int i=0;i<times;i++)
	{
		*(double*)(bridge) = data7;
	}
	
	total = 0;
	for (int i=0;i<times;i++)
	{
		start_time = Clock::now();
		data8 = *(double*)(bridge);
		end_time = Clock::now();
		total += (end_time - start_time).count();
	}

	if (data7 == data8)
	{
		printf("test succeeds.\n");
		std::cout << "MMIO tests 8 bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	else
	{
        printf("test fails.\n");
	}

    // 16 bytes
    for (int i=0;i<times;i++)
	{
		_mm_stream_si128 ((__m128i *)(bridge), data5);
    }

    total = 0;
    for (int i=0;i<times;i++)
	{
		start_time = Clock::now();
		data6 = _mm_stream_load_si128 ((__m128i *)(bridge));
		end_time = Clock::now();
		total += (end_time - start_time).count();
	}

	if ((data5[0] == data6[0]) & (data5[1] == data6[1]))
	{
		printf("test succeeds.\n");
        std::cout << "MMIO tests 16 bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	else
	{
        printf("test fails.\n");
	}

    // 32 bytes
    for (int i=0;i<times;i++)
	{
		_mm256_stream_si256 ((__m256i *)(bridge), data3);
    }

    total = 0;
    for (int i=0;i<times;i++)
	{
		start_time = Clock::now();
		data4 = _mm256_stream_load_si256 ((__m256i *)(bridge));
		end_time = Clock::now();
		total += (end_time - start_time).count();
	}

	if ((data3[0] == data4[0]) & (data3[1] == data4[1]) & (data3[2] == data4[2]) & (data3[3] == data4[3]))
	{
		printf("test succeeds.\n");
        std::cout << "MMIO tests 32 bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	else
	{
        printf("test fails.\n");
	}

    // 64 to 16K bytes
    for (int size = 64; size <= 16*1024; size=size*2)
    {
        for (int j=0; j<256; j++)
        {
            for (int i=0;i<8;i++)
            {
                data1[j][i]=10*j+i+size;
            }
        }

        for (int i=0;i<times;i++)
        {
            mmio_write(bridge, data1, size);
        }

        total = 0;
        for (int i=0;i<times;i++)
        {
            start_time = Clock::now();
            mmio_read(bridge, data2, size);
            end_time = Clock::now();
            total += (end_time - start_time).count();
        }

        if (memcmp(data1, data2, size)!=0)
        {
            printf("test fails.\n");
        }
        else
        {
            printf("test succeeds.\n");
            std::cout << "MMIO tests " << size << " bytes size latency: " << total/times << " nanoseconds" << std::endl;
        }
    }


}

void h2d_DMA_latency(unsigned char pci_bus)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	long total = 0;
	int times = 1000;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}

	for (int size=64; size <= 64*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			bar[20] = 1;
			bar[50] = (uint32_t)((unsigned long)p>>32);
			bar[51] = (uint32_t)((unsigned long)p);
			bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[54] = size;
			bar[55] = 32768;
			bar[56] = 1 << 0;
			bar[57] = (uint32_t)((unsigned long)s>>32);
			bar[58] = (uint32_t)((unsigned long)s);

			usleep(100);

			start_time = Clock::now();
			bar[59] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[59] = 0;
			s[0] = 0;
			total += (end_time - start_time).count();
		}
		std::cout << "h2d DMA tests " << size << " bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	std::cout << std::endl;
}

void d2h_DMA_latency(unsigned char pci_bus)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	long total = 0;
	int times = 1000;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}

	for (int size=64; size <= 64*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			bar[20] = 1;
			bar[70] = (uint32_t)((unsigned long)p>>32);
			bar[71] = (uint32_t)((unsigned long)p);
			bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[74] = size;
			bar[75] = 4096;
			bar[76] = 1 << 0;
			bar[77] = (uint32_t)((unsigned long)s>>32);
			bar[78] = (uint32_t)((unsigned long)s);

			uint32_t tag;
			writeConfig(0x1408/4, 0, pci_bus);
			tag = readConfig(0x140c/4, pci_bus);
			bar[80] = tag;

			usleep(100);

			start_time = Clock::now();
			bar[79] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[79] = 0;
			s[0] = 0;
			total += (end_time - start_time).count();
		}
		std::cout << "d2h DMA tests " << size << " bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	std::cout << std::endl;
}

void h2d_DMA_bandwidth(unsigned char pci_bus)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	long total = 0;
	int times = 1000;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}

	for (int size=64; size <= 16*1024*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			bar[20] = 1;
			bar[50] = (uint32_t)((unsigned long)p>>32);
			bar[51] = (uint32_t)((unsigned long)p);
			bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[54] = size;
			bar[55] = 32768;
			bar[56] = 1 << 0;
			bar[57] = (uint32_t)((unsigned long)s>>32);
			bar[58] = (uint32_t)((unsigned long)s);

			usleep(100);

			start_time = Clock::now();
			bar[59] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[59] = 0;
			s[0] = 0;
			total += double(size)*1e9/(end_time - start_time).count()/1024/1024;
		}
		std::cout << "h2d bandwidth tests " << size << " bytes size latency: " << total/times << " MB/s" << std::endl;
	}
	std::cout << std::endl;
}

void d2h_DMA_bandwidth(unsigned char pci_bus)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	long total = 0;
	int times = 1000;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}

	for (int size=64; size <= 16*1024*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			bar[20] = 1;
			bar[70] = (uint32_t)((unsigned long)p>>32);
			bar[71] = (uint32_t)((unsigned long)p);
			bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[74] = size;
			bar[75] = 4096;
			bar[76] = 1 << 0;
			bar[77] = (uint32_t)((unsigned long)s>>32);
			bar[78] = (uint32_t)((unsigned long)s);

			uint32_t tag;
			writeConfig(0x1408/4, 0, pci_bus);
			tag = readConfig(0x140c/4, pci_bus);
			bar[80] = tag;

			usleep(100);

			start_time = Clock::now();
			bar[79] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[79] = 0;
			s[0] = 0;
			total += double(size)*1e9/(end_time - start_time).count()/1024/1024;
		}
		std::cout << "d2h bandwidth tests " << size << " bytes size latency: " << total/times << " MB/s" << std::endl;
	}
	std::cout << std::endl;
}

void p2p_read_latency(unsigned char pci_bus1, unsigned char pci_bus2)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;

	long total = 0;
	int times = 1000;

	unsigned int * s = (unsigned int *)hpalloc(pci_bus1, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}

	for (int size=64; size <= 64*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
			bar[20] = 1;
			bar[50] = (uint32_t)((unsigned long)vaddr>>32);
			bar[51] = (uint32_t)((unsigned long)vaddr);
			bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[54] = size;
			bar[55] = 32768;
			bar[56] = 1 << 0;
			bar[57] = (uint32_t)((unsigned long)s>>32);
			bar[58] = (uint32_t)((unsigned long)s);

			usleep(100);

			start_time = Clock::now();
			bar[59] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[59] = 0;
			s[0] = 0;
			total += (end_time - start_time).count();
		}
		std::cout << "p2p read tests " << size << " bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	std::cout << std::endl;
}

void p2p_write_latency(unsigned char pci_bus1, unsigned char pci_bus2)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;

	long total = 0;
	int times = 1000;

	unsigned int * s1 = (unsigned int *)hpalloc(pci_bus1, 262144);
	unsigned int * s2 = (unsigned int *)hpalloc(pci_bus2, 262144);

	for(int i=0;i<262144/4;i++){
		s1[i] = 0;
		s2[i] = 0;
	}

	unsigned int * p1 = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p1[i] = -1;
	}
	h2d(pci_bus1, p1, 1024*1024*1024, 0x0000000040000000, s1, 0);
	while(s1[0]!=-1)
		{__sync_synchronize();}
	s1[0] = 0;

	for (int size=64; size <= 64*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
			bar1[20] = 1;
			bar1[70] = (uint32_t)((unsigned long)((unsigned int *)vaddr+i*size/4)>>32);
			bar1[71] = (uint32_t)((unsigned long)((unsigned int *)vaddr+i*size/4));
			bar1[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar1[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar1[74] = size;
			bar1[75] = 4096;
			bar1[76] = 1 << 0;
			bar1[77] = (uint32_t)((unsigned long)s1>>32);
			bar1[78] = (uint32_t)((unsigned long)s1);

			bar2[20] = 1;
			bar2[92] = (uint32_t)((unsigned long)s2>>32);
			bar2[93] = (uint32_t)((unsigned long)s2);
			bar2[94] = size;
			bar2[91] = 1;

			usleep(100);

			start_time = Clock::now();
			bar1[79] = 1;	//start
			while(s2[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar1[79] = 0;
			s2[0] = 0;
			total += (end_time - start_time).count();
			bar2[91] = 0;
		}
	std::cout << "p2p write tests " << size << " bytes size latency: " << total/times << " nanoseconds" << std::endl;
	}
	std::cout << std::endl;
}

void p2p_read_bandwidth(unsigned char pci_bus1, unsigned char pci_bus2)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;

	long total = 0;
	int times = 1000;

	unsigned int * s = (unsigned int *)hpalloc(pci_bus1, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}

	for (int size=64; size <= 16*1024*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
			bar[20] = 1;
			bar[50] = (uint32_t)((unsigned long)vaddr>>32);
			bar[51] = (uint32_t)((unsigned long)vaddr);
			bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[54] = size;
			bar[55] = 32768;
			bar[56] = 1 << 0;
			bar[57] = (uint32_t)((unsigned long)s>>32);
			bar[58] = (uint32_t)((unsigned long)s);

			usleep(100);

			start_time = Clock::now();
			bar[59] = 1;	//start
			while(s[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar[59] = 0;
			s[0] = 0;
			total += double(size)*1e9/(end_time - start_time).count()/1024/1024;
		}
		std::cout << "p2p read bandwidth tests " << size << " bytes size latency: " << total/times << " MB/s" << std::endl;
	}
	std::cout << std::endl;
}

void p2p_write_bandwidth(unsigned char pci_bus1, unsigned char pci_bus2)
{
	auto start_time = Clock::now();
	auto end_time = Clock::now();

	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;

	long total = 0;
	int times = 1000;

	unsigned int * s1 = (unsigned int *)hpalloc(pci_bus1, 262144);
	unsigned int * s2 = (unsigned int *)hpalloc(pci_bus2, 262144);

	for(int i=0;i<262144/4;i++){
		s1[i] = 0;
		s2[i] = 0;
	}

	for (int size=64; size <= 64*1024; size=size*2)
	{
		total = 0;
		for (int i=0; i<times; i++)
		{
			long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
			bar1[20] = 1;
			bar1[70] = (uint32_t)((unsigned long)vaddr>>32);
			bar1[71] = (uint32_t)((unsigned long)vaddr);
			bar1[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar1[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar1[74] = size;
			bar1[75] = 4096;
			bar1[76] = 1 << 0;
			bar1[77] = (uint32_t)((unsigned long)s1>>32);
			bar1[78] = (uint32_t)((unsigned long)s1);

			uint32_t tag;
			writeConfig(0x1408/4, 0, pci_bus1);
			tag = readConfig(0x140c/4, pci_bus1);
			bar1[80] = tag;
			writeConfig(0x1408/4, 0, pci_bus2);
			tag = readConfig(0x140c/4, pci_bus2);
			bar2[80] = tag;

			bar2[20] = 1;
			bar2[92] = (uint32_t)((unsigned long)s2>>32);
			bar2[93] = (uint32_t)((unsigned long)s2);
			bar2[94] = size;
			bar2[91] = 1;

			usleep(100);

			start_time = Clock::now();
			bar1[79] = 1;	//start
			while(s2[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			bar1[79] = 0;
			s2[0] = 0;
			total += double(size)*1e9/(end_time - start_time).count()/1024/1024;
			bar2[91] = 0;
		}
		std::cout << "p2p write bandwidth tests " << size << " bytes size latency: " << total/times << " MB/s" << std::endl;
	}
	std::cout << std::endl;
}

void h2d_latency_throughput(unsigned char pci_bus, bool hetero=false)
{
	int* lat = new int(10000);

	auto start_time = Clock::now();
	auto end_time = Clock::now();
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	unsigned int * p;
	unsigned int * r;
	
	p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}
	unsigned int * s=(unsigned int *)hpalloc(pci_bus, 262144);
	for(int i=0;i<262144/4;i++){//initial
		s[i] = 0;
	}

	for(int pkt_size=64; pkt_size <= 64*1024; pkt_size = pkt_size * 2)
	{
	for(int flows = 1; flows <= 16; flows++)
	{
		int test_size;
		if (hetero)
			{test_size = pkt_size;}
		else
			{test_size = 64;}
		double total = 0;

		for (int loop=0; loop<100; loop++)
		{
			start_time = Clock::now();
			for (int i=1; i<flows; i++)
			{
				if (flows < 4)
					{h2d(pci_bus, p, 512*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
				else if (flows < 8)
					{h2d(pci_bus, p, 256*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
				else
					{h2d(pci_bus, p, 128*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
			}

			bar[20] = 1;
			bar[50] = (uint32_t)((unsigned long)p>>32);
			bar[51] = (uint32_t)((unsigned long)p);
			bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[54] = test_size;
			bar[55] = pkt_size;
			bar[56] = 1 << 0;
			bar[57] = (uint32_t)((unsigned long)s>>32);
			bar[58] = (uint32_t)((unsigned long)s);

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar[59] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_lat = Clock::now();

				bar[59] = 0;
				s[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			for (int i=1; i<flows; i++)
			{
				while(s[16*i]!=-1)
				{__sync_synchronize();}
			}

			end_time = Clock::now();

			usleep(1000);

			for (int i=1; i<flows; i++)
			{
				s[16*i]=0;
			}

			if (flows < 4)
				{total += (double(512*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else if (flows < 8)
				{total += (double(256*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else
				{total += (double(128*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 10000;

		std::cout << "h2d latency-throughput test with " << flows << " flows and pkt_size = " << pkt_size << "  Bandwidth: " << total/100 << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
	}
}

void d2h_latency_throughput(unsigned char pci_bus, bool hetero=false)
{
	int* lat = new int(10000);

	auto start_time = Clock::now();
	auto end_time = Clock::now();
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	unsigned int * p;
	unsigned int * r;
	
	p = (unsigned int *)hpalloc(pci_bus, 1*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}
	unsigned int * s=(unsigned int *)hpalloc(pci_bus, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s[i] = 0;
	}

	for(int pkt_size=64; pkt_size <= 64*1024; pkt_size = pkt_size * 2)
	{
	for(int flows = 1; flows <= 16; flows++)
	{
		int test_size;
		if (hetero)
			{test_size = pkt_size;}
		else
			{test_size = 64;}
		double total = 0;

		for (int loop=0; loop<10; loop++)
		{
			start_time = Clock::now();
			for (int i=1; i<flows; i++)
			{
				if (flows < 4)
					{d2h(pci_bus, p, 512*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
				else if (flows < 8)
					{d2h(pci_bus, p, 256*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
				else
					{d2h(pci_bus, p, 128*1024*1024, 0x0000000040000000+0*0x4000000, s+16*i, i, pkt_size);}
			}

			bar[20] = 1;
			bar[70] = (uint32_t)((unsigned long)p>>32);
			bar[71] = (uint32_t)((unsigned long)p);
			bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[74] = test_size;
			bar[75] = 4096;
			bar[76] = 1 << 0;
			bar[77] = (uint32_t)((unsigned long)s>>32);
			bar[78] = (uint32_t)((unsigned long)s);

			uint32_t tag;
			writeConfig(0x1408/4, 0, pci_bus);
			tag = readConfig(0x140c/4, pci_bus);
			bar[80] = tag;

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_lat = Clock::now();

				bar[79] = 0;
				s[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			for (int i=1; i<flows; i++)
			{
				while(s[16*i]!=-1)
				{__sync_synchronize();}
			}

			end_time = Clock::now();

			usleep(1000);

			for (int i=1; i<flows; i++)
			{
				s[16*i]=0;
			}

			if (flows < 4)
				{total += (double(512*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else if (flows < 8)
				{total += (double(256*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else
				{total += (double(128*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 10000;

		std::cout << "d2h latency-throughput test with " << flows << " flows and pkt_size = " << pkt_size << "  Bandwidth: " << total/100 << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
	}

}

void p2p_read_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false)
{
	int* lat = new int(10000);
	
	auto start_time = Clock::now();
	auto end_time = Clock::now();
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;

	long total = 0;
	int times = 10000;

	unsigned int * s = (unsigned int *)hpalloc(pci_bus1, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}

	for(int pkt_size=64; pkt_size <= 64*1024; pkt_size = pkt_size * 2)
	{
	for(int flows = 1; flows <= 16; flows++)
	{
		int test_size;
		if (hetero)
			{test_size = pkt_size;}
		else
			{test_size = 64;}
		double total = 0;

		for (int loop=0; loop<100; loop++)
		{
			start_time = Clock::now();
			for (int i=1; i<flows; i++)
			{
				if (flows < 4)
					{p2p_read(pci_bus1, 0, pci_bus2, 256*1024*1024, 0x0000000040000000+0x10000, s+16*i, i, pkt_size);}
				else if (flows < 8)
					{p2p_read(pci_bus1, 0, pci_bus2, 128*1024*1024, 0x0000000040000000+0x10000, s+16*i, i, pkt_size);}
				else
					{p2p_read(pci_bus1, 0, pci_bus2, 64*1024*1024, 0x0000000040000000+0x10000, s+16*i, i, pkt_size);}
			}

				long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
				bar[20] = 1;
				bar[50] = (uint32_t)((unsigned long)vaddr>>32);
				bar[51] = (uint32_t)((unsigned long)vaddr);
				bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[54] = test_size;
				bar[55] = pkt_size;
				bar[56] = 1 << 0;
				bar[57] = (uint32_t)((unsigned long)s>>32);
				bar[58] = (uint32_t)((unsigned long)s);

				usleep(100);

				for (int i=0;i<100;i++)
				{
					usleep(10);
					start_lat = Clock::now();
					bar[59] = 1;	//start
					while(s[0]!=-1)
						{__sync_synchronize();}
					end_lat = Clock::now();

					bar[59] = 0;
					s[0] = 0;
					lat[100*loop+i] = (end_lat - start_lat).count();
				}

			for (int i=1; i<flows; i++)
			{
				while(s[16*i]!=-1)
				{__sync_synchronize();}
			}

			end_time = Clock::now();

			usleep(1000);

			for (int i=1; i<flows; i++)
			{
				s[16*i]=0;
			}

			if (flows < 4)
				{total += (double(512*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else if (flows < 8)
				{total += (double(256*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else
				{total += (double(128*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 10000;

		std::cout << "p2p read latency-throughput test with " << flows << " flows and pkt_size = " << pkt_size << "  Bandwidth: " << total/100 << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
	}
}

void p2p_write_latency_throughput(unsigned char pci_bus1, unsigned char pci_bus2, bool hetero=false)
{
	int* lat = new int(10000);
	
	auto start_time = Clock::now();
	auto end_time = Clock::now();
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;

	long total = 0;
	int times = 1000;

	unsigned int * s1 = (unsigned int *)hpalloc(pci_bus1, 262144);
	unsigned int * s2 = (unsigned int *)hpalloc(pci_bus2, 262144);

	for(int i=0;i<262144/4;i++){
		s1[i] = 0;
		s2[i] = 0;
	}

	for(int pkt_size=64; pkt_size <= 64*1024; pkt_size = pkt_size * 2)
	{
	for(int flows = 1; flows <= 16; flows++)
	{
		int test_size;
		if (hetero)
			{test_size = pkt_size;}
		else
			{test_size = 64;}
		double total = 0;

		for (int loop=0; loop<100; loop++)
		{
			start_time = Clock::now();
			for (int i=1; i<flows; i++)
			{
				if (flows < 4)
					{p2p_write(pci_bus1, 0, pci_bus2, 256*1024*1024, 0x0000000040000000+0x10000, s1+16*i, i, pkt_size);}
				else if (flows < 8)
					{p2p_write(pci_bus1, 0, pci_bus2, 128*1024*1024, 0x0000000040000000+0x10000, s1+16*i, i, pkt_size);}
				else
					{p2p_write(pci_bus1, 0, pci_bus2, 64*1024*1024, 0x0000000040000000+0x10000, s1+16*i, i, pkt_size);}
			}

			long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
			bar1[20] = 1;
			bar1[70] = (uint32_t)((unsigned long)vaddr>>32);
			bar1[71] = (uint32_t)((unsigned long)vaddr);
			bar1[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar1[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar1[74] = test_size;
			bar1[75] = pkt_size;
			bar1[76] = 1 << 0;
			bar1[77] = (uint32_t)((unsigned long)s1>>32);
			bar1[78] = (uint32_t)((unsigned long)s1);

			uint32_t tag;
			writeConfig(0x1408/4, 0, pci_bus1);
			tag = readConfig(0x140c/4, pci_bus1);
			bar1[80] = tag;
			writeConfig(0x1408/4, 0, pci_bus2);
			tag = readConfig(0x140c/4, pci_bus2);
			bar2[80] = tag;

			bar2[20] = 1;
			bar2[92] = (uint32_t)((unsigned long)s2>>32);
			bar2[93] = (uint32_t)((unsigned long)s2);
			bar2[94] = test_size;
			bar2[91] = 1;
			bar2[91] = 0;

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar1[79] = 1;	//start
				while(s2[0]!=-1)
					{__sync_synchronize();}
				end_lat = Clock::now();

				bar1[79] = 0;
				s2[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			for (int i=1; i<flows; i++)
			{
				while(s2[16*i]!=-1)
				{__sync_synchronize();}
			}

			end_time = Clock::now();

			usleep(1000);

			for (int i=1; i<flows; i++)
			{
				s2[16*i]=0;
			}

			if (flows < 4)
				{total += (double(512*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else if (flows < 8)
				{total += (double(256*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			else
				{total += (double(128*1024*1024)*(flows-1)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;}
			
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 10000;

		std::cout << "p2p write latency-throughput test with " << flows << " flows and pkt_size = " << pkt_size << "  Bandwidth: " << total/100 << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
	}
}

void h2d_bandwidth_partition(unsigned char pci_bus)
{
	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}
	
	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int f1pkt = 1024;
	int test_time = 100;
	double total1, total2, total3 = 0;
	for(int f1pkt=1024; f1pkt <= 64*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=64; f2pkt <= 64*1024; f2pkt = f2pkt * 2)
		{
			total1 = 0;
			total2 = 0;
			total3 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				h2d(pci_bus, p, 1024*1024*1024, 0x0000000440000000, s+16, 2, f2pkt);

				bar[20] = 1;
				bar[50] = (uint32_t)((unsigned long)p>>32);
				bar[51] = (uint32_t)((unsigned long)p);
				bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[54] = f1size;
				bar[55] = f1pkt;
				bar[56] = 1 << 1;
				bar[57] = (uint32_t)((unsigned long)s>>32);
				bar[58] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time1 = Clock::now();
				bar[59] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}

				bar[59] = 0;
				s[0] = 0;
				s[16] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;

				// flow2 test
				h2d(pci_bus, p, 1024*1024*1024, 0x0000000440000000, s+16, 1, f1pkt);

				bar[20] = 1;
				bar[50] = (uint32_t)((unsigned long)p>>32);
				bar[51] = (uint32_t)((unsigned long)p);
				bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[54] = f2size;
				bar[55] = f2pkt;
				bar[56] = 1 << 2;
				bar[57] = (uint32_t)((unsigned long)s>>32);
				bar[58] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time2 = Clock::now();
				bar[59] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				bar[59] = 0;
				s[0] = 0;
				s[16] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;
			}
			std::cout << "h2d bandwidth allocation test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << std::endl;
		}
	}
}

void d2h_bandwidth_partition(unsigned char pci_bus)
{
	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}

	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int f1pkt = 1024;
	int test_time = 100;
	double total1, total2, total3 = 0;
	for(int f1pkt=1024; f1pkt <= 64*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=64; f2pkt <= 64*1024; f2pkt = f2pkt * 2)
		{
			total1 = 0;
			total2 = 0;
			total3 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				d2h(pci_bus, p, 1024*1024*1024, 0x0000000440000000, s+16, 2, f2pkt);

				bar[20] = 1;
				bar[70] = (uint32_t)((unsigned long)p>>32);
				bar[71] = (uint32_t)((unsigned long)p);
				bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[74] = f1size;
				bar[75] = f1pkt;
				bar[76] = 1 << 1;
				bar[77] = (uint32_t)((unsigned long)s>>32);
				bar[78] = (uint32_t)((unsigned long)s);

				uint32_t tag;
				writeConfig(0x1408/4, 0, pci_bus);
				tag = readConfig(0x140c/4, pci_bus);
				bar[80] = tag;

				usleep(100);

				start_time1 = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				bar[79] = 0;
				s[0] = 0;
				s[16] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;

				// flow2 test
				d2h(pci_bus, p, 1024*1024*1024, 0x0000000440000000, s+16, 1, f1pkt);

				bar[20] = 1;
				bar[70] = (uint32_t)((unsigned long)p>>32);
				bar[71] = (uint32_t)((unsigned long)p);
				bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[74] = f2size;
				bar[75] = f2pkt;
				bar[76] = 1 << 2;
				bar[77] = (uint32_t)((unsigned long)s>>32);
				bar[78] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time2 = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				bar[79] = 0;
				s[0] = 0;
				s[16] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;
			}
			std::cout << "d2h bandwidth allocation test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << std::endl;
		}
	}

}

void p2p_read_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus1, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}

	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int f1pkt = 1024;
	int test_time = 100;
	double total1, total2, total3 = 0;
	
	for(int f1pkt=1024; f1pkt <= 64*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=64; f2pkt <= 64*1024; f2pkt = f2pkt * 2)
		{
			total1 = 0;
			total2 = 0;
			total3 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				p2p_read(pci_bus1, 0, pci_bus2, 1024*1024*1024, 0x0000000440000000, s+16, 2, f2pkt);
				bar[500] = 0;
				long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
				bar[20] = 1;
				bar[50] = (uint32_t)((unsigned long)vaddr>>32);
				bar[51] = (uint32_t)((unsigned long)vaddr);
				bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[54] = f1size;
				bar[55] = f1pkt;
				bar[56] = 1 << 1;
				bar[57] = (uint32_t)((unsigned long)s>>32);
				bar[58] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time1 = Clock::now();
				bar[59] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				s[0] = 0;
				s[16] = 0;
				bar[59] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;

				// flow2 test
				p2p_read(pci_bus1, 0, pci_bus2, 1024*1024*1024, 0x0000000440000000, s+16, 1, f1pkt);
				bar[20] = 1;
				bar[50] = (uint32_t)((unsigned long)vaddr>>32);
				bar[51] = (uint32_t)((unsigned long)vaddr);
				bar[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[54] = f2size;
				bar[55] = f2pkt;
				bar[56] = 1 << 2;
				bar[57] = (uint32_t)((unsigned long)s>>32);
				bar[58] = (uint32_t)((unsigned long)s);
				usleep(100);

				start_time2 = Clock::now();
				bar[59] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				s[0] = 0;
				s[16] = 0;
				bar[59] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;

			}
			std::cout << "p2p read bandwidth partition test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << std::endl;
		}
	}
}

void p2p_write_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;

	unsigned int * p = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);
	unsigned int * s = (unsigned int *)hpalloc(pci_bus1, 262144);

	for(int i=0;i<262144/4;i++){
		s[i] = 0;
	}
	
	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int f1pkt = 1024;
	int test_time = 100;
	double total1, total2, total3 = 0;

	for(int f1pkt=1024; f1pkt <= 64*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=64; f2pkt <= 64*1024; f2pkt = f2pkt * 2)
		{
			total1 = 0;
			total2 = 0;
			total3 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				p2p_write(pci_bus1, 0, pci_bus2, 1024*1024*1024, 0x0000000440000000, s+16, 2, f2pkt);
				bar[500] = 0;
				long int vaddr = p2p_paddr_translation(pci_bus2, pci_bus1, (long int)getBridgeAddr(pci_bus2));
				bar[20] = 1;
				bar[70] = (uint32_t)((unsigned long)vaddr>>32);
				bar[71] = (uint32_t)((unsigned long)vaddr);
				bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[74] = f1size;
				bar[75] = f1pkt;
				bar[76] = 1 << 1;
				bar[77] = (uint32_t)((unsigned long)s>>32);
				bar[78] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time1 = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				s[0] = 0;
				s[16] = 0;
				bar[79] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;
				usleep(1000);

				// flow2 test
				p2p_write(pci_bus1, 0, pci_bus2, 1024*1024*1024, 0x0000000440000000, s+16, 1, f1pkt);
				bar[500] = 0;
				bar[20] = 1;
				bar[70] = (uint32_t)((unsigned long)vaddr>>32);
				bar[71] = (uint32_t)((unsigned long)vaddr);
				bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
				bar[74] = f2size;
				bar[75] = f2pkt;
				bar[76] = 1 << 2;
				bar[77] = (uint32_t)((unsigned long)s>>32);
				bar[78] = (uint32_t)((unsigned long)s);

				usleep(100);

				start_time2 = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s[16]!=-1)
					{__sync_synchronize();}
				s[0] = 0;
				s[16] = 0;
				bar[79] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;
				usleep(1000);
			}
			std::cout << "p2p write bandwidth partition test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << std::endl;
		}
	}
}

void h2d_d2h_concurrent(unsigned char pci_bus)
{
	int* lat = new int(10000);

	auto start_time = Clock::now();
	auto end_time = Clock::now();
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	volatile uint32_t * bar = (volatile uint32_t*)getLiteAddr(pci_bus);
	volatile uint32_t * sta = (volatile uint32_t*)getLiteAddr(pci_bus)+512;

	unsigned int * p;
	unsigned int * r;
	
	p = (unsigned int *)hpalloc(pci_bus, 1*1024*1024);
	for (int i=0; i<1024*1024*1024/4; i++)
	{
		p[i] = i;
	}
	unsigned int * s=(unsigned int *)hpalloc(pci_bus, 262144);
	for(int i=0;i<262144/4;i++){//initial
		s[i] = 0;
	}

	int test_size = 64;
	int loops = 100;

	for(int pkt_size=64; pkt_size <= 64*1024; pkt_size = pkt_size * 2)
	{

		double total = 0;
		for (int loop=0; loop<loops; loop++)
		{
			start_time = Clock::now();
			h2d(pci_bus, p, 512*1024*1024, 0x0000000040000000+0*0x4000000, s+16, 1, pkt_size);

			bar[20] = 1;
			bar[70] = (uint32_t)((unsigned long)r>>32);
			bar[71] = (uint32_t)((unsigned long)r);
			bar[72] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar[73] = (uint32_t)((unsigned long)0x0000000040000000);
			bar[74] = test_size;
			bar[75] = 4096;
			bar[76] = 1 << 2;
			bar[77] = (uint32_t)((unsigned long)s>>32);
			bar[78] = (uint32_t)((unsigned long)s);

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar[79] = 1;	//start
				while(s[0]!=-1)
					{__sync_synchronize();}
				end_lat = Clock::now();

				bar[79] = 0;
				s[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			while(s[16]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			usleep(1000);
			s[16]=0;
			total += (double(512*1024*1024)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 100 / loops;

		std::cout << "h2d & d2h concurrent test with h2d pkt_size = " << pkt_size << "  Bandwidth: " << total/loops << " GB/s"  << " d2h Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
}

void HF_HF_h2d_latency(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	unsigned int * s1=(unsigned int *)hpalloc(pci_bus1, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s1[i] = 0;
	}
	unsigned int * p1;
	p1 = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);

	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;
	unsigned int * s2=(unsigned int *)hpalloc(pci_bus2, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s2[i] = 0;
	}
	unsigned int * p2;
	p2 = (unsigned int *)hpalloc(pci_bus2, 1024*1024*1024);

	auto start_time = Clock::now();
	auto end_time = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int test_time = 100;

	int test_size = 64;
	int* lat = new int(10000);
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	printf("\nHF+HF tail latency test h2d: \n");
	for(int pkt_size=64; pkt_size <= 4*1024; pkt_size = pkt_size * 2)
	{
		double total = 0;
		for (int loop=0; loop<test_time; loop++)
		{
			start_time = Clock::now();
			
			h2d(pci_bus2, p2, 512*1024*1024, 0x0000000040000000+0*0x4000000, s2, 1, pkt_size);

			bar1[20] = 1;
			bar1[50] = (uint32_t)((unsigned long)p1>>32);
			bar1[51] = (uint32_t)((unsigned long)p1);
			bar1[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar1[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar1[54] = test_size;
			bar1[55] = pkt_size;
			bar1[56] = 1 << 0;
			bar1[57] = (uint32_t)((unsigned long)s1>>32);
			bar1[58] = (uint32_t)((unsigned long)s1);

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar1[59] = 1;	//start
				while(s1[0]!=-1)
				{__sync_synchronize();}
				end_lat = Clock::now();

				bar1[59] = 0;
				s1[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			while(s2[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			usleep(1000);
			s2[0]=0;
			total += (double(512*1024*1024)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 100 / test_time;

		std::cout << "HF+HF h2d latency test with 2 flows and pkt_size = " << pkt_size << "  Bandwidth: " << total/test_time << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
}

void HF_HF_h2d_bandwidth_partition(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	unsigned int * s1=(unsigned int *)hpalloc(pci_bus1, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s1[i] = 0;
	}
	unsigned int * p1;
	p1 = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);

	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;
	unsigned int * s2=(unsigned int *)hpalloc(pci_bus2, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s2[i] = 0;
	}
	unsigned int * p2;
	p2 = (unsigned int *)hpalloc(pci_bus2, 1024*1024*1024);

	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int test_time = 100;

	int test_size = 64;
	int* lat = new int(10000);
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	for(int f1pkt=64; f1pkt <= 4*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=64; f2pkt <= 4*1024; f2pkt = f2pkt * 2)
		{
			long total1 = 0;
			long total2 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				h2d(pci_bus2, p2, 512*1024*1024, 0x0000000440000000, s2, 2, f2pkt);
				bar1[20] = 1;
				bar1[50] = (uint32_t)((unsigned long)p1>>32);
				bar1[51] = (uint32_t)((unsigned long)p1);
				bar1[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar1[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar1[54] = f1size;
				bar1[55] = f1pkt;
				bar1[56] = 1 << 1;
				bar1[57] = (uint32_t)((unsigned long)s1>>32);
				bar1[58] = (uint32_t)((unsigned long)s1);

				usleep(100);

				start_time1 = Clock::now();
				bar1[59] = 1;	//start
				while(s1[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s2[0]!=-1)
					{__sync_synchronize();}
				s1[0] = 0;
				s2[0] = 0;
				bar1[59] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;

				// flow2 test
				h2d(pci_bus1, p1, 512*1024*1024, 0x0000000440000000, s1, 1, f1pkt);
				bar2[20] = 1;
				bar2[50] = (uint32_t)((unsigned long)p2>>32);
				bar2[51] = (uint32_t)((unsigned long)p2);
				bar2[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar2[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar2[54] = f2size;
				bar2[55] = f2pkt;
				bar2[56] = 1 << 2;
				bar2[57] = (uint32_t)((unsigned long)s2>>32);
				bar2[58] = (uint32_t)((unsigned long)s2);

				usleep(100);

				start_time2 = Clock::now();
				bar2[59] = 1;	//start
				while(s2[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s1[0]!=-1)
					{__sync_synchronize();}
				s1[0] = 0;
				s2[0] = 0;
				bar2[59] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;
			}
			std::cout << "HF+HF h2d bandwidth partition test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << std::endl;
		}
	}
}

void HF_FF_latency(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	unsigned int * s1=(unsigned int *)hpalloc(pci_bus1, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s1[i] = 0;
	}
	unsigned int * p1;
	p1 = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);

	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;
	unsigned int * s2=(unsigned int *)hpalloc(pci_bus2, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s2[i] = 0;
	}
	unsigned int * p2;
	p2 = (unsigned int *)hpalloc(pci_bus2, 1024*1024*1024);

	auto start_time = Clock::now();
	auto end_time = Clock::now();
	int f1size = 16*1024*1024;
	int f2size = 16*1024*1024;
	int test_time = 100;

	int test_size = 64;
	int* lat = new int(10000);
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	for(int pkt_size=64; pkt_size <= 4*1024; pkt_size = pkt_size * 2)
	{
		double total = 0;
		for (int loop=0; loop<test_time; loop++)
		{
			start_time = Clock::now();
			
			p2p_read(pci_bus1, 0, pci_bus2, 64*1024*1024, 0x0000000040000000+0*0x4000000, s1, 1, pkt_size);

			bar2[20] = 1;
			bar2[50] = (uint32_t)((unsigned long)p2>>32);
			bar2[51] = (uint32_t)((unsigned long)p2);
			bar2[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
			bar2[53] = (uint32_t)((unsigned long)0x0000000040000000);
			bar2[54] = test_size;
			bar2[55] = pkt_size;
			bar2[56] = 1 << 0;
			bar2[57] = (uint32_t)((unsigned long)s2>>32);
			bar2[58] = (uint32_t)((unsigned long)s2);

			usleep(100);

			for (int i=0;i<100;i++)
			{
				usleep(10);
				start_lat = Clock::now();
				bar2[59] = 1;	//start
				while(s2[0]!=-1)
				{__sync_synchronize();}
				end_lat = Clock::now();

				bar1[59] = 0;
				s2[0] = 0;
				lat[100*loop+i] = (end_lat - start_lat).count();
			}

			while(s1[0]!=-1)
				{__sync_synchronize();}
			end_time = Clock::now();

			usleep(1000);
			s1[0]=0;
			total += (double(512*1024*1024)+100*64)*1e9/(end_time - start_time).count()/1024/1024/1024;
		}

		sort(lat, lat+10000);
		double avg_lat = 0;
		for (int i=0; i<10000; i++)
		{
			avg_lat += lat[i];
		}
		avg_lat = avg_lat / 100 / test_time;

		std::cout << "HF+FF latency test with pkt_size = " << pkt_size << "  Bandwidth: " << total/test_time << " GB/s"  << "  Average Latency: " << avg_lat << "  99.9% Latency: " << lat[9990-1] << "  99% Latency: " << lat[9900-1] << "  90% Latency: " << lat[9000-1] << "  50% Latency: " << lat[5000-1] << std::endl;
	}
}

void HF_FF_bandwidth_partition_h2d(unsigned char pci_bus1, unsigned char pci_bus2)
{
	volatile uint32_t * bar1 = (volatile uint32_t*)getLiteAddr(pci_bus1);
	volatile uint32_t * sta1 = (volatile uint32_t*)getLiteAddr(pci_bus1)+512;
	unsigned int * s1=(unsigned int *)hpalloc(pci_bus1, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s1[i] = 0;
	}
	unsigned int * p1;
	p1 = (unsigned int *)hpalloc(pci_bus1, 1024*1024*1024);

	volatile uint32_t * bar2 = (volatile uint32_t*)getLiteAddr(pci_bus2);
	volatile uint32_t * sta2 = (volatile uint32_t*)getLiteAddr(pci_bus2)+512;
	unsigned int * s2=(unsigned int *)hpalloc(pci_bus2, 262144);
	for(int i=0;i<262144/4;i++){//initial
			s2[i] = 0;
	}
	unsigned int * p2;
	p2 = (unsigned int *)hpalloc(pci_bus2, 1024*1024*1024);

	auto start_time1 = Clock::now();
	auto start_time2 = Clock::now();
	auto start_time3 = Clock::now();
	auto end_time1 = Clock::now();
	auto end_time2 = Clock::now();
	auto end_time3 = Clock::now();
	int f1size = 1*1024*1024;
	int f2size = 1*1024*1024;
	int test_time = 10;
	double total1, total2, total3 = 0;
	int test_size = 64;
	int* lat = new int(10000);
	auto start_lat = Clock::now();
	auto end_lat = Clock::now();

	// HF+FF bandwidth partition h2m 
	printf("\nHF+FF bandwidth partition test h2m: \n");
	for(int f1pkt=4*1024; f1pkt <= 4*1024; f1pkt = f1pkt * 2)
	{
		for(int f2pkt=4*1024; f2pkt <= 4*1024; f2pkt = f2pkt * 2)
		{
			total1 = 0;
			total2 = 0;
			total3 = 0;
			for (int i=0; i<test_time; i++)
			{
				// flow1 test
				p2p_read(pci_bus2, 0, pci_bus1, 64*1024*1024, 0x0000000440000000, s2, 2, f2pkt);
				bar1[20] = 1;
				bar1[50] = (uint32_t)((unsigned long)p1>>32);
				bar1[51] = (uint32_t)((unsigned long)p1);
				bar1[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar1[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar1[54] = f1size;
				bar1[55] = f1pkt;
				bar1[56] = 1 << 1;
				bar1[57] = (uint32_t)((unsigned long)s1>>32);
				bar1[58] = (uint32_t)((unsigned long)s1);

				usleep(100);

				start_time1 = Clock::now();
				bar1[59] = 1;	//start
				while(s1[0]!=-1)
					{__sync_synchronize();}
				end_time1 = Clock::now();

				while(s2[0]!=-1)
					{__sync_synchronize();}
				s1[0] = 0;
				s2[0] = 0;
				bar1[59] = 0;
				total1 += double(f1size)*1e9/(end_time1 - start_time1).count()/1024/1024/1024;

				// flow2 test
				h2d(pci_bus1, p1, 64*1024*1024, 0x0000000440000000, s1, 1, f1pkt);
				long int vaddr = p2p_paddr_translation(pci_bus1, pci_bus2, (long int)getBridgeAddr(pci_bus1));
				bar2[20] = 1;
				bar2[50] = (uint32_t)((unsigned long)(vaddr)>>32);  //+(i%1024)*size/sizeof(unsigned long)
				bar2[51] = (uint32_t)((unsigned long)(vaddr));  //+(i%1024)*size/sizeof(unsigned long)
				bar2[52] = (uint32_t)((unsigned long)0x0000000040000000>>32);
				bar2[53] = (uint32_t)((unsigned long)0x0000000040000000);
				bar2[54] = f2size;
				bar2[55] = f2pkt;
				bar2[56] = 1 << 2;
				bar2[57] = (uint32_t)((unsigned long)s2>>32);
				bar2[58] = (uint32_t)((unsigned long)s2);

				usleep(100);

				start_time2 = Clock::now();
				bar2[59] = 1;	//start
				while(s2[0]!=-1)
					{__sync_synchronize();}
				end_time2 = Clock::now();

				while(s1[0]!=-1)
					{__sync_synchronize();}
				s1[0] = 0;
				s2[0] = 0;
				bar2[59] = 0;
				total2 += double(f2size)*1e9/(end_time2 - start_time2).count()/1024/1024/1024;
			}
			std::cout << "HF+FF test with flow 1 packet size: " << f1pkt << " and flow 2 packet size: = " << f2pkt << "  flow 1 Bandwidth: " << total1/test_time << " GB/s"  << "  flow 2 Bandwidth: " << total2/test_time << " GB/s" << "  flow 3 Bandwidth: " << total3/test_time << std::endl;
		}
	}
}

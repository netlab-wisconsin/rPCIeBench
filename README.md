# rPCIeBench
Open-source repository for “Understanding Routable PCIe Performance for Composable Infrastructures”, in USENIX Symposium on Networked Systems Design and Implementation (NSDI), 2024


# Table of contents
- [Table of contents](#table-of-contents)
- [Hardware Requirements](#Hardware-Requirements)
- [rPCIeBench Project and Bitstream](#rPCIeBench-Project-and-Bitstream)
	- [Generating Verilog Codes](#Generating-Verilog-Codes)
	- [Running Vivado Implementation](#Running-Vivado-Implementation)
- [Running rPCIeBench](#Running-rPCIeBench)
	- [Driver Installation](#driver-installation)
	- [QDMA Lib Installation](#qdma-lib-installation)
 	- [Huge Page Setup](#Huge-Page-Setup)
	- [Before Running](#Before-Running)
- [Benchmark Tests](#Benchmark-Tests)
- [Communication Primitives](#Communication-Primitives)

# Hardware Requirement
One development server to generate FPGA projects and write bitstream to FPGAs. We use Xilinx Vivado 2022.2 and Alveo U55C cards.

One operation server installing FPGAs cards to launch the Benchmark tests.

# rPCIeBench Project and Bitstream
On development server, we use chisel based on Scala to generate Verilog Codes. Then we use Xilinx Vivado 2022.2 with intergrated Xilinx hardware IP to generate the bitstream.

## Generating Verilog Codes
1. Please make sure you have already installed mill: https://mill-build.com/mill/Intro_to_Mill.html

2. Clone full rPCIe Project on development server.
```
$ git clone git@github.com:ThomasH1881/rPCIeBench.git
$ cd rPCIeBench_project
```

3. Change path in config.json to your Vivado Project directory.

```
{
	"CommSub":{
		"destIPRepoPath" : "/home/hwt22/VivadoProject/u55c_CommSub/u55c_CommSub.srcs/ip",
		"destSrcPath" : "/home/hwt22/VivadoProject/u55c_CommSub/u55c_CommSub.srcs/sources_1/new"
	}
}
```

Edit fileName in postElaborating.py (line 91) to where you want to put the Verilog Codes (Top.sv for example).
```
	fileName = "/home/hwt22/rPCIeBench_project/Verilog/Top.sv"
```

4. Generate Verilog Codes
```
$ python3 postElaborating.py CommSub Top -t -p
```

The output should be like:
```
hwt22@netlab-node6:~/rPCIeBench_project$ python3 postElaborating.py CommSub Top -t -p
No txt file to be deleted
Running mill CommSub Top
[34/76] common.compile 
[info] compiling 31 Scala sources to /home/hwt22/rPCIeBench_project/out/common/compile.dest/classes ...
[info] done compiling
[76/76] CommSub.run 
Generating a Top class
Elaborating design...

The tcl below is used to generate HBM IP:

create_ip -name hbm -vendor xilinx.com -library ip -version 1.0 -module_name HBMBlackBox
set_property -dict [list CONFIG.USER_HBM_DENSITY {8GB}  CONFIG.USER_HBM_STACK {2}  CONFIG.USER_MEMORY_DISPLAY {8192}  CONFIG.USER_SWITCH_ENABLE_01 {TRUE}  CONFIG.USER_HBM_CP_1 {6}  CONFIG.USER_HBM_RES_1 {10}  CONFIG.USER_HBM_LOCK_REF_DLY_1 {31}  CONFIG.USER_HBM_LOCK_FB_DLY_1 {31}  CONFIG.USER_HBM_FBDIV_1 {36}  CONFIG.USER_HBM_HEX_CP_RES_1 {0x0000A600}  CONFIG.USER_HBM_HEX_LOCK_FB_REF_DLY_1 {0x00001f1f}  CONFIG.USER_HBM_HEX_FBDIV_CLKOUTDIV_1 {0x00000902}  CONFIG.USER_MC0_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC1_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC2_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC3_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC4_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC5_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC6_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC7_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC8_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC9_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC10_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC11_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC12_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC13_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC14_TRAFFIC_OPTION {Linear}  CONFIG.USER_MC15_TRAFFIC_OPTION {Linear}  CONFIG.USER_CLK_SEL_LIST1 {AXI_23_ACLK}  CONFIG.USER_MC_ENABLE_08 {TRUE}  CONFIG.USER_MC_ENABLE_09 {TRUE}  CONFIG.USER_MC_ENABLE_10 {TRUE}  CONFIG.USER_MC_ENABLE_11 {TRUE}  CONFIG.USER_MC_ENABLE_12 {TRUE}  CONFIG.USER_MC_ENABLE_13 {TRUE}  CONFIG.USER_MC_ENABLE_14 {TRUE}  CONFIG.USER_MC_ENABLE_15 {TRUE}  CONFIG.USER_MC_ENABLE_APB_01 {TRUE}  CONFIG.USER_MC0_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC1_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC2_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC3_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC4_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC5_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC6_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC7_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC8_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC9_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC10_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC11_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC12_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC13_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC14_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC15_LOOKAHEAD_SBRF {true}  CONFIG.USER_MC0_EN_SBREF {true}  CONFIG.USER_MC1_EN_SBREF {true}  CONFIG.USER_MC2_EN_SBREF {true}  CONFIG.USER_MC3_EN_SBREF {true}  CONFIG.USER_MC4_EN_SBREF {true}  CONFIG.USER_MC5_EN_SBREF {true}  CONFIG.USER_MC6_EN_SBREF {true}  CONFIG.USER_MC7_EN_SBREF {true}  CONFIG.USER_MC8_EN_SBREF {true}  CONFIG.USER_MC9_EN_SBREF {true}  CONFIG.USER_MC10_EN_SBREF {true}  CONFIG.USER_MC11_EN_SBREF {true}  CONFIG.USER_MC12_EN_SBREF {true}  CONFIG.USER_MC13_EN_SBREF {true}  CONFIG.USER_MC14_EN_SBREF {true}  CONFIG.USER_MC15_EN_SBREF {true}  CONFIG.USER_PHY_ENABLE_08 {TRUE}  CONFIG.USER_PHY_ENABLE_09 {TRUE}  CONFIG.USER_PHY_ENABLE_10 {TRUE}  CONFIG.USER_PHY_ENABLE_11 {TRUE}  CONFIG.USER_PHY_ENABLE_12 {TRUE}  CONFIG.USER_PHY_ENABLE_13 {TRUE}  CONFIG.USER_PHY_ENABLE_14 {TRUE}  CONFIG.USER_PHY_ENABLE_15 {TRUE}] [get_ips HBMBlackBox]
update_compile_order -fileset sources_1


create_ip -name qdma -vendor xilinx.com -library ip -version 4.0 -module_name QDMABlackBox
set_property -dict [list CONFIG.Component_Name {QDMABlackBox} CONFIG.axist_bypass_en {true} CONFIG.pcie_extended_tag {false} CONFIG.dsc_byp_mode {Descriptor_bypass_and_internal} CONFIG.cfg_mgmt_if {false} CONFIG.testname {st} CONFIG.pf0_bar4_enabled_qdma {true} CONFIG.pf0_bar4_64bit_qdma {true} CONFIG.pf0_bar4_scale_qdma {Gigabytes} CONFIG.pf0_bar4_size_qdma {1} CONFIG.pf1_bar4_enabled_qdma {true} CONFIG.pf1_bar4_64bit_qdma {true} CONFIG.pf1_bar4_scale_qdma {Gigabytes} CONFIG.pf1_bar4_size_qdma {1} CONFIG.pf2_bar4_enabled_qdma {true} CONFIG.pf2_bar4_64bit_qdma {true} CONFIG.pf2_bar4_scale_qdma {Gigabytes} CONFIG.pf2_bar4_size_qdma {1} CONFIG.pf3_bar4_enabled_qdma {true} CONFIG.pf3_bar4_64bit_qdma {true} CONFIG.pf3_bar4_scale_qdma {Gigabytes} CONFIG.pf3_bar4_size_qdma {1} CONFIG.dma_intf_sel_qdma {AXI_Stream_with_Completion} CONFIG.en_axi_mm_qdma {false}] [get_ips QDMABlackBox]
update_compile_order -fileset sources_1

Done elaborating.


/home/hwt22/rPCIeBench_project/Verilog/Top.sv
Done moving file
```

The generated Verilog file Top.sv is also provided in this repository. You may directly use it.

## Running Vivado Implementation
1. Create a new Vivado Project.

Add source file from where the Verilog Codes are located. ("/home/hwt22/rPCIeBench_project/Verilog/Top.sv" in this example)

2. Create several IPs which have already been defined in the project. (You can also customize them in the chisel file to your needs.)

create an AXI Clock Converter which must be named as AXIClockConverterBlackBox

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/913e4295-c598-4bb6-bfb8-3afd7b182063)

create an AXI Data Width Converter which must be named as AXIDataWidthConverterBlackBox

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/647b2947-aba6-4a30-8fc7-f9c02e49e0bc)

create an HBM which must be named as HBMBlackBox

We use both two stacks, with Memory Frequency to be 900 MHz, APB Clock Frequency to be 100MHz and AXI Clock Frequency to be 450 MHz for both of the stacks

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/d9b7d0fe-8f7f-418f-9908-1d5e2cdde0a1)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/8e648488-b65a-43f5-baa6-861af7ecd7e3)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/6a68944e-b469-4429-903d-7f013b3ece74)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/fa462d4e-aba7-498f-ba1b-aa66124c0c77)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/27da2d5a-9a60-4b4e-aebe-233abb7e6409)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/d2f27cfc-9fff-4584-99b2-f379046d7657)

create a QDMA which must be named as QDMABlackBox

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/5d4f3ae6-3755-4e4f-b0ab-34dbace9df3e)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/a3f4eab7-7fae-494f-a561-db968f8e108a)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/9ccc3233-767f-42ca-90fd-2766502a0e17)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/d86f815d-03d1-465e-821e-7757507aabef)

![image](https://github.com/netlab-wisconsin/rPCIeBench/assets/52404519/62c958d2-7ab4-411f-a3de-764bd54fe4c5)

If you have any questions regarding the IPs, feel free to contact me.

3. Generate Bitstream, wait for synthesis and implementation to finish.

The timing of this project is quite tight as we use 450 MHz High Band Width Memory. Sometimes the Worst Negative Slack will exceed a little but we observe it always works normally. Or you can try to optimize the timing with a better timing implementation algorithm in Vivado setting.

If you also have a U55C card, you may directly use the Top.bit file provided in this repository.

# Running rPCIeBench
For operation server, Ubuntu 20.04 LTS (Kernel 5.3.0.release.98.393a144ca0f7) has been tested.

## Driver Installation

1. prepare 
Clone rPCIeBench to your operation and install dependency.
```
$ git clone git@github.com:ThomasH1881/rPCIeBench.git
$ cd rPCIeBench
$ sudo apt-get install libaio1 libaio-dev
```

3. compile
```
$ cd qdma_driver
$ make
$ make apps
```

4. install apps and header files
```
$ sudo make install-apps
```

## QDMA Lib Installation
```
$ cd ~/lib_qdma
$ mkdir build
$ cd build
$ cmake ..
$ sudo make install 
```

## Huge Page Setup
1. Create a user group for hugepages, and retrieve its GID (in this example, 1001).
```
$ groupadd hugetlbfs

$ getent group hugetlbfs

$ adduser @yourusername hugetlbfs
```

2. Edit `/etc/sysctl.conf` and add this text to specify the number of pages you want to reserve
```
# Allocate 32768*2MiB for HugePageTables
vm.nr_hugepages = 32768

# Members of group hugetlbfs(1001) can allocate "huge" shared memory segments
vm.hugetlb_shm_group = 1001
```
3. Create a mount point for the file system
```
$ mkdir /media/huge
```

4. Add this line in `/etc/fstab` (The mode 1770 allows anyone in the group to create files but not unlink or rename each other's files.)
```
# hugetlbfs
hugetlbfs /media/huge hugetlbfs mode=1770,gid=1001 0 0
```

5. Reboot. When booting, disable the SRIOV in BIOS.

## Before Running

1. export qdma lib directory (change the path to where you put the rPCIeBench)

```
$ export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/netlab/rPCIeBench/lib_qdma/build
```

2. install kernel mod

```
$ cd rPCIeBench/qdma_driver
$ sudo insmod src/qdma-pf.ko
```

3. build benchmarking application

```
$ cd ../rPCIeRun/build
$ make
```

4. set DMA engine

Find the PCIe bus ID of your FPGA device (af:00.0 in this example), you can use lspci or other tools.

Then, use dmactl.sh script
```
../dmactl.sh af
```

5. Run the benchmarking program

```
./CommSub
```


# Benchmark Tests
Our rPCIeBench contains both communication primitives and benchmark test funtions.

Communication primitives are wrapped functions for users to directly invoke. However, these funtions have additional costs such as writing arguments to FPGA. For benchmarking, we should not use them.

Here we provide seperate funtions for benchmarking. 

For basic performance, we provide:

1. MMIO_read latency test
2. host-to-device and device-to-host DMA latency test
3. host-to-device and device-to-host bandwidth test
4. FPGA peer-to-peer read/write latency test (realized by DMA and memory-mapping FPGA's HBM)
5. FPGA peer-to-peer read/write bandwidth test

Combining above operations, we further provide:

6. latency-throughput performance for h2d/d2h/FPGA read/FPGA write
7. bandwidth partition on one path for h2d/d2h/FPGA read/FPGA write

To further benchmark multiple flows in topology, we can launch tests:

8. Current flows in reverse direction on one path (h2d+d2h / FPGA read+write)
9. Host-FPGA flow + Host-FPGA flow
10. Host-FPGA flow + FPGA-FPGA flow
11. FPGA-FPGA flow + FPGA-FPGA flow
    
We provide several examples, you can tailor your own settings.

Please refer to [./rPCIeRun/README.md](rPCIeBench/README.md) for more details.


# Communication Primitives

We built six basic communication primitives:
```c
void mmio_write(volatile __m512i* bridge, __m512i* data, uint32_t length);
void mmio_read(volatile __m512i* bridge, __m512i* data, uint32_t length);
void p2p_read(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 32768);
void p2p_write(unsigned char pci_bus1, unsigned long bias, unsigned char pci_bus2, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 4096);
void h2d(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 32768);
void d2h(unsigned char pci_bus, unsigned int * p, uint32_t length, unsigned long addr, unsigned int * s, uint32_t q_num, uint32_t pkt_size = 4096);
```
MMIO_write/read performs an MMIO to PCIe BAR address, which directly connects to HBM (may also be modified to BRAM) on FPGA.

p2p_read/write issues an DMA read/write from one FPGA's HBM to another FPGA's HBM.

h2d/d2h issues host-to-device or device-to-host DMA. Data moves between host memory and FPGA's HBM.

Please refer to [./rPCIeRun/README.md](rPCIeBench/README.md) for more details.




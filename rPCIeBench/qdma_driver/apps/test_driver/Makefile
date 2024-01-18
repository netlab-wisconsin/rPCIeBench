obj-m += hello.o
CURRENT_PATH := $(shell pwd)
LINUX_KERNEL := $(shell uname -r)
LINUX_KERNEL_PATH := /usr/src/linux-headers-$(LINUX_KERNEL)

all: hello clean_o
	

hello:
	make -C $(LINUX_KERNEL_PATH) M=$(CURRENT_PATH) modules
clean:
	make -C $(LINUX_KERNEL_PATH) M=$(CURRENT_PATH) clean
install:
	insmod hello.ko
unistall:
	rmmod hello

clean_o:
	cd /home/amax2/cj/dma_ip_drivers-master/QDMA/linux-kernel/apps/test_driver
	@rm -f *.o .*.mk .*.*.cmd *.order *.symvers hello.mod.c -r .tmp_versions
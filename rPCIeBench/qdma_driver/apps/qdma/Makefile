SHELL = /bin/bash

CFLAGS += -g
CFLAGS += -I. -I../include 
CFLAGS += $(EXTRA_FLAGS)

QDMA-CTL = qdma-ctl
QDMA-CTL_OBJS := $(patsubst %.c,%.o,$(wildcard *.c))

ifneq ($(CROSS_COMPILE_FLAG),)
	CC=$(CROSS_COMPILE_FLAG)gcc
endif
 
all: clean qdma-ctl clean_o

qdma-ctl: $(QDMA-CTL_OBJS)
	$(CC) -pthread -lrt $^ -o $(QDMA-CTL) -laio -D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE -D_LARGE_FILE_SOURCE
	
%.o: %.c
	$(CC) $(CFLAGS) -c -std=c99 -o $@ $< -D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE -D_LARGE_FILE_SOURCE -D_AIO_AIX_SOURCE

clean:
	@rm -f *.o */*.o  $(QDMA-CTL)

clean_o:
	@rm -f *.o */*.o 

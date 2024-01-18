#include <linux/ioctl.h>

#define rc4ml_major 90
#define rc4ml_minor 0

struct huge_mem{
	unsigned long vaddr;
	unsigned long size;
};

struct huge_mapping{
	unsigned long nhpages;
	unsigned long* phy_addr;
};

struct huge_table_t{
	struct page **huge_pages;
	unsigned long size;
	unsigned long nhpages;
	unsigned long vaddr_start;
};

#define QUERY_GET_VALUE _IOR('q', 1, int *)
#define QUERY_CLEAR_VALUE _IO('q', 2)
#define QUERY_SET_VALUE _IOW('q', 3, int *)

#define HUGE_MAPPING_SET _IOW('q', 4, struct huge_buffer*)
#define HUGE_MAPPING_GET _IOR('q', 5, struct huge_mapping*)

int rc4ml_init(void);
void rc4ml_cleanup(void);
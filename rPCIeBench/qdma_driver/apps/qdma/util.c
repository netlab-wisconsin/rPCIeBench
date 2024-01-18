#include "util.h"

int xnl_proc_cmd(struct cmd_info *cmd){
	cmd->log_msg_dump = xnl_dump_response;
	if (xnl_proc_fn[cmd->op])
		return xnl_proc_fn[cmd->op](cmd);

	return -EOPNOTSUPP;
}


static void xnl_dump_response(const char *resp){
	printf("%s", resp);
}

uint32_t xnl_dump_cmd_resp(struct cmd_info *cmd)
{

	switch(cmd->op) {
        case XNL_CMD_DEV_CAP:
        	dump_dev_cap(cmd);
		break;
        case XNL_CMD_DEV_INFO:
        	dump_dev_info(cmd);
		break;
        case XNL_CMD_DEV_STAT:
        	dump_dev_stat(cmd);
		break;
        case XNL_CMD_REG_RD:
		printf("qdma%s%05x, %02x:%02x.%02x, bar#%u, 0x%x = 0x%x.\n",
				cmd->vf ? "vf" :"",
				cmd->if_bdf, cmd->resp.dev_info.pci_bus,
				cmd->resp.dev_info.pci_dev,
				cmd->resp.dev_info.dev_func, cmd->req.reg.bar,
				cmd->req.reg.reg, cmd->req.reg.val);
		return cmd->req.reg.val;
		break;
        case XNL_CMD_REG_WRT:
		printf("qdma%s%05x, %02x:%02x.%02x, bar#%u, reg 0x%x, read back 0x%x.\n",
			   cmd->vf ? "vf" :"",
			   cmd->if_bdf, cmd->resp.dev_info.pci_bus,
			   cmd->resp.dev_info.pci_dev,
			   cmd->resp.dev_info.dev_func, cmd->req.reg.bar,
			   cmd->req.reg.reg, cmd->req.reg.val);
		break;
	case XNL_CMD_GLOBAL_CSR:
			dump_dev_global_csr(cmd);
		break;
	case XNL_CMD_REG_INFO_READ:
		break;
	default:
		break;
	}
}

static void dump_dev_global_csr(struct cmd_info *cmd){
	printf("Global Ring Sizes:");
	for ( int i=0; i < QDMA_GLOBAL_CSR_ARRAY_SZ; i++)
		printf("%d ",cmd->resp.csr.ring_sz[i]);
	printf("\nC2H Timer Counters:");
	for ( int i=0; i < QDMA_GLOBAL_CSR_ARRAY_SZ; i++)
		printf("%d ",cmd->resp.csr.c2h_timer_cnt[i]);
	printf("\nC2H Counter Thresholds:");
	for ( int i=0; i < QDMA_GLOBAL_CSR_ARRAY_SZ; i++)
		printf("%d ",cmd->resp.csr.c2h_cnt_th[i]);
	printf("\nC2H Buf Sizes:");
	for ( int i=0; i < QDMA_GLOBAL_CSR_ARRAY_SZ; i++)
		printf("%d ",cmd->resp.csr.c2h_cnt_th[i]);
	printf("\nWriteback Interval:%d\n",cmd->resp.csr.wb_intvl);
}

static void dump_dev_cap(struct cmd_info *cmd)
{
	printf("%s", cmd->resp.cap.version_str);
	printf("=============Hardware Capabilities============\n\n");
	printf("Number of PFs supported                : %u\n", cmd->resp.cap.num_pfs);
	printf("Total number of queues supported       : %u\n", cmd->resp.cap.num_qs);
	printf("MM channels                            : %u\n", cmd->resp.cap.mm_channel_max);
	printf("FLR Present                            : %s\n", cmd->resp.cap.flr_present ? "yes":"no");
	printf("ST enabled                             : %s\n",	cmd->resp.cap.st_en ? "yes":"no");
	printf("MM enabled                             : %s\n", cmd->resp.cap.mm_en ? "yes":"no");
	printf("Mailbox enabled                        : %s\n", cmd->resp.cap.mailbox_en ? "yes":"no");
	printf("MM completion enabled                  : %s\n", cmd->resp.cap.mm_cmpt_en ? "yes":"no");
	printf("Debug Mode enabled                     : %s\n", cmd->resp.cap.debug_mode ? "yes":"no");

	if (cmd->resp.cap.desc_eng_mode < sizeof(desc_engine_mode) / sizeof(desc_engine_mode[0])) {
		printf("Desc Engine Mode                       : %s\n",
			   desc_engine_mode[cmd->resp.cap.desc_eng_mode]);
	}else {
		printf("Desc Engine Mode                       : INVALID\n");
	}
}

static void dump_dev_info(struct cmd_info *cmd)
{
	printf("=============Device Information============\n");
	printf("PCI                                    : %02x:%02x.%01x\n",
	       cmd->resp.dev_info.pci_bus,
	       cmd->resp.dev_info.pci_dev,
	       cmd->resp.dev_info.dev_func);
	printf("HW q base                              : %u\n", cmd->resp.dev_info.qbase);
	printf("Max queues                             : %u\n",	cmd->resp.dev_info.qmax);
	printf("Config bar                             : %u\n", cmd->resp.dev_info.config_bar);
	printf("AXI Master Lite bar                    : %u\n", cmd->resp.dev_info.user_bar);
}

static void dump_dev_stat(struct cmd_info *cmd)
{
	unsigned long long mmh2c_pkts;
	unsigned long long mmc2h_pkts;
	unsigned long long sth2c_pkts;
	unsigned long long stc2h_pkts;
	unsigned long long min_ping_pong_lat = 0;
	unsigned long long max_ping_pong_lat = 0;
	unsigned long long avg_ping_pong_lat = 0;

	mmh2c_pkts = cmd->resp.dev_stat.mm_h2c_pkts;
	mmc2h_pkts = cmd->resp.dev_stat.mm_c2h_pkts;
	sth2c_pkts = cmd->resp.dev_stat.st_h2c_pkts;
	stc2h_pkts = cmd->resp.dev_stat.st_c2h_pkts;
	min_ping_pong_lat = cmd->resp.dev_stat.ping_pong_lat_min;
	max_ping_pong_lat = cmd->resp.dev_stat.ping_pong_lat_max;
	avg_ping_pong_lat = cmd->resp.dev_stat.ping_pong_lat_avg;

	printf("qdma%s%05x:statistics\n", cmd->vf ? "vf" : "", cmd->if_bdf);
	printf("Total MM H2C packets processed = %llu\n", mmh2c_pkts);
	printf("Total MM C2H packets processed = %llu\n", mmc2h_pkts);
	printf("Total ST H2C packets processed = %llu\n", sth2c_pkts);
	printf("Total ST C2H packets processed = %llu\n", stc2h_pkts);
	printf("Min Ping Pong Latency = %llu\n", min_ping_pong_lat);
	printf("Max Ping Pong Latency = %llu\n", max_ping_pong_lat);
	printf("Avg Ping Pong Latency = %llu\n", avg_ping_pong_lat);
}

void dump_throughput_result(uint64_t size, float result) {
	printf("size=%lu ", size);
	if (((long long)(result)/GB_DIV)) {
		printf("Average BW = %f GB/sec\n", ((double)result/GB_DIV));
	} else if (((long long)(result)/MB_DIV)) {
		printf("Average BW = %f MB/sec\n", ((double)result/MB_DIV));
	} else if (((long long)(result)/KB_DIV)) {
		printf("Average BW = %f KB/sec\n", ((double)result/KB_DIV));
	} else
		printf("Average BW = %f Bytes/sec\n", ((double)result));
}

static int timespec_check(struct timespec *t){
	if ((t->tv_nsec < 0) || (t->tv_nsec >= 1000000000))
		return -1;
	return 0;

}

void timespec_sub(struct timespec *t1, struct timespec *t2){
	if (timespec_check(t1) < 0) {
		fprintf(stderr, "invalid time #1: %lld.%.9ld.\n",
			(long long)t1->tv_sec, t1->tv_nsec);
		return;
	}
	if (timespec_check(t2) < 0) {
		fprintf(stderr, "invalid time #2: %lld.%.9ld.\n",
			(long long)t2->tv_sec, t2->tv_nsec);
		return;
	}
	t1->tv_sec -= t2->tv_sec;
	t1->tv_nsec -= t2->tv_nsec;
	if (t1->tv_nsec >= 1000000000) {
		t1->tv_sec++;
		t1->tv_nsec -= 1000000000;
	} else if (t1->tv_nsec < 0) {
		t1->tv_sec--;
		t1->tv_nsec += 1000000000;
	}
}
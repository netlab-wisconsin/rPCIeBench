#include <string.h>
#include <stdio.h>
#include <err.h>
#include <errno.h>
#include<unistd.h>
#include<stdlib.h>

#include <fcntl.h>
#include <time.h>

#include "main.h"
#include "cmd.h"
#include "util.h"

int cmd_type=0;
int transfer_data=0;
void get_opt(int argc, char *argv[]){
	int o;  // getopt() 的返回值
    const char *optstring = "at:d"; // 设置短参数类型及是否需要参数  后面一个冒号代表必选参数 即-c后面必须接参数，有无空格都行， 两个冒号可选参数，必须没空格

     while ((o = getopt(argc, argv, optstring)) != -1) {
        switch (o) {
			case 'd':
				transfer_data=1;
				break;
            case 't':
				cmd_type=atoi(optarg);
                break;
            case '?':
                printf("error optopt: %c\n", optopt);
                printf("error opterr: %d\n", opterr);
                break;
        }
    }
}

int main(int argc, char *argv[]){
	get_opt(argc,argv);
	if(transfer_data){
		if(cmd_type==0){
			printf("transfer data host -> fpga\n");
		}else{
			printf("transfer data host <- fpga\n");
		}
		char * dev = "/dev/qdma1a000-ST-0";
		char *infname = "../../scripts/datafile_16bit_pattern.bin";
		uint64_t size = 1024;
		uint64_t axi_addr = 0;
		uint64_t offset = 0;
		uint64_t count = 1;//default 1
		struct timespec ts_start, ts_end;
		double total_time = 0;
		double result;
		double avg_time = 0;

		int fpga_fd = open(dev, O_RDWR);
		if (fpga_fd < 0){
			fprintf(stderr, "unable to open device %s, %d.\n",dev, fpga_fd);
			perror("open device");
			return -EINVAL;
		}

		int infile_fd = open(infname, O_RDONLY);
		if (infile_fd < 0) {
			fprintf(stderr, "unable to open input file %s, %d.\n",
				infname, infile_fd);
			perror("open input file");
			return -EINVAL;
		}

		char *allocated = NULL;
		char *buffer = NULL;
		posix_memalign((void **)&allocated, 4096 /*alignment */ , size + 4096);
		if (!allocated) {
			fprintf(stderr, "OOM %lu.\n", size + 4096);
			return -ENOMEM;
		}
		buffer = allocated + offset;
		fprintf(stdout, "host buffer 0x%lx = %p\n",size + 4096, buffer); 
		
		for (int i = 0; i < count; i++){
			clock_gettime(CLOCK_MONOTONIC, &ts_start);
			ssize_t rc;
			rc = write_from_buffer(dev, fpga_fd, buffer, size, axi_addr);
			if (rc < 0){
				printf("error write from buffer, %ld\n",rc);
				return rc;
			}
			clock_gettime(CLOCK_MONOTONIC, &ts_end);
			timespec_sub(&ts_end, &ts_start);
			total_time += (ts_end.tv_sec + ((double)ts_end.tv_nsec/NSEC_DIV));
		}
		avg_time = (double)total_time/(double)count;
		result = ((double)size)/avg_time;
		dump_throughput_result(size, result);
	}




	if(transfer_data){
		return 0;
	}
	struct cmd_info cmd;
	unsigned int f_arg_set = 0;
	memset(&cmd,0,sizeof(cmd));
	{//only dev list don't need
		cmd.vf = 0;
		cmd.if_bdf = 0x1a000;
	}
	
	switch (cmd_type)
	{
	case 0:
		cmd.op = XNL_CMD_DEV_LIST;//dma-ctl dev list
		break;
	case 1:
		cmd.op = XNL_CMD_DEV_INFO;//dma-ctl qdma1a000
		break;
	case 2:
		cmd.op = XNL_CMD_DEV_STAT;//dma-ctl qdma1a000 stat
		break;
	case 3:
		cmd.op = XNL_CMD_DEV_STAT_CLEAR;//dma-ctl qdma1a000 stat clear
		break;
	case 4:
		cmd.op = XNL_CMD_DEV_CAP;//dma-ctl qdma1a000 cap
		break;
	case 5:
		cmd.op = XNL_CMD_Q_LIST;//dma-ctl qdma1a000 q list
		break;
	case 6:
		cmd.op = XNL_CMD_Q_ADD;//dma-ctl qdma1a000 q add idx 0 dir bi
		cmd.req.qparm.idx = 0;
		cmd.req.qparm.num_q = 1;
		cmd.req.qparm.flags |= XNL_F_QMODE_ST;
		cmd.req.qparm.flags |= XNL_F_QDIR_BOTH;//XNL_F_QDIR_BOTH   XNL_F_Q_CMPL
		f_arg_set |= 1 << QPARM_IDX;
		f_arg_set |= 1 << QPARM_MODE;
		f_arg_set |= 1 << QPARM_DIR;
		cmd.req.qparm.sflags = f_arg_set;
		break;
	case 7:
		cmd.op = XNL_CMD_Q_START;  //dma-ctl qdma1a000 q start idx 0 dir bi desc_bypass_en
		cmd.req.qparm.fetch_credit = Q_ENABLE_C2H_FETCH_CREDIT;
		cmd.req.qparm.flags |= (XNL_F_CMPL_STATUS_EN | XNL_F_CMPL_STATUS_ACC_EN |
				XNL_F_CMPL_STATUS_PEND_CHK | XNL_F_CMPL_STATUS_DESC_EN |
				XNL_F_FETCH_CREDIT);
		cmd.req.qparm.idx = 0;
		cmd.req.qparm.num_q = 1;
		cmd.req.qparm.flags |= XNL_F_QDIR_BOTH;
		cmd.req.qparm.flags |= XNL_F_DESC_BYPASS_EN; 
		cmd.req.qparm.qrngsz_idx = 9;//todo why 2048
		//c2h st need set cmpl
		//cmd.req.qparm.cmpt_entry_size = XNL_ST_C2H_CMPT_DESC_SIZE_8B;

		f_arg_set |= 1 << QPARM_IDX;
		f_arg_set |= 1 << QPARM_DIR;
		f_arg_set |= 1 << QPARM_RNGSZ_IDX;
		f_arg_set |= 1 << QPARM_CMPTSZ;
		cmd.req.qparm.sflags = f_arg_set;
		break;
	case 8:
		cmd.op = XNL_CMD_Q_STOP;  //dma-ctl qdma1a000 q stop idx 0 dir bi
		cmd.req.qparm.idx = 0;
		cmd.req.qparm.num_q = 1;
		cmd.req.qparm.flags |= XNL_F_QDIR_BOTH;

		f_arg_set |= 1 << QPARM_IDX;
		f_arg_set |= 1 << QPARM_DIR;
		cmd.req.qparm.sflags = f_arg_set;
		break;
	case 9:
		cmd.op = XNL_CMD_Q_DEL;  //dma-ctl qdma1a000 q del idx 0 dir bi
		cmd.req.qparm.idx = 0;
		cmd.req.qparm.num_q = 1;
		cmd.req.qparm.flags |= XNL_F_QDIR_BOTH;

		f_arg_set |= 1 << QPARM_IDX;
		f_arg_set |= 1 << QPARM_DIR;
		cmd.req.qparm.sflags = f_arg_set;
		break;
	case 10:
		cmd.op = XNL_CMD_REG_RD; //dma-ctl qdma1a000 reg read bar 2 0x90 
		cmd.req.reg.bar = 2;
		cmd.req.reg.reg = 4;
		cmd.req.reg.sflags |= CMD_REG_F_BAR_SET;
		cmd.req.reg.sflags |= CMD_REG_F_REG_SET;
		break;
	case 11:
		cmd.op = XNL_CMD_REG_WRT; //dma-ctl qdma1a000 reg write bar 2 0x90 2
		cmd.req.reg.bar = 2;
		cmd.req.reg.reg = 144;
		cmd.req.reg.val = 1;
		cmd.req.reg.sflags |= CMD_REG_F_BAR_SET;
		cmd.req.reg.sflags |= CMD_REG_F_REG_SET;
		cmd.req.reg.sflags |= CMD_REG_F_VAL_SET;
		break;
	
	default:
		printf("unknown cmd\n");
		break;
	}
	int rv = 0;
	rv = xnl_proc_cmd(&cmd);
	if (rv < 0)
		return rv;
	int v = xnl_dump_cmd_resp(&cmd);
	// test reg read lat
	cmd.req.reg.reg = v;
	rv = xnl_proc_cmd(&cmd);
	xnl_dump_cmd_resp(&cmd);
	return 0;
}


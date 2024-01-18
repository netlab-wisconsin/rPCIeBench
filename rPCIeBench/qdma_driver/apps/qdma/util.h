#ifndef _UTIL_H_
#define _UTIL_H_

#include <string.h>
#include <stdio.h>
#include <err.h>
#include <errno.h>
#include<unistd.h>
#include<stdlib.h>
#include "cmd.h"

#define NSEC_DIV 1000000000
#define GB_DIV 1000000000
#define MB_DIV 1000000
#define KB_DIV 1000

static int (*xnl_proc_fn[XNL_CMD_MAX])(struct cmd_info *cmd) = {
	qdma_dev_list_dump,      /* XNL_CMD_DEV_LIST */
	qdma_dev_info,           /* XNL_CMD_DEV_INFO */
	qdma_dev_stat,           /* XNL_CMD_DEV_STAT */
	qdma_dev_stat_clear,     /* XNL_CMD_DEV_STAT_CLEAR */
	qdma_reg_dump,           /* XNL_CMD_REG_DUMP */
	qdma_reg_read,           /* XNL_CMD_REG_RD */
	qdma_reg_write,          /* XNL_CMD_REG_WRT */
	qdma_reg_info_read,      /* XNL_CMD_REG_INFO_READ */
	qdma_dev_q_list_dump,    /* XNL_CMD_Q_LIST */
	qdma_q_add,              /* XNL_CMD_Q_ADD */
	qdma_q_start,            /* XNL_CMD_Q_START */
	qdma_q_stop,             /* XNL_CMD_Q_STOP */
	qdma_q_del,              /* XNL_CMD_Q_DEL */
	qdma_q_dump,             /* XNL_CMD_Q_DUMP */
	qdma_q_desc_dump,        /* XNL_CMD_Q_DESC */
	qdma_q_desc_dump,        /* XNL_CMD_Q_CMPT */
	NULL,                    /* XNL_CMD_Q_RX_PKT */
	qdma_q_cmpt_read,        /* XNL_CMD_Q_CMPT_READ */
	qdma_dev_intr_ring_dump, /* XNL_CMD_INTR_RING_DUMP */
	NULL,                    /* XNL_CMD_Q_UDD */
	qdma_dev_get_global_csr, /* XNL_CMD_GLOBAL_CSR */
	qdma_dev_cap,            /* XNL_CMD_DEV_CAP */
	NULL                     /* XNL_CMD_GET_Q_STATE */
};

static const char *desc_engine_mode[] = {
	"Internal and Bypass mode",
	"Bypass only mode",
	"Inernal only mode"
};

int xnl_proc_cmd(struct cmd_info *cmd);
static void xnl_dump_response(const char *resp);
static void dump_dev_global_csr(struct cmd_info *cmd);
uint32_t xnl_dump_cmd_resp(struct cmd_info *cmd);

static void dump_dev_cap(struct cmd_info *cmd);
static void dump_dev_info(struct cmd_info *cmd);
static void dump_dev_stat(struct cmd_info *cmd);
void dump_throughput_result(uint64_t size, float result);
void timespec_sub(struct timespec *t1, struct timespec *t2);

#endif
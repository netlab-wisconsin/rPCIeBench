#ifndef _CMD_H_
#define _CMD_H_

#include <sys/socket.h>
#include <linux/genetlink.h>
#include <unistd.h>
#include <bits/types.h>

typedef __uint64_t uint64_t;
typedef __uint32_t uint32_t;
typedef __uint8_t uint8_t;

#define RW_MAX_SIZE	0x7ffff000

#define get_syspath_bar_mmap(s, bus,dev,func,bar) \
	snprintf(s, sizeof(s), \
		"/sys/bus/pci/devices/0000:%02x:%02x.%x/resource%u", \
		bus, dev, func, bar)

#define XNL_F_QMODE_ST	        0x00000001
#define XNL_F_QMODE_MM	        0x00000002

#define XNL_F_QDIR_H2C	        0x00000004
#define XNL_F_QDIR_C2H	        0x00000008
#define XNL_F_QDIR_BOTH         (XNL_F_QDIR_H2C | XNL_F_QDIR_C2H)

#define XNL_F_PFETCH_EN         0x00000010

#define XNL_F_DESC_BYPASS_EN	0x00000020
#define XNL_F_FETCH_CREDIT      0x00000040

#define XNL_F_CMPL_STATUS_ACC_EN        0x00000080
#define XNL_F_CMPL_STATUS_EN            0x00000100
#define XNL_F_CMPL_STATUS_PEND_CHK      0x00000200
#define XNL_F_CMPL_STATUS_DESC_EN  0x00000400
#define XNL_F_C2H_CMPL_INTR_EN  0x00000800



#define QDMA_CFG_BAR_SIZE 0xB400
#define QDMA_USR_BAR_SIZE 0x100

#define XNL_RESP_BUFLEN_MIN	 256
#define XNL_RESP_BUFLEN_MAX	 (2048 * 10)
#define MAX_KMALLOC_SIZE	(4*1024*1024)

#define XNL_NAME_PF		"xnl_pf"
#define XNL_NAME_VF		"xnl_vf"
#define XNL_VERSION		0x1

#define QDMA_MM_EN_SHIFT          0
#define QDMA_CMPT_EN_SHIFT        1
#define QDMA_ST_EN_SHIFT          2
#define QDMA_MAILBOX_EN_SHIFT     3

#define QDMA_MM_MODE              (1 << QDMA_MM_EN_SHIFT)
#define QDMA_COMPLETION_MODE      (1 << QDMA_CMPT_EN_SHIFT)
#define QDMA_ST_MODE              (1 << QDMA_ST_EN_SHIFT)
#define QDMA_MAILBOX              (1 << QDMA_MAILBOX_EN_SHIFT)


#define QDMA_MM_ST_MODE \
	(QDMA_MM_MODE | QDMA_COMPLETION_MODE | QDMA_ST_MODE)


enum xnl_st_c2h_cmpt_desc_size {
	XNL_ST_C2H_CMPT_DESC_SIZE_8B,	/**< 8B descriptor */
	XNL_ST_C2H_CMPT_DESC_SIZE_16B,	/**< 16B descriptor */
	XNL_ST_C2H_CMPT_DESC_SIZE_32B,	/**< 32B descriptor */
	XNL_ST_C2H_CMPT_DESC_SIZE_64B,	/**< 64B descriptor */
	XNL_ST_C2H_NUM_CMPT_DESC_SIZES	/**< Num of desc sizes */
};

enum qdma_q_fetch_credit {
	Q_DISABLE_FETCH_CREDIT = 0,
	Q_ENABLE_H2C_FETCH_CREDIT,
	Q_ENABLE_C2H_FETCH_CREDIT,
	Q_ENABLE_H2C_C2H_FETCH_CREDIT,
};

enum qdma_q_parm_type {
	QPARM_IDX,
	QPARM_MODE,
	QPARM_DIR,
	QPARM_DESC,
	/** @QPARM_CMPT: q cmpt desc request param */
	QPARM_CMPT,
	/** @QPARM_CMPTSZ: q cmpt size param */
	QPARM_CMPTSZ,
	/** @QPARM_SW_DESC_SZ: q sw desc size param */
	QPARM_SW_DESC_SZ,
	/** @QPARM_RNGSZ_IDX: q ring size idx param */
	QPARM_RNGSZ_IDX,
	QPARM_C2H_BUFSZ_IDX,
	/** @QPARM_CMPT_TMR_IDX: q cmpt timer idx param */
	QPARM_CMPT_TMR_IDX,
	/** @QPARM_CMPT_CNTR_IDX: q cmpt counter idx param */
	QPARM_CMPT_CNTR_IDX,
	/** @QPARM_CMPT_TRIG_MODE: q cmpt trigger mode param  */
	QPARM_CMPT_TRIG_MODE,
	/** @QPARM_PING_PONG_EN: ping pong param  */
	QPARM_PING_PONG_EN,
	/** @KEYHOLE_PARAM: keyhole feature aperture */
	QPARM_KEYHOLE_EN,
	/** @QPARM_MM_CHANNEL: q mm channel enable param */
	QPARM_MM_CHANNEL,
	/** @QPARM_MAX: max q param */
	QPARM_MAX,
};

struct cmd_intr {
	/** @vector: vector number */
	unsigned int vector;
	int start_idx;
	int end_idx;
};

struct cmd_reg {
	unsigned int sflags;
#define CMD_REG_F_BAR_SET	0x1
#define CMD_REG_F_REG_SET	0x2
#define CMD_REG_F_VAL_SET	0x4
	/** @bar: bar number */
	unsigned int bar;
	/** @reg: register offset */
	unsigned int reg;
	/** @val: value */
	unsigned int val;
	/** @range_start: range start */
	unsigned int range_start;
	/** @range_end: range end */
	unsigned int range_end;
};

struct cmd_q_parm {

	unsigned int sflags;
	unsigned int flags;
	unsigned int idx;
	unsigned int num_q;
	unsigned int range_start;
	unsigned int range_end;
	unsigned char sw_desc_sz;
	/** @cmpt_entry_size: completion desc size */
	unsigned char cmpt_entry_size;
	/** @qrngsz_idx: ring size idx */
	unsigned char qrngsz_idx;
	unsigned char c2h_bufsz_idx;
	unsigned char cmpt_tmr_idx;//timer
	unsigned char cmpt_cntr_idx;//counter
	unsigned char cmpt_trig_mode;
	unsigned char mm_channel;//mm channel enable
	unsigned char fetch_credit;//fetch credit enable
	unsigned char is_qp;
	unsigned char ping_pong_en;
	/** @aperture_sz: aperture_size for keyhole transfers*/
	unsigned int aperture_sz;
};

struct cmd_dev_cap {
	char version_str[256];
	unsigned int num_qs;//qmax
	unsigned int num_pfs;
	unsigned int flr_present;
	unsigned int mm_en;
	unsigned int mm_cmpt_en;
	unsigned int st_en;
	unsigned int mailbox_en;
	unsigned int mm_channel_max;
	unsigned int debug_mode;
	unsigned int desc_eng_mode;
};

struct xnl_dev_stat {
	unsigned long long mm_h2c_pkts;
	unsigned long long mm_c2h_pkts;
	unsigned long long st_h2c_pkts;
	unsigned long long st_c2h_pkts;
	unsigned long long ping_pong_lat_max;
	unsigned long long ping_pong_lat_min;
	unsigned long long ping_pong_lat_avg;
};

enum qdma_q_state {
	Q_STATE_DISABLED = 0,
	Q_STATE_ENABLED,
	Q_STATE_ONLINE,
};

struct xnl_q_info {
	unsigned int flags;
	unsigned int qidx;
	enum qdma_q_state state;
};

struct xnl_dev_info {
	unsigned char pci_bus;
	unsigned char pci_dev;
	unsigned char dev_func;
	unsigned char config_bar;
	unsigned char user_bar;
	unsigned int qmax;
	unsigned int qbase;
};

#define QDMA_GLOBAL_CSR_ARRAY_SZ        16

struct global_csr_conf {
	unsigned int ring_sz[QDMA_GLOBAL_CSR_ARRAY_SZ];
	unsigned int c2h_timer_cnt[QDMA_GLOBAL_CSR_ARRAY_SZ];
	/** @c2h_cnt_th: C2H counter threshold list*/
	unsigned int c2h_cnt_th[QDMA_GLOBAL_CSR_ARRAY_SZ];
	unsigned int c2h_buf_sz[QDMA_GLOBAL_CSR_ARRAY_SZ];
	/** @wb_intvl: Writeback interval */
	unsigned int wb_intvl;
};



enum xnl_op_t {
	XNL_CMD_DEV_LIST,	/**< list all the qdma devices */
	XNL_CMD_DEV_INFO,	/**< dump the device information */
	XNL_CMD_DEV_STAT,	/**< dump the device statistics */
	XNL_CMD_DEV_STAT_CLEAR,	/**< reset the device statistics */

	XNL_CMD_REG_DUMP,	/**< dump the register information */
	XNL_CMD_REG_RD,		/**< read a register value */
	XNL_CMD_REG_WRT,	/**< write value to a register */
	XNL_CMD_REG_INFO_READ,

	XNL_CMD_Q_LIST,		/**< list all the queue present in the system */
	XNL_CMD_Q_ADD,		/**< add a queue */
	XNL_CMD_Q_START,	/**< start a queue */
	XNL_CMD_Q_STOP,		/**< stop a queue */
	XNL_CMD_Q_DEL,		/**< delete a queue */
	XNL_CMD_Q_DUMP,		/**< dump queue information*/
	XNL_CMD_Q_DESC,		/**< dump descriptor information*/
	XNL_CMD_Q_CMPT,		/**< dump writeback descriptor information*/
	XNL_CMD_Q_RX_PKT,	/**< dump packet information*/
	XNL_CMD_Q_CMPT_READ,	/**< read the cmpt data */

	XNL_CMD_INTR_RING_DUMP,	/**< dump interrupt ring information*/
	XNL_CMD_Q_UDD,		/**< dump the user defined data */
	XNL_CMD_GLOBAL_CSR,	/**< get all global csr register values */
	XNL_CMD_DEV_CAP,	/**< list h/w capabilities , hw and sw version */
	XNL_CMD_GET_Q_STATE,	/**< get the queue state */
	XNL_CMD_MAX,		/**< max number of XNL commands*/
};

enum xnl_attr_t {
	XNL_ATTR_GENMSG,		/**< generatl message */
	XNL_ATTR_DRV_INFO,		/**< device info */

	XNL_ATTR_DEV_IDX,		/**< device index */
	XNL_ATTR_PCI_BUS,		/**< pci bus number */
	XNL_ATTR_PCI_DEV,		/**< pci device number */
	XNL_ATTR_PCI_FUNC,		/**< pci function id */

	XNL_ATTR_DEV_STAT_MMH2C_PKTS1,	/**< number of MM H2C packets */
	XNL_ATTR_DEV_STAT_MMH2C_PKTS2,	/**< number of MM H2C packets */
	XNL_ATTR_DEV_STAT_MMC2H_PKTS1,	/**< number of MM C2H packets */
	XNL_ATTR_DEV_STAT_MMC2H_PKTS2,	/**< number of MM C2H packets */
	XNL_ATTR_DEV_STAT_STH2C_PKTS1,	/**< number of ST H2C packets */
	XNL_ATTR_DEV_STAT_STH2C_PKTS2,	/**< number of ST H2C packets */
	XNL_ATTR_DEV_STAT_STC2H_PKTS1,	/**< number of ST C2H packets */
	XNL_ATTR_DEV_STAT_STC2H_PKTS2,	/**< number of ST C2H packets */

	XNL_ATTR_DEV_CFG_BAR,		/**< device config bar number */
	XNL_ATTR_DEV_USR_BAR,		/**< device AXI Master Lite(user bar) number */
	XNL_ATTR_DEV_QSET_MAX,		/**< max queue sets */
	XNL_ATTR_DEV_QSET_QBASE,	/**< queue base start */

	XNL_ATTR_VERSION_INFO,		/**< version info */
	XNL_ATTR_DEVICE_TYPE,		/**< device type */
	XNL_ATTR_IP_TYPE,		/**< ip type */
	XNL_ATTR_DEV_NUMQS,		/**< num of queues */
	XNL_ATTR_DEV_NUM_PFS,		/**< num of PFs */
	XNL_ATTR_DEV_MM_CHANNEL_MAX,	/**< mm channels */
	XNL_ATTR_DEV_MAILBOX_ENABLE,	/**< mailbox enable */
	XNL_ATTR_DEV_FLR_PRESENT,	/**< flr present */
	XNL_ATTR_DEV_ST_ENABLE,		/**< device st capability */
	XNL_ATTR_DEV_MM_ENABLE,		/**< device mm capability */
	XNL_ATTR_DEV_MM_CMPT_ENABLE,	/**< device mm cmpt capability */

	XNL_ATTR_REG_BAR_NUM,		/**< register bar number */
	XNL_ATTR_REG_ADDR,		/**< register address */
	XNL_ATTR_REG_VAL,		/**< register value */

	XNL_ATTR_CSR_INDEX,		/**< csr index */
	XNL_ATTR_CSR_COUNT,		/**< csr count */

	XNL_ATTR_QIDX,			/**< queue index */
	XNL_ATTR_NUM_Q,			/**< number of queues */
	XNL_ATTR_QFLAG,			/**< queue config flags */

	XNL_ATTR_CMPT_DESC_SIZE,	/**< completion descriptor size */
	XNL_ATTR_SW_DESC_SIZE,		/**< software descriptor size */
	XNL_ATTR_QRNGSZ_IDX,		/**< queue ring index */
	XNL_ATTR_C2H_BUFSZ_IDX,		/**< c2h buffer idex */
	XNL_ATTR_CMPT_TIMER_IDX,	/**< completion timer index */
	XNL_ATTR_CMPT_CNTR_IDX,		/**< completion counter index */
	XNL_ATTR_CMPT_TRIG_MODE,	/**< completion trigger mode */
	XNL_ATTR_MM_CHANNEL,		/**< mm channel */
	XNL_ATTR_CMPT_ENTRIES_CNT,      /**< completion entries count */

	XNL_ATTR_RANGE_START,		/**< range start */
	XNL_ATTR_RANGE_END,		/**< range end */

	XNL_ATTR_INTR_VECTOR_IDX,	/**< interrupt vector index */
	XNL_ATTR_INTR_VECTOR_START_IDX, /**< interrupt vector start index */
	XNL_ATTR_INTR_VECTOR_END_IDX,	/**< interrupt vector end index */
	XNL_ATTR_RSP_BUF_LEN,		/**< response buffer length */
	XNL_ATTR_GLOBAL_CSR,		/**< global csr data */
	XNL_ATTR_PIPE_GL_MAX,		/**< max no. of gl for pipe */
	XNL_ATTR_PIPE_FLOW_ID,          /**< pipe flow id */
	XNL_ATTR_PIPE_SLR_ID,           /**< pipe slr id */
	XNL_ATTR_PIPE_TDEST,            /**< pipe tdest */
	XNL_ATTR_DEV_STM_BAR,		/**< device STM bar number */
	XNL_ATTR_Q_STATE,
	XNL_ATTR_ERROR,
	XNL_ATTR_PING_PONG_EN,
	XNL_ATTR_APERTURE_SZ,
	XNL_ATTR_DEV_STAT_PING_PONG_LATMIN1,
	XNL_ATTR_DEV_STAT_PING_PONG_LATMIN2,
	XNL_ATTR_DEV_STAT_PING_PONG_LATMAX1,
	XNL_ATTR_DEV_STAT_PING_PONG_LATMAX2,
	XNL_ATTR_DEV_STAT_PING_PONG_LATAVG1,
	XNL_ATTR_DEV_STAT_PING_PONG_LATAVG2,
	XNL_ATTR_DEV,
	XNL_ATTR_DEBUG_EN,	/** Debug Regs Capability*/
	XNL_ATTR_DESC_ENGINE_MODE, /** Descriptor Engine Capability */
#ifdef ERR_DEBUG
	XNL_ATTR_QPARAM_ERR_INFO,	/**< queue param info */
#endif
	XNL_ATTR_NUM_REGS,			/**< number of regs */
	XNL_ATTR_MAX,
};

struct cmd_info {
	unsigned char vf:1;
	unsigned char op:7;
	union {
		struct cmd_intr intr;
		struct cmd_reg reg;
		struct cmd_q_parm qparm;
	} req;
	union {
		struct cmd_dev_cap cap;
		struct xnl_dev_stat dev_stat;
		struct xnl_dev_info dev_info;
		struct xnl_q_info q_info;
		struct global_csr_conf csr;
	} resp;
	unsigned int if_bdf;
	void (*log_msg_dump)(const char *resp_str);
};

struct xnl_hdr {
	struct nlmsghdr n;
	struct genlmsghdr g;
};


struct xnl_gen_msg {
	struct xnl_hdr hdr;
	char data[0];
};

struct xnl_cb {
	int fd;
	unsigned short family;
	unsigned int snd_seq;
	unsigned int rcv_seq;
};

struct xreg_info {
	const char name[32];
	uint32_t addr;
	uint32_t repeat;
	uint32_t step;
	uint8_t shift;
	uint8_t len;
	uint8_t mode;
};

static struct xreg_info qdma_user_regs[] = {
	{"ST_C2H_QID", 0x0, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_PKTLEN", 0x4, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_CONTROL", 0x8, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	/*  ST_C2H_CONTROL:
	 *	[1] : start C2H
	 *	[2] : immediate data
	 *	[3] : every packet statrs with 00 instead of continuous data
	 *	      stream until # of packets is complete
	 *	[31]: gen_user_reset_n
	 */
	{"ST_H2C_CONTROL", 0xC, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	/*  ST_H2C_CONTROL:
	 *	[0] : clear match for H2C transfer
	 */
	{"ST_H2C_STATUS", 0x10, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_H2C_XFER_CNT", 0x14, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_PKT_CNT", 0x20, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_CMPT_DATA", 0x30, 8, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_CMPT_SIZE", 0x50, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_SCRATCH_REG", 0x60, 2, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_PKT_DROP", 0x88, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"ST_C2H_PKT_ACCEPT", 0x8C, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"DSC_BYPASS_LOOP", 0x90, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"USER_INTERRUPT", 0x94, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"USER_INTERRUPT_MASK", 0x98, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"USER_INTERRUPT_VEC", 0x9C, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"DMA_CONTROL", 0xA0, 0, 0, 0, 0, QDMA_MM_ST_MODE},
	{"VDM_MSG_READ", 0xA4, 0, 0, 0, 0, QDMA_MM_ST_MODE},

	{"", 0, 0, 0 }
};


int qdma_dev_list_dump(struct cmd_info *cmd);

int qdma_dev_info(struct cmd_info *cmd);

int qdma_dev_cap(struct cmd_info *cmd);

int qdma_dev_stat(struct cmd_info *cmd);

int qdma_dev_stat_clear(struct cmd_info *cmd);

int qdma_dev_intr_ring_dump(struct cmd_info *cmd);

int qdma_dev_get_global_csr(struct cmd_info *cmd);

int qdma_dev_q_list_dump(struct cmd_info *cmd);

int qdma_q_add(struct cmd_info *cmd);

int qdma_q_del(struct cmd_info *cmd);

int qdma_q_start(struct cmd_info *cmd);

int qdma_q_stop(struct cmd_info *cmd);

int qdma_q_get_state(struct cmd_info *cmd);

int qdma_q_dump(struct cmd_info *cmd);

int qdma_q_desc_dump(struct cmd_info *cmd);

int qdma_q_cmpt_read(struct cmd_info *cmd);

int qdma_reg_read(struct cmd_info *cmd);

int qdma_reg_info_read(struct cmd_info *cmd);

int qdma_reg_write(struct cmd_info *cmd);

int proc_reg_cmd(struct cmd_info *cmd);

int qdma_reg_dump(struct cmd_info *cmd);

int xnl_common_msg_send(struct cmd_info *cmd, uint32_t *attrs);

static int xnl_connect(struct xnl_cb *cb, int vf, void (*log_err)(const char *));

static int32_t reg_read_mmap(struct xnl_dev_info *dev_info, unsigned char barno, struct cmd_info *cmd);

static int32_t reg_write_mmap(struct xnl_dev_info *dev_info, unsigned char barno, struct cmd_info *cmd);

static inline void print_seperator(struct cmd_info *cmd);

static void reg_dump_mmap(struct xnl_dev_info *dev_info, unsigned char barno, struct xreg_info *reg_list, unsigned int max, struct cmd_info *cmd);

static inline struct xnl_gen_msg *xnl_msg_alloc(unsigned int dlen, void (*log_err)(const char *));

static int xnl_send(struct xnl_cb *cb, struct xnl_hdr *hdr, void (*log_err)(const char *));

static int xnl_recv(struct xnl_cb *cb, struct xnl_hdr *hdr, int dlen, void (*log_err)(const char *));

static void xnl_msg_set_hdr(struct xnl_hdr *hdr, int family, int op);

static int xnl_send_cmd(struct xnl_cb *cb, struct xnl_hdr *hdr, struct cmd_info *cmd, unsigned int dlen);

static int xnl_parse_response(struct xnl_cb *cb, struct xnl_hdr *hdr, struct cmd_info *cmd, unsigned int dlen, uint32_t *attrs);

static void xnl_parse_cmd_attrs(struct xnl_hdr *hdr, struct cmd_info *cmd, uint32_t *attrs);

void xnl_close(struct xnl_cb *cb);

static uint32_t *mmap_bar(char *fname, size_t len, int prot);

static void read_config_regs(uint32_t *bar, struct xnl_dev_info *xdev, struct cmd_info *cmd);

static void read_regs(uint32_t *bar, struct xreg_info *reg_list, struct xnl_dev_info *xdev, struct cmd_info *cmd, void (*log_reg)(const char *));

static int xnl_msg_add_int_attr(struct xnl_hdr *hdr, enum xnl_attr_t type, unsigned int v);

static void xnl_msg_add_extra_config_attrs(struct xnl_hdr *hdr, struct cmd_info *cmd);

static int recv_nl_msg(struct xnl_hdr *hdr, struct cmd_info *cmd, uint32_t *attrs);

void xnl_parse_dev_info_attrs(uint32_t *attrs, struct cmd_info *cmd);

void xnl_parse_dev_stat_attrs(uint32_t *attrs, struct cmd_info *cmd);

void xnl_parse_dev_cap_attrs(struct xnl_hdr *hdr, uint32_t *attrs, struct cmd_info *cmd);

void xnl_parse_reg_attrs(uint32_t *attrs, struct cmd_info *cmd);

static void xnl_parse_q_state_attrs(uint32_t *attrs, struct cmd_info *cmd);

static void xnl_parse_csr_attrs(struct xnl_hdr *hdr, uint32_t *attrs, struct cmd_info *cmd);

static void print_repeated_reg(uint32_t *bar, struct xreg_info *xreg, unsigned start, unsigned limit, struct xnl_dev_info *dev_info, struct cmd_info *cmd, void (*log_reg)(const char *));

static int recv_attrs(struct xnl_hdr *hdr, struct cmd_info *cmd, uint32_t *attrs);

ssize_t write_from_buffer(char *fname, int fd, char *buffer, uint64_t size, uint64_t base);



#endif
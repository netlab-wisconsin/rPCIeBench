#include "cmd.h"
#include <sys/mman.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <errno.h>




ssize_t write_from_buffer(char *fname, int fd, char *buffer, uint64_t size, uint64_t base){
	ssize_t rc;
	uint64_t count = 0;
	char *buf = buffer;
	off_t offset = base;

	do { /* Support zero byte transfer */
		uint64_t bytes = size - count;

		if (bytes > RW_MAX_SIZE)
			bytes = RW_MAX_SIZE;

		if (offset) {
			rc = lseek(fd, offset, SEEK_SET);
			if (rc < 0) {
				fprintf(stderr,
					"%s, seek off 0x%lx failed %zd.\n",
					fname, offset, rc);
				perror("seek file");
				return -EIO;
			}
			if (rc != offset) {
				fprintf(stderr,
					"%s, seek off 0x%lx != 0x%lx.\n",
					fname, rc, offset);
				return -EIO;
			}
		}

		/* write data to file from memory buffer */
		rc = write(fd, buf, bytes);
		if (rc < 0) {
			fprintf(stderr, "%s, W off 0x%lx, 0x%lx failed %zd.\n",
				fname, offset, bytes, rc);
			perror("write file");
			return -EIO;
		}
		if (rc != bytes) {
			fprintf(stderr, "%s, W off 0x%lx, 0x%lx != 0x%lx.\n",
				fname, offset, rc, bytes);
			return -EIO;
		}

		count += bytes;
		buf += bytes;
		offset += bytes;
	} while (count < size);

	if (count != size) {
		fprintf(stderr, "%s, R failed 0x%lx != 0x%lx.\n",
				fname, count, size);
		return -EIO;
	}
	return count;
}


int qdma_dev_list_dump(struct cmd_info *cmd){
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	xnl_common_msg_send(cmd, attrs);
	cmd->vf = 1;
	return xnl_common_msg_send(cmd, attrs);
}

int qdma_dev_info(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};
	int rv;

	rv = xnl_common_msg_send(cmd, attrs);
	if (rv < 0)
		return rv;
}

int qdma_dev_cap(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};
	int rv;

	rv = xnl_common_msg_send(cmd, attrs);
	if (rv < 0)
		return rv;
}

int qdma_dev_stat(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};
	int rv;

	rv = xnl_common_msg_send(cmd, attrs);
	if (rv < 0)
		return rv;
}

int qdma_dev_stat_clear(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_dev_intr_ring_dump(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_dev_get_global_csr(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_dev_q_list_dump(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_add(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_del(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_start(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};
	int rv = 0;
	if ((cmd->req.qparm.flags & XNL_F_QDIR_BOTH) ==
		XNL_F_QDIR_BOTH) {

		if ((cmd->req.qparm.fetch_credit &
			Q_ENABLE_C2H_FETCH_CREDIT) == Q_ENABLE_C2H_FETCH_CREDIT)
			cmd->req.qparm.flags |= XNL_F_FETCH_CREDIT;
		else
			cmd->req.qparm.flags &= ~XNL_F_FETCH_CREDIT;

		cmd->req.qparm.flags = ((cmd->req.qparm.flags &
						(~XNL_F_QDIR_BOTH)) | XNL_F_QDIR_C2H);

		rv = xnl_common_msg_send(cmd, attrs);
		if (rv < 0)
			return rv;

		if ((cmd->req.qparm.fetch_credit &
			Q_ENABLE_H2C_FETCH_CREDIT) == Q_ENABLE_H2C_FETCH_CREDIT)
			cmd->req.qparm.flags |= XNL_F_FETCH_CREDIT;
		else
			cmd->req.qparm.flags &= ~XNL_F_FETCH_CREDIT;

		cmd->req.qparm.flags = ((cmd->req.qparm.flags &
						(~XNL_F_QDIR_BOTH)) | XNL_F_QDIR_H2C);

		return xnl_common_msg_send(cmd, attrs);
	} else {
		if ((cmd->req.qparm.flags & XNL_F_QDIR_H2C) ==
			XNL_F_QDIR_H2C) {
			if ((cmd->req.qparm.fetch_credit &
				Q_ENABLE_H2C_FETCH_CREDIT) == Q_ENABLE_H2C_FETCH_CREDIT)
				cmd->req.qparm.flags |= XNL_F_FETCH_CREDIT;
			else
				cmd->req.qparm.flags &= ~XNL_F_FETCH_CREDIT;
		} else if ((cmd->req.qparm.flags & XNL_F_QDIR_C2H) ==
			XNL_F_QDIR_C2H) {
			if ((cmd->req.qparm.fetch_credit &
				Q_ENABLE_C2H_FETCH_CREDIT) == Q_ENABLE_C2H_FETCH_CREDIT)
					cmd->req.qparm.flags |= XNL_F_FETCH_CREDIT;
			else
				cmd->req.qparm.flags &= ~XNL_F_FETCH_CREDIT;
		}
		return xnl_common_msg_send(cmd, attrs);
	}
}

int qdma_q_stop(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_dump(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_get_state(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}


int qdma_q_desc_dump(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_q_cmpt_read(struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	return xnl_common_msg_send(cmd, attrs);
}

int qdma_reg_read(struct cmd_info *cmd)
{
	return proc_reg_cmd(cmd);
}

int qdma_reg_write(struct cmd_info *cmd)
{
	return proc_reg_cmd(cmd);
}

int qdma_reg_info_read(struct cmd_info *cmd)
{
	return proc_reg_cmd(cmd);
}

int qdma_reg_dump(struct cmd_info *cmd)
{
	return proc_reg_cmd(cmd);
}


int proc_reg_cmd(struct cmd_info *cmd)
{
	struct cmd_reg *regcmd;
	struct cmd_info dev_cmd;
	int32_t rv = 0;
	char reg_dump[100];
	unsigned int barno;
	unsigned char version[256];
	int32_t v;
	unsigned char op;
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	memcpy(&dev_cmd, cmd, sizeof(dev_cmd));

	dev_cmd.op = XNL_CMD_DEV_CAP;
	rv = qdma_dev_cap(&dev_cmd);
	if (rv < 0)
		return rv;

	strcpy(version, dev_cmd.resp.cap.version_str);

	op = cmd->op;
	cmd->op = XNL_CMD_DEV_INFO;
	rv = qdma_dev_info(cmd);
	if (rv < 0)
		return rv;
	cmd->op = op;
	regcmd = &cmd->req.reg;
	barno = (regcmd->sflags & CMD_REG_F_BAR_SET) ?
			 regcmd->bar : cmd->resp.dev_info.config_bar;
	regcmd->bar = barno;

	switch (cmd->op) {
	case XNL_CMD_REG_RD:
		rv = reg_read_mmap(&cmd->resp.dev_info, barno, cmd);
		if (rv < 0)
			return rv;
		break;
	case XNL_CMD_REG_WRT:
		v = reg_write_mmap(&cmd->resp.dev_info, barno, cmd);
		if (v < 0)
			return rv;
		rv = reg_read_mmap(&cmd->resp.dev_info, barno, cmd);
		if (rv < 0)
			return rv;
		break;
	case XNL_CMD_REG_DUMP:
		print_seperator(cmd);

		if (cmd->vf)
			snprintf(reg_dump, 100, "\n###\t\tqdmavf%05x, pci %02x:%02x.%02x, reg dump\n",
				cmd->if_bdf, cmd->resp.dev_info.pci_bus,
				cmd->resp.dev_info.pci_dev, cmd->resp.dev_info.dev_func);
		else
			snprintf(reg_dump, 100, "\n###\t\tqdma%05x, pci %02x:%02x.%02x, reg dump\n",
				cmd->if_bdf, cmd->resp.dev_info.pci_bus,
				cmd->resp.dev_info.pci_dev, cmd->resp.dev_info.dev_func);
		if (cmd->log_msg_dump)
			cmd->log_msg_dump(reg_dump);

		print_seperator(cmd);
		snprintf(reg_dump, 100, "\nAXI Master Lite Bar #%d\n",
			 cmd->resp.dev_info.user_bar);
		if (cmd->log_msg_dump)
			cmd->log_msg_dump(reg_dump);

		reg_dump_mmap(&cmd->resp.dev_info, cmd->resp.dev_info.user_bar,
			      qdma_user_regs, QDMA_USR_BAR_SIZE, cmd);

		snprintf(reg_dump, 100, "\nCONFIG BAR #%d\n",
			cmd->resp.dev_info.config_bar);
		if (cmd->log_msg_dump)
			cmd->log_msg_dump(reg_dump);
		reg_dump_mmap(&cmd->resp.dev_info, cmd->resp.dev_info.config_bar, NULL,
				QDMA_CFG_BAR_SIZE, cmd);
		break;
	case XNL_CMD_REG_INFO_READ:
		xnl_common_msg_send(cmd, attrs);
		break;
	default:
		break;
	}

	return 0;
}

static int xnl_connect(struct xnl_cb *cb, int vf, void (*log_err)(const char *))
{
	int fd;
	struct sockaddr_nl addr;
	struct xnl_gen_msg *msg = xnl_msg_alloc(0, log_err);
	struct xnl_hdr *hdr = &msg->hdr;
	struct nlattr *attr;
	int rv = -1;

	fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_GENERIC);
	if (fd < 0) {
		if (log_err)
			log_err("nl socket err");
		rv = fd;
		goto out;
    }
	cb->fd = fd;

	memset(&addr, 0, sizeof(struct sockaddr_nl));
	addr.nl_family = AF_NETLINK;
	rv = bind(fd, (struct sockaddr *)&addr, sizeof(struct sockaddr_nl));
	if (rv < 0) {
		if (log_err)
			log_err("nl bind err");
		goto out;
	}

	hdr->n.nlmsg_type = GENL_ID_CTRL;
	hdr->n.nlmsg_flags = NLM_F_REQUEST;
	hdr->n.nlmsg_pid = getpid();
	hdr->n.nlmsg_len = NLMSG_LENGTH(GENL_HDRLEN);

	hdr->g.cmd = CTRL_CMD_GETFAMILY;
	hdr->g.version = XNL_VERSION;

	attr = (struct nlattr *)(hdr + 1);
	attr->nla_type = CTRL_ATTR_FAMILY_NAME;
	cb->family = CTRL_ATTR_FAMILY_NAME;
	
	if (vf) {
        	attr->nla_len = strlen(XNL_NAME_VF) + 1 + NLA_HDRLEN;
        	strcpy((char *)(attr + 1), XNL_NAME_VF);

	} else {
        	attr->nla_len = strlen(XNL_NAME_PF) + 1 + NLA_HDRLEN;
        	strcpy((char *)(attr + 1), XNL_NAME_PF);
	}
        hdr->n.nlmsg_len += NLMSG_ALIGN(attr->nla_len);

	rv = xnl_send(cb, hdr, log_err);
	if (rv < 0)
		goto out;

	rv = xnl_recv(cb, hdr, XNL_RESP_BUFLEN_MIN, NULL);
	if (rv < 0)
		goto out;

	attr = (struct nlattr *)((char *)attr + NLA_ALIGN(attr->nla_len));
	/* family ID */
	if (attr->nla_type == CTRL_ATTR_FAMILY_ID)
		cb->family = *(__u16 *)(attr + 1);
	rv = 0;
out:
	free(msg);
	return rv;
}

static int get_cmd_resp_buf_len(enum xnl_op_t op, struct cmd_info *cmd)
{
	int buf_len = XNL_RESP_BUFLEN_MAX;
	unsigned int row_len = 50;

	switch (op) {
		case XNL_CMD_Q_DESC:
        	row_len *= 2;
        case XNL_CMD_Q_CMPT:
        	buf_len += ((cmd->req.qparm.range_end -
        			cmd->req.qparm.range_start)*row_len);
        	break;
        case XNL_CMD_INTR_RING_DUMP:
        	buf_len += ((cmd->req.intr.end_idx -
				     cmd->req.intr.start_idx)*row_len);
        	break;
        case XNL_CMD_DEV_LIST:
        case XNL_CMD_DEV_INFO:
        case XNL_CMD_DEV_CAP:
        case XNL_CMD_Q_START:
        case XNL_CMD_Q_STOP:
        case XNL_CMD_Q_DEL:
        case XNL_CMD_GLOBAL_CSR:
        	return buf_len;
        case XNL_CMD_Q_ADD:
        case XNL_CMD_Q_DUMP:
        case XNL_CMD_Q_CMPT_READ:
        	break;
        case XNL_CMD_Q_LIST:
        	buf_len = XNL_RESP_BUFLEN_MAX * 10;
        	break;
        case XNL_CMD_REG_DUMP:
		case XNL_CMD_REG_INFO_READ:
        	buf_len = XNL_RESP_BUFLEN_MAX * 6;
        break;
		case XNL_CMD_DEV_STAT:
			buf_len = XNL_RESP_BUFLEN_MAX;
		break;
		default:
        	buf_len = XNL_RESP_BUFLEN_MIN;
        	return buf_len;
	}
	if ((cmd->req.qparm.flags & XNL_F_QDIR_BOTH) == XNL_F_QDIR_BOTH)
		buf_len *= 2;
	if(cmd->req.qparm.num_q > 1)
			buf_len *= cmd->req.qparm.num_q;
	if(buf_len > MAX_KMALLOC_SIZE)
		buf_len = MAX_KMALLOC_SIZE;
	return buf_len;
}

int xnl_common_msg_send(struct cmd_info *cmd, uint32_t *attrs){
	int rv;
	struct xnl_gen_msg *msg = NULL;
	struct xnl_hdr *hdr;
	struct xnl_cb cb;
	unsigned int dlen;

	rv = xnl_connect(&cb, cmd->vf, cmd->log_msg_dump);
	if (rv < 0)
		return rv;
	dlen = get_cmd_resp_buf_len(cmd->op, cmd);
	msg = xnl_msg_alloc(dlen, cmd->log_msg_dump);
	if (!msg)
		goto close;
	hdr = &msg->hdr;

	xnl_msg_set_hdr(hdr, cb.family, cmd->op);
	
	rv = xnl_send_cmd(&cb, hdr, cmd, dlen);
	if (rv < 0)
		goto free_mem;
	
	rv = xnl_parse_response(&cb, hdr, cmd, dlen, attrs);
	if (rv < 0)
		goto free_mem;
	xnl_parse_cmd_attrs(hdr, cmd, attrs);
	
free_mem:
	free(msg);
close:
	xnl_close(&cb);

	return rv;
}

static int32_t reg_read_mmap(struct xnl_dev_info *dev_info,
			     unsigned char barno,
			     struct cmd_info *cmd)
{
	uint32_t *bar;
	char fname[256];
	int rv = 0;
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	get_syspath_bar_mmap(fname, dev_info->pci_bus, dev_info->pci_dev,
			     dev_info->dev_func, barno);

	bar = mmap_bar(fname, cmd->req.reg.reg + 4, PROT_READ);
	if (!bar) {
		if (cmd->op == XNL_CMD_REG_WRT)
			cmd->op = XNL_CMD_REG_RD;

		rv  = xnl_common_msg_send(cmd, attrs);

		return rv;
	}

	cmd->req.reg.val = le32toh(bar[cmd->req.reg.reg / 4]);
	munmap(bar, cmd->req.reg.reg + 4);

	return rv;
}

static int32_t reg_write_mmap(struct xnl_dev_info *dev_info,
			      unsigned char barno,
			      struct cmd_info *cmd)
{
	uint32_t *bar;
	char fname[256];
	int rv = 0;
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	get_syspath_bar_mmap(fname, dev_info->pci_bus, dev_info->pci_dev,
			     dev_info->dev_func, barno);

	bar = mmap_bar(fname, cmd->req.reg.reg + 4, PROT_WRITE);
	if (!bar) {
		rv  = xnl_common_msg_send(cmd, attrs);

		return rv;
	}

	bar[cmd->req.reg.reg / 4] = htole32(cmd->req.reg.val);
	munmap(bar, cmd->req.reg.reg + 4);
	return 0;
}

static inline void print_seperator(struct cmd_info *cmd)
{
	char buffer[81];

	memset(buffer, '#', 80);
	buffer[80] = '\0';

	if (cmd && cmd->log_msg_dump)
		cmd->log_msg_dump(buffer);
}

static void reg_dump_mmap(struct xnl_dev_info *dev_info, unsigned char barno,
			struct xreg_info *reg_list, unsigned int max,
			struct cmd_info *cmd)
{
	uint32_t *bar;
	char fname[256];

	get_syspath_bar_mmap(fname, dev_info->pci_bus, dev_info->pci_dev,
			     dev_info->dev_func, barno);

	if ((barno == dev_info->config_bar) && (reg_list == NULL))
		read_config_regs(NULL, dev_info, cmd);
	else {
		bar = mmap_bar(fname, max, PROT_READ);
		if (!bar) {
			cmd->req.reg.bar = barno;
			cmd->op = XNL_CMD_REG_RD;
			read_regs(NULL, reg_list, dev_info, cmd,
					  cmd->log_msg_dump);
		} else {
			read_regs(bar, reg_list, NULL, NULL, cmd->log_msg_dump);
			munmap(bar, max);
		}
	}
	return;
}

static inline struct xnl_gen_msg *xnl_msg_alloc(unsigned int dlen,void (*log_err)(const char *))
{
	struct xnl_gen_msg *msg;
	unsigned int extra_mem = (XNL_ATTR_MAX * (sizeof(struct nlattr) + sizeof(uint32_t)));

	if (dlen)
		extra_mem = dlen;

	msg = malloc(sizeof(struct xnl_gen_msg) + extra_mem);
	if (!msg) {
		char err_msg[100] = {'\0'};

		snprintf(err_msg, 100, "%s: OOM, %u.\n",
			 __FUNCTION__, extra_mem);
		if (log_err)
			log_err(err_msg);
		return NULL;
	}

	memset(msg, 0, sizeof(struct xnl_gen_msg) + extra_mem);
	return msg;
}

static int xnl_send(struct xnl_cb *cb, struct xnl_hdr *hdr,void (*log_err)(const char *))
{
	int rv;
	struct sockaddr_nl addr = {
		.nl_family = AF_NETLINK,
	};

	hdr->n.nlmsg_seq = cb->snd_seq;
	cb->snd_seq++;

	rv = sendto(cb->fd, (char *)hdr, hdr->n.nlmsg_len, 0, (struct sockaddr *)&addr, sizeof(addr));
	if (rv != hdr->n.nlmsg_len) {
		if (log_err)
			log_err("nl send err");
		return -1;
	}

	return 0;
}

static int xnl_recv(struct xnl_cb *cb, struct xnl_hdr *hdr, int dlen,
		    void (*log_err)(const char *))
{
	int rv;

	memset(hdr, 0, sizeof(struct xnl_gen_msg) + dlen);

	rv = recv(cb->fd, hdr, dlen, 0);
	if (rv < 0) {
		if (log_err)
			log_err("nl recv err");
		return -1;
	}
	/* as long as there is attribute, even if it is shorter than expected */
	if (!NLMSG_OK((&hdr->n), rv) && (rv <= sizeof(struct xnl_hdr))) {
		char err_msg[100] = {'\0'};

		snprintf(err_msg, 100,
			 "nl recv:, invalid message, cmd 0x%x, %d,%d.\n",
			 hdr->g.cmd, dlen, rv);
		if (log_err)
			log_err(err_msg);
		return -1;
	}

	if (hdr->n.nlmsg_type == NLMSG_ERROR) {
		char err_msg[100] = {'\0'};

		snprintf(err_msg, 100, "nl recv, msg error, cmd 0x%x\n",
				hdr->g.cmd);
		if (log_err)
			log_err(err_msg);
		return -1;
	}

	return 0;
}

static void xnl_msg_set_hdr(struct xnl_hdr *hdr, int family, int op)
{
	hdr->n.nlmsg_len = NLMSG_LENGTH(GENL_HDRLEN);
	hdr->n.nlmsg_type = family;
	hdr->n.nlmsg_flags = NLM_F_REQUEST;
	hdr->n.nlmsg_pid = getpid();

	hdr->g.cmd = op;
}

static int xnl_send_cmd(struct xnl_cb *cb, struct xnl_hdr *hdr,
			struct cmd_info *cmd, unsigned int dlen)
{
	struct nlattr *attr;
	int rv;

	attr = (struct nlattr *)(hdr + 1);

	xnl_msg_add_int_attr(hdr, XNL_ATTR_DEV_IDX, cmd->if_bdf);

	switch(cmd->op) {
        case XNL_CMD_DEV_LIST:
        case XNL_CMD_DEV_INFO:
        case XNL_CMD_DEV_STAT:
        case XNL_CMD_DEV_STAT_CLEAR:
        case XNL_CMD_Q_LIST:
		/* no parameter */
		break;
        case XNL_CMD_Q_ADD:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QIDX, cmd->req.qparm.idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_Q, cmd->req.qparm.num_q);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QFLAG, cmd->req.qparm.flags);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN, dlen );
		break;
        case XNL_CMD_Q_START:
        	xnl_msg_add_extra_config_attrs(hdr, cmd);
        case XNL_CMD_Q_STOP:
        case XNL_CMD_Q_DEL:
        case XNL_CMD_Q_DUMP:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QIDX, cmd->req.qparm.idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_Q, cmd->req.qparm.num_q);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QFLAG, cmd->req.qparm.flags);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN, dlen);
		break;
        case XNL_CMD_Q_DESC:
        case XNL_CMD_Q_CMPT:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QIDX, cmd->req.qparm.idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_Q, cmd->req.qparm.num_q);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QFLAG, cmd->req.qparm.flags);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RANGE_START,
					cmd->req.qparm.range_start);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RANGE_END,
					cmd->req.qparm.range_end);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN, dlen);
		break;
        case XNL_CMD_Q_RX_PKT:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QIDX, cmd->req.qparm.idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_Q, cmd->req.qparm.num_q);
		/* hard coded to C2H */
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QFLAG, XNL_F_QDIR_C2H);
		break;
        case XNL_CMD_Q_CMPT_READ:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QIDX, cmd->req.qparm.idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QFLAG, cmd->req.qparm.flags);
		/*xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_ENTRIES,
					cmd->u.qparm.num_entries);*/
		break;
        case XNL_CMD_INTR_RING_DUMP:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_INTR_VECTOR_IDX,
		                     cmd->req.intr.vector);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_INTR_VECTOR_START_IDX,
		                     cmd->req.intr.start_idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_INTR_VECTOR_END_IDX,
		                     cmd->req.intr.end_idx);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN, dlen);
		break;
        case XNL_CMD_REG_RD:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_BAR_NUM,
    							 cmd->req.reg.bar);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_ADDR,
							 cmd->req.reg.reg);
		break;
        case XNL_CMD_REG_WRT:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_BAR_NUM,
    							 cmd->req.reg.bar);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_ADDR,
							 cmd->req.reg.reg);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_VAL,
							 cmd->req.reg.val);
		break;
		case XNL_CMD_REG_INFO_READ:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_BAR_NUM,
								 cmd->req.reg.bar);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_REG_ADDR,
							 cmd->req.reg.reg);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_NUM_REGS,
							 cmd->req.reg.range_end);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN,
				dlen);
		break;
        case XNL_CMD_REG_DUMP:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_RSP_BUF_LEN,
				dlen);
		break;
		case XNL_CMD_GLOBAL_CSR:
		xnl_msg_add_int_attr(hdr, XNL_ATTR_CSR_INDEX,
				0);
		xnl_msg_add_int_attr(hdr, XNL_ATTR_CSR_COUNT,
				QDMA_GLOBAL_CSR_ARRAY_SZ);
		break;
	default:
		break;
	}

	rv = xnl_send(cb, hdr, cmd->log_msg_dump);
	if (rv < 0)
		goto out;

out:
	return rv;
}

static int xnl_parse_response(struct xnl_cb *cb, struct xnl_hdr *hdr,
			      struct cmd_info *cmd, unsigned int dlen,
			      uint32_t *attrs)
{
	struct nlattr *attr;
	int rv;

	rv = xnl_recv(cb, hdr, dlen, cmd->log_msg_dump);
	if (rv < 0)
		goto out;

	rv = recv_nl_msg(hdr, cmd, attrs);
out:
	return rv;
}

static void xnl_parse_cmd_attrs(struct xnl_hdr *hdr, struct cmd_info *cmd,
				uint32_t *attrs)
{
	switch(cmd->op) {
        case XNL_CMD_DEV_INFO:
        	xnl_parse_dev_info_attrs(attrs, cmd);
		break;
        case XNL_CMD_DEV_STAT:
        	xnl_parse_dev_stat_attrs(attrs, cmd);
		break;
        case XNL_CMD_DEV_CAP:
        	xnl_parse_dev_cap_attrs(hdr, attrs, cmd);
		break;
        case XNL_CMD_REG_RD:
        case XNL_CMD_REG_WRT:
        	xnl_parse_reg_attrs(attrs, cmd);
		break;
        case XNL_CMD_GET_Q_STATE:
        	xnl_parse_q_state_attrs(attrs, cmd);
		break;
        case XNL_CMD_GLOBAL_CSR:
		xnl_parse_csr_attrs(hdr, attrs, cmd);
		break;
	default:
		break;
	}
}

void xnl_close(struct xnl_cb *cb)
{
	close(cb->fd);
}

static uint32_t *mmap_bar(char *fname, size_t len, int prot)
{
	int fd;
	uint32_t *bar;

	fd = open(fname, (prot & PROT_WRITE) ? O_RDWR : O_RDONLY);
	if (fd < 0)
		return NULL;

	bar = mmap(NULL, len, prot, MAP_SHARED, fd, 0);
	close(fd);

	return bar == MAP_FAILED ? NULL : bar;
}

static void read_config_regs(uint32_t *bar, struct xnl_dev_info *xdev,
			     struct cmd_info *cmd)
{
	uint32_t attrs[XNL_ATTR_MAX] = {0};

	/* Get the capabilities of the Device */
	cmd->op = XNL_CMD_REG_DUMP;
	xnl_common_msg_send(cmd, attrs);
}

static void read_regs(uint32_t *bar, struct xreg_info *reg_list,
		      struct xnl_dev_info *xdev, struct cmd_info *cmd,
		      void (*log_reg)(const char *))
{
	struct xreg_info *xreg = reg_list;
	uint32_t val;
	int32_t rv = 0;
	char reg_dump[100] = {'\0'};

	for (xreg = reg_list; strlen(xreg->name); xreg++) {

		if (!xreg->len) {
			if (xreg->repeat) {
				if (cmd == NULL)
					print_repeated_reg(bar, xreg, 0,
							   xreg->repeat, NULL,
							   NULL, log_reg);
				else
					print_repeated_reg(NULL, xreg, 0,
							   xreg->repeat, xdev,
							   cmd, log_reg);
			} else {
				uint32_t addr = xreg->addr;
				if (cmd == NULL) {
					val = le32toh(bar[addr / 4]);
				} else {
					cmd->req.reg.reg = addr;
					rv = reg_read_mmap(xdev,
							   cmd->req.reg.bar,
							   cmd);
					if (rv < 0)
						continue;
				}
				snprintf(reg_dump, 100,
					 "[%#7x] %-47s %#-10x %u\n",
					 addr, xreg->name, val, val);
				if (log_reg)
					log_reg(reg_dump);
			}
		} else {
			uint32_t addr = xreg->addr;
			uint32_t val = 0;
			if (cmd == NULL) {
				val = le32toh(bar[addr / 4]);
			} else {
				cmd->req.reg.reg = addr;
				rv = reg_read_mmap(xdev, cmd->req.reg.bar,
						   cmd);
				if (rv < 0)
					continue;
			}

			uint32_t v = (val >> xreg->shift) &
					((1 << xreg->len) - 1);

			snprintf(reg_dump, 100,
				 "    %*u:%d %-47s %#-10x %u\n",
				 xreg->shift < 10 ? 3 : 2,
				 xreg->shift + xreg->len - 1,
				 xreg->shift, xreg->name, v, v);
			if (log_reg)
				log_reg(reg_dump);
		}
		memset(reg_dump, '\0', 100);
	}
}

static int xnl_msg_add_int_attr(struct xnl_hdr *hdr, enum xnl_attr_t type,
				unsigned int v)
{
	struct nlattr *attr = (struct nlattr *)((char *)hdr + hdr->n.nlmsg_len);

        attr->nla_type = (__u16)type;
        attr->nla_len = sizeof(__u32) + NLA_HDRLEN;
	*(__u32 *)(attr+ 1) = v;

        hdr->n.nlmsg_len += NLMSG_ALIGN(attr->nla_len);
	return 0;
}

static void xnl_msg_add_extra_config_attrs(struct xnl_hdr *hdr,
                                       struct cmd_info *cmd)
{
	if (cmd->req.qparm.sflags & (1 << QPARM_RNGSZ_IDX))
		xnl_msg_add_int_attr(hdr, XNL_ATTR_QRNGSZ_IDX,
		                     cmd->req.qparm.qrngsz_idx);
	if (cmd->req.qparm.sflags & (1 << QPARM_C2H_BUFSZ_IDX))
		xnl_msg_add_int_attr(hdr, XNL_ATTR_C2H_BUFSZ_IDX,
		                     cmd->req.qparm.c2h_bufsz_idx);
	if (cmd->req.qparm.sflags & (1 << QPARM_CMPTSZ))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_CMPT_DESC_SIZE,
		                     cmd->req.qparm.cmpt_entry_size);
	if (cmd->req.qparm.sflags & (1 << QPARM_SW_DESC_SZ))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_SW_DESC_SIZE,
		                     cmd->req.qparm.sw_desc_sz);
	if (cmd->req.qparm.sflags & (1 << QPARM_CMPT_TMR_IDX))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_CMPT_TIMER_IDX,
		                     cmd->req.qparm.cmpt_tmr_idx);
	if (cmd->req.qparm.sflags & (1 << QPARM_CMPT_CNTR_IDX))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_CMPT_CNTR_IDX,
		                     cmd->req.qparm.cmpt_cntr_idx);
	if (cmd->req.qparm.sflags & (1 << QPARM_MM_CHANNEL))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_MM_CHANNEL,
		                     cmd->req.qparm.mm_channel);
	if (cmd->req.qparm.sflags & (1 << QPARM_CMPT_TRIG_MODE))
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_CMPT_TRIG_MODE,
		                     cmd->req.qparm.cmpt_trig_mode);
	if (cmd->req.qparm.sflags & (1 << QPARM_PING_PONG_EN)) {
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_PING_PONG_EN,
							 cmd->req.qparm.ping_pong_en);
	}
	if (cmd->req.qparm.sflags & (1 << QPARM_KEYHOLE_EN)) {
		xnl_msg_add_int_attr(hdr,  XNL_ATTR_APERTURE_SZ,
							 cmd->req.qparm.aperture_sz);
	}
}


static int recv_nl_msg(struct xnl_hdr *hdr, struct cmd_info *cmd,
		       uint32_t *attrs)
{
	if (!attrs)
		return 0;
	recv_attrs(hdr, cmd, attrs);

	if (attrs[XNL_ATTR_ERROR] != 0)
		return (int)attrs[XNL_ATTR_ERROR];

	return 0;
}

void xnl_parse_dev_info_attrs(uint32_t *attrs, struct cmd_info *cmd)
{
	unsigned int usr_bar;
	struct xnl_dev_info *dev_info = &cmd->resp.dev_info;

	dev_info->config_bar = attrs[XNL_ATTR_DEV_CFG_BAR];
	usr_bar = (int)attrs[XNL_ATTR_DEV_USR_BAR];
	dev_info->qmax = attrs[XNL_ATTR_DEV_QSET_MAX];
	dev_info->qbase = attrs[XNL_ATTR_DEV_QSET_QBASE];
	dev_info->pci_bus = attrs[XNL_ATTR_PCI_BUS];
	dev_info->pci_dev = attrs[XNL_ATTR_PCI_DEV];
	dev_info->dev_func = attrs[XNL_ATTR_PCI_FUNC];

	if (usr_bar+1 == 0)
		dev_info->user_bar = 2;
	else
		dev_info->user_bar = usr_bar;
}

void xnl_parse_dev_stat_attrs(uint32_t *attrs, struct cmd_info *cmd)
{
	unsigned int pkts;
	struct xnl_dev_stat *dev_stat = &cmd->resp.dev_stat;

	pkts = attrs[XNL_ATTR_DEV_STAT_MMH2C_PKTS1];
	dev_stat->mm_h2c_pkts = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_MMH2C_PKTS2];
	dev_stat->mm_h2c_pkts |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_MMC2H_PKTS1];
	dev_stat->mm_c2h_pkts = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_MMC2H_PKTS2];
	dev_stat->mm_c2h_pkts |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_STH2C_PKTS1];
	dev_stat->st_h2c_pkts = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_STH2C_PKTS2];
	dev_stat->st_h2c_pkts |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_STC2H_PKTS1];
	dev_stat->st_c2h_pkts = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_STC2H_PKTS2];
	dev_stat->st_c2h_pkts |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATMAX1];
	dev_stat->ping_pong_lat_max = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATMAX2];
	dev_stat->ping_pong_lat_max |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATMIN1];
	dev_stat->ping_pong_lat_min = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATMIN2];
	dev_stat->ping_pong_lat_min |= (((unsigned long long)pkts) << 32);

	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATAVG1];
	dev_stat->ping_pong_lat_avg = pkts;
	pkts = attrs[XNL_ATTR_DEV_STAT_PING_PONG_LATAVG2];
	dev_stat->ping_pong_lat_avg |= (((unsigned long long)pkts) << 32);
}

void xnl_parse_dev_cap_attrs(struct xnl_hdr *hdr, uint32_t *attrs,
				    struct cmd_info *cmd)
{
	unsigned char *p = (unsigned char *)(hdr + 1);
	struct nlattr *na = (struct nlattr *)p;

	cmd->resp.cap.mailbox_en = attrs[XNL_ATTR_DEV_MAILBOX_ENABLE];
	cmd->resp.cap.mm_channel_max = attrs[XNL_ATTR_DEV_MM_CHANNEL_MAX];
	cmd->resp.cap.num_pfs = attrs[XNL_ATTR_DEV_NUM_PFS];
	cmd->resp.cap.num_qs = attrs[XNL_ATTR_DEV_NUMQS];
	cmd->resp.cap.flr_present = attrs[XNL_ATTR_DEV_FLR_PRESENT];
	cmd->resp.cap.mm_en = attrs[XNL_ATTR_DEV_MM_ENABLE];
	cmd->resp.cap.debug_mode = attrs[XNL_ATTR_DEBUG_EN];
	cmd->resp.cap.desc_eng_mode = attrs[XNL_ATTR_DESC_ENGINE_MODE];
	cmd->resp.cap.mm_cmpt_en =
			attrs[XNL_ATTR_DEV_MM_CMPT_ENABLE];
	cmd->resp.cap.st_en = attrs[XNL_ATTR_DEV_ST_ENABLE];
	if (na->nla_type == XNL_ATTR_VERSION_INFO) {
		strncpy(cmd->resp.cap.version_str, (char *)(na + 1), 256);
	}
	if (na->nla_type == XNL_ATTR_DEVICE_TYPE) {
		strncpy(cmd->resp.cap.version_str, (char *)(na + 1), 256);
	}
	if (na->nla_type == XNL_ATTR_IP_TYPE) {
		strncpy(cmd->resp.cap.version_str, (char *)(na + 1), 256);
	}
}

void xnl_parse_reg_attrs(uint32_t *attrs, struct cmd_info *cmd)
{
	cmd->req.reg.val = attrs[XNL_ATTR_REG_VAL];
}

static void xnl_parse_q_state_attrs(uint32_t *attrs, struct cmd_info *cmd)
{
	cmd->resp.q_info.flags = attrs[XNL_ATTR_QFLAG];
	cmd->resp.q_info.qidx = attrs[XNL_ATTR_QIDX];
	cmd->resp.q_info.state = attrs[XNL_ATTR_Q_STATE];
}

static void xnl_parse_csr_attrs(struct xnl_hdr *hdr, uint32_t *attrs, struct cmd_info *cmd)
{
	unsigned char *p = (unsigned char *)(hdr + 1);
	struct nlattr *na = (struct nlattr *)p;

	if (na->nla_type == XNL_ATTR_GLOBAL_CSR) {
		memcpy(&cmd->resp.csr,(void *) (na + 1),
			   sizeof(struct global_csr_conf));

	}

}

static void print_repeated_reg(uint32_t *bar, struct xreg_info *xreg,
		unsigned start, unsigned limit, struct xnl_dev_info *dev_info,
		struct cmd_info *cmd, void (*log_reg)(const char *))
{
	int i;
	int end = start + limit;
	int step = xreg->step ? xreg->step : 4;
	uint32_t val;
	int32_t rv = 0;
	char reg_dump[100];

	for (i = start; i < end; i++) {
		uint32_t addr = xreg->addr + (i * step);
		char name[40];
		snprintf(name, 40, "%s_%d",
				xreg->name, i);

		if (cmd == NULL) {
			val = le32toh(bar[addr / 4]);
		} else {
			cmd->req.reg.reg = addr;
			rv = reg_read_mmap(dev_info, cmd->req.reg.bar, cmd);
			if (rv < 0) {
				snprintf(reg_dump, 100, "\n");
				if (log_reg)
					log_reg(reg_dump);
				continue;
			}
		}
		snprintf(reg_dump, 100, "[%#7x] %-47s %#-10x %u\n",
			addr, name, val, val);
		if (log_reg)
			log_reg(reg_dump);
	}
}

static int recv_attrs(struct xnl_hdr *hdr, struct cmd_info *cmd,uint32_t *attrs){
	unsigned char *p = (unsigned char *)(hdr + 1);
	int maxlen = hdr->n.nlmsg_len - NLMSG_LENGTH(GENL_HDRLEN);
	
	while (maxlen > 0) {
		struct nlattr *na = (struct nlattr *)p;
		int len = NLA_ALIGN(na->nla_len);

		if (na->nla_type >= XNL_ATTR_MAX) {
			void (*log_err)(const char *) = cmd->log_msg_dump;
			char err_msg[100] = {'\0'};

			snprintf(err_msg, 100,
				 "unknown attr type %d, len %d.\n",
				 na->nla_type, na->nla_len);
			if (log_err)
				log_err(err_msg);
			return -EINVAL;
		}

		if (na->nla_type == XNL_ATTR_GENMSG) {  //general info, print recv data
			if (cmd->log_msg_dump)
				cmd->log_msg_dump((const char *)(na + 1));
		} else {
			attrs[na->nla_type] = *(uint32_t *)(na + 1);
		}

		p += len;
		maxlen -= len;
	}
	

	return 0;
}

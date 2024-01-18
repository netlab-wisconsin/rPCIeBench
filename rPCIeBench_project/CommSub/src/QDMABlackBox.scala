package CommSub

import chisel3._
import chisel3.util._

class QDMABlackBox(VIVADO_VERSION:String) extends BlackBox{
	val io = IO(new Bundle{
		val sys_rst_n 					= Input(Bool())
		val sys_clk 					= Input(Clock())
		val sys_clk_gt 					= Input(Clock())

		val pci_exp_txn					= Output(UInt(16.W))
		val pci_exp_txp					= Output(UInt(16.W))
		val pci_exp_rxn					= Input(UInt(16.W))
		val pci_exp_rxp					= Input(UInt(16.W))

		val m_axib_awid					= Output(UInt(4.W))
		val m_axib_awaddr				= Output(UInt(64.W))
		val m_axib_awlen				= Output(UInt(8.W))
		val m_axib_awsize				= Output(UInt(3.W))
		val m_axib_awburst				= Output(UInt(2.W))
		val m_axib_awprot				= Output(UInt(3.W))
		val m_axib_awlock				= Output(UInt(1.W))
		val m_axib_awcache				= Output(UInt(4.W))
		val m_axib_awvalid				= Output(UInt(1.W))
		val m_axib_awready				= Input(UInt(1.W))

		val m_axib_wdata				= Output(UInt(512.W))
		val m_axib_wstrb				= Output(UInt(64.W))
		val m_axib_wlast				= Output(UInt(1.W))
		val m_axib_wvalid				= Output(UInt(1.W))
		val m_axib_wready				= Input(UInt(1.W))

		val m_axib_bid					= Input(UInt(4.W))
		val m_axib_bresp				= Input(UInt(2.W))
		val m_axib_bvalid				= Input(UInt(1.W))
		val m_axib_bready				= Output(UInt(1.W))

		val m_axib_arid					= Output(UInt(4.W))
		val m_axib_araddr				= Output(UInt(64.W))
		val m_axib_arlen				= Output(UInt(8.W))
		val m_axib_arsize				= Output(UInt(3.W))
		val m_axib_arburst				= Output(UInt(2.W))
		val m_axib_arprot				= Output(UInt(3.W))
		val m_axib_arlock				= Output(UInt(1.W))
		val m_axib_arcache				= Output(UInt(4.W))
		val m_axib_arvalid				= Output(UInt(1.W))
		val m_axib_arready				= Input(UInt(1.W))

		val m_axib_rid					= Input(UInt(4.W))
		val m_axib_rdata				= Input(UInt(512.W))
		val m_axib_rresp				= Input(UInt(2.W))
		val m_axib_rlast				= Input(UInt(1.W))
		val m_axib_rvalid				= Input(UInt(1.W))
		val m_axib_rready				= Output(UInt(1.W))


		val m_axil_awaddr				= Output(UInt(32.W))
		val m_axil_awvalid				= Output(UInt(1.W))
		val m_axil_awready				= Input(UInt(1.W))

		val m_axil_wdata				= Output(UInt(32.W))
		val m_axil_wstrb				= Output(UInt(4.W))
		val m_axil_wvalid				= Output(UInt(1.W))
		val m_axil_wready				= Input(UInt(1.W))

		val m_axil_bresp				= Input(UInt(2.W))
		val m_axil_bvalid				= Input(UInt(1.W))
		val m_axil_bready				= Output(UInt(1.W))

		val m_axil_araddr				= Output(UInt(32.W))
		val m_axil_arvalid				= Output(UInt(1.W))
		val m_axil_arready				= Input(UInt(1.W))

		val m_axil_rdata				= Input(UInt(32.W))
		val m_axil_rresp				= Input(UInt(2.W))
		val m_axil_rvalid				= Input(UInt(1.W))
		val m_axil_rready				= Output(UInt(1.W))

		val axi_aclk					= Output(Clock())
		val axi_aresetn					= Output(Bool())
		val soft_reset_n				= Input(Bool())

		val h2c_byp_in_st_addr			= Input(UInt(64.W))
		val h2c_byp_in_st_len			= Input(UInt(32.W))
		val h2c_byp_in_st_eop			= Input(UInt(1.W))
		val h2c_byp_in_st_sop			= Input(UInt(1.W))
		val h2c_byp_in_st_mrkr_req		= Input(UInt(1.W))
		val h2c_byp_in_st_sdi			= Input(UInt(1.W))
		val h2c_byp_in_st_qid			= Input(UInt(11.W))
		val h2c_byp_in_st_error			= Input(UInt(1.W))
		val h2c_byp_in_st_func			= Input(UInt(8.W))
		val h2c_byp_in_st_cidx			= Input(UInt(16.W))
		val h2c_byp_in_st_port_id		= Input(UInt(3.W))
		val h2c_byp_in_st_no_dma		= Input(UInt(1.W))
		val h2c_byp_in_st_vld			= Input(UInt(1.W))
		val h2c_byp_in_st_rdy			= Output(UInt(1.W))

		val c2h_byp_in_st_csh_addr		= Input(UInt(64.W))
		val c2h_byp_in_st_csh_qid		= Input(UInt(11.W))
		val c2h_byp_in_st_csh_error		= Input(UInt(1.W))
		val c2h_byp_in_st_csh_func		= Input(UInt(8.W))
		val c2h_byp_in_st_csh_port_id	= Input(UInt(3.W))
		val c2h_byp_in_st_csh_pfch_tag	= Input(UInt(7.W))
		val c2h_byp_in_st_csh_vld		= Input(UInt(1.W))
		val c2h_byp_in_st_csh_rdy		= Output(UInt(1.W))

		val s_axis_c2h_tdata			= Input(UInt(512.W))
		val s_axis_c2h_tcrc				= Input(UInt(32.W))
		val s_axis_c2h_ctrl_marker		= Input(UInt(1.W))
		val s_axis_c2h_ctrl_ecc			= Input(UInt(7.W))
		val s_axis_c2h_ctrl_len			= Input(UInt(32.W))
		val s_axis_c2h_ctrl_port_id		= Input(UInt(3.W))
		val s_axis_c2h_ctrl_qid			= Input(UInt(11.W))
		val s_axis_c2h_ctrl_has_cmpt	= Input(UInt(1.W))
		val s_axis_c2h_mty				= Input(UInt(6.W))
		val s_axis_c2h_tlast			= Input(UInt(1.W))
		val s_axis_c2h_tvalid			= Input(UInt(1.W))
		val s_axis_c2h_tready			= Output(UInt(1.W))

		
		val m_axis_h2c_tdata			= Output(UInt(512.W))
		val m_axis_h2c_tcrc				= Output(UInt(32.W))
		val m_axis_h2c_tuser_qid		= Output(UInt(11.W))
		val m_axis_h2c_tuser_port_id	= Output(UInt(3.W))
		val m_axis_h2c_tuser_err		= Output(UInt(1.W))
		val m_axis_h2c_tuser_mdata		= Output(UInt(32.W))
		val m_axis_h2c_tuser_mty		= Output(UInt(6.W))
		val m_axis_h2c_tuser_zero_byte	= Output(UInt(1.W))
		val m_axis_h2c_tlast			= Output(UInt(1.W))
		val m_axis_h2c_tvalid			= Output(UInt(1.W))
		val m_axis_h2c_tready			= Input(UInt(1.W))

		val axis_c2h_status_drop		= Output(UInt(1.W))
		val axis_c2h_status_last		= Output(UInt(1.W))
		val axis_c2h_status_cmp			= Output(UInt(1.W))
		val axis_c2h_status_valid		= Output(UInt(1.W))
		val axis_c2h_status_error		= Output(UInt(1.W))
		val axis_c2h_status_qid			= Output(UInt(11.W))

		val s_axis_c2h_cmpt_tdata					= Input(UInt(512.W))
		val s_axis_c2h_cmpt_size					= Input(UInt(2.W))
		val s_axis_c2h_cmpt_dpar					= Input(UInt(16.W))
		val s_axis_c2h_cmpt_tvalid					= Input(UInt(1.W))
		val s_axis_c2h_cmpt_tready					= Output(UInt(1.W))
		val s_axis_c2h_cmpt_ctrl_qid				= Input(UInt(11.W))
		val s_axis_c2h_cmpt_ctrl_cmpt_type			= Input(UInt(2.W))
		val s_axis_c2h_cmpt_ctrl_wait_pld_pkt_id	= Input(UInt(16.W))
		val s_axis_c2h_cmpt_ctrl_no_wrb_marker		= if(VIVADO_VERSION=="202101" || VIVADO_VERSION=="202002") Some(Input(UInt(1.W))) else None
		val s_axis_c2h_cmpt_ctrl_port_id			= Input(UInt(3.W))
		val s_axis_c2h_cmpt_ctrl_marker				= Input(UInt(1.W))
		val s_axis_c2h_cmpt_ctrl_user_trig			= Input(UInt(1.W))
		val s_axis_c2h_cmpt_ctrl_col_idx			= Input(UInt(3.W))
		val s_axis_c2h_cmpt_ctrl_err_idx			= Input(UInt(3.W))

		//ignore other
		val h2c_byp_out_rdy							= Input(UInt(1.W))

		//ignore other
		val c2h_byp_out_rdy							= Input(UInt(1.W))

		//ignore other
		val tm_dsc_sts_rdy							= Input(UInt(1.W))

		val dsc_crdt_in_vld							= Input(UInt(1.W))
		val dsc_crdt_in_rdy							= Output(UInt(1.W))
		val dsc_crdt_in_dir							= Input(UInt(1.W))
		val dsc_crdt_in_fence						= Input(UInt(1.W))
		val dsc_crdt_in_qid							= Input(UInt(11.W))
		val dsc_crdt_in_crdt						= Input(UInt(16.W))

		//ignore other
		val qsts_out_rdy							= Input(UInt(1.W))

		val usr_irq_in_vld							= Input(UInt(1.W))
		val usr_irq_in_vec							= Input(UInt(11.W))
		val usr_irq_in_fnc							= Input(UInt(8.W))
		val usr_irq_out_ack							= Output(UInt(1.W))
		val usr_irq_out_fail						= Output(UInt(1.W))

	})
}
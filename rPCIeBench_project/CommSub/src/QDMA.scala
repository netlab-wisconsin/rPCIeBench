package CommSub

import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero


class AXIL extends AXI(
	ADDR_WIDTH=32,
	DATA_WIDTH=32,
	ID_WIDTH=0,
	USER_WIDTH=0,
	LEN_WIDTH=0,
){
}

class AXIB extends AXI(
	ADDR_WIDTH=64,
	DATA_WIDTH=512,
	ID_WIDTH=4,
	USER_WIDTH=0,
	LEN_WIDTH=8,
){
}

class QDMA(VIVADO_VERSION:String) extends RawModule{
	require(VIVADO_VERSION == "202001" || VIVADO_VERSION == "202002" || VIVADO_VERSION == "202101")
	def getTCL() = {
		val s1 = "\ncreate_ip -name qdma -vendor xilinx.com -library ip -version 4.0 -module_name QDMABlackBox\n"
		val s2 = 	if (VIVADO_VERSION=="202001") 	"set_property -dict [list CONFIG.Component_Name {QDMABlackBox} CONFIG.axist_bypass_en {true} CONFIG.pcie_extended_tag {false} CONFIG.dsc_bypass_rd {true} CONFIG.dsc_bypass_wr {true} CONFIG.cfg_mgmt_if {false} CONFIG.testname {st} CONFIG.pf0_bar4_enabled_qdma {true} CONFIG.pf0_bar4_64bit_qdma {true} CONFIG.pf0_bar4_scale_qdma {Gigabytes} CONFIG.pf0_bar4_size_qdma {1} CONFIG.pf1_bar4_enabled_qdma {true} CONFIG.pf1_bar4_64bit_qdma {true} CONFIG.pf1_bar4_scale_qdma {Gigabytes} CONFIG.pf1_bar4_size_qdma {1} CONFIG.pf2_bar4_enabled_qdma {true} CONFIG.pf2_bar4_64bit_qdma {true} CONFIG.pf2_bar4_scale_qdma {Gigabytes} CONFIG.pf2_bar4_size_qdma {1} CONFIG.pf3_bar4_enabled_qdma {true} CONFIG.pf3_bar4_64bit_qdma {true} CONFIG.pf3_bar4_scale_qdma {Gigabytes} CONFIG.pf3_bar4_size_qdma {1} CONFIG.dma_intf_sel_qdma {AXI_Stream_with_Completion} CONFIG.en_axi_mm_qdma {false}] [get_ips QDMABlackBox]"
					else 							"set_property -dict [list CONFIG.Component_Name {QDMABlackBox} CONFIG.axist_bypass_en {true} CONFIG.pcie_extended_tag {false} CONFIG.dsc_byp_mode {Descriptor_bypass_and_internal} CONFIG.cfg_mgmt_if {false} CONFIG.testname {st} CONFIG.pf0_bar4_enabled_qdma {true} CONFIG.pf0_bar4_64bit_qdma {true} CONFIG.pf0_bar4_scale_qdma {Gigabytes} CONFIG.pf0_bar4_size_qdma {1} CONFIG.pf1_bar4_enabled_qdma {true} CONFIG.pf1_bar4_64bit_qdma {true} CONFIG.pf1_bar4_scale_qdma {Gigabytes} CONFIG.pf1_bar4_size_qdma {1} CONFIG.pf2_bar4_enabled_qdma {true} CONFIG.pf2_bar4_64bit_qdma {true} CONFIG.pf2_bar4_scale_qdma {Gigabytes} CONFIG.pf2_bar4_size_qdma {1} CONFIG.pf3_bar4_enabled_qdma {true} CONFIG.pf3_bar4_64bit_qdma {true} CONFIG.pf3_bar4_scale_qdma {Gigabytes} CONFIG.pf3_bar4_size_qdma {1} CONFIG.dma_intf_sel_qdma {AXI_Stream_with_Completion} CONFIG.en_axi_mm_qdma {false}] [get_ips QDMABlackBox]\n"
		val s4 = "update_compile_order -fileset sources_1\n"
		println(s1 + s2 + s4)
	}
	val io = IO(new Bundle{
		val pin		= new QDMAPin

		val pcie_clk 	= Output(Clock())
		val pcie_arstn	= Output(Bool())

		val user_clk	= Input(Clock())
		val user_arstn	= Input(Bool())

		val h2c_cmd		= Flipped(Decoupled(new H2C_CMD))
		val h2c_data	= Decoupled(new H2C_DATA)
		val c2h_cmd		= Flipped(Decoupled(new C2H_CMD))
		val c2h_data	= Flipped(Decoupled(new C2H_DATA))

		val reg_control = Output(Vec(512,UInt(32.W)))
		val reg_status	= Input(Vec(512,UInt(32.W)))

		val axib 		= new AXIB

		val c2h_status_last		= Output(UInt(1.W))
		val c2h_status_cmp		= Output(UInt(1.W))
		val c2h_status_valid	= Output(UInt(1.W))
		val c2h_status_error	= Output(UInt(1.W))
		val c2h_status_drop		= Output(UInt(1.W))
		val tlb_miss_count		= Output(UInt(32.W))
	})
 

	val sw_reset = io.reg_control(14) === 1.U

	val perst_n = IBUF(io.pin.sys_rst_n)

	val ibufds_gte4_inst = Module(new IBUFDS_GTE4(REFCLK_HROW_CK_SEL=0))
	ibufds_gte4_inst.io.IB		:= io.pin.sys_clk_n
	ibufds_gte4_inst.io.I		:= io.pin.sys_clk_p
	ibufds_gte4_inst.io.CEB		:= 0.U
	val pcie_ref_clk_gt			= ibufds_gte4_inst.io.O
	val pcie_ref_clk			= ibufds_gte4_inst.io.ODIV2

	val fifo_h2c_data		= XConverter(new H2C_DATA, io.pcie_clk, io.pcie_arstn, io.user_clk)
	fifo_h2c_data.io.out	<> io.h2c_data

	val fifo_c2h_data		= XConverter(new C2H_DATA, io.user_clk, io.user_arstn, io.pcie_clk)
	val fifo_h2c_cmd		= XConverter(new H2C_CMD, io.user_clk, io.user_arstn, io.pcie_clk)
	val fifo_c2h_cmd		= XConverter(new C2H_CMD, io.user_clk, io.user_arstn, io.pcie_clk)

	val check_c2h			= withClockAndReset(io.user_clk,!io.user_arstn){Module(new CMDBoundaryCheck(new C2H_CMD, 0x200000, 0x1000))}//(31*128 Byte)
	check_c2h.io.in			<> io.c2h_cmd
	val check_h2c			= withClockAndReset(io.user_clk,!io.user_arstn){Module(new CMDBoundaryCheck(new H2C_CMD, 0x200000, 0x8000))}
	check_h2c.io.in			<> io.h2c_cmd

	val tlb			= withClockAndReset(io.user_clk,!io.user_arstn){Module(new TLB)}
	tlb.io.h2c_in	<> check_h2c.io.out
	tlb.io.c2h_in	<> check_c2h.io.out
	tlb.io.h2c_out	<> fifo_h2c_cmd.io.in


	val fifo_wr_tlb		= XConverter(new WR_TLB, io.pcie_clk, io.pcie_arstn, io.user_clk)
	fifo_wr_tlb.io.in.bits.is_base		:= io.reg_control(12)(0)
	fifo_wr_tlb.io.in.bits.paddr_high	:= io.reg_control(11)
	fifo_wr_tlb.io.in.bits.paddr_low	:= io.reg_control(10)
	fifo_wr_tlb.io.in.bits.vaddr_high	:= io.reg_control(9)
	fifo_wr_tlb.io.in.bits.vaddr_low	:= io.reg_control(8)

	fifo_wr_tlb.io.in.valid		:=  withClockAndReset(io.pcie_clk,!io.pcie_arstn)(RegNext(!RegNext(io.reg_control(13)(0)) & io.reg_control(13)(0)))

	tlb.io.wr_tlb.bits 			:= fifo_wr_tlb.io.out.bits
	tlb.io.wr_tlb.valid 		:= fifo_wr_tlb.io.out.valid
	fifo_wr_tlb.io.out.ready	:= 1.U

	val axil2reg = withClockAndReset(io.pcie_clk,!io.pcie_arstn){Module(new PoorAXIL2Reg(new AXIL, 512, 32))}

	axil2reg.io.reg_status	<> io.reg_status
	axil2reg.io.reg_control <> io.reg_control
	io.tlb_miss_count		<> tlb.io.tlb_miss_count
	
	val axib = io.axib
	ToZero(axib.ar.bits)
	ToZero(axib.aw.bits)
	ToZero(axib.w.bits)

	val axil = axil2reg.io.axi
	ToZero(axil.ar.bits)
	ToZero(axil.aw.bits)
	ToZero(axil.w.bits)
	axil.w.bits.last	:= 1.U

	//all refer to c2h
	val boundary_split			= withClockAndReset(io.user_clk,!io.user_arstn){Module(new DataBoundarySplit)}
	boundary_split.io.cmd_in	<> tlb.io.c2h_out
	boundary_split.io.data_in	<> io.c2h_data
	boundary_split.io.cmd_out	<> fifo_c2h_cmd.io.in
	boundary_split.io.data_out	<> fifo_c2h_data.io.in
	boundary_split.io.debug_clk := io.user_clk
	
	val qdma_inst = Module(new QDMABlackBox(VIVADO_VERSION))
	qdma_inst.io.sys_rst_n				:= perst_n
	qdma_inst.io.sys_clk				:= pcie_ref_clk
	qdma_inst.io.sys_clk_gt				:= pcie_ref_clk_gt

	qdma_inst.io.pci_exp_txn			<> io.pin.tx_n
	qdma_inst.io.pci_exp_txp			<> io.pin.tx_p
	qdma_inst.io.pci_exp_rxn			:= io.pin.rx_n
	qdma_inst.io.pci_exp_rxp			:= io.pin.rx_p

	qdma_inst.io.axi_aclk				<> io.pcie_clk
	qdma_inst.io.axi_aresetn			<> io.pcie_arstn
	qdma_inst.io.soft_reset_n			:= 1.U

	//h2c cmd
	qdma_inst.io.h2c_byp_in_st_addr		:= fifo_h2c_cmd.io.out.bits.addr
	qdma_inst.io.h2c_byp_in_st_len		:= fifo_h2c_cmd.io.out.bits.len
	qdma_inst.io.h2c_byp_in_st_eop		:= fifo_h2c_cmd.io.out.bits.eop
	qdma_inst.io.h2c_byp_in_st_sop		:= fifo_h2c_cmd.io.out.bits.sop
	qdma_inst.io.h2c_byp_in_st_mrkr_req	:= fifo_h2c_cmd.io.out.bits.mrkr_req
	qdma_inst.io.h2c_byp_in_st_sdi		:= fifo_h2c_cmd.io.out.bits.sdi
	qdma_inst.io.h2c_byp_in_st_qid		:= fifo_h2c_cmd.io.out.bits.qid
	qdma_inst.io.h2c_byp_in_st_error	:= fifo_h2c_cmd.io.out.bits.error
	qdma_inst.io.h2c_byp_in_st_func		:= fifo_h2c_cmd.io.out.bits.func
	qdma_inst.io.h2c_byp_in_st_cidx		:= fifo_h2c_cmd.io.out.bits.cidx
	qdma_inst.io.h2c_byp_in_st_port_id	:= fifo_h2c_cmd.io.out.bits.port_id
	qdma_inst.io.h2c_byp_in_st_no_dma	:= fifo_h2c_cmd.io.out.bits.no_dma
	qdma_inst.io.h2c_byp_in_st_vld		:= fifo_h2c_cmd.io.out.valid
	qdma_inst.io.h2c_byp_in_st_rdy		<> fifo_h2c_cmd.io.out.ready

	//c2h cmd
	qdma_inst.io.c2h_byp_in_st_csh_addr		:= fifo_c2h_cmd.io.out.bits.addr
	qdma_inst.io.c2h_byp_in_st_csh_qid		:= fifo_c2h_cmd.io.out.bits.qid
	qdma_inst.io.c2h_byp_in_st_csh_error	:= fifo_c2h_cmd.io.out.bits.error
	qdma_inst.io.c2h_byp_in_st_csh_func		:= fifo_c2h_cmd.io.out.bits.func
	qdma_inst.io.c2h_byp_in_st_csh_port_id	:= fifo_c2h_cmd.io.out.bits.port_id
	qdma_inst.io.c2h_byp_in_st_csh_pfch_tag	:= fifo_c2h_cmd.io.out.bits.pfch_tag
	qdma_inst.io.c2h_byp_in_st_csh_vld		:= fifo_c2h_cmd.io.out.valid
	qdma_inst.io.c2h_byp_in_st_csh_rdy		<> fifo_c2h_cmd.io.out.ready

	//c2h data
	qdma_inst.io.s_axis_c2h_tdata			:= fifo_c2h_data.io.out.bits.data
	qdma_inst.io.s_axis_c2h_tcrc			:= fifo_c2h_data.io.out.bits.tcrc
	qdma_inst.io.s_axis_c2h_ctrl_marker		:= fifo_c2h_data.io.out.bits.ctrl_marker
	qdma_inst.io.s_axis_c2h_ctrl_ecc		:= fifo_c2h_data.io.out.bits.ctrl_ecc
	qdma_inst.io.s_axis_c2h_ctrl_len		:= fifo_c2h_data.io.out.bits.ctrl_len
	qdma_inst.io.s_axis_c2h_ctrl_port_id	:= fifo_c2h_data.io.out.bits.ctrl_port_id
	qdma_inst.io.s_axis_c2h_ctrl_qid		:= fifo_c2h_data.io.out.bits.ctrl_qid
	qdma_inst.io.s_axis_c2h_ctrl_has_cmpt	:= fifo_c2h_data.io.out.bits.ctrl_has_cmpt
	qdma_inst.io.s_axis_c2h_mty				:= fifo_c2h_data.io.out.bits.mty
	qdma_inst.io.s_axis_c2h_tlast			:= fifo_c2h_data.io.out.bits.last
	qdma_inst.io.s_axis_c2h_tvalid			:= fifo_c2h_data.io.out.valid
	qdma_inst.io.s_axis_c2h_tready			<> fifo_c2h_data.io.out.ready

	//c2h status
	qdma_inst.io.axis_c2h_status_last		<> io.c2h_status_last
	qdma_inst.io.axis_c2h_status_cmp		<> io.c2h_status_cmp
	qdma_inst.io.axis_c2h_status_valid		<> io.c2h_status_valid
	qdma_inst.io.axis_c2h_status_error		<> io.c2h_status_error
	qdma_inst.io.axis_c2h_status_drop		<> io.c2h_status_drop

	//h2c data
	qdma_inst.io.m_axis_h2c_tdata			<> fifo_h2c_data.io.in.bits.data
	qdma_inst.io.m_axis_h2c_tcrc			<> fifo_h2c_data.io.in.bits.tcrc
	qdma_inst.io.m_axis_h2c_tuser_qid		<> fifo_h2c_data.io.in.bits.tuser_qid
	qdma_inst.io.m_axis_h2c_tuser_port_id	<> fifo_h2c_data.io.in.bits.tuser_port_id
	qdma_inst.io.m_axis_h2c_tuser_err		<> fifo_h2c_data.io.in.bits.tuser_err
	qdma_inst.io.m_axis_h2c_tuser_mdata		<> fifo_h2c_data.io.in.bits.tuser_mdata
	qdma_inst.io.m_axis_h2c_tuser_mty		<> fifo_h2c_data.io.in.bits.tuser_mty
	qdma_inst.io.m_axis_h2c_tuser_zero_byte	<> fifo_h2c_data.io.in.bits.tuser_zero_byte
	qdma_inst.io.m_axis_h2c_tlast			<> fifo_h2c_data.io.in.bits.last
	qdma_inst.io.m_axis_h2c_tvalid			<> fifo_h2c_data.io.in.valid
	qdma_inst.io.m_axis_h2c_tready			:= fifo_h2c_data.io.in.ready

	qdma_inst.io.m_axib_awid				<> axib.aw.bits.id
	qdma_inst.io.m_axib_awaddr				<> axib.aw.bits.addr
	qdma_inst.io.m_axib_awlen				<> axib.aw.bits.len
	qdma_inst.io.m_axib_awsize				<> axib.aw.bits.size
	qdma_inst.io.m_axib_awburst				<> axib.aw.bits.burst
	qdma_inst.io.m_axib_awprot				<> axib.aw.bits.prot
	qdma_inst.io.m_axib_awlock				<> axib.aw.bits.lock
	qdma_inst.io.m_axib_awcache				<> axib.aw.bits.cache
	qdma_inst.io.m_axib_awvalid				<> axib.aw.valid
	qdma_inst.io.m_axib_awready				<> axib.aw.ready

	qdma_inst.io.m_axib_wdata				<> axib.w.bits.data
	qdma_inst.io.m_axib_wstrb				<> axib.w.bits.strb
	qdma_inst.io.m_axib_wlast				<> axib.w.bits.last
	qdma_inst.io.m_axib_wvalid				<> axib.w.valid
	qdma_inst.io.m_axib_wready				<> axib.w.ready

	qdma_inst.io.m_axib_bid					<> axib.b.bits.id
	qdma_inst.io.m_axib_bresp				<> axib.b.bits.resp
	qdma_inst.io.m_axib_bvalid				<> axib.b.valid
	qdma_inst.io.m_axib_bready				<> axib.b.ready

	qdma_inst.io.m_axib_arid				<> axib.ar.bits.id
	qdma_inst.io.m_axib_araddr				<> axib.ar.bits.addr
	qdma_inst.io.m_axib_arlen				<> axib.ar.bits.len
	qdma_inst.io.m_axib_arsize				<> axib.ar.bits.size
	qdma_inst.io.m_axib_arburst				<> axib.ar.bits.burst
	qdma_inst.io.m_axib_arprot				<> axib.ar.bits.prot
	qdma_inst.io.m_axib_arlock				<> axib.ar.bits.lock
	qdma_inst.io.m_axib_arcache				<> axib.ar.bits.cache
	qdma_inst.io.m_axib_arvalid				<> axib.ar.valid
	qdma_inst.io.m_axib_arready				<> axib.ar.ready

	qdma_inst.io.m_axib_rid					<> axib.r.bits.id
	qdma_inst.io.m_axib_rdata				<> axib.r.bits.data
	qdma_inst.io.m_axib_rresp				<> axib.r.bits.resp
	qdma_inst.io.m_axib_rlast				<> axib.r.bits.last
	qdma_inst.io.m_axib_rvalid				<> axib.r.valid
	qdma_inst.io.m_axib_rready				<> axib.r.ready


	qdma_inst.io.m_axil_awaddr				<> axil.aw.bits.addr
	qdma_inst.io.m_axil_awvalid				<> axil.aw.valid
	qdma_inst.io.m_axil_awready				<> axil.aw.ready

	qdma_inst.io.m_axil_wdata				<> axil.w.bits.data
	qdma_inst.io.m_axil_wstrb				<> axil.w.bits.strb
	qdma_inst.io.m_axil_wvalid				<> axil.w.valid
	qdma_inst.io.m_axil_wready				<> axil.w.ready

	qdma_inst.io.m_axil_bresp				<> axil.b.bits.resp
	qdma_inst.io.m_axil_bvalid				<> axil.b.valid
	qdma_inst.io.m_axil_bready				<> axil.b.ready

	qdma_inst.io.m_axil_araddr				<> axil.ar.bits.addr
	qdma_inst.io.m_axil_arvalid				<> axil.ar.valid
	qdma_inst.io.m_axil_arready				<> axil.ar.ready

	qdma_inst.io.m_axil_rdata				<> axil.r.bits.data
	qdma_inst.io.m_axil_rresp				<> axil.r.bits.resp
	qdma_inst.io.m_axil_rvalid				<> axil.r.valid
	qdma_inst.io.m_axil_rready				<> axil.r.ready

val c2h_counter = withClockAndReset(io.user_clk,!io.user_arstn){Module(new c2hcounter())}
c2h_counter.io.fire1 := io.c2h_cmd.fire()
c2h_counter.io.fire2 := tlb.io.c2h_in.fire()
c2h_counter.io.fire3 := boundary_split.io.cmd_in.fire()
c2h_counter.io.fire4 := fifo_c2h_cmd.io.in.fire()

c2h_counter.io.fire5 := io.c2h_data.fire()
c2h_counter.io.fire6 := fifo_c2h_data.io.in.fire()

// class ila_qdma1(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_qdma1 = Module(new ila_qdma1(Seq(	
	
// 	qdma_inst.io.c2h_byp_in_st_csh_addr,
// 	qdma_inst.io.c2h_byp_in_st_csh_pfch_tag,

// 	qdma_inst.io.s_axis_c2h_tdata,
// 	qdma_inst.io.s_axis_c2h_ctrl_len,
// 	qdma_inst.io.s_axis_c2h_tlast,

// )))
// inst_ila_qdma1.connect(io.pcie_clk)

// class ila_qdma(seq:Seq[Data]) extends BaseILA(seq)
// val inst_qdma = Module(new ila_qdma(Seq(	

// 	fifo_c2h_data.io.out.bits.data,
// 	fifo_c2h_data.io.out.fire(),
// 	fifo_c2h_cmd.io.out.bits,
// 	fifo_c2h_cmd.io.out.fire(),

// )))
// inst_qdma.connect(io.pcie_clk)


	qdma_inst.io.s_axis_c2h_cmpt_tdata					:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_size					:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_dpar					:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_tvalid					:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_qid				:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_cmpt_type			:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_wait_pld_pkt_id	:= 0.U
	if(qdma_inst.io.s_axis_c2h_cmpt_ctrl_no_wrb_marker != None){
		qdma_inst.io.s_axis_c2h_cmpt_ctrl_no_wrb_marker.get		:= 0.U	
	}
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_port_id			:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_marker			:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_user_trig			:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_col_idx			:= 0.U
	qdma_inst.io.s_axis_c2h_cmpt_ctrl_err_idx			:= 0.U

	qdma_inst.io.h2c_byp_out_rdy			:= 1.U
	qdma_inst.io.c2h_byp_out_rdy			:= 1.U
	qdma_inst.io.tm_dsc_sts_rdy				:= 1.U
	
	qdma_inst.io.dsc_crdt_in_vld				:= 0.U
	qdma_inst.io.dsc_crdt_in_dir				:= 0.U
	qdma_inst.io.dsc_crdt_in_fence				:= 0.U
	qdma_inst.io.dsc_crdt_in_qid				:= 0.U
	qdma_inst.io.dsc_crdt_in_crdt				:= 0.U
	
	qdma_inst.io.qsts_out_rdy					:= 1.U
	
	qdma_inst.io.usr_irq_in_vld					:= 0.U
	qdma_inst.io.usr_irq_in_vec					:= 0.U
	qdma_inst.io.usr_irq_in_fnc					:= 0.U

}
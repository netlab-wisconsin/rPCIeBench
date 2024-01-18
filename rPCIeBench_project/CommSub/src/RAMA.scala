package CommSub

import chisel3._
import chisel3.util._
import common.axi._

class RAMA(ADDR_WIDTH:Int=33, ID_WIDTH:Int=6) extends Module{
	def getTCL(path:String) = {
		val s1 = "\ncreate_ip -name rama -vendor xilinx.com -library ip -version 1.1 -module_name RAMABlackBox\n"
		val s2 = f"set_property -dict [list CONFIG.ID_WIDTH {${ID_WIDTH}} CONFIG.ADDR_WIDTH {${ADDR_WIDTH}}] [get_ips RAMABlackBox]\n"
		val s3 = "generate_target {instantiation_template} [get_files %s/RAMABlackBox/RAMABlackBox.xci]\n"
		val s4 = "update_compile_order -fileset sources_1\n"
		println("\nThe tcl below is used to generate RAMA IP:\n" + s1 + s2 + s3.format(path) + s4)
	}
	
	val io = IO(new Bundle{
		val s_axi = Flipped(new AXI(ADDR_WIDTH, 256, ID_WIDTH, 0, 7))
		val m_axi = (new AXI(ADDR_WIDTH, 256, ID_WIDTH, 0, 7))
	})

	val instRama=Module(new RAMABlackBox(ADDR_WIDTH=ADDR_WIDTH, ID_WIDTH=ID_WIDTH))
	instRama.io.axi_aclk		:= clock
	instRama.io.axi_aresetn		:= !(reset.asBool)

	instRama.io.s_axi_araddr    <> io.s_axi.ar.bits.addr
	instRama.io.s_axi_arburst   <> io.s_axi.ar.bits.burst
	instRama.io.s_axi_arid      <> io.s_axi.ar.bits.id
	instRama.io.s_axi_arlen     <> io.s_axi.ar.bits.len
	instRama.io.s_axi_arsize    <> io.s_axi.ar.bits.size
	instRama.io.s_axi_arvalid   <> io.s_axi.ar.valid
	instRama.io.s_axi_arready   <> io.s_axi.ar.ready
	instRama.io.s_axi_awaddr    <> io.s_axi.aw.bits.addr
	instRama.io.s_axi_awburst   <> io.s_axi.aw.bits.burst
	instRama.io.s_axi_awid      <> io.s_axi.aw.bits.id
	instRama.io.s_axi_awlen     <> io.s_axi.aw.bits.len
	instRama.io.s_axi_awsize    <> io.s_axi.aw.bits.size
	instRama.io.s_axi_awvalid   <> io.s_axi.aw.valid
	instRama.io.s_axi_awready   <> io.s_axi.aw.ready
	instRama.io.s_axi_wdata     <> io.s_axi.w.bits.data
	instRama.io.s_axi_wlast     <> io.s_axi.w.bits.last
	instRama.io.s_axi_wstrb     <> io.s_axi.w.bits.strb
	instRama.io.s_axi_wvalid    <> io.s_axi.w.valid
	instRama.io.s_axi_wready    <> io.s_axi.w.ready
	instRama.io.s_axi_rdata     <> io.s_axi.r.bits.data
	instRama.io.s_axi_rid       <> io.s_axi.r.bits.id
	instRama.io.s_axi_rlast     <> io.s_axi.r.bits.last
	instRama.io.s_axi_rresp     <> io.s_axi.r.bits.resp
	instRama.io.s_axi_rvalid    <> io.s_axi.r.valid
	instRama.io.s_axi_rready    <> io.s_axi.r.ready
	instRama.io.s_axi_bid       <> io.s_axi.b.bits.id
	instRama.io.s_axi_bresp     <> io.s_axi.b.bits.resp
	instRama.io.s_axi_bvalid    <> io.s_axi.b.valid
	instRama.io.s_axi_bready    <> io.s_axi.b.ready

	instRama.io.m_axi_araddr    <> io.m_axi.ar.bits.addr
	instRama.io.m_axi_arburst   <> io.m_axi.ar.bits.burst
	instRama.io.m_axi_arid      <> io.m_axi.ar.bits.id
	instRama.io.m_axi_arlen     <> io.m_axi.ar.bits.len
	instRama.io.m_axi_arsize    <> io.m_axi.ar.bits.size
	instRama.io.m_axi_arvalid   <> io.m_axi.ar.valid
	instRama.io.m_axi_arready   <> io.m_axi.ar.ready
	instRama.io.m_axi_awaddr    <> io.m_axi.aw.bits.addr
	instRama.io.m_axi_awburst   <> io.m_axi.aw.bits.burst
	instRama.io.m_axi_awid      <> io.m_axi.aw.bits.id
	instRama.io.m_axi_awlen     <> io.m_axi.aw.bits.len
	instRama.io.m_axi_awsize    <> io.m_axi.aw.bits.size
	instRama.io.m_axi_awvalid   <> io.m_axi.aw.valid
	instRama.io.m_axi_awready   <> io.m_axi.aw.ready
	instRama.io.m_axi_wdata     <> io.m_axi.w.bits.data
	instRama.io.m_axi_wlast     <> io.m_axi.w.bits.last
	instRama.io.m_axi_wstrb     <> io.m_axi.w.bits.strb
	instRama.io.m_axi_wvalid    <> io.m_axi.w.valid
	instRama.io.m_axi_wready    <> io.m_axi.w.ready
	instRama.io.m_axi_rdata     <> io.m_axi.r.bits.data
	instRama.io.m_axi_rid       <> io.m_axi.r.bits.id
	instRama.io.m_axi_rlast     <> io.m_axi.r.bits.last
	instRama.io.m_axi_rresp     <> io.m_axi.r.bits.resp
	instRama.io.m_axi_rvalid    <> io.m_axi.r.valid
	instRama.io.m_axi_rready    <> io.m_axi.r.ready
	instRama.io.m_axi_bid       <> io.m_axi.b.bits.id
	instRama.io.m_axi_bresp     <> io.m_axi.b.bits.resp
	instRama.io.m_axi_bvalid    <> io.m_axi.b.valid
	instRama.io.m_axi_bready    <> io.m_axi.b.ready

	// Empty bits
	io.m_axi.aw.bits.region	<> DontCare
	io.m_axi.aw.bits.lock	<> DontCare
	io.m_axi.aw.bits.user	<> DontCare
	io.m_axi.aw.bits.prot	<> DontCare
	io.m_axi.aw.bits.cache	<> DontCare
	io.m_axi.aw.bits.qos	<> DontCare
	io.m_axi.ar.bits.region	<> DontCare
	io.m_axi.ar.bits.lock	<> DontCare
	io.m_axi.ar.bits.user	<> DontCare
	io.m_axi.ar.bits.prot	<> DontCare
	io.m_axi.ar.bits.cache	<> DontCare
	io.m_axi.ar.bits.qos	<> DontCare
	io.m_axi.w.bits.user	<> DontCare
	io.s_axi.r.bits.user	<> DontCare
	io.s_axi.b.bits.user	<> DontCare
}

class RAMABlackBox(ADDR_WIDTH:Int, ID_WIDTH:Int) extends BlackBox {
	val io = IO(new Bundle {
		val axi_aclk		= Input(Clock())
		val axi_aresetn 	= Input(Bool())
        val s_axi_araddr  	= Input(UInt(ADDR_WIDTH.W))
        val s_axi_arburst 	= Input(UInt(2.W))
        val s_axi_arid    	= Input(UInt(ID_WIDTH.W))
        val s_axi_arlen   	= Input(UInt(4.W))
        val s_axi_arsize  	= Input(UInt(3.W))
        val s_axi_arvalid 	= Input(UInt(1.W))
        val s_axi_arready 	= Output(UInt(1.W))
        val s_axi_awaddr  	= Input(UInt(ADDR_WIDTH.W))
        val s_axi_awburst 	= Input(UInt(2.W))
        val s_axi_awid    	= Input(UInt(ID_WIDTH.W))
        val s_axi_awlen   	= Input(UInt(4.W))
        val s_axi_awsize  	= Input(UInt(3.W))
        val s_axi_awvalid 	= Input(UInt(1.W))
        val s_axi_awready 	= Output(UInt(1.W))
        val s_axi_wdata   	= Input(UInt(256.W))
        val s_axi_wlast   	= Input(UInt(1.W))
        val s_axi_wstrb   	= Input(UInt(32.W))
        val s_axi_wvalid  	= Input(UInt(1.W))
        val s_axi_wready  	= Output(UInt(1.W))
        val s_axi_rdata   	= Output(UInt(256.W))
        val s_axi_rid     	= Output(UInt(ID_WIDTH.W))
        val s_axi_rlast   	= Output(UInt(1.W))
        val s_axi_rresp   	= Output(UInt(2.W))
        val s_axi_rvalid  	= Output(UInt(1.W))
        val s_axi_rready  	= Input(UInt(1.W))
        val s_axi_bid     	= Output(UInt(ID_WIDTH.W))
        val s_axi_bresp   	= Output(UInt(2.W))
        val s_axi_bvalid  	= Output(UInt(1.W))
        val s_axi_bready  	= Input(UInt(1.W))
        val m_axi_araddr  	= Output(UInt(ADDR_WIDTH.W))
        val m_axi_arburst 	= Output(UInt(2.W))
        val m_axi_arid    	= Output(UInt(ID_WIDTH.W))
        val m_axi_arlen   	= Output(UInt(4.W))
        val m_axi_arsize  	= Output(UInt(3.W))
        val m_axi_arvalid 	= Output(UInt(1.W))
        val m_axi_arready 	= Input(UInt(1.W))
        val m_axi_awaddr  	= Output(UInt(ADDR_WIDTH.W))
        val m_axi_awburst 	= Output(UInt(2.W))
        val m_axi_awid    	= Output(UInt(ID_WIDTH.W))
        val m_axi_awlen   	= Output(UInt(4.W))
        val m_axi_awsize  	= Output(UInt(3.W))
        val m_axi_awvalid 	= Output(UInt(1.W))
        val m_axi_awready 	= Input(UInt(1.W))
        val m_axi_wdata   	= Output(UInt(256.W))
        val m_axi_wlast   	= Output(UInt(1.W))
        val m_axi_wstrb   	= Output(UInt(32.W))
        val m_axi_wvalid  	= Output(UInt(1.W))
        val m_axi_wready  	= Input(UInt(1.W))
        val m_axi_rdata   	= Input(UInt(256.W))
        val m_axi_rid     	= Input(UInt(ID_WIDTH.W))
        val m_axi_rlast   	= Input(UInt(1.W))
        val m_axi_rresp   	= Input(UInt(2.W))
        val m_axi_rvalid  	= Input(UInt(1.W))
        val m_axi_rready  	= Output(UInt(1.W))
        val m_axi_bid     	= Input(UInt(ID_WIDTH.W))
        val m_axi_bresp   	= Input(UInt(2.W))
        val m_axi_bvalid  	= Input(UInt(1.W))
        val m_axi_bready  	= Output(UInt(1.W))
	})
}
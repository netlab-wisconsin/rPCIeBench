// package common.axi

// import chisel3._
// import chisel3.util._


// class SV_HBM_DRIVER(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends BlackBox(
// 	Map(
// 		"ADDR_WIDTH"->ADDR_WIDTH,
// 		"DATA_WIDTH"->DATA_WIDTH,
// 		"ID_WIDTH"->ID_WIDTH,
// 		"USER_WIDTH"->USER_WIDTH
// 	)
// ){
// 	val io = IO(new Bundle{
// 		val sys_clk_100M	= Input(Clock())
// 		val hbm_clk			= Output(Clock())
// 		val hbm_rstn 		= Output(UInt(1.W))
		
// 		val araddr		= Input(Vec(32,UInt(ADDR_WIDTH.W)))
// 		val arburst		= Input(Vec(32,UInt(2.W)))
// 		val arcache		= Input(Vec(32,UInt(4.W)))
// 		val arid		= Input(Vec(32,UInt(ID_WIDTH.W)))
// 		val arlen		= Input(Vec(32,UInt(LEN_WIDTH.W)))
// 		val arlock		= Input(Vec(32,UInt(1.W)))
// 		val arprot		= Input(Vec(32,UInt(3.W)))
// 		val arqos		= Input(Vec(32,UInt(4.W)))
// 		val arregion	= Input(Vec(32,UInt(4.W)))
// 		val arsize		= Input(Vec(32,UInt(3.W)))
// 		val aruser		= Input(Vec(32,UInt(USER_WIDTH.W)))
// 		val arvalid		= Input(Vec(32,UInt(1.W)))
// 		val arready		= Output(Vec(32,UInt(1.W)))

// 		val awaddr		= Input(Vec(32,UInt(ADDR_WIDTH.W)))
// 		val awburst		= Input(Vec(32,UInt(2.W)))
// 		val awcache		= Input(Vec(32,UInt(4.W)))
// 		val awid		= Input(Vec(32,UInt(ID_WIDTH.W)))
// 		val awlen		= Input(Vec(32,UInt(LEN_WIDTH.W)))
// 		val awlock		= Input(Vec(32,UInt(1.W)))
// 		val awprot		= Input(Vec(32,UInt(3.W)))
// 		val awqos		= Input(Vec(32,UInt(4.W)))
// 		val awregion	= Input(Vec(32,UInt(4.W)))
// 		val awsize		= Input(Vec(32,UInt(3.W)))
// 		val awuser		= Input(Vec(32,UInt(USER_WIDTH.W)))		
// 		val awvalid		= Input(Vec(32,UInt(1.W)))
// 		val awready		= Output(Vec(32,UInt(1.W)))

// 		val rdata		= Output(Vec(32,UInt(DATA_WIDTH.W)))
// 		val rid			= Output(Vec(32,UInt(ID_WIDTH.W)))
// 		val rlast		= Output(Vec(32,UInt(1.W)))
// 		val rresp		= Output(Vec(32,UInt(2.W)))
// 		val ruser		= Output(Vec(32,UInt(USER_WIDTH.W)))
// 		val rvalid		= Output(Vec(32,UInt(1.W)))
// 		val rready		= Input(Vec(32,UInt(1.W)))

// 		val wdata		= Input(Vec(32,UInt(DATA_WIDTH.W)))
// 		val wlast		= Input(Vec(32,UInt(1.W)))
// 		val wstrb		= Input(Vec(32,UInt((DATA_WIDTH/8).W)))
// 		val wuser		= Input(Vec(32,UInt(USER_WIDTH.W)))
// 		val wvalid		= Input(Vec(32,UInt(1.W)))
// 		val wready		= Output(Vec(32,UInt(1.W)))

// 		val bid			= Output(Vec(32,UInt(ID_WIDTH.W)))
// 		val bresp		= Output(Vec(32,UInt(3.W)))
// 		val buser		= Output(Vec(32,UInt(USER_WIDTH.W)))
// 		val bvalid		= Output(Vec(32,UInt(1.W)))
// 		val bready		= Input(Vec(32,UInt(1.W)))
// 	})
// }

// class HBM_DRIVER(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int) extends Module{
// 	val io = IO(new Bundle{
// 		// val sys_clk_100M	= Input(Clock())
// 		val hbm_clk			= Output(Clock())
// 		val hbm_rstn 		= Output(UInt(1.W))
// 		val axi_hbm 		= Vec(32,Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)))
// 	})

// 	val sv = Module(new SV_HBM_DRIVER(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 	sv.io.sys_clk_100M			:= clock
// 	io.hbm_clk					:= sv.io.hbm_clk
// 	io.hbm_rstn					:= sv.io.hbm_rstn
// 	for(i <- 0 until 32){

// 		sv.io.araddr(i)								:= io.axi_hbm(i).ar.bits.addr
// 		sv.io.arburst(i)							:= io.axi_hbm(i).ar.bits.burst
// 		sv.io.arcache(i)							:= io.axi_hbm(i).ar.bits.cache
// 		sv.io.arid(i)								:= io.axi_hbm(i).ar.bits.id
// 		sv.io.arlen(i)								:= io.axi_hbm(i).ar.bits.len
// 		sv.io.arlock(i)								:= io.axi_hbm(i).ar.bits.lock
// 		sv.io.arprot(i)								:= io.axi_hbm(i).ar.bits.prot
// 		sv.io.arqos(i)								:= io.axi_hbm(i).ar.bits.qos
// 		sv.io.arregion(i)							:= io.axi_hbm(i).ar.bits.region
// 		sv.io.arsize(i)								:= io.axi_hbm(i).ar.bits.size
// 		sv.io.aruser(i)								:= io.axi_hbm(i).ar.bits.user
// 		sv.io.arvalid(i)							:= io.axi_hbm(i).ar.valid
// 		io.axi_hbm(i).ar.ready 						:= sv.io.arready(i)

// 		sv.io.awaddr(i)								:= io.axi_hbm(i).aw.bits.addr
// 		sv.io.awburst(i)							:= io.axi_hbm(i).aw.bits.burst
// 		sv.io.awcache(i)							:= io.axi_hbm(i).aw.bits.cache
// 		sv.io.awid(i)								:= io.axi_hbm(i).aw.bits.id
// 		sv.io.awlen(i)								:= io.axi_hbm(i).aw.bits.len
// 		sv.io.awlock(i)								:= io.axi_hbm(i).aw.bits.lock
// 		sv.io.awprot(i)								:= io.axi_hbm(i).aw.bits.prot
// 		sv.io.awqos(i)								:= io.axi_hbm(i).aw.bits.qos
// 		sv.io.awregion(i)							:= io.axi_hbm(i).aw.bits.region
// 		sv.io.awsize(i)								:= io.axi_hbm(i).aw.bits.size
// 		sv.io.awuser(i)								:= io.axi_hbm(i).aw.bits.user
// 		sv.io.awvalid(i)							:= io.axi_hbm(i).aw.valid
// 		io.axi_hbm(i).aw.ready						:= sv.io.awready(i)

// 		io.axi_hbm(i).r.bits.data					:= sv.io.rdata(i)
// 		io.axi_hbm(i).r.bits.id						:= sv.io.rid(i)
// 		io.axi_hbm(i).r.bits.last					:= sv.io.rlast(i)
// 		io.axi_hbm(i).r.bits.resp					:= sv.io.rresp(i)
// 		io.axi_hbm(i).r.bits.user					:= sv.io.ruser(i)
// 		io.axi_hbm(i).r.valid						:= sv.io.rvalid(i)
// 		sv.io.rready(i)								:= io.axi_hbm(i).r.ready

// 		sv.io.wdata(i)								:= io.axi_hbm(i).w.bits.data
// 		sv.io.wlast(i)								:= io.axi_hbm(i).w.bits.last
// 		sv.io.wstrb(i)								:= io.axi_hbm(i).w.bits.strb
// 		sv.io.wuser(i)								:= io.axi_hbm(i).w.bits.user
// 		sv.io.wvalid(i)								:= io.axi_hbm(i).w.valid
// 		io.axi_hbm(i).w.ready						:= sv.io.wready(i)

// 		io.axi_hbm(i).b.bits.id						:= sv.io.bid(i)
// 		io.axi_hbm(i).b.bits.resp					:= sv.io.bresp(i)
// 		io.axi_hbm(i).b.bits.user					:= sv.io.buser(i)
// 		io.axi_hbm(i).b.valid						:= sv.io.bvalid(i)
// 		sv.io.bready(i)								:= io.axi_hbm(i).b.ready
// 	}
	
// }



// class SV_RAMA(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends BlackBox (
// 	Map(
// 		"ADDR_WIDTH"->ADDR_WIDTH,
// 		"DATA_WIDTH"->DATA_WIDTH,
// 		"ID_WIDTH"->ID_WIDTH,
// 		"USER_WIDTH"->USER_WIDTH
// 	)
// ){
// 	val io = IO(new Bundle{
// 		val axi_aclk	= Input(Clock())
// 		val axi_aresetn	= Input(UInt(1.W))
// 		val s_axi = Flipped(new RAW_M_AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 		val m_axi = new RAW_M_AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
// 	})
// }
// class RAMA(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int) extends Module{
// 	val io = IO(new Bundle{
// 		val s_axi = Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 		val m_axi = (new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 	})

// 	val sv=Module(new SV_RAMA(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 	sv.io.axi_aclk		:= clock
// 	sv.io.axi_aresetn	:= !(reset.asUInt)

// 	Connect_AXI(io.s_axi,sv.io.s_axi)
// 	Connect_AXI(io.m_axi,sv.io.m_axi)				
// }

// class SV_AXI_CLK_CVT_HBM(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends BlackBox {
// 	val io = IO(new Bundle{
// 		val s_axi_aclk		= Input(Clock())
// 		val s_axi_aresetn	= Input(UInt(1.W))
// 		val m_axi_aclk		= Input(Clock())
// 		val m_axi_aresetn	= Input(UInt(1.W))
// 		val s_axi 			= Flipped(new RAW_M_AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 		val m_axi 			= new RAW_M_AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
// 	})
// }
// class AXI_CLOCK_CVT_HBM(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends RawModule {
// 	val io = IO(new Bundle{
// 		val s_axi_aclk		= Input(Clock())
// 		val s_axi_aresetn	= Input(UInt(1.W))
// 		val m_axi_aclk		= Input(Clock())
// 		val m_axi_aresetn	= Input(UInt(1.W))
// 		val s_axi = Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 		val m_axi = (new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
// 	})
// 	val sv = Module(new SV_AXI_CLK_CVT_HBM(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))

// 	io.s_axi_aclk			<> sv.io.s_axi_aclk
// 	io.s_axi_aresetn		<> sv.io.s_axi_aresetn
// 	io.m_axi_aclk			<> sv.io.m_axi_aclk
// 	io.m_axi_aresetn		<> sv.io.m_axi_aresetn

// 	Connect_AXI(io.s_axi,sv.io.s_axi)
// 	Connect_AXI(io.m_axi,sv.io.m_axi)	

// }
package common.axi

import chisel3._
import chisel3.util._
import common.ToAllOnes
import common.ToZero

trait HasAddrLen extends Bundle{
	val addr		= Output(UInt(64.W))
	val len			= Output(UInt(8.W))
}

trait HasLast extends Bundle{
	val last		= Output(UInt(1.W))
}

class AXI_ADDR(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends Bundle with HasAddrLen{
	override val addr			= UInt(ADDR_WIDTH.W)
	val burst 					= UInt(2.W)
	val cache					= UInt(4.W)
	val id						= UInt(ID_WIDTH.W)
	override val len			= UInt(LEN_WIDTH.W) 
	val lock					= UInt(1.W)
	val prot					= UInt(3.W)
	val qos						= UInt(4.W)
	val region					= UInt(4.W)
	val size					= UInt(3.W)
	val user					= UInt(USER_WIDTH.W)

	def hbm_init() = {
		ToZero(addr)
		ToZero(len)
		ToZero(id)
		burst		:= 1.U //burst type: 01 (INC), 00 (FIXED)
		size		:= 5.U
		
		// The following signals are unused by HBM IP.
		region		:= DontCare
		lock		:= DontCare
		user		:= DontCare
		prot		:= DontCare
		cache		:= DontCare
		qos			:= DontCare
	}

	def qdma_init() = {
		ToZero(addr)
		ToZero(len)
		ToZero(id)
		ToZero(user)
		burst		:= 1.U //burst type: 01 (INC), 00 (FIXED)
		size		:= 6.U
		
		// The following signals are unused by QDMA slave bridge.
		region		:= DontCare
		lock		:= DontCare
		prot		:= DontCare
		cache		:= DontCare
		qos			:= DontCare
	}
}


class AXI_DATA_W(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int)extends HasLast{
	val data	= UInt(DATA_WIDTH.W)
	val user	= UInt(USER_WIDTH.W)
	override val last	= UInt(1.W)
	val strb	= UInt((DATA_WIDTH/8).W)

	def hbm_init() = {
		ToZero(data)
		ToZero(last)
		ToAllOnes(strb)
		user		:= DontCare
	}

	def qdma_init() = {
		ToZero(data)
		ToZero(last)
		ToZero(user)
		ToAllOnes(strb)
	}
}

class AXI_DATA_R(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int)extends HasLast{
	val data	= UInt(DATA_WIDTH.W)
	val user	= UInt(USER_WIDTH.W)
	override val last	= UInt(1.W)
	val resp	= UInt(2.W)
	val id		= UInt(ID_WIDTH.W)
}



class AXI_BACK(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int)extends Bundle{
	val id		= UInt(ID_WIDTH.W)
	val resp	= UInt(2.W)
	val user	= UInt(USER_WIDTH.W)
}

object AXI_HBM{
	def apply()={
		new AXI(33, 256, 6, 0, 4)
	}
}
object AXI_HBM_ADDR{
	def apply()={
		new AXI_ADDR(33, 256, 6, 0, 4)
	}
}
object AXI_HBM_W{
	def apply()={
		new AXI_DATA_W(33, 256, 6, 0)
	}
}
object AXI_HBM_R{
	def apply()={
		new AXI_DATA_R(33, 256, 6, 0)
	}
}
object AXI_HBM_B{
	def apply()={
		new AXI_BACK(33, 256, 6, 0)
	}
}

class AXI(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int) extends Bundle{
	val aw	= (Decoupled(new AXI_ADDR(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH,LEN_WIDTH)))
	val ar	= (Decoupled(new AXI_ADDR(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH,LEN_WIDTH)))
	val w	= (Decoupled(new AXI_DATA_W(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
	val r	= Flipped(Decoupled(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
	val b	= Flipped(Decoupled(new AXI_BACK(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
	def slave_init() = {
		ar.ready			:= 1.U
		aw.ready			:= 1.U
		w.ready				:= 1.U
		r.valid				:= 0.U
		b.valid				:= 0.U
		r.bits				:= 0.U.asTypeOf(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH))
		b.bits				:= 0.U.asTypeOf(new AXI_BACK(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH))
	}
	def init() = {
		ar.bits 			:= 0.U.asTypeOf(new AXI_ADDR(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH,LEN_WIDTH))
		aw.bits 			:= 0.U.asTypeOf(new AXI_ADDR(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH,LEN_WIDTH))
		w.bits 				:= 0.U.asTypeOf(new AXI_DATA_W(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH))
		ar.valid 			:= 0.U
		aw.valid 			:= 0.U 
		w.valid 			:= 0.U
		r.ready 			:= 0.U
		b.ready 			:= 1.U

		aw.bits.burst		:= 1.U //burst type: 01 (INC), 00 (FIXED)
		aw.bits.prot		:= 2.U
		aw.bits.size		:= 5.U

		ar.bits.burst		:= 1.U //burst type: 01 (INC), 00 (FIXED)
		ar.bits.prot		:= 2.U
		ar.bits.size		:= 5.U
		
		// r.bits				:= 0.U.asTypeOf(new AXIRChannel(512))
		// r.bits.resp			:= 3.U
		// b.bits				:= 0.U.asTypeOf(new AXIBChannel)
		// b.bits.resp			:= 3.U
	}

	/* User notes:
	 * 
	 * hbm_init helps to initialize HBM AXI data bits and remove unused bits.
	 * NOTICE: Make sure you FIRST initialize AXI buses THEN assign AXI's signals.
	 *         Otherwise this will override your signal assignments!
	 *	
	 * When using HBM, following signals could be considered:
	 * <id> bits: 	Used for transaction reordering, normally set to 0.
	 * 				Note: If RAMA is used, this signal should be set to 0.
	 * <addr> bits:	Address in HBM to visit.
	 * <len> bits:	Zero based value. Beats to transfer data (i.e. 0 stands for 1 beat / 256bit).
	 *				Note: HBM IP uses AXI3, thus len bits are only 4-bit wide.
	 * <strb> bits:	Used for narrow burst, normally set to all 1's.
	 * For B channel, normally you can just set b.ready to 1 and ignore other signals.
	 */
	def hbm_init() = {
		ar.valid 			:= 0.U
		aw.valid 			:= 0.U 
		w.valid 			:= 0.U
		r.ready 			:= 0.U
		b.ready 			:= 1.U

		aw.bits.hbm_init()
		ar.bits.hbm_init()
		w.bits.hbm_init()
	}

	// Use this only when you are initializing master side of QDMA slave bridge.
	// Otherwise, simply ignore this.

	def qdma_init() = {
		ar.valid 			:= 0.U
		aw.valid 			:= 0.U 
		w.valid 			:= 0.U
		r.ready 			:= 0.U
		b.ready 			:= 1.U

		aw.bits.qdma_init()
		ar.bits.qdma_init()
		w.bits.qdma_init()
	}

	// def ar_init(){
	// 	ar.bits.addr	:= ar_addr
	// 	ar.valid		:= ar_valid
	// }
}


class RAW_M_AXI(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends Bundle{
	val araddr		= Output(UInt(ADDR_WIDTH.W))
	val arburst		= Output(UInt(2.W))
	val arcache		= Output(UInt(4.W))
	val arid		= Output(UInt(ID_WIDTH.W))
	val arlen		= Output(UInt(LEN_WIDTH.W))
	val arlock		= Output(UInt(1.W))
	val arprot		= Output(UInt(3.W))
	val arqos		= Output(UInt(4.W))
	val arregion	= Output(UInt(4.W))
	val arsize		= Output(UInt(3.W))
	val aruser		= Output(UInt(USER_WIDTH.W))
	val arvalid		= Output(UInt(1.W))
	val arready		= Input(UInt(1.W))

	val awaddr		= Output(UInt(ADDR_WIDTH.W))
	val awburst		= Output(UInt(2.W))
	val awcache		= Output(UInt(4.W))
	val awid		= Output(UInt(ID_WIDTH.W))
	val awlen		= Output(UInt(LEN_WIDTH.W))
	val awlock		= Output(UInt(1.W))
	val awprot		= Output(UInt(3.W))
	val awqos		= Output(UInt(4.W))
	val awregion	= Output(UInt(4.W))
	val awsize		= Output(UInt(3.W))
	val awuser		= Output(UInt(USER_WIDTH.W))
	val awvalid		= Output(UInt(1.W))
	val awready		= Input(UInt(1.W))

	val rdata		= Input(UInt(DATA_WIDTH.W))
	val rid			= Input(UInt(ID_WIDTH.W))
	val rlast		= Input(UInt(1.W))
	val rresp		= Input(UInt(2.W))
	val ruser		= Input(UInt(USER_WIDTH.W))
	val rvalid		= Input(UInt(1.W))
	val rready		= Output(UInt(1.W))

	val wdata		= Output(UInt(DATA_WIDTH.W))
	val wlast		= Output(UInt(1.W))
	val wstrb		= Output(UInt((DATA_WIDTH/8).W))
	val wuser		= Output(UInt(USER_WIDTH.W))
	val wvalid		= Output(UInt(1.W))
	val wready		= Input(UInt(1.W))

	val bid			= Input(UInt(ID_WIDTH.W))
	val bresp		= Input(UInt(3.W))
	val buser		= Input(UInt(USER_WIDTH.W))
	val bvalid		= Input(UInt(1.W))
	val bready		= Output(UInt(1.W))
}

class RAW_S_AXI(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int)extends Bundle{
	val araddr		= Input(UInt(ADDR_WIDTH.W))
	val arburst		= Input(UInt(2.W))
	val arcache		= Input(UInt(4.W))
	val arid		= Input(UInt(ID_WIDTH.W))
	val arlen		= Input(UInt(LEN_WIDTH.W))
	val arlock		= Input(UInt(1.W))
	val arprot		= Input(UInt(3.W))
	val arqos		= Input(UInt(4.W))
	val arregion	= Input(UInt(4.W))
	val arsize		= Input(UInt(3.W))
	val aruser		= Input(UInt(USER_WIDTH.W))
	val arvalid		= Input(UInt(1.W))
	val arready		= Output(UInt(1.W))

	val awaddr		= Input(UInt(ADDR_WIDTH.W))
	val awburst		= Input(UInt(2.W))
	val awcache		= Input(UInt(4.W))
	val awid		= Input(UInt(ID_WIDTH.W))
	val awlen		= Input(UInt(LEN_WIDTH.W))
	val awlock		= Input(UInt(1.W))
	val awprot		= Input(UInt(3.W))
	val awqos		= Input(UInt(4.W))
	val awregion	= Input(UInt(4.W))
	val awsize		= Input(UInt(3.W))
	val awuser		= Input(UInt(USER_WIDTH.W))
	val awvalid		= Input(UInt(1.W))
	val awready		= Output(UInt(1.W))

	val rdata		= Output(UInt(DATA_WIDTH.W))
	val rid			= Output(UInt(ID_WIDTH.W))
	val rlast		= Output(UInt(1.W))
	val rresp		= Output(UInt(2.W))
	val ruser		= Output(UInt(USER_WIDTH.W))
	val rvalid		= Output(UInt(1.W))
	val rready		= Input(UInt(1.W))

	val wdata		= Input(UInt(DATA_WIDTH.W))
	val wlast		= Input(UInt(1.W))
	val wstrb		= Input(UInt((DATA_WIDTH/8).W))
	val wuser		= Input(UInt(USER_WIDTH.W))
	val wvalid		= Input(UInt(1.W))
	val wready		= Output(UInt(1.W))

	val bid			= Output(UInt(ID_WIDTH.W))
	val bresp		= Output(UInt(3.W))
	val buser		= Output(UInt(USER_WIDTH.W))
	val bvalid		= Output(UInt(1.W))
	val bready		= Input(UInt(1.W))
}


object Connect_AXI{
	def apply(axi:AXI,raw_axi: RAW_M_AXI)={
		raw_axi.araddr		<> axi.ar.bits.addr
		raw_axi.arburst		<> axi.ar.bits.burst
		raw_axi.arcache		<> axi.ar.bits.cache
		raw_axi.arid		<> axi.ar.bits.id
		raw_axi.arlen		<> axi.ar.bits.len
		raw_axi.arlock		<> axi.ar.bits.lock
		raw_axi.arprot		<> axi.ar.bits.prot
		raw_axi.arqos		<> axi.ar.bits.qos
		raw_axi.arregion	<> axi.ar.bits.region
		raw_axi.arsize		<> axi.ar.bits.size
		raw_axi.aruser		<> axi.ar.bits.user
		raw_axi.arvalid		<> axi.ar.valid
		raw_axi.arready		<> axi.ar.ready

		raw_axi.awaddr		<> axi.aw.bits.addr
		raw_axi.awburst		<> axi.aw.bits.burst
		raw_axi.awcache		<> axi.aw.bits.cache
		raw_axi.awid		<> axi.aw.bits.id
		raw_axi.awlen		<> axi.aw.bits.len
		raw_axi.awlock		<> axi.aw.bits.lock
		raw_axi.awprot		<> axi.aw.bits.prot
		raw_axi.awqos		<> axi.aw.bits.qos
		raw_axi.awregion	<> axi.aw.bits.region
		raw_axi.awsize		<> axi.aw.bits.size
		raw_axi.awuser		<> axi.aw.bits.user
		raw_axi.awvalid		<> axi.aw.valid
		raw_axi.awready		<> axi.aw.ready

		raw_axi.rdata		<> axi.r.bits.data
		raw_axi.rid			<> axi.r.bits.id
		raw_axi.rlast		<> axi.r.bits.last
		raw_axi.rresp		<> axi.r.bits.resp
		raw_axi.ruser		<> axi.r.bits.user
		raw_axi.rvalid		<> axi.r.valid
		raw_axi.rready		<> axi.r.ready

		raw_axi.wdata		<> axi.w.bits.data
		raw_axi.wlast		<> axi.w.bits.last
		raw_axi.wstrb		<> axi.w.bits.strb
		raw_axi.wuser		<> axi.w.bits.user
		raw_axi.wvalid		<> axi.w.valid
		raw_axi.wready		<> axi.w.ready

		raw_axi.bid			<> axi.b.bits.id
		raw_axi.bresp		<> axi.b.bits.resp
		raw_axi.buser		<> axi.b.bits.user
		raw_axi.bvalid		<> axi.b.valid
		raw_axi.bready		<> axi.b.ready
	}
}

object AXIS{
	def apply(DATA_WIDTH:Int) = {
		new AXIS(DATA_WIDTH)
	}
}
class AXIS(val DATA_WIDTH:Int)extends Bundle with HasLast{
	val data = Output(UInt(DATA_WIDTH.W))
	val keep = Output(UInt((DATA_WIDTH/8).W))
}
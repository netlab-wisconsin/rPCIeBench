package CommSub

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, requireIsChiselType}
import common.axi._
import common.storage._

class QDMAPin extends Bundle{
	val tx_p 		= Output(UInt(16.W))
	val tx_n 		= Output(UInt(16.W))
	val rx_p 		= Input(UInt(16.W))
	val rx_n 		= Input(UInt(16.W))
	val sys_clk_p   = Input(Clock())
	val sys_clk_n   = Input(Clock())
	val sys_rst_n   = Input(Bool())
}

class PCIeIO extends Bundle{
	val tx_p 		= Output(UInt(16.W))
	val tx_n 		= Output(UInt(16.W))
	val rx_p 		= Input(UInt(16.W))
	val rx_n 		= Input(UInt(16.W))
}

class H2C_CMD extends HasAddrLen {
	override val addr		= Output(UInt(64.W))
	override val len 		= Output(UInt(32.W))
	val eop					= Output(Bool())
	val sop					= Output(Bool())
	val mrkr_req			= Output(Bool())
	val sdi					= Output(Bool())
	val qid					= Output(UInt(11.W))
	val error				= Output(Bool())
	val func				= Output(UInt(8.W))
	val cidx				= Output(UInt(16.W))
	val port_id				= Output(UInt(3.W))
	val no_dma				= Output(Bool())
}

class H2C_DATA extends Bundle{
	val	data				= Output(UInt(512.W))
	val	tcrc				= Output(UInt(32.W))	
	val	tuser_qid			= Output(UInt(11.W))	
	val	tuser_port_id		= Output(UInt(3.W))	
	val	tuser_err			= Output(Bool())	
	val	tuser_mdata			= Output(UInt(32.W))	
	val	tuser_mty			= Output(UInt(6.W))	
	val	tuser_zero_byte		= Output(Bool())	
	val	last				= Output(Bool())	
}

class C2H_CMD extends HasAddrLen{
	override val addr 		= Output(UInt(64.W))
	val qid 				= Output(UInt(11.W))
	val error 				= Output(Bool())
	val func 				= Output(UInt(8.W))
	val port_id 			= Output(UInt(3.W))
	val pfch_tag 			= Output(UInt(7.W))
	override val len 		= Output(UInt(32.W))
}

class C2H_DATA extends HasLast{
	val data			= Output(UInt(512.W))
	val tcrc			= Output(UInt(32.W))
	val ctrl_marker		= Output(Bool())
	val ctrl_ecc		= Output(UInt(7.W))
	val ctrl_len		= Output(UInt(32.W))
	val ctrl_port_id	= Output(UInt(3.W))
	val ctrl_qid		= Output(UInt(11.W))
	val ctrl_has_cmpt	= Output(Bool())
	override val last	= Output(Bool())
	val mty				= Output(UInt(6.W))
}

class H2M_CMD extends Bundle{
	val h2c_start_addr	= Output(UInt(64.W))
	val h2m_start_addr 	= Output(UInt(34.W))
	val h2m_length		= Output(UInt(32.W))
	val pkt_size		= Output(UInt(32.W))
	val h2c_cpt_addr	= Output(UInt(64.W))
}

class M2H_CMD extends Bundle{
	val c2h_start_addr	= Output(UInt(64.W))

	val m2h_start_addr 	= Output(UInt(34.W))
	val m2h_length		= Output(UInt(32.W))

	val c2h_cpt_addr	= Output(UInt(64.W))

	// val c2h_pfch_tag	= Output(UInt(32.W))
	// val c2h_tag_index	= Output(UInt(32.W))
	
	val pkt_size		= Output(UInt(32.W))
	
	// val c2h_queue_num	= Output(UInt(32.W))
}

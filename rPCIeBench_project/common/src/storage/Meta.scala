package common.storage

import common.Math
import chisel3._
import chisel3.util._

class SV_STREAM_FIFO(
	DATA_WIDTH:Int,
	DEPTH_BIT:Int,
	RAM_TYPE:String,
	CLOCKING_MODE:String,
	PACKET_FIFO:String,
) extends RawModule{
	val io = IO(new Bundle{
		val m_clk				= Input(Clock())
		val s_clk				= Input(Clock())
		val reset_n 			= Input(Reset())
		val in_data 			= Input(UInt(DATA_WIDTH.W))
		val in_valid 			= Input(UInt(1.W))
		val in_ready 			= Output(UInt(1.W))

		val out_data 			= Output(UInt(DATA_WIDTH.W))
		val out_valid			= Output(UInt(1.W))
		val out_ready			= Input(UInt(1.W))

		val rd_count			= Output(UInt((DEPTH_BIT+1).W))
		val wr_count			= Output(UInt((DEPTH_BIT+1).W))
	})

	val meta = Module(new xpm_fifo_axis(DATA_WIDTH,DEPTH_BIT+1,RAM_TYPE,CLOCKING_MODE,PACKET_FIFO))
	meta.io.m_axis_tdata		<> io.out_data
	meta.io.m_axis_tvalid		<> io.out_valid
	meta.io.rd_data_count_axis	<> io.rd_count
	meta.io.s_axis_tready		<> io.in_ready
	meta.io.wr_data_count_axis	<> io.wr_count

	meta.io.m_aclk				<> io.m_clk
	meta.io.m_axis_tready		<> io.out_ready
	meta.io.s_aclk				<> io.s_clk
	meta.io.s_aresetn			<> io.reset_n
	meta.io.s_axis_tdata		<> io.in_data
	meta.io.s_axis_tdest		<> 0.U
	meta.io.s_axis_tid			<> 0.U
	meta.io.s_axis_tkeep		<> Fill(DATA_WIDTH/8, 1.U(1.W))
	meta.io.s_axis_tlast		<> 1.U
	meta.io.s_axis_tstrb		<> Fill(DATA_WIDTH/8, 1.U(1.W))
	meta.io.s_axis_tuser		<> 0.U
	meta.io.s_axis_tvalid		<> io.in_valid
}

class xpm_fifo_axis(
	DATA_WIDTH:Int,
	DEPTH_BIT_PULS_ONE:Int,
	RAM_TYPE:String,
	CLOCKING_MODE:String,
	PACKET_FIFO:String,
) extends BlackBox(Map(
	"CASCADE_HEIGHT" 			-> 0,
	"CDC_SYNC_STAGES" 			-> 2,
	"CLOCKING_MODE" 			-> CLOCKING_MODE,
	"ECC_MODE" 					-> "no_ecc",
	"FIFO_DEPTH" 				-> Math.pow2(DEPTH_BIT_PULS_ONE-1),
	"FIFO_MEMORY_TYPE" 			-> RAM_TYPE,
	"PACKET_FIFO" 				-> PACKET_FIFO,
	"PROG_EMPTY_THRESH" 		-> 10,
	"PROG_FULL_THRESH" 			-> 10,
	"RD_DATA_COUNT_WIDTH" 		-> DEPTH_BIT_PULS_ONE,
	"RELATED_CLOCKS" 			-> 0,
	"SIM_ASSERT_CHK" 			-> 0,
	"TDATA_WIDTH" 				-> DATA_WIDTH,
	"TDEST_WIDTH" 				-> 1,
	"TID_WIDTH" 				-> 1,
	"TUSER_WIDTH" 				-> 1,
	"USE_ADV_FEATURES" 			-> "0404",
	"WR_DATA_COUNT_WIDTH" 		-> DEPTH_BIT_PULS_ONE,
	)){
	val io = IO(new Bundle{

		val m_axis_tdata		= Output(UInt(DATA_WIDTH.W))
		val m_axis_tkeep		= Output(UInt((DATA_WIDTH/8).W))
		val m_axis_tlast		= Output(UInt(1.W))
		val m_axis_tvalid		= Output(UInt(1.W))
		val rd_data_count_axis	= Output(UInt(DEPTH_BIT_PULS_ONE.W))
		val s_axis_tready		= Output(UInt(1.W))
		val wr_data_count_axis	= Output(UInt(DEPTH_BIT_PULS_ONE.W))

		val m_aclk 				= Input(Clock())
		val m_axis_tready 		= Input(UInt(1.W))
		val s_aclk 				= Input(Clock())
		val s_aresetn 			= Input(Reset())
		val s_axis_tdata		= Input(UInt(DATA_WIDTH.W))
		val s_axis_tdest		= Input(UInt(1.W))
		val s_axis_tid			= Input(UInt(1.W))
		val s_axis_tkeep		= Input(UInt((DATA_WIDTH/8).W))
		val s_axis_tlast		= Input(UInt(1.W))
		val s_axis_tstrb		= Input(UInt((DATA_WIDTH/8).W))
		val s_axis_tuser		= Input(UInt(1.W))
		val s_axis_tvalid		= Input(UInt(1.W))
	})

}
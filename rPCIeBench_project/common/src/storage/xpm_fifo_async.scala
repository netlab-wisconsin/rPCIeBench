package common.storage

import common.Math
import chisel3._
import chisel3.util._

class async_fifo(
	WRITE_DATA_WIDTH:Int,
	READ_DATA_WIDTH:Int,
	WRITE_DEPTH:Int,
) extends RawModule{
	val io = IO(new Bundle{
		
		val almost_full			= Output(UInt(1.W))
		val data_valid			= Output(UInt(1.W))
		val dout				= Output(UInt(READ_DATA_WIDTH.W))
		val din					= Input(UInt(WRITE_DATA_WIDTH.W))
		val rd_clk				= Input(Clock())
		val rd_en				= Input(UInt(1.W))
		val rst					= Input(UInt(1.W))
		val wr_clk				= Input(Clock())
		val wr_en				= Input(UInt(1.W))

		val almost_empty		= Output(UInt(1.W))
		val dbiterr				= Output(UInt(1.W))
		val empty				= Output(UInt(1.W))
		val full				= Output(UInt(1.W))
		val overflow 			= Output(UInt(1.W))
		val prog_empty 			= Output(UInt(1.W))
		val prog_full 			= Output(UInt(1.W))
		val rd_data_count 		= Output(UInt(1.W))
		val rd_rst_busy			= Output(UInt(1.W))
		val sbiterr				= Output(UInt(1.W))
		val underflow			= Output(UInt(1.W))
		val wr_ack				= Output(UInt(1.W))
		val wr_data_count		= Output(UInt(1.W))
		val wr_rst_busy			= Output(UInt(1.W))

		val injectdbiterr		= Input(UInt(1.W))
		val injectsbiterr		= Input(UInt(1.W))
		val sleep				= Input(UInt(1.W))

	})

	val meta = Module(new xpm_fifo_async(WRITE_DATA_WIDTH,READ_DATA_WIDTH,WRITE_DEPTH))


	meta.io.almost_empty		<> io.almost_empty
	meta.io.almost_full			<> io.almost_full
	meta.io.data_valid			<> io.data_valid
	meta.io.dout				<> io.dout
	meta.io.empty				<> io.empty
	meta.io.full				<> io.full
	meta.io.overflow 			<> io.overflow
	meta.io.prog_empty 			<> io.prog_empty
	meta.io.prog_full 			<> io.prog_empty
	meta.io.rd_data_count 		<> io.rd_data_count
	meta.io.rd_rst_busy			<> io.rd_rst_busy
	meta.io.sbiterr				<> io.sbiterr
	meta.io.underflow			<> io.underflow
	meta.io.wr_ack				<> io.wr_ack
	meta.io.wr_data_count		<> io.wr_data_count
	meta.io.wr_rst_busy			<> io.wr_rst_busy
	meta.io.din					<> io.din
	meta.io.injectdbiterr		:= 0.U
	meta.io.injectsbiterr		:= 0.U
	meta.io.rd_clk				<> io.rd_clk
	meta.io.rd_en				<> io.rd_en
	meta.io.rst					<> io.rst
	meta.io.sleep				:= 0.U
	meta.io.wr_clk				<> io.wr_clk
	meta.io.wr_en				<> io.wr_en
}

class xpm_fifo_async(
	WRITE_DATA_WIDTH:Int,
	READ_DATA_WIDTH:Int,
	WRITE_DEPTH:Int,
) extends BlackBox(Map(
	"CASCADE_HEIGHT" 			-> 0,
	"CDC_SYNC_STAGES" 			-> 2,
	"DOUT_RESET_VALUE"			-> "0",
	"ECC_MODE" 					-> "no_ecc",
	"FIFO_MEMORY_TYPE" 			-> "block",
	"FIFO_READ_LATENCY"			-> 1,
	"FIFO_WRITE_DEPTH"			-> WRITE_DEPTH,
	"FULL_RESET_VALUE"			-> 0,
	"PROG_EMPTY_THRESH" 		-> 10,
	"PROG_FULL_THRESH" 			-> 10,
	"RD_DATA_COUNT_WIDTH" 		-> 1,
	"READ_DATA_WIDTH"			-> READ_DATA_WIDTH,
	"READ_MODE"					-> "std",
	"RELATED_CLOCKS" 			-> 0,
	"SIM_ASSERT_CHK" 			-> 0,
	"USE_ADV_FEATURES" 			-> "1415",
	"WAKEUP_TIME"				-> 0,
	"WRITE_DATA_WIDTH"			-> WRITE_DATA_WIDTH,
	"WR_DATA_COUNT_WIDTH" 		-> 1,
	)){
	val io = IO(new Bundle{

		val almost_empty		= Output(UInt(1.W))
		val almost_full			= Output(UInt(1.W))
		val data_valid			= Output(UInt(1.W))
		val dbiterr				= Output(UInt(1.W))
		val dout				= Output(UInt(READ_DATA_WIDTH.W))
		val empty				= Output(UInt(1.W))
		val full				= Output(UInt(1.W))
		val overflow 			= Output(UInt(1.W))
		val prog_empty 			= Output(UInt(1.W))
		val prog_full 			= Output(UInt(1.W))
		val rd_data_count 		= Output(UInt(1.W))
		val rd_rst_busy			= Output(UInt(1.W))
		val sbiterr				= Output(UInt(1.W))
		val underflow			= Output(UInt(1.W))
		val wr_ack				= Output(UInt(1.W))
		val wr_data_count		= Output(UInt(1.W))
		val wr_rst_busy			= Output(UInt(1.W))

		val din					= Input(UInt(WRITE_DATA_WIDTH.W))
		val injectdbiterr		= Input(UInt(1.W))
		val injectsbiterr		= Input(UInt(1.W))
		val rd_clk				= Input(Clock())
		val rd_en				= Input(UInt(1.W))
		val rst					= Input(UInt(1.W))
		val sleep				= Input(UInt(1.W))
		val wr_clk				= Input(Clock())
		val wr_en				= Input(UInt(1.W))

	})

}
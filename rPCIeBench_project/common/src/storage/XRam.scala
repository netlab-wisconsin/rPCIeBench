package common.storage

import common.Math
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}

class XRamInitIO[T <: Data](private val gen: T, val entries: Int) extends Bundle
{ 
	val depth_bits = log2Up(entries)
	val ram_width = Math.round_up((gen.getWidth),8)

	val init_en		= Input(UInt(1.W))
	val addr_a		= Input(UInt(depth_bits.W))
	val wr_en_a 	= Input(Bool())
	val data_in_a	= Input(gen)
}

class XRamIO[T <: Data](private val gen: T, val entries: Int, val use_musk:Int=0) extends Bundle
{ 
	val depth_bits = log2Up(entries)
	val ram_width = Math.round_up((gen.getWidth),8)
	val musk_width = ram_width/8

	val addr_a		= Input(UInt(depth_bits.W))
	val addr_b		= Input(UInt(depth_bits.W))
	val wr_en_a 	= Input(Bool())
	val musk_a		= if(use_musk==1) Some(Input(UInt((ram_width/8).W))) else None
	val data_in_a	= Input(gen)
	val data_out_a	= Output(gen)
	val data_out_b	= Output(gen)
}

// object XRam{
// 	def apply[T<:Data](num:Int)(gen: T, entries:Int) = {
// 		Seq.fill(num)(Module(new XRam(gen,entries,"auto",2,0)))
// 	}

// 	def apply[T<:Data](gen: T, entries:Int, memory_type:String="auto", latency:Int=2, use_musk:Int=0) = {
// 		Module(new XRam(gen,entries,memory_type,latency,use_musk))
// 	}

// 	class XRam[T<:Data](
// 		val gen:T,
// 		val entries:Int,
// 		val memory_type:String="auto",
// 		val latency:Int=2,
// 		val use_musk:Int=0
// 	)extends Module(){

// 		require(entries >= 2, "XRam must has at least 2 entries")
// 		require(entries <= 1024*1024, "XRam out of range")
// 		require(latency >= 0, "XRam's latency can't be less than 0")
// 		require(!(latency == 0 && memory_type=="block"), "block ram can't be set 0 latency")

// 		val genType = if (compileOptions.declaredTypeMustBeUnbound) {//maybe always satisfy?
// 			requireIsChiselType(gen)
// 			gen
// 		} else {
// 			if (DataMirror.internal.isSynthesizable(gen)) {
// 				chiselTypeOf(gen)
// 			}else {
// 				gen
// 			}
// 		}
// 		val depth_bits = log2Up(entries)
// 		val width = Math.round_up((genType.getWidth),8)
// 		val musk_width = width/8

// 		val io = IO(new XRamIO(genType,entries,use_musk))

// 		val ram = if(memory_type == "distributed"){
// 			Module(new SV_RAM(width, depth_bits, memory_type, latency, use_musk, "read_first"))
// 		}else{
// 			Module(new SV_RAM(width, depth_bits, memory_type, latency, use_musk, "no_change"))
// 		}

		
// 		ram.io.clk			:= clock
// 		ram.io.usr_rst		:= reset.asUInt
// 		ram.io.addr_a		:= io.addr_a
// 		ram.io.addr_b		:= io.addr_b
// 		if(use_musk==0){
// 			ram.io.wr_en_a		:= Fill(width/8,io.wr_en_a)
// 		}else{
// 			ram.io.wr_en_a		:= io.musk_a.get & Fill(width/8,io.wr_en_a)
// 		}
// 		ram.io.data_in_a	:= io.data_in_a.asUInt
// 		io.data_out_a		:= ram.io.data_out_a.asTypeOf(genType)
// 		io.data_out_b		:= ram.io.data_out_b.asTypeOf(genType)
		

// 	}
// }

object XRam{
	def apply[T<:Data](num:Int)(gen: T, entries:Int, latency: Int) = {
		Seq.fill(num)(Module(new XRam(gen,entries,"auto",latency,0)))
	}

	def apply[T<:Data](gen: T, entries:Int, memory_type:String="auto", latency:Int=2, use_musk:Int=0, initFile: String = "none") = {
		Module(new XRam(gen,entries,memory_type,latency,use_musk, initFile))
	}

	class XRam[T<:Data](
		val gen:T,
		val entries:Int,
		val memory_type:String="auto",
		val latency:Int=2,
		val use_musk:Int=0,
		val initFile: String = "none"
	)extends Module(){
		require(entries >= 2, "XRam must has at least 2 entries")
		require(entries <= 1024*1024, "XRam out of range")
		require(latency >= 0, "XRam's latency can't be less than 0")
		require(!(latency == 0 && memory_type=="block"), "block ram can't be set 0 latency")

		val genType = if (compileOptions.declaredTypeMustBeUnbound) {//maybe always satisfy?
			requireIsChiselType(gen)
			gen
		} else {
			if (DataMirror.internal.isSynthesizable(gen)) {
				chiselTypeOf(gen)
			}else {
				gen
			}
		}

		val depth_bits = log2Up(entries)
		val width = Math.round_up((genType.getWidth),8)
		val musk_width = width/8
		val write_mode = if(memory_type == "distributed") "read_first" else "no_change"

		val io = IO(new XRamIO(genType,entries,use_musk))

		val ram = Module(new xpm_memory_tdpram(depth_bits, width, memory_type, latency, write_mode, initFile))

		val wr_en_a = if(use_musk==0) Fill(width/8,io.wr_en_a) else io.musk_a.get & Fill(width/8,io.wr_en_a)

		val usr_rst_delay		= ShiftRegister(reset,4)
		val reset_addr			= Reg(UInt(depth_bits.W))

		when(usr_rst_delay.asBool()){
			reset_addr			:= reset_addr+1.U
		}.otherwise{
			reset_addr			:= 0.U
		}

		if(use_musk == 1){
			io.data_out_b		:= ram.io.doutb.asTypeOf(genType)
		}else{
			if(latency==0){
				when(io.addr_b === io.addr_a & io.wr_en_a){
					io.data_out_b		:= io.data_in_a
				}.otherwise{
					io.data_out_b		:= ram.io.doutb.asTypeOf(genType)
				}
			}else if(latency==1){
				when(RegNext(io.addr_a) === RegNext(io.addr_b) & RegNext(io.wr_en_a)){
					io.data_out_b		:= RegNext(io.data_in_a)
				}.otherwise{
					io.data_out_b		:= ram.io.doutb.asTypeOf(genType)
				}
			}else if(latency==2){
				when(ShiftRegister(io.addr_b,2)===RegNext(io.addr_a) & RegNext(io.wr_en_a)){
					io.data_out_b		:= RegNext(io.data_in_a)
				}.elsewhen(ShiftRegister(io.addr_b,2)===ShiftRegister(io.addr_a,2) & ShiftRegister(io.wr_en_a,2)){
					io.data_out_b		:= ShiftRegister(io.data_in_a,2)
				}.otherwise{
					io.data_out_b		:= ram.io.doutb.asTypeOf(genType)
				}
			}
		}

		io.data_out_a			:= ram.io.douta.asTypeOf(genType)
		// io.data_out_b			:= 

		ram.io.addra			:= Mux(usr_rst_delay.asBool(), reset_addr, io.addr_a)
		ram.io.addrb			:= io.addr_b

		ram.io.clka				:= clock
		ram.io.clkb				:= clock

		ram.io.dina				:= Mux(usr_rst_delay.asBool(), 0.U, io.data_in_a.asUInt)
		ram.io.dinb				:= 0.U

		ram.io.ena				:= 1.U
		ram.io.enb				:= 1.U

		ram.io.injectdbiterra	:= 0.U
		ram.io.injectdbiterrb	:= 0.U

		ram.io.injectsbiterra	:= 0.U
		ram.io.injectsbiterrb	:= 0.U

		ram.io.regcea			:= 1.U
		ram.io.regceb			:= 1.U

		ram.io.rsta				:= 0.U
		ram.io.rstb				:= 0.U

		ram.io.sleep			:= 0.U
		if (initFile != "none") {
			ram.io.wea			:= wr_en_a
		} else {
			ram.io.wea			:= Mux(usr_rst_delay.asBool(), Fill(width/8,1.U), wr_en_a)
		}
		ram.io.web				:= 0.U

	}
}

class xpm_memory_tdpram(
	DEPTH_BIT: Int,
	DATA_WIDTH: Int,
	MEMORY_TYPE: String,
	LATENCY: Int,
	WRITE_MODE: String,
	INIT_FILE: String
) extends BlackBox(Map(
	"ADDR_WIDTH_A" 				-> DEPTH_BIT,
	"ADDR_WIDTH_B" 				-> DEPTH_BIT,
	"AUTO_SLEEP_TIME" 			-> 0,
	"BYTE_WRITE_WIDTH_A" 		-> 8,
	"BYTE_WRITE_WIDTH_B" 		-> 8,
	"CASCADE_HEIGHT" 			-> 0,
	"CLOCKING_MODE" 			-> "common_clock",
	"ECC_MODE" 					-> "no_ecc",
	"MEMORY_INIT_FILE" 			-> INIT_FILE,
	"MEMORY_INIT_PARAM" 		-> "",
	"MEMORY_OPTIMIZATION" 		-> "true",
	"MEMORY_PRIMITIVE" 			-> MEMORY_TYPE,
	"MEMORY_SIZE"				-> DATA_WIDTH * Math.pow2(DEPTH_BIT),
	"MESSAGE_CONTROL"			-> 0,
	"READ_DATA_WIDTH_A"			-> DATA_WIDTH,
	"READ_DATA_WIDTH_B"			-> DATA_WIDTH,
	"READ_LATENCY_A"			-> LATENCY,
	"READ_LATENCY_B"			-> LATENCY,
	"READ_RESET_VALUE_A"		-> "0",
	"READ_RESET_VALUE_B"		-> "0",
	"RST_MODE_A"				-> "SYNC",
	"RST_MODE_B"				-> "SYNC",
	"SIM_ASSERT_CHK"			-> 0,
	"USE_EMBEDDED_CONSTRAINT"	-> 0,
	"USE_MEM_INIT"				-> 1,
	"WAKEUP_TIME"				-> "disable_sleep",
	"WRITE_DATA_WIDTH_A"		-> DATA_WIDTH,
	"WRITE_DATA_WIDTH_B"		-> DATA_WIDTH,
	"WRITE_MODE_A"				-> WRITE_MODE,
	"WRITE_MODE_B"				-> WRITE_MODE,
)){
	val io = IO(new Bundle{
		val douta			= Output(UInt(DATA_WIDTH.W))
		val doutb			= Output(UInt(DATA_WIDTH.W))

		val addra 			= Input(UInt(DEPTH_BIT.W))
		val addrb 			= Input(UInt(DEPTH_BIT.W))

		val clka			= Input(Clock())
		val clkb			= Input(Clock())

		val dina 			= Input(UInt(DATA_WIDTH.W))
		val dinb 			= Input(UInt(DATA_WIDTH.W))

		val ena 			= Input(UInt(1.W))
		val enb 			= Input(UInt(1.W))

		val injectdbiterra	= Input(UInt(1.W))
		val injectdbiterrb	= Input(UInt(1.W))

		val injectsbiterra	= Input(UInt(1.W))
		val injectsbiterrb	= Input(UInt(1.W))

		val regcea			= Input(UInt(1.W))
		val regceb			= Input(UInt(1.W))

		val rsta			= Input(UInt(1.W))
		val rstb			= Input(UInt(1.W))

		val sleep 			= Input(UInt(1.W))

		val wea 			= Input(UInt((DATA_WIDTH/8).W))
		val web 			= Input(UInt((DATA_WIDTH/8).W))
	})
}
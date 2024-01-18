package CommSub

import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, requireIsChiselType}
import common.axi._
import common.storage._
import common._


class DataBoundarySplit extends Module{
	val io = IO(new Bundle{
		val data_in = Flipped(Decoupled(new C2H_DATA))
		val cmd_in = Flipped(Decoupled(new C2H_CMD))
		val data_out = Decoupled(new C2H_DATA)
		val cmd_out = Decoupled(new C2H_CMD)
		val debug_clk = Input(Clock())
	})

	val data_fifo = XQueue(new C2H_DATA(),16)
	val cmd_fifo = XQueue(new C2H_CMD(),16)
	val cmd_temp = Reg(new C2H_CMD())
	val clength = RegInit(0.U(32.W))
	io.data_out	<> data_fifo.io.out
	io.cmd_out	<> cmd_fifo.io.out

	val sIDLE :: sREAD_DATA :: Nil 	= Enum(2)
	val state                   	= RegInit(sIDLE)
	val tmp_reg		= RegInit(false.B)

	val cmd_counter = RegInit(0.U(32.W))
	val data_count = RegInit(0.U(32.W))

	io.cmd_in.ready 				:= (state === sIDLE) & (cmd_fifo.io.in.ready === 1.U)
	io.data_in.ready 				:= (data_fifo.io.in.ready === 1.U) & tmp_reg

	data_fifo.io.in.valid 				:= 0.U
	data_fifo.io.in.bits				:= 0.U.asTypeOf(data_fifo.io.in.bits)
	cmd_fifo.io.in.valid 				:= 0.U
	cmd_fifo.io.in.bits					:= 0.U.asTypeOf(cmd_fifo.io.in.bits)

	when (io.data_out.ready & io.data_out.valid)
	{data_count := data_count + 1.U}

	switch(state){
		is(sIDLE){
			when(io.cmd_in.fire()){
				cmd_temp				:= io.cmd_in.bits
                state               	:= sREAD_DATA
				cmd_fifo.io.in.valid	:= 1.U
				cmd_fifo.io.in.bits 	:= io.cmd_in.bits
				tmp_reg					:= true.B
				cmd_counter				:= cmd_counter + 1.U
			}
			when(io.data_in.fire()){
                data_fifo.io.in.valid 			:= 1.U
				data_fifo.io.in.bits 			<> io.data_in.bits 
				data_fifo.io.in.bits.last		:= false.B
				data_fifo.io.in.bits.ctrl_len	:= io.cmd_in.bits.len
				clength							:= clength + 64.U
				when((clength + 64.U) >= io.cmd_in.bits.len){
					data_fifo.io.in.bits.last	:= true.B
					state						:= sIDLE
					tmp_reg						:= false.B
					clength						:= 0.U
				}
			}
		}
		is(sREAD_DATA){
			when(io.data_in.fire()){
                data_fifo.io.in.valid 			:= 1.U
				data_fifo.io.in.bits 			<> io.data_in.bits 
				data_fifo.io.in.bits.last		:= false.B
				data_fifo.io.in.bits.ctrl_len	:= cmd_temp.len
				clength							:= clength + 64.U
				when((clength + 64.U) >= cmd_temp.len){
					data_fifo.io.in.bits.last	:= true.B
					state						:= sIDLE
					tmp_reg						:= false.B
					clength						:= 0.U
				}
			}
		}
	}

// class ila_bdr(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_bdr = Module(new ila_bdr(Seq(	
//    data_fifo.io.in.valid,
//    data_fifo.io.in.ready,
//    io.data_in.ready,
//    io.data_in.valid,
//    cmd_temp.len,
//    data_fifo.io.in.bits.last,
//    state,
//    clength,
//    io.data_in.bits.data(15,0),
//    data_fifo.io.in.bits.data(15,0),
// )))
// inst_ila_bdr.connect(io.debug_clk)
// class ila_bdrcmd(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_bdrcmd = Module(new ila_bdrcmd(Seq(	
//    cmd_counter,
//    data_count,
//    cmd_fifo.io.in.ready,
//    cmd_fifo.io.in.valid,
// )))
// inst_ila_bdrcmd.connect(io.debug_clk)

}

class CMDBoundaryCheck[T<:HasAddrLen](private val gen:T, page_size:Int, mini_page_size:Int) extends Module{
	val genType = if (compileOptions.declaredTypeMustBeUnbound) {
		requireIsChiselType(gen)
		gen
	} else {
		if (DataMirror.internal.isSynthesizable(gen)) {
			chiselTypeOf(gen)
		}else {
			gen
		}
	}
	val io = IO(new Bundle{
		val in = Flipped(Decoupled(gen))
		val out = Decoupled(gen)
		val cmd_counter = Output(UInt(32.W))
	})
	val page_offset = page_size-1

	val offset_addr = RegInit(0.U(24.W))
	val new_length = RegInit(0.U(24.W))
	val cmd_addr = RegInit(0.U(64.W))
	val cmd_len = RegInit(0.U(32.W))
	val mini_addr = RegInit(0.U(64.W))
	val mini_len = RegInit(0.U(32.W))	
	val cmd_temp = Reg(gen)

	val sIDLE :: sFIRSTCMD :: sSPLIT :: sMINISPLIT :: sLASTSPLIT :: Nil 	= Enum(5)
	val state = RegInit(sIDLE)

	io.in.ready 	:= (state === sIDLE)

	io.out.valid 	:= 0.U
	io.out.bits		:= 0.U.asTypeOf(io.out.bits)

	val cmd_counter = RegInit(0.U(32.W))
	io.cmd_counter	:= cmd_counter

	switch(state){
		is(sIDLE){
			when(io.in.fire()){
				cmd_addr					:= io.in.bits.addr
				cmd_len						:= io.in.bits.len			
				cmd_temp					:= io.in.bits
				offset_addr					:= io.in.bits.addr & page_offset.U
                state               		:= sFIRSTCMD
				new_length					:= page_size.U - (io.in.bits.addr & page_offset.U)
			}
		}
		is(sFIRSTCMD){
			when((offset_addr + cmd_len) > page_size.U){
				mini_addr					:= cmd_addr
				mini_len					:= new_length					
				cmd_addr					:= cmd_addr + new_length
				cmd_len						:= cmd_len - new_length
				state               		:= sMINISPLIT
			}.otherwise{
				mini_addr					:= cmd_addr
				mini_len					:= cmd_len					
				state               		:= sLASTSPLIT
			}
		}
		is(sSPLIT){
			when(cmd_len > page_size.U){
				mini_addr					:= cmd_addr
				mini_len					:= page_size.U					
				cmd_addr					:= cmd_addr + page_size.U
				cmd_len						:= cmd_len - page_size.U
				state               		:= sMINISPLIT
			}.otherwise{
				mini_addr					:= cmd_addr
				mini_len					:= cmd_len					
				state               		:= sLASTSPLIT
			}
		}
		is(sMINISPLIT){
			when(io.out.ready === 1.U){
				cmd_counter				:= cmd_counter + 1.U
				when(mini_len > mini_page_size.U){
					mini_addr				:= mini_addr + mini_page_size.U
					mini_len				:= mini_len - mini_page_size.U
					io.out.valid			:= 1.U
					io.out.bits				:= cmd_temp
					io.out.bits.len			:= mini_page_size.U
					io.out.bits.addr		:= mini_addr
				}.otherwise{
					io.out.valid			:= 1.U
					io.out.bits				:= cmd_temp
					io.out.bits.len			:= mini_len
					io.out.bits.addr		:= mini_addr	
					state					:= sSPLIT			
				}
			}
		}
		is(sLASTSPLIT){
			when(io.out.ready === 1.U){
				cmd_counter					:= cmd_counter + 1.U
				when(mini_len > mini_page_size.U){
					mini_addr				:= mini_addr + mini_page_size.U
					mini_len				:= mini_len - mini_page_size.U
					io.out.valid			:= 1.U
					io.out.bits				:= cmd_temp
					io.out.bits.len			:= mini_page_size.U
					io.out.bits.addr		:= mini_addr
				}.otherwise{
					io.out.valid			:= 1.U
					io.out.bits				:= cmd_temp
					io.out.bits.len			:= mini_len
					io.out.bits.addr		:= mini_addr	
					state					:= sIDLE			
				}
			}
		}		
	}

}
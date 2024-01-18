package CommSub

import chisel3._
import chisel3.util._
import common._

class C2H extends Module{
	val io = IO(new Bundle{
		val start_addr	= Input(UInt(64.W))
		val length		= Input(UInt(32.W))
		val start		= Input(Bool())
		val c2h_cmd		= Decoupled(new C2H_CMD)
		val pfch_tag	= Input(UInt(32.W))
		val complete	= Output(Bool())

		val count_time	= Output(UInt(32.W))
		val send_cmd_count 	= Output(UInt(32.W))
		val send_data_count = Output(UInt(64.W))

		val debug_clk	= Input(Clock())
	})
	
	val addr			= RegInit(UInt(64.W),0.U)
	val length			= RegInit(UInt(32.W),0.U)
	val valid_cmd		= RegInit(Bool(),false.B)
	val valid_data		= RegInit(UInt(32.W),0.U)
	val cur_q			= RegInit(UInt(11.W),0.U)

	val count_time		= RegInit(UInt(32.W),0.U)
	val send_cmd_count	= RegInit(UInt(32.W),0.U)
	val send_data_count = RegInit(UInt(64.W),0.U)
	
	val complete		= RegInit(Bool(),true.B)

	val hold			= RegInit(UInt(4.W),0.U)

	// cmd
	val cmd_bits		= io.c2h_cmd.bits
	cmd_bits			:= 0.U.asTypeOf(new C2H_CMD)
	cmd_bits.qid		:= cur_q
	cmd_bits.addr		:= addr
	cmd_bits.pfch_tag	:= io.pfch_tag
	cmd_bits.len		:= length
	io.c2h_cmd.valid 	:= valid_cmd

	// state machine
	val sIDLE :: sSEND :: sDONE :: Nil = Enum(3)
	val state_cmd		= RegInit(sIDLE)
	val state_data		= RegInit(sIDLE)
	
	switch(state_cmd){
		is(sIDLE){
			when(io.start){
				state_cmd			:= sSEND
				complete			:= false.B
				addr				:= io.start_addr
				length				:= io.length
				send_data_count		:= send_data_count + io.length
				count_time			:= 0.U
				cur_q				:= 0.U // := io.queue_num(10,0)
			}
		}
		is(sSEND){
			count_time				:= count_time + 1.U
			valid_cmd				:= true.B
			when(io.c2h_cmd.fire()){
				state_cmd			:= sDONE
				valid_cmd			:= false.B
				hold				:= 10.U
			}
		}
		is(sDONE){
			hold := hold - 1.U
			when (hold === 0.U){
				complete			:= true.B
				state_cmd			:= sIDLE
			}
		}
	}

	when(io.c2h_cmd.fire()){
		send_cmd_count	:= send_cmd_count + 1.U
	}

    io.complete			:= complete
    io.count_time		:= count_time
	io.send_cmd_count	:= send_cmd_count
	io.send_data_count	:= send_data_count

// class ila_c2h(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_c2h = Module(new ila_c2h(Seq(	
// 	io.start,
// 	io.start_addr,
// 	io.length,
// 	io.complete,
// 	length,
// 	addr,
// 	valid_cmd,
// 	cur_q,
// 	state_cmd,
// 	data_count,
// )))
// inst_ila_c2h.connect(io.debug_clk)

}
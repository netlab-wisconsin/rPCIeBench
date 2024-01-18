package CommSub

import chisel3._
import chisel3.util._
import common._

class H2C extends Module{
	val io = IO(new Bundle{
		val start_addr	= Input(UInt(64.W))
		val length		= Input(UInt(32.W))
		val start		= Input(Bool())
		val h2c_cmd		= Decoupled(new H2C_CMD)
		val h2c_data	= Flipped(Decoupled(new H2C_DATA))
		val complete	= Output(Bool())

		val count_time	= Output(UInt(32.W))
		val send_cmd_count 	= Output(UInt(32.W))
		val send_data_count = Output(UInt(64.W))
		
		val debug_clk	= Input(Clock())
	})

	val addr			= RegInit(UInt(64.W),0.U)
	val length 			= RegInit(UInt(32.W),0.U)
	val valid_cmd		= RegInit(Bool(),false.B)
	val cur_q			= RegInit(UInt(11.W),0.U)

	val count_time		= RegInit(UInt(32.W),0.U)
	val send_cmd_count	= RegInit(UInt(32.W),0.U)
	val send_data_count = RegInit(UInt(64.W),0.U)

	val complete		= RegInit(Bool(),true.B)

	val hold			= RegInit(UInt(4.W),0.U)

	// cmd
	val cmd_bits		= io.h2c_cmd.bits
	cmd_bits			:= 0.U.asTypeOf(new H2C_CMD)
	cmd_bits.sop		:= 1.U
	cmd_bits.eop		:= 1.U
	cmd_bits.len		:= length
	cmd_bits.qid		:= cur_q
	cmd_bits.addr		:= addr
	io.h2c_cmd.valid	:= valid_cmd
	
	// data
	val data_bits		= io.h2c_data.bits
	io.h2c_data.ready	:= true.B

	// state machine
	val sIDLE :: sSEND_CMD :: sDONE :: Nil = Enum(3) //must be lower case for the first letter!!!
	val state_cmd		= RegInit(sIDLE)

	switch(state_cmd){
		is(sIDLE){
			when(io.start){
				state_cmd			:= sSEND_CMD
				complete			:= false.B
				addr				:= io.start_addr
				length				:= io.length
				send_data_count		:= send_data_count + io.length
				count_time			:= 0.U
				cur_q				:= 0.U
			}
		}
		is(sSEND_CMD){
			count_time				:= count_time + 1.U
			valid_cmd				:= true.B
			when(io.h2c_cmd.fire()){
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

	when(io.h2c_cmd.fire()){
		send_cmd_count	:= send_cmd_count + 1.U
	}

    io.complete			:= complete
    io.count_time		:= count_time
	io.send_cmd_count	:= send_cmd_count
	io.send_data_count	:= send_data_count

// class ila_h2c(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_h2c = Module(new ila_h2c(Seq(	
// 	io.start,
// 	length,
// 	start_addr,
// 	valid_cmd,
// 	state_cmd,
// )))
// inst_ila_h2c.connect(io.debug_clk)

}
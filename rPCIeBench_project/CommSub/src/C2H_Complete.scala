package CommSub

import chisel3._
import chisel3.util._
import common._

class C2H_Complete extends Module{
	val io = IO(new Bundle{
		val h2c_cpt_addr	    = Input(UInt(64.W))
        val c2h_cpt_addr        = Input(UInt(64.W))
        val p2p_cpt_addr        = Input(UInt(64.W))
		val h2c_complete	    = Input(UInt(1.W))
        val c2h_complete        = Input(UInt(1.W))
        val p2p_complete        = Input(Bool())
        val pfch_tag            = Input(UInt(32.W))
		// val pfch_tag	        = Input(UInt(32.W))
		// val tag_index	        = Input(UInt(32.W))
        val h2c_start           = Input(Bool())
        val c2h_start           = Input(Bool())
        val h2c_cpt_complete    = Output(Bool())
        val c2h_cpt_complete    = Output(Bool())
        val p2p_cpt_complete    = Output(Bool())

		// val queue_num	        = Input(UInt(11.W))
        
        val polling             = Input(UInt(32.W))

		val c2h_cmd		        = Decoupled(new C2H_CMD)
		val c2h_data	        = Decoupled(new C2H_DATA) 
		val debug_clk	        = Input(Clock())
	})

    val h2c_cpt_start = (io.polling===1.U) & (io.h2c_complete===1.U) & !RegNext(io.h2c_complete===1.U)
    val c2h_cpt_start = (io.polling===1.U) & (io.c2h_complete===1.U) & !RegNext(io.c2h_complete===1.U)
    val p2p_cpt_start = (io.polling===1.U) & io.p2p_complete
    val cmd_valid = RegInit(Bool(), false.B)
    val data_valid = RegInit(Bool(), false.B)
    val cpt_addr = RegInit(UInt(64.W), 0.U)
    val h2c_cpt_addr = RegInit(UInt(64.W), 0.U)
    val c2h_cpt_addr = RegInit(UInt(64.W), 0.U)
    val p2p_cpt_addr = RegInit(UInt(64.W), 0.U)
    val h2c_cpt_complete = RegInit(Bool(), true.B)
    val c2h_cpt_complete = RegInit(Bool(), true.B)
    val p2p_cpt_complete = RegInit(Bool(), true.B)
    io.h2c_cpt_complete := h2c_cpt_complete
    io.c2h_cpt_complete := c2h_cpt_complete
    io.p2p_cpt_complete := p2p_cpt_complete

    when (io.h2c_start)
        {   
            h2c_cpt_addr := io.h2c_cpt_addr
            h2c_cpt_complete := false.B
        }
    when (io.c2h_start)
        {
            c2h_cpt_addr := io.c2h_cpt_addr
            c2h_cpt_complete := false.B
        }
    when (p2p_cpt_complete)
    {
        p2p_cpt_complete := false.B
    }

	// val tags = RegInit(VecInit(Seq.fill(1)(0.U(7.W))))
	// when(io.tag_index === (RegNext(io.tag_index)+1.U)){
	// 	tags(RegNext(io.tag_index))	:= io.pfch_tag
	// }

	val cur_q = RegInit(UInt(11.W),0.U)

    io.c2h_cmd.bits             := 0.U.asTypeOf(new C2H_CMD)
    io.c2h_cmd.bits.qid		    := cur_q
    io.c2h_cmd.bits.addr		:= cpt_addr
    // io.c2h_cmd.bits.pfch_tag	:= tags(0.U)
    io.c2h_cmd.bits.pfch_tag    := io.pfch_tag
    io.c2h_cmd.bits.len		    := 64.U
    io.c2h_cmd.valid            := cmd_valid

    io.c2h_data.bits            := 0.U.asTypeOf(new C2H_DATA)
    io.c2h_data.valid           := data_valid
	io.c2h_data.bits.data		:= Fill(512,1.U)
	io.c2h_data.bits.ctrl_qid	:= 0.U

    val cmd_fire = RegInit(Bool(), false.B)
    val data_fire = RegInit(Bool(), false.B)
    val p2p_started = RegInit(Bool(), false.B)
    val h2c_started = RegInit(Bool(), false.B)
    val c2h_started = RegInit(Bool(), false.B)

    val both = RegInit(Bool(), false.B)
    when (p2p_cpt_start)
        {
            cmd_valid := true.B
            data_valid := true.B
            cpt_addr := io.p2p_cpt_addr
            p2p_started := true.B
            // p2p_cpt_complete := true.B
        }
    when (h2c_cpt_start)
        {
            cmd_valid := true.B
            data_valid := true.B
            cpt_addr := h2c_cpt_addr
            h2c_started := true.B
            // h2c_cpt_complete := true.B
            // when (c2h_cpt_start) {both := true.B}
        }
    when (c2h_cpt_start)
        {
            cmd_valid := true.B
            data_valid := true.B
            cpt_addr := c2h_cpt_addr
            c2h_started := true.B
            // c2h_cpt_complete := true.B
            // when (both) {both := false.B}
        }
    when (io.c2h_cmd.fire())
        {
            cmd_valid := false.B
            cmd_fire := true.B
        }
    when (io.c2h_data.fire())
        {
            data_valid := false.B
            data_fire := true.B
        }
    when (cmd_fire & data_fire)
        {
            cmd_fire := false.B
            data_fire := false.B
            when (p2p_started)
            {
                p2p_started := false.B
                p2p_cpt_complete := true.B
            }
            when (h2c_started)
            {
                h2c_started := false.B
                h2c_cpt_complete := true.B
            }
            when (c2h_started)
            {
                c2h_started := false.B
                c2h_cpt_complete := true.B
            }
        }


// class ila_cpt(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_cpt = Module(new ila_cpt(Seq(	
	
//     cpt_addr,
//     // p2p_cpt_addr,
//     // p2p_start,
//     // p2p_cpt_complete,

// 	io.h2c_complete,
//     io.c2h_complete,
//     h2c_started,
//     c2h_started,
//     io.h2c_start,
//     io.c2h_start,
//     io.h2c_cpt_complete,
//     io.c2h_cpt_complete,

// 	cmd_valid,
// 	data_valid,

// )))
// inst_ila_cpt.connect(io.debug_clk)

}
package CommSub

import chisel3._
import chisel3.util._
import common._

class d2hcmdqueuehead() extends Module{
    val io = IO(new Bundle{
        val cmd_in                  = Flipped(Decoupled(new M2H_CMD))
        val cmd_out                 = Decoupled(new M2H_CMD)

        val c2h_length              = Output(UInt(32.W))
        val m2h_complete            = Output(Bool())
        val c2h_finish              = Input(Bool())
        val m2h_finish              = Input(Bool())
        val m2h_cpt_complete        = Input(Bool())

        val last                    = Output(Bool())
        val working                 = Output(Bool())
        val continue                = Output(Bool())
        // val debug_clk               = Input(Clock())
    })

    val cmd_in_ready                = RegInit(Bool(),true.B)
    val cmd_out_valid               = RegInit(Bool(),false.B)
    val m2h_complete                = RegInit(Bool(),false.B)
    val working                     = RegInit(Bool(),false.B)
    val last                        = RegInit(Bool(),false.B)
    val continue                    = RegInit(Bool(),false.B)
    io.cmd_in.ready                 := cmd_in_ready
    io.cmd_out.valid                := cmd_out_valid                  
    io.m2h_complete                 := m2h_complete
    io.last                         := last
    io.working                      := working
    io.continue                     := continue

    val length                      = RegInit(UInt(32.W),0.U)
    val c2h_addr                    = RegInit(UInt(64.W),0.U)
    val m2h_addr                    = RegInit(UInt(34.W),0.U)
    val length_reg                  = RegInit(UInt(32.W),0.U)
    val m2h_addr_reg                = RegInit(UInt(34.W),0.U)
    val next_addr                   = RegInit(UInt(34.W),0.U)
    val end_addr                    = RegInit(UInt(34.W),0.U)
    val c2h_cpt_addr                = RegInit(UInt(64.W),0.U)
    val pkt_size                    = RegInit(UInt(32.W),4096.U)
    val c2h_pfch_tag                = RegInit(UInt(32.W),0.U)
    val c2h_tag_index               = RegInit(UInt(32.W),0.U)

    io.cmd_out.bits.m2h_length      := length_reg
    io.c2h_length                   := length
    io.cmd_out.bits.c2h_start_addr  := c2h_addr
    io.cmd_out.bits.m2h_start_addr  := m2h_addr_reg

    // io.cmd_out.bits.c2h_pfch_tag    := c2h_pfch_tag
    // io.cmd_out.bits.c2h_tag_index   := c2h_tag_index
    // io.cmd_out.bits.c2h_queue_num   := c2h_queue_num
    io.cmd_out.bits.c2h_cpt_addr    := c2h_cpt_addr
    io.cmd_out.bits.pkt_size        := pkt_size

    when(io.cmd_in.fire()){
        c2h_addr                    := io.cmd_in.bits.c2h_start_addr
        m2h_addr                    := io.cmd_in.bits.m2h_start_addr
        m2h_addr_reg                := io.cmd_in.bits.m2h_start_addr
        end_addr                    := io.cmd_in.bits.m2h_start_addr + io.cmd_in.bits.m2h_length
        c2h_cpt_addr                := io.cmd_in.bits.c2h_cpt_addr
        pkt_size                    := io.cmd_in.bits.pkt_size
        // c2h_pfch_tag                := io.cmd_in.bits.c2h_pfch_tag
        // c2h_tag_index               := io.cmd_in.bits.c2h_tag_index
        // c2h_queue_num               := io.cmd_in.bits.c2h_queue_num
        when (io.cmd_in.bits.m2h_length > io.cmd_in.bits.pkt_size){
            length                  := io.cmd_in.bits.pkt_size
            length_reg              := io.cmd_in.bits.pkt_size
            next_addr               := io.cmd_in.bits.m2h_start_addr + io.cmd_in.bits.pkt_size
        } .otherwise{
            length                  := io.cmd_in.bits.m2h_length
            length_reg              := io.cmd_in.bits.m2h_length
            next_addr               := io.cmd_in.bits.m2h_start_addr + io.cmd_in.bits.m2h_length
        }
        cmd_in_ready                := false.B
        cmd_out_valid               := true.B
        working                     := true.B
    }

    when(io.cmd_out.fire()){
        length_reg                  := length
        m2h_addr_reg                := m2h_addr
        when (next_addr === end_addr){
            cmd_out_valid           := false.B
            last                    := true.B
            continue                := false.B
        } .otherwise{
            continue                := true.B
            c2h_addr                := c2h_addr + length
            m2h_addr                := m2h_addr + length
            when (next_addr + length < end_addr){
                length              := pkt_size
                next_addr           := next_addr + length
            } .otherwise{
                length              := end_addr - next_addr
                next_addr           := end_addr
            }
        }
    }

    when (continue){
        continue        := false.B
    }

    when (last & io.c2h_finish & io.m2h_finish){
        m2h_complete    := true.B
    }

    when (last & m2h_complete & io.m2h_cpt_complete){
        m2h_complete    := false.B
        last            := false.B
        cmd_in_ready    := true.B
        working         := false.B
        continue        := true.B
    }


// class ila_m2hhead(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_m2hhead = Module(new ila_m2hhead(Seq(	

//     cmd_in_ready,
//     cmd_out_valid,
//     m2h_complete,
//     length,
//     c2h_addr,
//     m2h_addr,
//     next_addr,
//     end_addr,
//     io.m2h_cpt_complete,
//     last,
//     io.cmd_out.ready,

// )))
// inst_ila_m2hhead.connect(io.debug_clk)
    

}
package CommSub

import chisel3._
import chisel3.util._
import common._

class h2dcmdqueuehead() extends Module{
    val io = IO(new Bundle{
        val cmd_in                  = Flipped(Decoupled(new H2M_CMD))
        val cmd_out                 = Decoupled(new H2M_CMD)

        val h2c_length              = Output(UInt(32.W))
        val h2m_complete            = Output(Bool())
        val h2m_cpt_complete        = Input(Bool())

        val last                    = Output(Bool())
        val h2m_last                = Output(Bool())
        val working                 = Output(Bool())
        val continue                = Output(Bool())
        // val debug_clk               = Input(Clock())
    })

    val cmd_in_ready                = RegInit(Bool(),true.B)
    val cmd_out_valid               = RegInit(Bool(),false.B)
    val h2m_complete                = RegInit(Bool(),false.B)
    val working                     = RegInit(Bool(),false.B)
    val last                        = RegInit(Bool(),false.B)
    val h2m_last                    = RegInit(Bool(),false.B)
    val continue                    = RegInit(Bool(),false.B)
    io.cmd_in.ready                 := cmd_in_ready
    io.cmd_out.valid                := cmd_out_valid                  
    io.h2m_complete                 := h2m_complete
    io.last                         := last
    io.h2m_last                     := h2m_last
    io.working                      := working
    io.continue                     := continue

    val length                      = RegInit(UInt(32.W),0.U)
    val h2c_addr                    = RegInit(UInt(64.W),0.U)
    val h2m_addr                    = RegInit(UInt(34.W),0.U)
    // val length_reg                  = RegInit(UInt(32.W),0.U)
    // val h2m_addr_reg                = RegInit(UInt(34.W),0.U)
    val next_addr                   = RegInit(UInt(34.W),0.U)
    val end_addr                    = RegInit(UInt(34.W),0.U)
    val h2c_cpt_addr                = RegInit(UInt(64.W),0.U)
    val pkt_size                    = RegInit(UInt(32.W),32768.U)
    io.cmd_out.bits.h2m_length      := length // length_reg
    io.h2c_length                   := length
    io.cmd_out.bits.h2c_start_addr  := h2c_addr
    io.cmd_out.bits.h2m_start_addr  := h2m_addr // h2m_addr_reg

    io.cmd_out.bits.h2c_cpt_addr    := h2c_cpt_addr
    io.cmd_out.bits.pkt_size        := pkt_size
    
    when(io.cmd_in.fire()){
        h2c_addr                    := io.cmd_in.bits.h2c_start_addr
        h2m_addr                    := io.cmd_in.bits.h2m_start_addr
        // h2m_addr_reg                := io.cmd_in.bits.h2m_start_addr
        end_addr                    := io.cmd_in.bits.h2m_start_addr + io.cmd_in.bits.h2m_length
        h2c_cpt_addr                := io.cmd_in.bits.h2c_cpt_addr
        pkt_size                    := io.cmd_in.bits.pkt_size
        when (io.cmd_in.bits.h2m_length > io.cmd_in.bits.pkt_size){
            length                  := io.cmd_in.bits.pkt_size
            // length_reg              := 32768.U
            next_addr               := io.cmd_in.bits.h2m_start_addr + io.cmd_in.bits.pkt_size
        } .otherwise{
            h2m_last                := true.B
            length                  := io.cmd_in.bits.h2m_length
            // length_reg              := io.cmd_in.bits.h2m_length
            next_addr               := io.cmd_in.bits.h2m_start_addr + io.cmd_in.bits.h2m_length
        }
        cmd_in_ready                := false.B
        cmd_out_valid               := true.B
        working                     := true.B
    }

    when(io.cmd_out.fire()){
        // length_reg                  := length
        // h2m_addr_reg                := h2m_addr
        when (next_addr === end_addr){
            cmd_out_valid           := false.B
            last                    := true.B
            h2m_last                := false.B
            continue                := false.B
        } .otherwise{
            continue                := true.B
            h2c_addr                := h2c_addr + length
            h2m_addr                := h2m_addr + length
            when (next_addr + length < end_addr){
                length              := pkt_size
                next_addr           := next_addr + length
                h2m_last            := false.B
            } .otherwise{
                length              := end_addr - next_addr
                next_addr           := end_addr
                h2m_last            := true.B
            }
        }
    }

    when (continue){
        continue        := false.B
    }

    when (last & io.cmd_out.ready){
        h2m_complete    := true.B
    }

    when (last & h2m_complete & io.h2m_cpt_complete){
        h2m_complete    := false.B
        last            := false.B
        cmd_in_ready    := true.B
        working         := false.B
        continue        := true.B
    }


// class ila_h2mhead(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_h2mhead = Module(new ila_h2mhead(Seq(	

//     h2m_complete,
//     last,
//     io.cmd_out.ready,
//     io.h2m_cpt_complete,
//     // cmd_in_ready,
//     // cmd_out_valid,
//     // h2m_complete,
//     // length,
//     // h2c_addr,
//     // h2m_addr,
//     // next_addr,
//     // end_addr,
//     // io.h2m_cpt_complete,
//     // last,
//     // io.cmd_out.ready,

// )))
// inst_ila_h2mhead.connect(io.debug_clk)
}
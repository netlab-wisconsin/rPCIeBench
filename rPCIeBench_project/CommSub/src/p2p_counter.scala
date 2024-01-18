package CommSub

import chisel3._
import chisel3.util._
import common._

class p2p_counter() extends Module{
    val io = IO(new Bundle{
        val start               = Input(Bool())
        val length              = Input(UInt(32.W))
        val cpt_addr            = Input(UInt(64.W))

        val wready              = Input(Bool())
        val wvalid              = Input(Bool())
        val wdatasample         = Input(UInt(32.W))

        val p2p_complete        = Output(Bool())
        val p2p_cpt_addr        = Output(UInt(64.W))
        val p2p_cpt_complete    = Input(Bool())

        val debug_clk           = Input(Clock())
    })

    val counter  = RegInit(UInt(32.W),0.U)
    when (io.start & io.wready & io.wvalid & io.wdatasample === Fill(32,1.U))
    {
        counter := counter + 32.U
    }
    val length = RegInit(UInt(32.W), 0.U)
    val cpt_addr = RegInit(UInt(64.W), 0.U)
    val p2p_complete = RegInit(Bool(), false.B)
    val working = RegInit(Bool(), false.B)
    io.p2p_complete := p2p_complete
    io.p2p_cpt_addr := cpt_addr

    when (io.start)
    {
        length      := io.length
        working     := true.B
        cpt_addr    := io.cpt_addr
    }.otherwise
    {
        working     := false.B
        counter     := 0.U
    }

    when (working & counter === length)
    {
        p2p_complete := true.B
        counter     := 0.U
    }

    when (io.p2p_cpt_complete & p2p_complete)
    {
        p2p_complete := false.B
    }


// class ila_p2pcount(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_p2pcount = Module(new ila_p2pcount(Seq(

//     counter,
//     length,
//     working,
//     p2p_complete,
//     io.p2p_cpt_complete,

// )))
// inst_ila_p2pcount.connect(io.debug_clk)

}
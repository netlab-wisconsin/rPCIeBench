package CommSub

import chisel3._
import chisel3.util._
import common._

class h2mcmdbufferready() extends Module{
    val io = IO(new Bundle{
        val ready   = Output(Bool())
        val valid   = Input(Bool())
        val complete = Input(Bool())
    })

    val ready = RegInit(Bool(), false.B)
    val hold  = RegInit(Bool(), true.B)
    io.ready := ready

    when (hold)
    {
        ready := io.complete
    } .otherwise{
        ready := false.B
    }

    when (io.ready & io.valid)
    {
        ready := false.B
        hold := false.B
    }

    when (io.complete && !RegNext(io.complete))
    {
        hold := true.B
    }

}
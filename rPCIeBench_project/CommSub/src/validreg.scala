package CommSub

import chisel3._
import chisel3.util._
import common._

class validreg() extends Module{
    val io = IO(new Bundle{
        val ready   = Input(UInt(1.W))
        val valid   = Input(UInt(1.W))
        val tmpreg  = Output(UInt(1.W))

        // val data    = Input(UInt(256.W))
        // val regdata = Output(UInt(256.W))
    })

    val tmpreg  = RegInit(UInt(1.W),0.U)
    // val regdata = RegInit(UInt(256.W),0.U)

    when(io.valid === 1.U & io.ready === 0.U)
        {tmpreg := 1.U}
    
    when(tmpreg === 1.U & io.ready === 1.U)
        {tmpreg := 0.U}

    io.tmpreg := tmpreg

    // when(io.valid === 1.U)
    //     {regdata := io.data}

    // io.regdata := regdata
}
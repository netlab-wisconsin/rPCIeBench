package CommSub

import chisel3._
import chisel3.util._
import common._

class c2hcounter() extends Module{
    val io = IO(new Bundle{
        val counter1 = Output(UInt(64.W))
        val counter2 = Output(UInt(64.W))
        val counter3 = Output(UInt(64.W))
        val counter4 = Output(UInt(64.W))
        val counter5 = Output(UInt(64.W))
        val counter6 = Output(UInt(64.W))
        val fire1    = Input(Bool())
        val fire2    = Input(Bool())
        val fire3    = Input(Bool())
        val fire4    = Input(Bool())
        val fire5    = Input(Bool())
        val fire6    = Input(Bool())
    })

    val counter1  = RegInit(UInt(64.W),0.U)
    val counter2  = RegInit(UInt(64.W),0.U)
    val counter3  = RegInit(UInt(64.W),0.U)
    val counter4  = RegInit(UInt(64.W),0.U)
    val counter5  = RegInit(UInt(64.W),0.U)
    val counter6  = RegInit(UInt(64.W),0.U)

    when(io.fire1){counter1 := counter1 + 1.U}
    when(io.fire2){counter2 := counter2 + 1.U}
    when(io.fire3){counter3 := counter3 + 1.U}
    when(io.fire4){counter4 := counter4 + 1.U}
    when(io.fire5){counter5 := counter5 + 1.U}
    when(io.fire6){counter6 := counter6 + 1.U}

    io.counter1 := counter1
    io.counter2 := counter2
    io.counter3 := counter3
    io.counter4 := counter4
    io.counter5 := counter5
    io.counter6 := counter6

}
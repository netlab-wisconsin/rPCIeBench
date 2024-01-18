package common

import chisel3._
import chisel3.util._

object OffsetGenerator{
    def apply(num:UInt,range:UInt,step:UInt,en:Bool,num_width:Int = 8,addr_width:Int=32) = {
        val t = Module(new OffsetGenerator(num_width,addr_width))
        t.io.num        := num
        t.io.range      := range
        t.io.step       := step
        t.io.en         := en
        t.io.offset
    }
    
}
class OffsetGenerator(num_width:Int,addr_width:Int) extends Module{
    val io = IO(new Bundle{
        val num     = Input(UInt(num_width.W))
        val range   = Input(UInt(addr_width.W))
        val step    = Input(UInt(addr_width.W))
        val en      = Input(Bool())

        val offset  = Output(UInt(addr_width.W))
    })

    val outer_offset    = RegInit(UInt(addr_width.W),0.U)
    val inner_offset    = RegInit(UInt(addr_width.W),0.U)
    val cur_idx         = RegInit(UInt(num_width.W),0.U)

    when(io.en){
        when(cur_idx+1.U === io.num){
            when(inner_offset+io.step === io.range){
                inner_offset    := 0.U
            }.otherwise{
                inner_offset    := inner_offset+io.step
            }
            
            cur_idx         := 0.U
            outer_offset    := 0.U
        }.otherwise{
            cur_idx         := cur_idx+1.U
            outer_offset    := outer_offset+io.range
			inner_offset	:= inner_offset
        }
    }
    io.offset           := inner_offset+outer_offset
}
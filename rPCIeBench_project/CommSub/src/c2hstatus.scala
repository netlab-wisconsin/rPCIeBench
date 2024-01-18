package CommSub

import chisel3._
import chisel3.util._
import common._

class c2h_status() extends Module{
	val io = IO(new Bundle{
        val c2h_start           = Input(UInt(1.W))
		val c2h_status_last		= Input(UInt(1.W))
		val c2h_status_cmp		= Input(UInt(1.W))
		val c2h_status_valid	= Input(UInt(1.W))
        val c2h_status_error	= Input(UInt(1.W))
        val c2h_status_drop     = Input(UInt(1.W))
        val c2h_status_last_count   = Output(UInt(32.W))
        val c2h_status_cmp_count    = Output(UInt(32.W))
        val c2h_status_valid_count  = Output(UInt(32.W))
        val c2h_status_error_count  = Output(UInt(32.W))
        val c2h_status_drop_count   = Output(UInt(32.W))
	})

    val c2h_status_last_count   = RegInit(UInt(32.W),0.U)
    val c2h_status_cmp_count    = RegInit(UInt(32.W),0.U)
    val c2h_status_valid_count  = RegInit(UInt(32.W),0.U)
    val c2h_status_error_count  = RegInit(UInt(32.W),0.U)
    val c2h_status_drop_count   = RegInit(UInt(32.W),0.U)

    when (io.c2h_start === 1.U){
        c2h_status_last_count   := 0.U
        c2h_status_cmp_count    := 0.U
        c2h_status_valid_count  := 0.U
        c2h_status_error_count  := 0.U
        c2h_status_drop_count   := 0.U
    }

    when (io.c2h_status_last === 1.U & io.c2h_status_valid === 1.U)
        {c2h_status_last_count := c2h_status_last_count + 1.U}
    when (io.c2h_status_cmp === 1.U & io.c2h_status_valid === 1.U)
        {c2h_status_cmp_count := c2h_status_cmp_count + 1.U}
    when (io.c2h_status_valid === 1.U)
        {c2h_status_valid_count := c2h_status_valid_count + 1.U}
    when (io.c2h_status_error === 1.U & io.c2h_status_valid === 1.U)
        {c2h_status_error_count := c2h_status_error_count + 1.U}
    when (io.c2h_status_drop === 1.U & io.c2h_status_valid === 1.U)
        {c2h_status_drop_count := c2h_status_drop_count + 1.U}
    io.c2h_status_last_count := c2h_status_last_count
    io.c2h_status_cmp_count := c2h_status_cmp_count
    io.c2h_status_valid_count := c2h_status_valid_count
    io.c2h_status_error_count := c2h_status_error_count
    io.c2h_status_drop_count := c2h_status_drop_count
}
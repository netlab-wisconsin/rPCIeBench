package common.storage

import common.Math
import chisel3._
import chisel3.util._

object cdc_array{

    def apply(WIDTH:Int)(in_clk:Clock, out_clk:Clock) = {
        val cdc = Module(new xpm_cdc_array_single(WIDTH))
        cdc.io.src_clk     := in_clk
        cdc.io.dest_clk    := out_clk
        cdc
    }

    class xpm_cdc_array_single(
        WIDTH:Int
    ) extends BlackBox(Map(
        "DEST_SYNC_FF"              -> 4,
        "INIT_SYNC_FF"              -> 0,
        "SIM_ASSERT_CHK"            -> 0,
        "SRC_INPUT_REG"             -> 1,
        "WIDTH"                     -> WIDTH
        )){
        val io = IO(new Bundle{
            val dest_out            = Output(UInt(WIDTH.W))
            val dest_clk            = Input(Clock())
            val src_clk             = Input(Clock())
            val src_in              = Input(UInt(WIDTH.W))
        })
    }
}



object cdc_pulse{

    def apply(in_clk:Clock, out_clk:Clock) = {
        val cdc = Module(new xpm_cdc_pulse)
        cdc.io.src_clk      := in_clk
        cdc.io.dest_clk     := out_clk
        cdc
    }


    class xpm_cdc_pulse() extends BlackBox(Map(
        "DEST_SYNC_FF"              -> 4,
        "INIT_SYNC_FF"              -> 0,
        "REG_OUTPUT"                -> 1,
        "RST_USED"                  -> 0,
        "SIM_ASSERT_CHK"            -> 0,
        )){
        val io = IO(new Bundle{
            val dest_pulse          = Output(UInt(1.W))
            val dest_clk            = Input(Clock())
            val src_clk             = Input(Clock())
            val src_pulse           = Input(UInt(1.W))
        })
    }
}


package CommSub

import chisel3._
import chisel3.util._
import common._

class M2H extends Module{
    val io = IO(new Bundle{
		val start_addr	= Input(UInt(34.W))
		val length		= Input(UInt(32.W))
        val start		= Input(Bool())
        val complete    = Output(Bool())

        val araddr      = Output(UInt(34.W))
        val arvalid     = Output(UInt(1.W))
        val arready     = Input(UInt(1.W))
        val arlen       = Output(UInt(8.W))

        val rfire       = Input(Bool())
        
        val debug_clk   = Input(Clock())

        val last        = Input(Bool())

        val m2h_queue_empty = Input(Bool())
        val m2h_valid_tmpreg = Input(Bool())
        val read_count_equal = Output(Bool())
    })

    val sIDLE :: sAR :: sR :: sFinish :: Nil = Enum(4)
    val state           = RegInit(sIDLE)
    val init            = RegNext(io.start)

    val length          = RegInit(UInt(32.W),0.U)
    val total_length    = RegInit(UInt(32.W),0.U)
    val complete        = RegInit(Bool(),true.B)
    val araddr          = RegInit(UInt(34.W),0.U)
    val arvalid         = RegInit(UInt(1.W),0.U)
    val arlen           = RegInit(UInt(8.W),0.U)
    val read_count      = RegInit(UInt(32.W),0.U)
    val hold1           = RegInit(UInt(4.W),10.U)
    val hold2           = RegInit(UInt(4.W),5.U)

    val end_addr        = RegInit(UInt(34.W),0.U)
    val tmp_addr        = RegInit(UInt(34.W),0.U)
    val next_addr       = RegInit(UInt(34.W),0.U)

    val data_count = RegInit(UInt(64.W),0.U)

    when (io.rfire)
        {read_count := read_count + 32.U}
    io.read_count_equal := (read_count === total_length)
    
    switch(state){
        is(sIDLE){
            when (io.start){
                total_length    := total_length + io.length
                length          := io.length
                data_count		:= data_count + io.length
                hold1           := 10.U
                hold2           := 5.U
                complete        := false.B
                end_addr        := io.start_addr + io.length  // final address
                tmp_addr        := io.start_addr                 // current address

                when (512.U < io.start_addr(8,0) + io.length){
                    next_addr   := Cat(io.start_addr(33,9)+1.U,0.U(9.W))
                } .otherwise{
                    next_addr   := io.start_addr + io.length
                } // next address is the smaller of 512B address boundry and final address.

            }
            when (init)
            {
                tmp_addr        := next_addr
                araddr          := tmp_addr

                when (next_addr(33,9)+1.U <= end_addr(33,9)){
                    next_addr   := next_addr + 512.U //Cat(next_addr(33,9)+1.U,0.U(9.W))
                } .otherwise{
                    next_addr   := end_addr
                }
                arlen       := (next_addr-tmp_addr)(8,5) - 1.U  // remember number of transfers: Length = Axlen + 1
                when (length === 0.U) {state := sFinish} .otherwise {state := sAR}  // now start address write
                arvalid         := 1.U                              // ready
            }
        }
        is(sAR){
            when ((arvalid & io.arready) === 1.U){
                tmp_addr        := next_addr
                araddr          := tmp_addr

                when (next_addr(33,9)+1.U <= end_addr(33,9)){
                    next_addr   := next_addr + 512.U //Cat(next_addr(33,9)+1.U,0.U(9.W))
                } .otherwise{
                    next_addr   := end_addr
                }
                arlen       := (next_addr-tmp_addr)(8,5) - 1.U  // remember number of transfers: Length = Axlen + 1
                when (tmp_addr === end_addr) {
                    arvalid     := 0.U
                    state       := sFinish
                }    // already reaches the end address, H2M finish.
            }
        }
        is(sFinish){
            when (io.last){
                when (read_count === total_length & hold1 =/= 0.U)
                    {
                        hold1 := hold1 - 1.U
                    }
                when (hold1 === 0.U & io.m2h_queue_empty & hold2 =/= 0.U)
                    {
                        hold2 := hold2 - 1.U
                    }
                when (hold2 === 0.U & !io.m2h_valid_tmpreg)
                    {
                        complete    := true.B
                        state       := sIDLE
                        araddr      := 0.U
                        arvalid     := 0.U
                        arlen       := 0.U
                    }
                .otherwise
                    {complete       := false.B}
            }. otherwise{
                complete            := true.B
                state               := sIDLE
                araddr              := 0.U
                arvalid             := 0.U
                arlen               := 0.U
            }

        }
    }

    io.araddr               := araddr
    io.arvalid              := arvalid
    io.arlen                := Cat(0.U(3.W),arlen)
    io.complete             := complete

// class ila_m2h(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_m2h = Module(new ila_m2h(Seq(	

//     io.start,
//     io.last,
//     // io.araddr,
//     // io.arvalid,
//     // io.arready,
//     // io.arlen,
//     io.complete,
//     hold1,
//     hold2,
//     io.m2h_valid_tmpreg,
//     io.m2h_queue_empty,
//     state,
    
//     total_length,
//     read_count,
//     data_count,
    
// )))
// inst_ila_m2h.connect(io.debug_clk)
}
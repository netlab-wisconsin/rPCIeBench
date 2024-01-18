package CommSub

import chisel3._
import chisel3.util._
import common._

class H2M() extends Module{
    val io = IO(new Bundle{
        val start_addr	= Input(UInt(34.W))
        val length		= Input(UInt(32.W))
        val start		= Input(Bool())
        val complete    = Output(Bool())

        val awaddr      = Output(UInt(34.W))
        val awvalid     = Output(UInt(1.W))
        val awready     = Input(UInt(1.W))
        val awlen       = Output(UInt(8.W))
        
        val wfire       = Input(Bool())

        val fifo_rden   = Output(Bool())

        val wlast       = Output(UInt(1.W))
        val debug_clk   = Input(Clock())

        val last        = Input(Bool())
        val clear       = Output(Bool())
    })

    val sIDLE :: sAW ::sW :: sFinish :: Nil = Enum(4)
    val state           = RegInit(sIDLE)
    val init            = RegNext(io.start)

    val length          = RegInit(UInt(32.W),0.U)
    val total_length    = RegInit(UInt(32.W),0.U)
    val complete        = RegInit(Bool(),true.B)
    val awaddr          = RegInit(UInt(34.W),0.U)
    val awvalid         = RegInit(UInt(1.W),0.U)
    val awlen           = RegInit(UInt(8.W),0.U)
    val wlast           = RegInit(UInt(1.W),0.U)
    val last            = RegInit(Bool(),false.B)
    val clear           = RegInit(Bool(),false.B)
    val write_count     = RegInit(UInt(32.W),0.U)

    val end_addr        = RegInit(UInt(34.W),0.U)
    val tmp_addr        = RegInit(UInt(34.W),0.U)
    val next_addr       = RegInit(UInt(34.W),0.U)
    val write_len       = RegInit(UInt(8.W),0.U)

    io.fifo_rden        := (state === sW)

    when (io.wfire)
        {write_count := write_count + 32.U}

    switch(state){
        is(sIDLE){
            when (io.start){
                clear           := false.B
                last            := io.last
                total_length    := total_length + io.length
                length          := io.length
                complete        := false.B
                wlast           := 0.U
                end_addr        := io.start_addr + io.length  // final address
                tmp_addr        := io.start_addr                 // current address
                awaddr          := io.start_addr                // first address write is start address
                when (512.U < io.start_addr(8,0) + io.length){
                    next_addr   := Cat(io.start_addr(33,9)+1.U,0.U(9.W))
                } .otherwise{
                    next_addr   := io.start_addr + io.length
                } // next address is the smaller of 512B address boundry and final address.
            }
            when (init){
                when (length === 0.U) {
                    state       := sFinish
                } .otherwise {
                    state       := sAW                                // now start address write
                    write_len   := 0.U                          // start counting write transfers
                }  
                awaddr          := tmp_addr
                tmp_addr        := next_addr
                when (next_addr(33,9)+1.U <= end_addr(33,9)){
                    next_addr   := next_addr + 512.U //Cat(next_addr(33,9)+1.U,0.U(9.W))
                } .otherwise{
                    next_addr   := end_addr
                }
                awvalid  := 1.U                             // ready
                awlen   := (next_addr-tmp_addr)(8,5) - 1.U  // remember number of transfers: Length = Axlen + 1
            }
        }
        is(sAW){
            when ((awvalid & io.awready) === 1.U){                           // start address write
                awvalid     := 0.U                             // stop ready signal
                state       := sW                               // write transaction starts
            }

            when (io.wfire){
                write_len               := write_len + 1.U    // one successful transfer
                when (awlen === 0.U) {  // all transfers finish
                    when (tmp_addr === end_addr) {
                        state           := sFinish    // already reaches the end address, H2M finish.
                    }
                    .otherwise {
                        state           := sAW
                        write_len       := 0.U
                        awaddr          := tmp_addr
                        tmp_addr        := next_addr
                        when (next_addr(33,9)+1.U <= end_addr(33,9)){
                            next_addr   := next_addr + 512.U //Cat(next_addr(33,9)+1.U,0.U(9.W))
                        } .otherwise{
                            next_addr   := end_addr
                        }
                        awvalid         := 1.U
                        awlen       := (next_addr-tmp_addr)(8,5) - 1.U  // remember number of transfers: Length = Axlen + 1
                    }
                } 
            }
        }
        is(sW){
            when (io.wfire){
                when (write_len === awlen - 1.U){
                    wlast               := 1.U                    // the last transfer, wlast = 1
                } .otherwise{
                    wlast               := 0.U
                }
                write_len               := write_len + 1.U    // one successful transfer
                when (write_len === awlen) {  // all transfers finish
                    when (tmp_addr === end_addr) {
                        state           := sFinish    // already reaches the end address, H2M finish.
                    }
                    .otherwise {
                        state           := sAW
                        write_len       := 0.U
                        awaddr          := tmp_addr
                        tmp_addr        := next_addr
                        when (next_addr(33,9)+1.U <= end_addr(33,9)){
                            next_addr   := next_addr + 512.U //Cat(next_addr(33,9)+1.U,0.U(9.W))
                        } .otherwise{
                            next_addr   := end_addr
                        }
                        awvalid         := 1.U
                        awlen       := (next_addr-tmp_addr)(8,5) - 1.U  // remember number of transfers: Length = Axlen + 1
                    }
                } 
            }
        }
        is(sFinish){
            when (last){
                when (write_count === total_length)
                    {
                        state           := sIDLE
                        awaddr          := 0.U
                        awvalid         := 0.U
                        awlen           := 0.U
                        wlast           := 0.U

                        write_len       := 0.U
                        complete        := true.B
                        clear           := true.B
                    }
                .otherwise
                    {complete       := false.B}
            }. otherwise{
                state           := sIDLE
                awaddr          := 0.U
                awvalid         := 0.U
                awlen           := 0.U
                wlast           := 0.U

                write_len       := 0.U
                complete        := true.B
            }
        }
    }

    io.awaddr               := awaddr
    io.awvalid              := awvalid
    io.awlen                := Cat(0.U(3.W),awlen)
    io.wlast                := wlast
    io.complete             := complete
    io.clear                := clear

// class ila_h2m(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_h2m = Module(new ila_h2m(Seq(	

//     last,
//     write_count,
//     total_length,
//     clear,
    
//     // io.start,
//     // io.awaddr,
//     // io.awvalid,
//     // io.awready,
//     // io.awlen,

//     // io.fifo_rden,
//     // state,
//     // complete,
//     // end_addr,
// )))
// inst_ila_h2m.connect(io.debug_clk)
}
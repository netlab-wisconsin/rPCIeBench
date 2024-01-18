package CommSub

import chisel3._
import chisel3.util._
import common._
import common.storage._

class h2dcmdqueue() extends Module{
    val io = IO(new Bundle{
        val cmd_in              = Flipped(Decoupled(new H2M_CMD))
        val qin                 = Input(UInt(32.W))

        val cmd_out             = Decoupled(new H2M_CMD)
        val h2c_length          = Output(UInt(32.W))
        val h2m_complete        = Output(Bool())
        val h2m_cpt_complete    = Input(Bool())

        val last                = Output(Bool())
        val h2m_last            = Output(Bool())
        val debug_clk           = Input(Clock())

        val counter             = Output(UInt(32.W))
    })

    val q_num = 16
    val Q = (0 until q_num).map(x => XQueue(new H2M_CMD(),64)).toList
    val Qh = (0 until q_num).map(x => Module(new h2dcmdqueuehead())).toList

    val counter = RegInit(UInt(32.W),0.U)
    io.counter := counter
    when (io.cmd_out.fire())
    {
        counter := counter + 1.U
    }

    val out  = RegInit(UInt(5.W),0.U)
    val next = RegInit(UInt(5.W),0.U)

    for(i <- 0 until q_num){
        Q(i).io.in.valid            := io.cmd_in.valid & io.qin(i)
        Q(i).io.in.bits             := io.cmd_in.bits
        Qh(i).io.cmd_in             <> Q(i).io.out
        Qh(i).io.cmd_out.ready      := io.cmd_out.ready & (out === i.U)
        Qh(i).io.h2m_cpt_complete   := io.h2m_cpt_complete & (out === i.U)
    }

    io.cmd_in.ready := true.B

    for(i <- 0 until q_num){
        when (Qh(i).io.working === false.B & out === i.U){
            out := next
        }
        when (Qh(i).io.continue === true.B & out === i.U){
            out := next
            when (next === q_num.U - 1.U){
                next := 0.U
            } .otherwise{
                next := next + 1.U
            }
        }
        when(Qh(i).io.working === false.B & next === i.U){
            when (next === q_num.U - 1.U){
                next := 0.U
            } .otherwise{
                next := next + 1.U
            }
        }
    }

    io.cmd_out.bits := MuxLookup(out, Qh(0).io.cmd_out.bits, (Array(
        0.U -> Qh(0).io.cmd_out.bits,
        1.U -> Qh(1).io.cmd_out.bits,
        2.U -> Qh(2).io.cmd_out.bits,
        3.U -> Qh(3).io.cmd_out.bits,
        4.U -> Qh(4).io.cmd_out.bits,
        5.U -> Qh(5).io.cmd_out.bits,
        6.U -> Qh(6).io.cmd_out.bits,
        7.U -> Qh(7).io.cmd_out.bits,
        8.U -> Qh(8).io.cmd_out.bits,
        9.U -> Qh(9).io.cmd_out.bits,
        10.U -> Qh(10).io.cmd_out.bits,
        11.U -> Qh(11).io.cmd_out.bits,
        12.U -> Qh(12).io.cmd_out.bits,
        13.U -> Qh(13).io.cmd_out.bits,
        14.U -> Qh(14).io.cmd_out.bits,
        15.U -> Qh(15).io.cmd_out.bits,
    )))
    io.cmd_out.valid := MuxLookup(out, Qh(0).io.cmd_out.valid, (Array(
        0.U -> Qh(0).io.cmd_out.valid,
        1.U -> Qh(1).io.cmd_out.valid,
        2.U -> Qh(2).io.cmd_out.valid,
        3.U -> Qh(3).io.cmd_out.valid,
        4.U -> Qh(4).io.cmd_out.valid,
        5.U -> Qh(5).io.cmd_out.valid,
        6.U -> Qh(6).io.cmd_out.valid,
        7.U -> Qh(7).io.cmd_out.valid,
        8.U -> Qh(8).io.cmd_out.valid,
        9.U -> Qh(9).io.cmd_out.valid,
        10.U -> Qh(10).io.cmd_out.valid,
        11.U -> Qh(11).io.cmd_out.valid,
        12.U -> Qh(12).io.cmd_out.valid,
        13.U -> Qh(13).io.cmd_out.valid,
        14.U -> Qh(14).io.cmd_out.valid,
        15.U -> Qh(15).io.cmd_out.valid,
    )))
    io.h2c_length := MuxLookup(out, Qh(0).io.h2c_length, (Array(
        0.U -> Qh(0).io.h2c_length,
        1.U -> Qh(1).io.h2c_length,
        2.U -> Qh(2).io.h2c_length,
        3.U -> Qh(3).io.h2c_length,
        4.U -> Qh(4).io.h2c_length,
        5.U -> Qh(5).io.h2c_length,
        6.U -> Qh(6).io.h2c_length,
        7.U -> Qh(7).io.h2c_length,
        8.U -> Qh(8).io.h2c_length,
        9.U -> Qh(9).io.h2c_length,
        10.U -> Qh(10).io.h2c_length,
        11.U -> Qh(11).io.h2c_length,
        12.U -> Qh(12).io.h2c_length,
        13.U -> Qh(13).io.h2c_length,
        14.U -> Qh(14).io.h2c_length,
        15.U -> Qh(15).io.h2c_length,
    )))
    io.h2m_complete := MuxLookup(out, Qh(0).io.h2m_complete, (Array(
        0.U -> Qh(0).io.h2m_complete,
        1.U -> Qh(1).io.h2m_complete,
        2.U -> Qh(2).io.h2m_complete,
        3.U -> Qh(3).io.h2m_complete,
        4.U -> Qh(4).io.h2m_complete,
        5.U -> Qh(5).io.h2m_complete,
        6.U -> Qh(6).io.h2m_complete,
        7.U -> Qh(7).io.h2m_complete,
        8.U -> Qh(8).io.h2m_complete,
        9.U -> Qh(9).io.h2m_complete,
        10.U -> Qh(10).io.h2m_complete,
        11.U -> Qh(11).io.h2m_complete,
        12.U -> Qh(12).io.h2m_complete,
        13.U -> Qh(13).io.h2m_complete,
        14.U -> Qh(14).io.h2m_complete,
        15.U -> Qh(15).io.h2m_complete,
    )))
    io.last := MuxLookup(out, Qh(0).io.last, (Array(
        0.U -> Qh(0).io.last,
        1.U -> Qh(1).io.last,
        2.U -> Qh(2).io.last,
        3.U -> Qh(3).io.last,
        4.U -> Qh(4).io.last,
        5.U -> Qh(5).io.last,
        6.U -> Qh(6).io.last,
        7.U -> Qh(7).io.last,
        8.U -> Qh(8).io.last,
        9.U -> Qh(9).io.last,
        10.U -> Qh(10).io.last,
        11.U -> Qh(11).io.last,
        12.U -> Qh(12).io.last,
        13.U -> Qh(13).io.last,
        14.U -> Qh(14).io.last,
        15.U -> Qh(15).io.last,
    )))
    io.h2m_last := MuxLookup(out, Qh(0).io.h2m_last, (Array(
        0.U -> Qh(0).io.h2m_last,
        1.U -> Qh(1).io.h2m_last,
        2.U -> Qh(2).io.h2m_last,
        3.U -> Qh(3).io.h2m_last,
        4.U -> Qh(4).io.h2m_last,
        5.U -> Qh(5).io.h2m_last,
        6.U -> Qh(6).io.h2m_last,
        7.U -> Qh(7).io.h2m_last,
        8.U -> Qh(8).io.h2m_last,
        9.U -> Qh(9).io.h2m_last,
        10.U -> Qh(10).io.h2m_last,
        11.U -> Qh(11).io.h2m_last,
        12.U -> Qh(12).io.h2m_last,
        13.U -> Qh(13).io.h2m_last,
        14.U -> Qh(14).io.h2m_last,
        15.U -> Qh(15).io.h2m_last,
    )))

// class ila_h2mcmdqueue(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_h2mcmdqueue = Module(new ila_h2mcmdqueue(Seq(	

//     io.h2m_complete,
//     out,
//     io.last,
//     io.h2m_last,
//     io.cmd_out.ready,
//     io.h2m_cpt_complete,

//     // io.cmd_in.valid,

//     // io.qin,

//     // io.cmd_out.ready,
//     // // io.cmd_out.valid,
//     // io.cmd_out.bits.h2m_start_addr,
//     // io.cmd_out.bits.h2m_length,

//     // io.h2c_length,
//     // io.h2m_complete,
//     // io.h2m_cpt_complete,
//     // io.last,

//     // out,
//     // next,

//     Q(0).io.in.ready,
//     Q(0).io.in.valid,
//     Q(0).io.out.ready,
//     Q(0).io.out.valid,
//     Qh(0).io.continue,
//     Qh(0).io.working,

// )))
// inst_ila_h2mcmdqueue.connect(io.debug_clk)
}
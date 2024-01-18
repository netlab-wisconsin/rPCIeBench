package CommSub

import chisel3._
import chisel3.util._
import common._
import common.storage._

class d2hcmdqueue() extends Module{
    val io = IO(new Bundle{
        val cmd_in              = Flipped(Decoupled(new M2H_CMD))
        val qin                 = Input(UInt(32.W))

        val cmd_out             = Decoupled(new M2H_CMD)
        val c2h_length          = Output(UInt(32.W))
        val m2h_complete        = Output(Bool())
        val c2h_finish          = Input(Bool())
        val m2h_finish          = Input(Bool())
        val m2h_cpt_complete    = Input(Bool())
        val read_count_equal    = Input(Bool())
        val empty               = Input(Bool())

        val h2m_complete_start  = Input(Bool())
        val h2m_complete        = Output(Bool())
        val h2m_cpt_complete    = Input(Bool())
        val m2h_valid_tmpreg    = Input(Bool())

        val last                = Output(Bool())
        val debug_clk           = Input(Clock())

        val counter             = Output(UInt(32.W))
    })

    val q_num = 16
    val Q = (0 until q_num).map(x => XQueue(new M2H_CMD(),64)).toList
    val Qh = (0 until q_num).map(x => Module(new d2hcmdqueuehead())).toList

    val counter = RegInit(UInt(32.W),0.U)
    io.counter := counter
    when (io.cmd_out.fire())
    {
        counter := counter + 1.U
    }

    val working = Wire(Bool())

    val h2m_cpt_working = RegInit(Bool(), false.B)
    val h2m_complete = RegInit(Bool(), false.B)
    io.h2m_complete := h2m_complete
    when (io.h2m_complete_start)
    {
        h2m_cpt_working := true.B
    }

    val out  = RegInit(UInt(5.W),0.U)
    val next = RegInit(UInt(5.W),0.U)
    val hold1 = RegInit(UInt(5.W),3.U)
    val hold2 = RegInit(UInt(5.W),3.U)

    for(i <- 0 until q_num){
        Q(i).io.in.valid            := io.cmd_in.valid & io.qin(i)
        Q(i).io.in.bits             := io.cmd_in.bits
        Qh(i).io.cmd_in             <> Q(i).io.out
        Qh(i).io.cmd_out.ready      := io.cmd_out.ready & (out === i.U)
        Qh(i).io.c2h_finish         := io.c2h_finish & (out === i.U)
        Qh(i).io.m2h_finish         := io.m2h_finish & (out === i.U)
        Qh(i).io.m2h_cpt_complete   := io.m2h_cpt_complete & (out === i.U)
    }

    io.cmd_in.ready := true.B

    for(i <- 0 until q_num){
        when (Qh(i).io.working === false.B & out === i.U & !h2m_cpt_working){
            out := next
        }
        .elsewhen(Qh(i).io.working === false.B & out === i.U & h2m_cpt_working){
            out := 31.U
        }
        when (Qh(i).io.continue === true.B & out === i.U & !h2m_cpt_working){
            out := next
            when (next === q_num.U - 1.U){
                next := 0.U
            } .otherwise{
                next := next + 1.U
            }
        }
        .elsewhen(Qh(i).io.continue === true.B & out === i.U & h2m_cpt_working){
            out := 31.U
        }
        when(Qh(i).io.working === false.B & next === i.U){
            when (next === q_num.U - 1.U){
                next := 0.U
            } .otherwise{
                next := next + 1.U
            }
        }
    }
    when (out === 31.U){
        when (hold1 =/= 0.U)
            {hold1 := hold1 - 1.U}
        .elsewhen(io.c2h_finish & io.m2h_finish & io.read_count_equal & io.empty)
            {
                hold2 := hold2 - 1.U
                when (hold2 === 0.U & !io.m2h_valid_tmpreg)
                    {h2m_complete := true.B}
            }
        when (h2m_complete & io.h2m_cpt_complete)
            {
                h2m_complete := false.B
                h2m_cpt_working := false.B
                out := next
                hold1 := 3.U
                hold2 := 3.U
            }
    }


    working := MuxLookup(out, Qh(0).io.working, (Array(
        0.U -> Qh(0).io.working,
        1.U -> Qh(1).io.working,
        2.U -> Qh(2).io.working,
        3.U -> Qh(3).io.working,
        4.U -> Qh(4).io.working,
        5.U -> Qh(5).io.working,
        6.U -> Qh(6).io.working,
        7.U -> Qh(7).io.working,
        8.U -> Qh(8).io.working,
        9.U -> Qh(9).io.working,
        10.U -> Qh(10).io.working,
        11.U -> Qh(11).io.working,
        12.U -> Qh(12).io.working,
        13.U -> Qh(13).io.working,
        14.U -> Qh(14).io.working,
        15.U -> Qh(15).io.working,
    )))
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
    io.c2h_length := MuxLookup(out, Qh(0).io.c2h_length, (Array(
        0.U -> Qh(0).io.c2h_length,
        1.U -> Qh(1).io.c2h_length,
        2.U -> Qh(2).io.c2h_length,
        3.U -> Qh(3).io.c2h_length,
        4.U -> Qh(4).io.c2h_length,
        5.U -> Qh(5).io.c2h_length,
        6.U -> Qh(6).io.c2h_length,
        7.U -> Qh(7).io.c2h_length,
        8.U -> Qh(8).io.c2h_length,
        9.U -> Qh(9).io.c2h_length,
        10.U -> Qh(10).io.c2h_length,
        11.U -> Qh(11).io.c2h_length,
        12.U -> Qh(12).io.c2h_length,
        13.U -> Qh(13).io.c2h_length,
        14.U -> Qh(14).io.c2h_length,
        15.U -> Qh(15).io.c2h_length,
    )))
    io.m2h_complete := MuxLookup(out, Qh(0).io.m2h_complete, (Array(
        0.U -> Qh(0).io.m2h_complete,
        1.U -> Qh(1).io.m2h_complete,
        2.U -> Qh(2).io.m2h_complete,
        3.U -> Qh(3).io.m2h_complete,
        4.U -> Qh(4).io.m2h_complete,
        5.U -> Qh(5).io.m2h_complete,
        6.U -> Qh(6).io.m2h_complete,
        7.U -> Qh(7).io.m2h_complete,
        8.U -> Qh(8).io.m2h_complete,
        9.U -> Qh(9).io.m2h_complete,
        10.U -> Qh(10).io.m2h_complete,
        11.U -> Qh(11).io.m2h_complete,
        12.U -> Qh(12).io.m2h_complete,
        13.U -> Qh(13).io.m2h_complete,
        14.U -> Qh(14).io.m2h_complete,
        15.U -> Qh(15).io.m2h_complete,
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

// class ila_m2hcmdqueue(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_m2hcmdqueue = Module(new ila_m2hcmdqueue(Seq(	

//     io.cmd_in.valid,
//     io.qin,

//     // io.cmd_out.bits.m2h_start_addr,
//     // io.cmd_out.bits.m2h_length,

//     out,
//     next,

//     working,
//     io.h2m_complete_start,
//     io.h2m_complete,
//     io.h2m_cpt_complete,
//     h2m_cpt_working,
//     Qh(0).io.working,
//     Qh(0).io.continue,

//     io.c2h_finish,
//     io.m2h_finish,
//     io.read_count_equal,
//     io.empty,
//     hold1,
//     hold2,
//     // Qh(0).io.cmd_out.fire(),
//     // Qh(0).io.continue,
//     // Qh(0).io.working,

//     // Qh(0).io.cmd_in.ready,
//     // Qh(0).io.cmd_out.valid,
//     // Qh(0).io.m2h_complete,
//     // Qh(0).io.c2h_length,
//     // Qh(0).io.m2h_cpt_complete,
//     // Qh(0).io.last,
//     // Qh(0).io.cmd_out.ready,

// )))
// inst_ila_m2hcmdqueue.connect(io.debug_clk)
}
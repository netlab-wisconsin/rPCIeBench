package common.axi

import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.connection._

object AXIRouter {
    def apply(n : Int, shape : AXI) = {
        Module(new AXIRouter(
			shape.ar.bits.addr.getWidth,
			shape.r.bits.data.getWidth, 
			shape.ar.bits.id.getWidth, 
			shape.ar.bits.user.getWidth, 
			shape.ar.bits.len.getWidth,
            n
        ))
    }

    class AXIRouter(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int, n: Int) extends Module {
        val io = IO(new Bundle {
            val in      = Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
            val out     = Vec(n, new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
            val wrIdx   = Input(UInt(log2Up(n).W))
            val rdIdx   = Input(UInt(log2Up(n).W))
        })

        // For write channel. just use composite router.

        val wrRt = CompositeRouter(
            new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH),
            new AXI_DATA_W(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH),
            n
        )

        wrRt.io.idx         := io.wrIdx
        wrRt.io.in_meta     <> io.in.aw
        wrRt.io.in_data     <> io.in.w
        for (i <- 0 until n) {
            io.out(i).aw    <> wrRt.io.out_meta(i)
            io.out(i).w     <> wrRt.io.out_data(i)
        }

        // Response channel. Assume no more than 256 outstanding W transactions

        val inResp      = Wire(Decoupled(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH)))
        val outResp     = Wire(Vec(n, Decoupled(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH))))

        io.in.b  <> RegSlice(inResp)
        for (i <- 0 until n) {
            val regSlice = Module(new RegSlice(new AXI_BACK(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH)))
            regSlice.io.upStream    <> io.out(i).b
            regSlice.io.downStream  <> outResp(i)
        }

        val respFifo    = XQueue(UInt(log2Up(n).W), 256)
        
        respFifo.io.in.valid    := io.in.aw.fire
        respFifo.io.in.bits     := io.wrIdx

        for (i <- 0 until n) {
            outResp(i).ready    := (respFifo.io.out.valid) && (respFifo.io.out.bits === i.U)
        }
        inResp.bits    := outResp(respFifo.io.out.bits).bits
        inResp.valid   := outResp(respFifo.io.out.bits).valid
        respFifo.io.out.ready   := inResp.fire

        // Read channel. Max 256 outstanding transactions supported

        val inReadAddrRaw  = Wire(Decoupled(new Bundle{
            val idx     = Output(UInt(log2Up(n).W))
            val genBits = new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
        }))
        val inReadAddr  = RegSlice(inReadAddrRaw)
        val outReadAddr = Wire(Vec(n, Decoupled(new AXI_ADDR(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))))
        val inRead      = Wire(Decoupled(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
        val outRead     = Wire(Vec(n, Decoupled(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH))))

        inReadAddrRaw.bits.idx      := io.rdIdx
        inReadAddrRaw.bits.genBits  <> io.in.ar.bits
        inReadAddrRaw.ready         <> io.in.ar.ready
        inReadAddrRaw.valid         <> io.in.ar.valid

        io.in.r <> RegSlice(inRead)
        for (i <- 0 until n) {
            io.out(i).ar <> RegSlice(outReadAddr(i))
            val regSlice = Module(new RegSlice(new AXI_DATA_R(ADDR_WIDTH,DATA_WIDTH,ID_WIDTH,USER_WIDTH)))
            regSlice.io.upStream    <> RegSlice(io.out(i).r)
            regSlice.io.downStream  <> outRead(i)
        }

        val readFifo    = XQueue(UInt(log2Up(n).W), 256)

        readFifo.io.in.valid    := inReadAddr.fire
        readFifo.io.in.bits     := inReadAddr.bits.idx
        inReadAddr.ready  := readFifo.io.in.ready && outReadAddr(io.rdIdx).ready
        for (i <- 0 until n) {
            outReadAddr(i).valid  := readFifo.io.in.ready && inReadAddr.valid && (inReadAddr.bits.idx === i.U)
            outReadAddr(i).bits   := inReadAddr.bits.genBits
        }
        
        inRead.bits     := outRead(readFifo.io.out.bits).bits
        inRead.valid    := outRead(readFifo.io.out.bits).valid && readFifo.io.out.valid
        for (i <- 0 until n) {
            outRead(i).ready    := inRead.ready && readFifo.io.out.valid && (readFifo.io.out.bits === i.U)
        }
        readFifo.io.out.ready   := inRead.fire && inRead.bits.last.asBool
    }
}
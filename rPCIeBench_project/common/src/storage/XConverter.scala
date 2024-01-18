package common.storage

import common.Math
import common.axi._
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}

class XConverterIO[T <: Data](private val gen: T) extends Bundle
{ 
	val in_clk = Input(Clock())
	val out_clk = Input(Clock())
	val rstn = Input(Bool())
	val in = Flipped(InIO(gen))
	val out = Flipped(OutIO(gen))
	// val count = Output(UInt(log2Ceil(entries + 1).W))
	// val almostfull = Output(UInt(1.W))
}

object XAXIConverter{
	def apply[T<:AXI](gen:T, m_clk:Clock, m_rstn:Bool, s_clk:Clock, s_rstn:Bool) = {
		val slave 	= Wire(new AXI(
			gen.ar.bits.addr.getWidth,
			gen.r.bits.data.getWidth, 
			gen.ar.bits.id.getWidth, 
			gen.ar.bits.user.getWidth, 
			gen.ar.bits.len.getWidth
		))
		val cvt_aw 	= XConverter(chiselTypeOf(gen.aw.bits), m_clk, m_rstn, s_clk)
		val cvt_ar 	= XConverter(chiselTypeOf(gen.ar.bits), m_clk, m_rstn, s_clk)
		val cvt_w 	= XConverter(chiselTypeOf(gen.w.bits), m_clk, m_rstn, s_clk)
		val cvt_r 	= XConverter(chiselTypeOf(gen.r.bits), s_clk, s_rstn, m_clk)
		val cvt_b 	= XConverter(chiselTypeOf(gen.b.bits), s_clk, s_rstn, m_clk)

		cvt_aw.io.in 	<> gen.aw
		cvt_ar.io.in 	<> gen.ar
		cvt_w.io.in 	<> gen.w
		cvt_r.io.out	<> gen.r
		cvt_b.io.out	<> gen.b

		cvt_aw.io.out 	<> slave.aw
		cvt_ar.io.out 	<> slave.ar
		cvt_w.io.out 	<> slave.w
		cvt_r.io.in		<> slave.r
		cvt_b.io.in		<> slave.b

		slave
	}
}

object XConverter{
	def apply[T<:Data](num:Int)(gen: T) = {
		Seq.fill(num)(Module(new XConverter(gen)))
	}

	def apply[T<:Data](gen: T) = {
		Module(new XConverter(gen))
	}

	def apply[T<:Data](gen: T, in_clk:Clock, rstn:Bool, out_clk:Clock) = {//rstn is with in_clk
		val cvt = Module(new XConverter(gen))
		cvt.io.in_clk	:= in_clk
		cvt.io.out_clk	:= out_clk
		cvt.io.rstn		:= rstn
		cvt
	}

	def apply[T<:Data](in:DecoupledIO[T], in_clk:Clock, rstn:Bool, out_clk:Clock) = {
		val cvt = Module(new XConverter(chiselTypeOf(in.bits)))
		cvt.io.in_clk	:= in_clk
		cvt.io.out_clk	:= out_clk
		cvt.io.rstn		:= rstn
		cvt.io.in		<> in

		cvt.io.out
	}

	class XConverter[T<:Data](
		val gen:T,
	)extends RawModule(){
		val entries = 16

		val genType = if (compileOptions.declaredTypeMustBeUnbound) {
			requireIsChiselType(gen)
			gen
		} else {
			if (DataMirror.internal.isSynthesizable(gen)) {
				chiselTypeOf(gen)
			}else {
				gen
			}
		}
		val io = IO(new XConverterIO(genType))

		val width = Math.round_up((genType.getWidth),8)

		val fifo = Module(new SV_STREAM_FIFO(width,log2Up(entries),"auto","independent_clock","false"))

		fifo.io.s_clk		<> io.in_clk
		fifo.io.m_clk		<> io.out_clk
		fifo.io.reset_n		<> io.rstn

		fifo.io.in_data 	:= io.in.bits.asTypeOf(UInt())
		fifo.io.in_valid	<> io.in.valid
		fifo.io.in_ready	<> io.in.ready

		io.out.bits			:= fifo.io.out_data.asTypeOf(chiselTypeOf(io.out.bits))
		io.out.valid		:= fifo.io.out_valid
		fifo.io.out_ready	:= io.out.ready
		
		// io.count			:= fifo.io.wr_count
		// io.almostfull	:= fifo.io.wr_count >= (entries-almostfull_threshold).U

		
	}
}
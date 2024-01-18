package common.storage

import common.Math
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}
import common.Collector

object XQueueConfig{
	var Debug = false
}

object InIO {
  def apply[T<:Data](gen: T): DecoupledIO[T] = Decoupled(gen)
}

object OutIO {
  def apply[T<:Data](gen: T): DecoupledIO[T] = Flipped(Decoupled(gen))
}

class XQueueIO[T <: Data](private val gen: T, val entries: Int) extends Bundle
{ 
  val in = Flipped(InIO(gen))
  val out = Flipped(OutIO(gen))
  val count = Output(UInt(log2Ceil(entries + 1).W))
  val almostfull = Output(UInt(1.W))
}

object XQueue{
	def apply[T<:Data](num:Int)(gen: T, entries:Int) = {
		Seq.fill(num)(Module(new XQueue(gen,entries,2)))
	}

	def apply[T<:Data](gen: T, entries:Int, almostfull_threshold:Int=2, packet_fifo:String="false") = {
		Module(new XQueue(gen,entries,almostfull_threshold,packet_fifo))
	}

	def apply[T<:Data](in:DecoupledIO[T], entries:Int) = {
		val t = Module(new XQueue(chiselTypeOf(in.bits),entries,2))
		t.io.in	<> in
		t.io.out
	}

	class XQueue[T<:Data](
		val gen:T,
		val entries:Int,
		val almostfull_threshold:Int=2, 
		val packet_fifo:String="false"
	)extends Module(){
		require(entries >= 1, "XQueue must has positive entries")
		require(entries <= 4096, "XQueue out of range")
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
		val io = IO(new XQueueIO(genType, entries))

		if(entries > 32){
			val width = Math.round_up((genType.getWidth),8)

			val fifo = Module(new SV_STREAM_FIFO(width,log2Up(entries),"auto","common_clock",packet_fifo))

			fifo.io.s_clk		<> clock
			fifo.io.m_clk		<> clock
			fifo.io.reset_n		<> !reset.asUInt

			fifo.io.in_data 	:= io.in.bits.asTypeOf(UInt())
			fifo.io.in_valid	<> io.in.valid
			fifo.io.in_ready	<> io.in.ready

			io.out.bits			:= fifo.io.out_data.asTypeOf(chiselTypeOf(io.out.bits))
			io.out.valid		:= fifo.io.out_valid
			fifo.io.out_ready	:= io.out.ready
			
			io.count			:= fifo.io.wr_count
			io.almostfull		:= fifo.io.wr_count >= (entries-almostfull_threshold).U

		}else{
			val q = Module(new Queue(genType,entries))

			q.io.enq <> io.in
			q.io.deq <> io.out
			io.count := q.io.count
			io.almostfull := q.io.count >= (entries-almostfull_threshold).U
		}

		if(XQueueConfig.Debug==true){
			Collector.report(io.in.valid&io.in.ready===0.U,"overflow")
		}
	}
}


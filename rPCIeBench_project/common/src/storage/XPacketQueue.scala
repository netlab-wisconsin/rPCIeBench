package common.storage

import common.Math
import common.axi._
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}

object XPacketQueue{
	def apply(width:Int, entries:Int, almostfull_threshold:Int=2) = {
		Module(new XPacketQueue(width, entries, almostfull_threshold))
	}

	class XPacketQueue(
		val width:Int,
		val entries:Int,
		val almostfull_threshold:Int, 
	)extends Module(){
		val io = IO(new Bundle{
			val in = Flipped(Decoupled(new AXIS(width)))
			val out = Decoupled(new AXIS(width))
			val count = Output(UInt(log2Ceil(entries + 1).W))
			val almostfull = Output(UInt(1.W))
		})

		val meta = Module(new xpm_fifo_axis(width,log2Up(entries),"auto","common_clock","true"))

		io.almostfull		:= io.count >= (entries-almostfull_threshold).U

		meta.io.m_axis_tdata		<> io.out.bits.data
		meta.io.m_axis_tkeep		<> io.out.bits.keep
		meta.io.m_axis_tlast		<> io.out.bits.last
		meta.io.m_axis_tvalid		<> io.out.valid
		meta.io.rd_data_count_axis	<> io.count
		meta.io.s_axis_tready		<> io.in.ready

		meta.io.m_aclk				<> clock
		meta.io.m_axis_tready		<> io.out.ready
		meta.io.s_aclk				<> clock
		meta.io.s_aresetn			<> (!reset.asUInt)
		meta.io.s_axis_tdata		<> io.in.bits.data
		meta.io.s_axis_tdest		<> 0.U
		meta.io.s_axis_tid			<> 0.U
		meta.io.s_axis_tkeep		<> io.in.bits.keep
		meta.io.s_axis_tlast		<> io.in.bits.last
		meta.io.s_axis_tstrb		<> io.in.bits.keep
		meta.io.s_axis_tuser		<> 0.U
		meta.io.s_axis_tvalid		<> io.in.valid
	}
}





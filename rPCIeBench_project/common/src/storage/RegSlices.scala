package common.storage
import chisel3._
import chisel3.util._
import chisel3.experimental.{DataMirror, Direction, requireIsChiselType}
import common.axi._

object AXIRegSlice{
	def apply(stage: Int)(upStream: AXI) = {
		val downStream = Wire(chiselTypeOf(upStream))

		val regSlice = Seq.fill(stage)(Module(new AXIRegSlice(
			upStream.ar.bits.addr.getWidth,
			upStream.r.bits.data.getWidth, 
			upStream.ar.bits.id.getWidth, 
			upStream.ar.bits.user.getWidth, 
			upStream.ar.bits.len.getWidth
		)))

		regSlice(0).io.upStream			<> upStream
		regSlice(stage-1).io.downStream	<> downStream

		var i = 0
		for (i <- 0 until stage-1) {
			regSlice(i).io.downStream   <> regSlice(i+1).io.upStream
		}

		downStream
	}

	def apply(upStream: AXI) = {
		val downStream = Wire(chiselTypeOf(upStream))

		val regSlice = Module(new AXIRegSlice(
			upStream.ar.bits.addr.getWidth,
			upStream.r.bits.data.getWidth, 
			upStream.ar.bits.id.getWidth, 
			upStream.ar.bits.user.getWidth, 
			upStream.ar.bits.len.getWidth
		))

		regSlice.io.upStream    <> upStream
		regSlice.io.downStream  <> downStream

		downStream
	}
	
	class AXIRegSlice(ADDR_WIDTH:Int, DATA_WIDTH:Int, ID_WIDTH:Int, USER_WIDTH:Int, LEN_WIDTH:Int) extends Module() {
		val io = IO(new Bundle{
			val upStream = Flipped(new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH))
			val downStream = new AXI(ADDR_WIDTH, DATA_WIDTH, ID_WIDTH, USER_WIDTH, LEN_WIDTH)
		})

		io.downStream.aw	<> RegSlice(io.upStream.aw)
		io.downStream.ar	<> RegSlice(io.upStream.ar)
		io.downStream.w		<> RegSlice(io.upStream.w)
		io.upStream.r		<> RegSlice(io.downStream.r)
		io.upStream.b		<> RegSlice(io.downStream.b)
	}
}

object RegSlice{
	def apply[T<:Data](upStream: DecoupledIO[T]) = {
		val downStream = Wire(chiselTypeOf(upStream))
		val queue = Module(new RegSlice(chiselTypeOf(upStream.bits)))
		queue.io.upStream	<> upStream
		queue.io.downStream	<> downStream

		downStream
	}

	def apply[T<:Data](stage: Int)(upStream: DecoupledIO[T]) = {
		val downStream = Wire(chiselTypeOf(upStream))
		val queue = Seq.fill(stage)(Module(new RegSlice(chiselTypeOf(upStream.bits))))
		queue(0).io.upStream			<> upStream
		queue(stage-1).io.downStream	<> downStream

		var i = 0
		for (i <- 0 until stage-1) {
			queue(i).io.downStream	<> queue(i+1).io.upStream
		}

		downStream
	}
}

class RegSlice[T<:Data](gen : T) extends Module{

	val genType = if (compileOptions.declaredTypeMustBeUnbound) {
		requireIsChiselType(gen)
		gen
	} else {
		if (DataMirror.internal.isSynthesizable(gen)) {
			chiselTypeOf(gen)
		} else {
			gen
		}
	}
	var io = IO(new Bundle {
		var upStream = Flipped(DecoupledIO(genType))
		var downStream = DecoupledIO(genType)
	})

	// Use the implementation of Analog Devices, Inc.

	val bwd_data_s 	= Wire(genType)
	val bwd_valid_s = Wire(Bool())
	val bwd_ready_s = Wire(UInt(1.W))
	val fwd_data_s 	= Wire(genType)
	val fwd_valid_s = Wire(UInt(1.W))
	val fwd_ready_s = Wire(Bool())

	val fwd_valid 	= RegInit(Bool(), false.B)
	val fwd_data 	= Reg(genType)

	fwd_ready_s := ~fwd_valid | io.downStream.ready
	fwd_valid_s := fwd_valid
	fwd_data_s	:= fwd_data

	fwd_data	:= Mux(~fwd_valid | io.downStream.ready, bwd_data_s, fwd_data)

	fwd_valid	:= Mux(bwd_valid_s, 1.U, Mux(io.downStream.ready, 0.U, fwd_valid))

	val bwd_ready	= RegInit(Bool(), true.B)
	val bwd_data 	= Reg(genType)

	bwd_valid_s	:= ~bwd_ready | io.upStream.valid
	bwd_data_s	:= Mux(bwd_ready, io.upStream.bits, bwd_data)
	bwd_ready_s	:= bwd_ready

	bwd_data	:= Mux(bwd_ready, io.upStream.bits, bwd_data)

	bwd_ready	:= Mux(fwd_ready_s, 1.U, Mux(io.upStream.valid, 0.U, bwd_ready))

	io.downStream.bits	:= fwd_data_s
	io.downStream.valid	:= fwd_valid_s
	io.upStream.ready	:= bwd_ready_s
}
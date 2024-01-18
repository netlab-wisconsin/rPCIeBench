package common

import chisel3._
import chisel3.util._
import common.axi.AXIS

object AddHeader{
	def apply[TMeta<:Data,TData<:AXIS](genMeta:TMeta, genData:TData, header_width:Int) = {
		Module(new AddHeader(genMeta,genData,header_width))
	}
	def apply[TMeta<:Data,TData<:AXIS](genMeta:DecoupledIO[TMeta], genData:DecoupledIO[TData], header_width:Int) = {
		val t = Module(new AddHeader(chiselTypeOf(genMeta.bits),chiselTypeOf(genData.bits),header_width))
			t.io.inMeta	<> genMeta
			t.io.inData	<> genData
			t.io.outData
	}
}
class AddHeader[TMeta<:Data,TData<:AXIS](genMeta:TMeta, genData:TData, header_width:Int) extends Module{
	val io = IO(new Bundle{
		val inMeta	= Flipped(Decoupled(genMeta))
		val inData	= Flipped(Decoupled(genData))
		val outData	= Decoupled(genData)
	})
	ToZero(io.outData.bits)
	val data_width	= io.outData.bits.DATA_WIDTH
	val shift		= Module(new LSHIFT(header_width,data_width)) 
	shift.io.in		<> io.inData
	
	val sHeader :: sPayload :: Nil = Enum(2)
	val state	= RegInit(sHeader)

	when(state === sHeader){
		io.outData.valid	:= io.inMeta.valid & shift.io.out.valid
		io.inMeta.ready		:= io.outData.fire()
		shift.io.out.ready		:= io.outData.fire()
	}.otherwise{
		io.outData.valid	:= shift.io.out.valid
		io.inMeta.ready		:= 0.U
		shift.io.out.ready		:= io.outData.ready
	}
	switch(state){
		is(sHeader){
			io.outData.bits.data	:= Cat(shift.io.out.bits.data(data_width-1,header_width*8), io.inMeta.bits.asUInt())
			when(io.outData.fire() && !io.outData.bits.last){
				state		:= sPayload
			}.otherwise{
				state		:= sHeader
			}
		}
		is(sPayload){
			io.outData.bits.data	:= shift.io.out.bits.data
			when(io.outData.fire() && io.outData.bits.last===1.U){
				state		:= sHeader
			}.otherwise{
				state		:= sPayload
			}
		}
	}
	io.outData.bits.keep	:= shift.io.out.bits.keep
	io.outData.bits.last	:= shift.io.out.bits.last
}

object SplitHeader{
	def apply[TMeta<:Data,TData<:AXIS](genMeta:TMeta, genData:TData, header_width:Int) = {
		Module(new SplitHeader(genMeta,genData,header_width))
	}
	def apply[TMeta<:Data,TData<:AXIS](inData:DecoupledIO[TData], outMeta:DecoupledIO[TMeta], outData:DecoupledIO[TData], header_width:Int) = {
		val t = Module(new SplitHeader(chiselTypeOf(outMeta.bits),chiselTypeOf(outData.bits),header_width))
		t.io.inData		<> inData
		t.io.outData	<> outData
		t.io.outMeta	<> outMeta
	}
}

class SplitHeader[TMeta<:Data,TData<:AXIS](genMeta:TMeta, genData:TData, header_width:Int) extends Module{
	val io = IO(new Bundle{
		val inData	= Flipped(Decoupled(genData))
		val outMeta	= Decoupled(genMeta)
		val outData	= Decoupled(genData)
	})
	ToZero(io.outData.bits)
	ToZero(io.outMeta.bits)
	val data_width	= io.outData.bits.DATA_WIDTH
	val shift		= Module(new RSHIFT(header_width,data_width)) 
	shift.io.out	<> io.outData

	val sHeader :: sPayload :: Nil = Enum(2)
	val state	= RegInit(sHeader)

	when(state === sHeader){
		io.inData.ready		:= io.outMeta.ready & shift.io.in.ready
		io.outMeta.valid	:= io.inData.fire()
		shift.io.in.valid	:= io.inData.fire()
	}.otherwise{
		shift.io.in.valid	:= io.inData.valid
		io.inData.ready		:= shift.io.in.ready
		io.outMeta.valid	:= 0.U
	}

	switch(state){
		is(sHeader){
			when(io.inData.fire() & !io.inData.bits.last===1.U){
				state		:= sPayload
			}.otherwise{
				state		:= sHeader
			}
		}
		is(sPayload){
			when(io.inData.fire() && io.inData.bits.last===1.U){
				state		:= sHeader
			}.otherwise{
				state		:= sPayload
			}
		}
	}
	shift.io.in.bits		:= io.inData.bits
	io.outMeta.bits			:= io.inData.bits.data(header_width-1,0)
}
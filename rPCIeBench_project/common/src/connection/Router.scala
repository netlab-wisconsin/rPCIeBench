package common.connection

import chisel3._
import chisel3.util._
import common.axi.HasLast
import common.storage.RegSlice

object SimpleRouter{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SimpleRouter(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new SimpleRouter(gen,n))
	}

	class SimpleRouter[T<:Data](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Flipped(Decoupled(gen))
			val out		= Vec(n,Decoupled(gen))
			val idx		= Input(UInt(log2Up(n).W))
		})

		val in_pro	= Wire(Decoupled(new Bundle{
			val idx		= Output(UInt(log2Up(n).W))
			val genbits	= Output(gen)
		}))
		in_pro.ready			<> io.in.ready
		in_pro.valid			:= io.in.valid
		in_pro.bits.genbits		:= io.in.bits
		in_pro.bits.idx			:= io.idx

		val in				= RegSlice(in_pro)
		val out				= Wire(Vec(n,Decoupled(gen)))

		in.ready			:= 0.U
		for(i<-0 until n){
			out(i).bits		:= in.bits.genbits
			out(i).valid	:= 0.U
			when(in.bits.idx === i.U){
				out(i).valid	:= in.valid
				in.ready		:= out(i).ready
			}
		}
		for(i<-0 until n){
			io.out(i)				<> RegSlice(out(i))
		}
	}
}

object SerialRouter{
	def apply[T<:HasLast](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new SerialRouter(gen,n)))
	}
	def apply[T<:HasLast](gen:T, n:Int) = {
		Module(new SerialRouter(gen,n))
	}
	class SerialRouter[T<:HasLast](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Flipped(Decoupled(gen))
			val out		= Vec(n,Decoupled(gen))
			val idx		= Input(UInt(log2Up(n).W))
		})

		val in_pro	= Wire(Decoupled(new Bundle{
			val idx		= Output(UInt(log2Up(n).W))
			val genbits	= Output(gen)
		}))
		in_pro.ready			<> io.in.ready
		in_pro.valid			:= io.in.valid
		in_pro.bits.genbits		:= io.in.bits
		in_pro.bits.idx			:= io.idx

		val in				= RegSlice(in_pro)
		val out				= Wire(Vec(n,Decoupled(gen)))
		

		val is_head 		= RegInit(UInt(1.W),1.U)
		val idx				= RegInit(UInt(log2Up(n).W),0.U)

		when(in.fire() && in.bits.genbits.last===1.U){
			is_head		:= 1.U
		}.elsewhen(in.fire()){
			is_head		:= 0.U
		}

		when(is_head === 1.U){
			idx			:= in.bits.idx
		}

		in.ready		:= 0.U
		for(i<-0 until n){
			out(i).bits			:= in.bits.genbits
			out(i).valid		:= 0.U
			when(is_head === 1.U){
				when(in.bits.idx === i.U){
					out(i).valid	:= in.valid
					in.ready		:= out(i).ready
				}
			}.otherwise{
				when(idx === i.U){
					out(i).valid	:= in.valid
					in.ready		:= out(i).ready
				}
			}
		}

		for(i<-0 until n){
			io.out(i)				<> RegSlice(out(i))
		}
	}
}

object CompositeRouter{
	def apply[TMeta<:Data,TData<:HasLast](genMeta:TMeta, genData:TData, n:Int) = {
		Module(new CompositeRouter(genMeta,genData,n))
	}

	class CompositeRouter[TMeta<:Data,TData<:HasLast](val genMeta:TMeta, val genData:TData, val n:Int)extends Module{
		val io = IO(new Bundle{
			val idx			= Input(UInt(log2Up(n).W))
			val in_meta		= Flipped(Decoupled(genMeta))
			val in_data		= Flipped(Decoupled(genData))
			val out_meta	= Vec(n, Decoupled(genMeta))
			val out_data	= Vec(n, Decoupled(genData))
		})
		val meta_pro	= Wire(Decoupled(new Bundle{
			val idx		= Output(UInt(log2Up(n).W))
			val genbits	= Output(genMeta)
		}))
		meta_pro.ready			<> io.in_meta.ready
		meta_pro.valid			:= io.in_meta.valid
		meta_pro.bits.genbits	:= io.in_meta.bits
		meta_pro.bits.idx		:= io.idx

		val in_meta		= RegSlice(meta_pro)
		val in_data		= RegSlice(io.in_data)

		val out_meta	= Wire(Vec(n,Decoupled(genMeta)))
		val out_data	= Wire(Vec(n,Decoupled(genData)))

		val last_idx	= RegInit(UInt(log2Up(n).W),0.U)
		val sFirst :: sMiddle :: Nil = Enum(2)
		val state 	= RegInit(sFirst)
		switch(state){
			is(sFirst){
				last_idx		:= in_meta.bits.idx
				when(in_meta.fire()){
					when(in_data.fire() && in_data.bits.last===1.U){
						state	:= sFirst
					}.otherwise{
						state	:= sMiddle
					}
				}
			}
			is(sMiddle){
				when(in_data.fire() && in_data.bits.last===1.U){
					state		:= sFirst
				}
			}
		}
		in_meta.ready	:= 0.U
		in_data.ready	:= 0.U
		for(i<-0 until n){
			out_meta(i).valid		:= 0.U
			out_data(i).valid		:= 0.U
			out_meta(i).bits		:= in_meta.bits.genbits
			out_data(i).bits		:= in_data.bits
			when(state===sFirst && in_meta.bits.idx===i.U){
				out_meta(i).valid	:= in_meta.valid
				in_meta.ready 		:= out_meta(i).ready

				out_data(i).valid	:= in_data.valid & in_meta.fire()
				in_data.ready		:= out_data(i).ready & in_meta.fire()
			}.elsewhen(state===sMiddle && last_idx===i.U){
				out_data(i).valid	:= in_data.valid
				in_data.ready		:= out_data(i).ready
			}
		}

		for(i<-0 until n){
			io.out_meta(i)				<> RegSlice(out_meta(i))
			io.out_data(i)				<> RegSlice(out_data(i))
		}
	}
}


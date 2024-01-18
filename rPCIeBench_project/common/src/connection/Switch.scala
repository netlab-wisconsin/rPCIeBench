package common.connection

import common.connection._
import common.storage._
import chisel3._
import chisel3.util._

object Switch{
	def apply[T<:Data](num:Int)(gen:T, n:Int) = {
		Seq.fill(num)(Module(new Switch(gen,n)))
	}
	def apply[T<:Data](gen:T, n:Int) = {
		Module(new Switch(gen,n))
	}
	class Switch[T<:Data](val gen:T, val n:Int)extends Module{
		val io = IO(new Bundle{
			val in		= Vec(n,Flipped(Decoupled(gen)))
			val out		= Vec(n,Decoupled(gen))
			val idx		= Vec(n,Input(UInt(log2Up(n).W)))
			val last 	= Vec(n,Input(UInt(1.W)))
		})

		// val routers = SerialRouter(n)(gen,n)
		// val arbiters = SerialArbiter(n)(gen,n)

		// class t[T <: Data](private val gen:T)extends Bundle{
		// 	val data = gen
		// 	val last = UInt(1.W)
		// }

		// val q = XQueue(n*n)(new t(gen), 16)
		// // val q_last = XQueue(n*n)(UInt(1.W), 16)

		// for(i <- 0 until n){ 
		// 	routers(i).io.in	<> io.in(i)

		// 	routers(i).io.idx	<> io.idx(i)
		// 	routers(i).io.last	<> io.last(i)

		// 	arbiters(i).io.out	<> io.out(i)

		// 	for(j <-0 until n){
		// 		q(i*n+j).io.in.valid				<> routers(i).io.out(j).valid
		// 		q(i*n+j).io.in.ready				<> routers(i).io.out(j).ready
		// 		q(i*n+j).io.in.bits.data			<> routers(i).io.out(j).bits
		// 		q(i*n+j).io.in.bits.last			<> routers(i).io.last_o
		// 	}

		// 	for(j <-0 until n){
		// 		q(i+j*n).io.out.valid				<> arbiters(i).io.in(j).valid
		// 		q(i+j*n).io.out.ready				<> arbiters(i).io.in(j).ready
		// 		q(i+j*n).io.out.bits.data			<> arbiters(i).io.in(j).bits
		// 		q(i+j*n).io.out.bits.last			<> arbiters(i).io.last(j)
		// 	}
		// }
	}
}

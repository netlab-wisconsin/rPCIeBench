package common.connection

import chisel3._
import chisel3.util._
import common.storage.RegSlice
import common.Collector

object GrantIndex{
	def apply(req:UInt,en:Bool) = {
		val n 				= req.getWidth
		val base            = RegInit(UInt(n.W),1.U)
		val double_req      = Cat(req,req)
		val double_grant	= double_req & ~(double_req-base)
		val grant			= double_grant(n-1,0) | double_grant(2*n-1,n)
		val grant_index		= OHToUInt(grant)
		when(en){
			base			:= Cat(base(n-2,0),base(n-1))
		}
		grant_index
	}
}
object Connection{
	def one2many(one:DecoupledIO[Data]) (many:DecoupledIO[Data]*)	= {
		one.ready	:= many.map(_.ready).reduce(_ & _)
		many.map(t => t.valid := one.fire())
	}

	def many2one(many:DecoupledIO[Data]*)(one:DecoupledIO[Data])	= {
		one.valid	:= many.map(_.valid).reduce(_ & _)
		many.map(t => t.ready := one.fire())
	}

	def one2one(one:DecoupledIO[Data])(two:DecoupledIO[Data])		= {
		one.valid	<> two.valid
		one.ready	<> two.ready
	}

	def limit(in:DecoupledIO[Data], out:DecoupledIO[Data], en_cycles:UInt,total_cycles:UInt) = {
		val reg_count	= RegInit(UInt(32.W),0.U)
		val en			= Wire(Bool())
		when(reg_count<en_cycles){
			en			:= true.B
		}.otherwise{
			en			:= false.B
		}
		when(reg_count+1.U>=total_cycles){
			reg_count	:= 0.U
		}.otherwise{
			reg_count	:= reg_count+1.U
		}
		out.bits		:= in.bits
		out.valid		:= in.valid	& en
		in.ready		:= out.ready & en
	}
		
}

class CreditQ(initCredit:Int=0, inStep:Int=1, outStep:Int=1) extends Module{
	val io = IO(new Bundle{
		val in	= Flipped(Decoupled())
		val out = Decoupled()
		val maxCredit = Input(UInt(32.W))
	})

	val realMaxCredit = io.maxCredit - 4.U//4 is absorbed by regslice
	val cur_credit = RegInit(UInt(32.W),initCredit.U)

	val out = Wire(Decoupled())
	val in	= RegSlice(io.in)

	out.valid	:= cur_credit >= outStep.U
	in.ready	:= cur_credit <= realMaxCredit-inStep.U 

	when(out.fire() & in.fire()){
		cur_credit		:= cur_credit + inStep.U - outStep.U
	}.elsewhen(out.fire()){
		cur_credit		:= cur_credit - outStep.U
	}.elsewhen(in.fire()){
		cur_credit		:= cur_credit + inStep.U
	}.otherwise{
		cur_credit		:= cur_credit
	}
	io.out		<> RegSlice(out)
	
}
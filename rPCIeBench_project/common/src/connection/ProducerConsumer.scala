package common.connection

import chisel3._
import chisel3.util._
import common.storage.RegSlice
import common.ToZero

object ProducerConsumer{
    def apply[T<:Data](gen:T, n:Int) = {
        Module(new ProducerConsumer(gen,n))
    }
	def apply[T<:Data](seq:Seq[Int])(in:DecoupledIO[T],outs:Seq[DecoupledIO[T]]) = {
		val gen = chiselTypeOf(in.bits)
		def connect(node:ProducerConsumer[T], seq:Seq[Int]):Seq[ProducerConsumer[T]] = {
			val num = seq(0)
			val fanout = seq(1)
			val leaf_nodes = Seq.fill(num)(Module(new ProducerConsumer(gen, fanout)))

			for(i<-0 until num){
				leaf_nodes(i).io.in	<> node.io.out(i)
			}
			if(seq.size>1){
				return leaf_nodes
			}else{
				return leaf_nodes.foldLeft(Seq[ProducerConsumer[T]]())((s,a) => s++connect(a,seq.drop(1)))
			}
		}
		val node		= Module(new ProducerConsumer(gen,seq(0)))
		val leaf_nodes	= connect(node,seq)
		node.io.in		<> in
		for(i <-0 until leaf_nodes.size){
			for(j <-0 until seq.last){
				val index	= i*seq.last+j
				outs(index)	<> leaf_nodes(i).io.out(j)
			}
		}
	}
}
class ProducerConsumer[T<:Data](val gen:T, val n:Int) extends Module{
    val io = IO(new Bundle{
        val in  = Flipped(Decoupled(gen))
        val out = Vec(n,Decoupled(gen))
    })
    
    val in              = RegSlice(io.in)
    val out             = Wire(Vec(n,Decoupled(gen)))

	val grant_index		= GrantIndex(Cat(out.map(_.ready).reverse), in.fire())

    in.ready            := 0.U
    for(i<-0 until n){
        out(i).bits     := in.bits
        out(i).valid    := 0.U
        when(grant_index === i.U){
            out(i).valid    := in.valid
            in.ready        := out(i).ready
        }
    }
    for(i<-0 until n){
        io.out(i)				<> RegSlice(out(i))
    }
}
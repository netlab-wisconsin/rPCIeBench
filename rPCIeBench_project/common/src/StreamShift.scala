package common

import common.storage._
import common.axi._

import chisel3._
import chisel3.util._

//offset(bytes)
class LSHIFT(offset:Int, width:Int) extends Module{
	val io = IO(new Bundle{

		val in	= Flipped(Decoupled(new AXIS(width)))
		val out	= (Decoupled(new AXIS(width)))
	})

	val write_reminder = RegInit(0.U(1.W))
	val write_first = RegInit(1.U(1.W))
	val pre_word = Reg(new AXIS(width))

	val fifo_data = XQueue(new AXIS(width), 64)
	io.out <>fifo_data.io.out

	io.in.ready := fifo_data.io.in.ready & (fifo_data.io.count < 60.U) & (write_reminder === 0.U)

	val tmp_data = WireInit(VecInit(Seq.fill(width)(0.U(1.W))))
	val tmp_keep = WireInit(VecInit(Seq.fill(width/8)(0.U(1.W))))

	fifo_data.io.in.bits.data	:= tmp_data.asUInt
	fifo_data.io.in.bits.keep	:= tmp_keep.asUInt
	fifo_data.io.in.bits.last	:= 0.U
	fifo_data.io.in.valid   	:= 0.U
	when(write_reminder === 1.U ){
		for(i<- 0 until offset*8){
			tmp_data(i) := pre_word.data(width-(8*offset)+i)
		}
		for(i<- 0 until offset){
			tmp_keep(i) := pre_word.keep(width/8-offset+i)
		}
		fifo_data.io.in.bits.last := 1.U
		fifo_data.io.in.valid := 1.U
		write_reminder := 0.U		
	}.elsewhen(io.in.fire()){
		when(write_first === 1.U){
			
			for(i<- 0 until (width-(offset*8))){
				tmp_data(i+offset*8) := io.in.bits.data(i)
			}
			for(i<- 0 until offset){
				tmp_keep(i) := 1.U
			}	
			for(i<- 0 until (width/8-offset)){
				tmp_keep(i+offset) := io.in.bits.keep(i)
			}						
			fifo_data.io.in.bits.last := io.in.bits.keep(width/8-1,width/8-offset) === 0.U
		}.otherwise{
			for(i<- 0 until offset*8){
				tmp_data(i) := pre_word.data(width-(8*offset)+i)
			}			
			for(i<- 0 until (width-(offset*8))){
				tmp_data(i+offset*8) := io.in.bits.data(i)
			}
			for(i<- 0 until offset){
				tmp_keep(i) := pre_word.keep(width/8-offset+i)
			}	
			for(i<- 0 until (width/8-offset)){
				tmp_keep(i+offset) := io.in.bits.keep(i)
			}	
			fifo_data.io.in.bits.last := io.in.bits.keep(width/8-1,width/8-offset) === 0.U			
		}
		fifo_data.io.in.valid := 1.U
		pre_word 	<> io.in.bits
		write_first	:= 0.U
		when(io.in.bits.last === 1.U){
			write_first := 1.U
			write_reminder := !fifo_data.io.in.bits.last
		}

	}

}

class RSHIFT(offset:Int, width:Int) extends Module{
	val io = IO(new Bundle{

		val in	= Flipped(Decoupled(new AXIS(width)))
		val out	= (Decoupled(new AXIS(width)))
	})

	val write_reminder = RegInit(0.U(1.W))
	val write_first = RegInit(1.U(1.W))
	val pre_word = Reg(new AXIS(width))

	val fifo_data = XQueue(new AXIS(width), 64)
	io.out <>fifo_data.io.out

	io.in.ready := fifo_data.io.in.ready & (fifo_data.io.count < 60.U) 

	val tmp_data = WireInit(VecInit(Seq.fill(width)(0.U(1.W))))
	val tmp_keep = WireInit(VecInit(Seq.fill(width/8)(0.U(1.W))))

	fifo_data.io.in.bits.data	:= tmp_data.asUInt
	fifo_data.io.in.bits.keep	:= tmp_keep.asUInt
	fifo_data.io.in.bits.last	:= 0.U
	fifo_data.io.in.valid   	:= 0.U
	when(write_reminder === 1.U ){
		for(i<- 0 until (width-(offset*8))){
			tmp_data(i) := pre_word.data((8*offset)+i)
		}			
		for(i<- 0 until (width/8-offset)){
			tmp_keep(i) := pre_word.keep(offset+i)
		}	

		fifo_data.io.in.bits.last := 1.U
		fifo_data.io.in.valid := 1.U
		write_reminder := 0.U
		when(io.in.fire()){
			pre_word 	<> io.in.bits
			write_first	:= 0.U
			when(io.in.bits.last === 1.U){
				write_first := 1.U
				write_reminder := 1.U
			}
		}
	}.elsewhen(io.in.fire()){
		when(write_first === 1.U){
			fifo_data.io.in.valid := 0.U
		}.otherwise{
			for(i<- 0 until (width-(offset*8))){
				tmp_data(i) := pre_word.data((8*offset)+i)
			}			
			for(i<- 0 until (offset*8)){
				tmp_data(i+(width-(offset*8))) := io.in.bits.data(i)
			}
			for(i<- 0 until (width/8-offset)){
				tmp_keep(i) := pre_word.keep(offset+i)
			}	
			for(i<- 0 until offset){
				tmp_keep(i+(width/8-offset)) := io.in.bits.keep(i)
			}	

			fifo_data.io.in.bits.last := io.in.bits.keep(width/8-1,offset) === 0.U	
			fifo_data.io.in.valid := 1.U		
		}
		pre_word 	<> io.in.bits
		write_first	:= 0.U
		when(io.in.bits.last === 1.U){
			write_first := 1.U
			write_reminder := !fifo_data.io.in.bits.last
		}

	}

}
package common

import common.storage._
import chisel3._
import chisel3.util._
import scala.math
import scala.collection.mutable.ListBuffer

class LEADING_ZERO_COUNTER(WIDTH:Int)extends Module{
	val io = IO(new Bundle{
		val data		= Input(UInt(WIDTH.W))
		val result		= Output(UInt((1+log2Up(WIDTH)).W))
	})
	val RWIDTH		= log2Up(WIDTH)
	val data		= io.data
	val result		= Wire(Vec(RWIDTH,UInt(1.W)))

	when(data(WIDTH-1,WIDTH/2) === 0.U){
		result(RWIDTH-1)	:= 1.U
	}.otherwise{
		result(RWIDTH-1)	:= 0.U
	}

	val tmps = ListBuffer(Wire(UInt(1.W)))

	for (i <- 1 to RWIDTH-1){
		val cur_width 	= math.pow(2,i+1).toInt
		tmps+=Wire(UInt((cur_width).W))
	}
	tmps(0)			:=	0.U
	tmps(RWIDTH-1)	:=	data

	for (i <- (1 to RWIDTH-2).reverse){
		val cur_width 	= math.pow(2,i+1).toInt
		when(result(i+1)===1.U){
			tmps(i) 		:= tmps(i+1)(cur_width-1,0)
		}.otherwise{
			tmps(i)		:= tmps(i+1)(2*cur_width-1,cur_width)
		}
		when(tmps(i)(cur_width-1,cur_width/2) === 0.U){
			result(i)	:= 1.U
		}.otherwise{
			result(i)	:= 0.U
		}
		if(i==1){
			when(result(i) === 1.U){
				result(0)	:= !tmps(i)(1)
			}.otherwise{
				result(0)	:= !tmps(i)(3)
			}
		}
	}
	val result_r	= WireInit(UInt((RWIDTH+1).W),0.U)

	when(io.data === 0.U){
		result_r 	:= Cat(1.U(1.W),0.U(RWIDTH.W))
	}.otherwise{
		result_r	:= Cat(0.U,result.asTypeOf(UInt()))
	}
	io.result		:= result_r
}


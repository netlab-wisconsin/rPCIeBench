package common

import chisel3._
import chisel3.util._
import chisel3.experimental.{requireIsChiselType, DataMirror, Direction}

object ToZero{
	// def apply[T<:Bundle](x:T):Unit={
	// 	x	:= 0.U.asTypeOf(x)
	// }

	def apply[T<:Data](x:T):Unit={
		x	:= 0.U.asTypeOf(x)
	}

	def apply[T<:Data](v:Vec[T]):Unit={
		v.foreach(x => apply(x))
	}
}

object Init{
	def apply[T<:Data](x:DecoupledIO[T])={
		if(DataMirror.directionOf(x.bits) == Direction.Output){
			x.ready		:= 1.U
		}else{
			x.bits		:= 0.U.asTypeOf(x.bits)
			x.valid		:= 0.U
		}
	}
}

object ToAllOnes{
	def apply[T<:Data](x:T):Unit={
		x	:= -1.S(x.getWidth.W).asTypeOf(x)
	}
}

object ValidCounter{
	def record_signals_asyn(is_high:Bool,is_reset:Bool,cur_clock:Clock)={
		withClockAndReset(cur_clock,is_reset){
			val count = RegInit(UInt(32.W),0.U)
			when(is_high){
				count	:= count+1.U
			}
			count
		}
	}

	def record_signals_sync(is_high:Bool)={
		val count = RegInit(UInt(32.W),0.U)
		when(is_high){
			count	:= count+1.U
		}
		count
	}
}

case class Timer(start_origin:UInt,end_origin:UInt){
	val start							= RegNext(start_origin)
	val end								= RegNext(end_origin)
	private val reg_time				= RegInit(UInt(64.W),0.U)
	private val reg_latency				= RegInit(UInt(64.W),0.U)
	private val reg_cnt_start			= RegInit(UInt(32.W),0.U)
	private val reg_cnt_end				= RegInit(UInt(32.W),0.U)
	private val reg_en					= RegInit(UInt(1.W),0.U)

	when(start===1.U){
		reg_cnt_start			:= reg_cnt_start+1.U
	}.otherwise{
		reg_cnt_start			:= reg_cnt_start
	}

	when(end===1.U){
		reg_cnt_end				:= reg_cnt_end+1.U
	}.otherwise{
		reg_cnt_end				:= reg_cnt_end
	}

	when(start===1.U){
		reg_en					:= 1.U
	}.otherwise{
		reg_en					:= reg_en
	}

	when(reg_en===1.U){
		reg_time				:= reg_time+1.U
	}

	when(start===1.U && end===1.U){
		reg_latency				:= reg_latency
	}.elsewhen(start===1.U){
		reg_latency				:= reg_latency - reg_time
	}.elsewhen(end===1.U){
		reg_latency				:= reg_latency + reg_time
	}.otherwise{
		reg_latency				:= reg_latency
	}	

	val latency					= reg_latency
	val cnt_start				= reg_cnt_start
	val cnt_end					= reg_cnt_end
	val time					= reg_time
}

object Statistics{
	def everMax(cur_value:UInt)={
		val reg_max = RegInit(UInt(cur_value.getWidth.W),0.U)
		when(cur_value > reg_max){
			reg_max			:= cur_value
		}
		reg_max
	}

	def longestActive(en:Bool, width:Int=32)={
		val reg_cur = RegInit(UInt(width.W),0.U)
		when(en){
			reg_cur			:= reg_cur+1.U
		}.otherwise{
			reg_cur			:= 0.U
		}
		everMax(reg_cur)
	}

	def count(en:Bool)={
		val reg_cur = RegInit(UInt(32.W),0.U)
		when(en){
			reg_cur	:= reg_cur+1.U
		}.otherwise{
			reg_cur	:= reg_cur
		}
		reg_cur
	}
}
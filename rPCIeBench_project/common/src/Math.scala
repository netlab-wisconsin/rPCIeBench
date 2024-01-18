package common

import chisel3._
import chisel3.util._

object Math{
	def round_up(x:Int,m:Int):Int={
		((x + m - 1) / m) * m
	}
	def pow2(x:Int):Int={
		scala.math.pow(2,x).toInt
	}
	def log2(x:Int):Int={
		(scala.math.log(x)/scala.math.log(2)).toInt
	}
}


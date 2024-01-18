package CommSub
import chisel3._
import chisel3.util._
import common.storage._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.options.TargetDirAnnotation

object elaborate extends App {
	println("Generating a %s class".format(args(0)))
	val stage	= new chisel3.stage.ChiselStage
	val arr		= Array("-X", "sverilog", "--full-stacktrace")
	val dir 	= TargetDirAnnotation("Verilog")

    class TestXQueue extends Module(){
		val io = IO(new Bundle{
			val in = Flipped(Decoupled(UInt(32.W)))
			val out = (Decoupled(UInt(32.W)))
		})
		val q = XQueue(UInt(32.W),64)
		q.io.in		<> io.in
		q.io.out	<> io.out
	}
	class TestXConverter extends Module(){
		val io = IO(new Bundle{
			val out_clk = Input(Clock())
			val req = Flipped(Decoupled(UInt(32.W)))
			val res = Decoupled(UInt(32.W))
		})
		val converter = XConverter(UInt(32.W),clock,true.B,io.out_clk)
		converter.io.in <> io.req
		converter.io.out <> io.res
    }
	args(0) match{
		case "Top" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new Top()),dir))
		case "TLB" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new TLB()),dir))
		case "DataBoundarySplit" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new DataBoundarySplit()),dir))
		case "TestXQueue" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new TestXQueue()),dir))
		case "TestXConverter" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new TestXConverter()),dir))
		case _ => println("Module match failed!")
	}
}
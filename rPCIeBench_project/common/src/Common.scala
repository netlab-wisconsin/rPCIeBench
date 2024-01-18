package common
import chisel3._
import chisel3.util._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.options.TargetDirAnnotation

object elaborate extends App {
//   (new chisel3.stage.ChiselStage).execute(
//     Array("-X", "verilog", "--full-stacktrace"),
//     Seq(ChiselGeneratorAnnotation(() => new delay_lib1()),
//       TargetDirAnnotation("Verilog"))
//   )

	println("Hello common")
}
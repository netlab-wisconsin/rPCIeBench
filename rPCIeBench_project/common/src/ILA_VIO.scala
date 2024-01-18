package common
import chisel3._
import chisel3.util._
import firrtl.options.CustomFileEmission
import firrtl.annotations.SingleTargetAnnotation
import firrtl.annotations.ReferenceTarget
import firrtl.AnnotationSeq
import chisel3.experimental.{annotate,ChiselAnnotation}
import java.io._

class Pack{
	var targets = Seq[String]()
}
object AnnotationCount{
	var count = 0
}


class BaseILA(seq:Seq[Data])extends BlackBox{
	val t = seq.map(chiselTypeOf(_))
	val io = IO(new Bundle{
		val clk                     = Input(Clock())
		val data					= Input(MixedVec(t))
	})
	def connect(clock:Clock) = {
		assert(name.startsWith("ila"))
		io.clk	:= clock
		io.data.zip(seq).foreach(node => node._1 := node._2)
		val p = new Pack
		val count = AnnotationCount.count
		seq.foreach(data => annotate(new ChiselAnnotation {def toFirrtl = MyMetadataAnno(count, name, seq.length, p, data.toTarget)}))
		AnnotationCount.count += 1
	}
}

class BaseVIO(seq:Seq[Data])extends BlackBox{
	val t = seq.map(chiselTypeOf(_))
	val	io = IO(new Bundle{
			val clk                     = Input(Clock())
			val data					= Output(MixedVec(t))
		})
	def connect(clock:Clock) = {
		assert(name.startsWith("vio"))
		io.clk	:= clock
		io.data.zip(seq).foreach(node => node._2 := node._1)
		val p = new Pack
		val count = AnnotationCount.count
		seq.foreach(data => annotate(new ChiselAnnotation {def toFirrtl = MyMetadataAnno(count, name, seq.length, p, data.toTarget)}))
		AnnotationCount.count += 1
	}
}

case class MyMetadataAnno(count:Int, name:String, l:Int, p:Pack, target:ReferenceTarget) extends SingleTargetAnnotation[ReferenceTarget] {
	def duplicate(n: ReferenceTarget) = this //this.copy(n)
	p.targets = p.targets :+ target.serialize.split('>')(1).replace('.','_').replace('[','_').replace("]","")
	if(l == p.targets.length){
		val moduleName = target.circuit
		val isAppend = count != 0
		val	writer = new PrintWriter(new FileOutputStream(new File("Verilog/"+moduleName+".txt"), isAppend))

		p.targets.foreach(str => writer.append(str+"\n"))
		writer.append(name+":\n")
		writer.close()
	}
}
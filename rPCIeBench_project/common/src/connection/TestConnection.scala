package common.connection

import chisel3._
import chisel3.util._
import common.axi.AXIS

class TestCompositeRouter() extends Module{
	val io = IO(new Bundle{
		val in_meta 		= Flipped(Decoupled(UInt(8.W)))
		val in_data 		= Flipped(Decoupled(AXIS(32)))

		val out_meta0		= Decoupled(UInt(8.W))
		val out_meta1		= Decoupled(UInt(8.W))
		val out_meta2		= Decoupled(UInt(8.W))
		val out_meta3		= Decoupled(UInt(8.W))

		val out_data0		= Decoupled(AXIS(32))
		val out_data1		= Decoupled(AXIS(32))
		val out_data2		= Decoupled(AXIS(32))
		val out_data3		= Decoupled(AXIS(32))
	})
	val router	= CompositeRouter(UInt(8.W),AXIS(32),4)
	router.io.in_meta		<> io.in_meta
	router.io.in_data		<> io.in_data
	router.io.idx			<> io.in_meta.bits

	router.io.out_meta(0)	<> io.out_meta0
	router.io.out_meta(1)	<> io.out_meta1
	router.io.out_meta(2)	<> io.out_meta2
	router.io.out_meta(3)	<> io.out_meta3
	router.io.out_data(0)	<> io.out_data0
	router.io.out_data(1)	<> io.out_data1
	router.io.out_data(2)	<> io.out_data2
	router.io.out_data(3)	<> io.out_data3
}
class TestCompositeArbiter() extends Module{
	val io = IO(new Bundle{
		val in_meta0 		= Flipped(Decoupled(UInt(8.W)))
		val in_meta1 		= Flipped(Decoupled(UInt(8.W)))
		val in_meta2 		= Flipped(Decoupled(UInt(8.W)))
		val in_meta3 		= Flipped(Decoupled(UInt(8.W)))
		val in_data0 		= Flipped(Decoupled(AXIS(32)))
		val in_data1 		= Flipped(Decoupled(AXIS(32)))
		val in_data2 		= Flipped(Decoupled(AXIS(32)))
		val in_data3 		= Flipped(Decoupled(AXIS(32)))

		val out_meta		= Decoupled(UInt(8.W))
		val out_data		= Decoupled(AXIS(32))
	})

	val arbiter = CompositeArbiter(UInt(8.W), AXIS(32), 4)
	arbiter.io.in_meta(0)	<> io.in_meta0
	arbiter.io.in_meta(1)	<> io.in_meta1
	arbiter.io.in_meta(2)	<> io.in_meta2
	arbiter.io.in_meta(3)	<> io.in_meta3
	arbiter.io.in_data(0)	<> io.in_data0
	arbiter.io.in_data(1)	<> io.in_data1
	arbiter.io.in_data(2)	<> io.in_data2
	arbiter.io.in_data(3)	<> io.in_data3

	arbiter.io.out_meta		<> io.out_meta
	arbiter.io.out_data		<> io.out_data
}
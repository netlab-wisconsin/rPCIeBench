package common

import chisel3._
import chisel3.util._
import common.storage._
import math.pow

class LatencyBucket (
    BUCKET_SIZE     : Int,
    LATENCY_STRIDE  : Int,
    COUNTER_LEN     : Int = 32,
    MAX_INFLIGHT    : Int = 256
) extends Module {
    assert(pow(2, log2Ceil(LATENCY_STRIDE)).toInt == LATENCY_STRIDE, 
        "LATENCY_STRIDE of LatencyBucket should be exponential of 2."
    )

    val LATENCY_STRIDE_SHIFT= log2Ceil(LATENCY_STRIDE)
    val BUCKET_SIZE_SHIFT   = log2Ceil(BUCKET_SIZE)
    val MAX_INFLIGHT_SHIFT  = log2Ceil(MAX_INFLIGHT)

    assert(LATENCY_STRIDE_SHIFT + BUCKET_SIZE_SHIFT < 64,
        "LATENCY_STRIDE*BUCKET_SIZE should be lower than 2^64."
    )

    val io = IO(new Bundle {
        // Count latency
        val enable      = Input(Bool()) // Set high when counting
        val start       = Input(Bool())
        val end         = Input(Bool())
        // Get result, make sure enable is low now.
        val bucketRdId  = Input(UInt(BUCKET_SIZE_SHIFT.W))
        val bucketValue = Output(UInt(COUNTER_LEN.W))   // Has 2 cycle latency behind bucketRdId
        // Reset result
        val resetBucket = Input(Bool())
        val resetDone   = Output(Bool())
    })

    val gblTimer = RegInit(UInt(64.W), 0.U)
    gblTimer    := gblTimer + 1.U(64.W)

    // Start Time RAM.

    val startTimeRam = XRam(UInt(64.W), MAX_INFLIGHT, latency=2)

    val startId = RegInit(UInt(MAX_INFLIGHT_SHIFT.W), 0.U)
    val endId   = RegInit(UInt(MAX_INFLIGHT_SHIFT.W), 0.U)

    when (io.start) {
        startId := startId + 1.U
    }

    when (io.end) {
        endId   := endId + 1.U
    }

    startTimeRam.io.addr_a      := startId
    startTimeRam.io.data_in_a   := gblTimer
    startTimeRam.io.wr_en_a     := io.start
    startTimeRam.io.addr_b      := endId

    val latencyBits = Wire(UInt(64.W))
    latencyBits := gblTimer - startTimeRam.io.data_out_b

    // Latency RAM. 

    val latencyRam = XRam(UInt(COUNTER_LEN.W), BUCKET_SIZE, latency=2)

    val resetDone   = RegInit(Bool(), true.B)
    val resetId     = RegInit(UInt(BUCKET_SIZE_SHIFT.W), 0.U)
    io.resetDone    := resetDone

    val A = LATENCY_STRIDE_SHIFT
    val B = A + BUCKET_SIZE_SHIFT

    latencyRam.io.addr_a    := Mux(resetDone, 
        Mux(io.enable, 
            RegNext(RegNext(latencyRam.io.addr_b)), 
            io.bucketRdId
        ),
        resetId
    )
    latencyRam.io.wr_en_a   := io.enable && RegNext(RegNext(io.end))
    latencyRam.io.addr_b    := Mux(latencyBits(63, B) === 0.U, latencyBits(B-1, A), -1.S(BUCKET_SIZE_SHIFT.W).asUInt)
    latencyRam.io.data_in_a := Mux(resetDone, latencyRam.io.data_out_b + 1.U, 0.U)
    io.bucketValue          := latencyRam.io.data_out_a

    when (!resetDone) {
        resetId     := resetId + 1.U
    }

    when (io.resetBucket) {
        resetDone   := false.B
        resetId     := 0.U
    }.elsewhen (~resetDone && (resetId === (BUCKET_SIZE-1).U)) {
        resetDone   := true.B
    }
}
package common

import common.storage._
import chisel3._
import chisel3.util._


/** IBUF -- Input Buffer */
class IBUF extends BlackBox {
	val io = IO(new Bundle {
		val O = Output(Bool())
		val I = Input(Bool())
	})
}
object IBUF {
	def apply(pin: Bool): Bool = {
		val pad = Module (new IBUF)
		pad.io.I := pin
		pad.io.O
	}
}

/** BUFG */
class BUFG extends BlackBox{
	val io = IO(new Bundle {
		val O = Output(Clock())
		val I = Input(Clock())
	})
}

object BUFG {
	def apply(pin: Clock): Clock = {
		val pad = Module (new BUFG)
		pad.io.I := pin
		pad.io.O
	}
}


/** IBUFDS, diff->clock, 普通CLK */
class IBUFDS extends BlackBox{
	val io = IO(new Bundle{
		val O = Output(Clock())
		val I = Input(Clock())
		val IB = Input(Clock())
	})
}

object IBUFDS {
	def apply(I: Clock, IB: Clock): Clock = {
		val pad = Module (new IBUFDS)
		pad.io.I := I
		pad.io.IB := IB
		pad.io.O
	}
}

/** IBUFDS_GTE4, 输入为高速口差分时钟 */
class IBUFDS_GTE4(
    REFCLK_EN_TX_PATH:  Int = 0,
    REFCLK_HROW_CK_SEL: Int = 0,
    REFCLK_ICNTL_RX:    Int = 0)
  extends BlackBox(Map(
    "REFCLK_EN_TX_PATH"  -> REFCLK_EN_TX_PATH,
    "REFCLK_HROW_CK_SEL" -> REFCLK_HROW_CK_SEL,
    "REFCLK_ICNTL_RX"    -> REFCLK_ICNTL_RX
	))
{
  val io = IO(new Bundle {
    val O     = Output(Clock())
    val ODIV2 = Output(Clock())
    val CEB   = Input(Bool())//0.U
    val I     = Input(Clock())//p
    val IB    = Input(Clock())//n
  })

}

class MMCME4_ADV_Wrapper(
    MMCM_DIVCLK_DIVIDE:Int,
    CLKFBOUT_MULT_F:Int,
    MMCM_CLKOUT0_DIVIDE_F:Int,
    MMCM_CLKOUT1_DIVIDE_F:Int = 2,
    MMCM_CLKOUT2_DIVIDE_F:Int = 2,
    MMCM_CLKOUT3_DIVIDE_F:Int = 2,
    MMCM_CLKOUT4_DIVIDE_F:Int = 2,
    MMCM_CLKOUT5_DIVIDE_F:Int = 2,
    MMCM_CLKOUT6_DIVIDE_F:Int = 2,
    MMCM_CLKIN1_PERIOD:Int,
)extends RawModule{
  val io = IO(new Bundle{
      val CLKIN1    = Input(Clock())
      val RST       = Input(UInt(1.W))

      val LOCKED    = Output(UInt(1.W))
      val CLKOUT0 	= Output(Clock())
      val CLKOUT1 	= Output(Clock())
      val CLKOUT2 	= Output(Clock())
      val CLKOUT3 	= Output(Clock())
      val CLKOUT4 	= Output(Clock())
      val CLKOUT5 	= Output(Clock())
      val CLKOUT6 	= Output(Clock())
  })
  // CLKOUT = CLKIN1 * CLKFBOUT_MULT_F / MMCM_DIVCLK_DIVIDE / MMCM_CLKOUT0_DIVIDE_F
  // CLKIN1 * CLKFBOUT_MULT_F / MMCM_DIVCLK_DIVIDE ~ 600-1200
  // 250 = 100 * 20 / 2 / 4
  // 300 = 100 * 18 / 2 / 3
  // 450 = 100 * 18 /2 / 2
  val mmcm4_adv = Module(new MMCME4_ADV(
    MMCM_DIVCLK_DIVIDE,
    CLKFBOUT_MULT_F,
    MMCM_CLKOUT0_DIVIDE_F,
    MMCM_CLKOUT1_DIVIDE_F,
    MMCM_CLKOUT2_DIVIDE_F,
    MMCM_CLKOUT3_DIVIDE_F,
    MMCM_CLKOUT4_DIVIDE_F,
    MMCM_CLKOUT5_DIVIDE_F,
    MMCM_CLKOUT6_DIVIDE_F,
    MMCM_CLKIN1_PERIOD,
    ))
    mmcm4_adv.io.CLKIN1  := io.CLKIN1
    mmcm4_adv.io.RST     := io.RST
    io.LOCKED            := mmcm4_adv.io.LOCKED
    io.CLKOUT0           := mmcm4_adv.io.CLKOUT0
    io.CLKOUT1           := mmcm4_adv.io.CLKOUT1
    io.CLKOUT2           := mmcm4_adv.io.CLKOUT2
    io.CLKOUT3           := mmcm4_adv.io.CLKOUT3
    io.CLKOUT4           := mmcm4_adv.io.CLKOUT4
    io.CLKOUT5           := mmcm4_adv.io.CLKOUT5
    io.CLKOUT6           := mmcm4_adv.io.CLKOUT6

    mmcm4_adv.io.CLKIN2       := 0.U
    mmcm4_adv.io.PWRDWN      := 0.U
    mmcm4_adv.io.CDDCREQ       := 0.U
    mmcm4_adv.io.CLKINSEL      := 1.U
    mmcm4_adv.io.DADDR       := 0.U
    mmcm4_adv.io.DEN       := 0.U
    mmcm4_adv.io.DI      := 0.U
    mmcm4_adv.io.DWE       := 0.U
    mmcm4_adv.io.PSCLK       := 0.U
    mmcm4_adv.io.PSEN      := 0.U
    mmcm4_adv.io.DCLK      := 0.U
    mmcm4_adv.io.PSINCDEC      := 0.U


}

class MMCME4_ADV(
    MMCM_DIVCLK_DIVIDE:Int,
    CLKFBOUT_MULT_F:Int,
    MMCM_CLKOUT0_DIVIDE_F:Int,
    MMCM_CLKOUT1_DIVIDE_F:Int,
    MMCM_CLKOUT2_DIVIDE_F:Int,
    MMCM_CLKOUT3_DIVIDE_F:Int,
    MMCM_CLKOUT4_DIVIDE_F:Int,
    MMCM_CLKOUT5_DIVIDE_F:Int,
    MMCM_CLKOUT6_DIVIDE_F:Int,
    MMCM_CLKIN1_PERIOD:Int,

) extends BlackBox(Map(
    "BANDWIDTH" -> "OPTIMIZED",
    "CLKOUT4_CASCADE"         -> "FALSE",
    "COMPENSATION"            -> "INTERNAL",
    "STARTUP_WAIT"            -> "FALSE",
    "DIVCLK_DIVIDE"           -> MMCM_DIVCLK_DIVIDE,
    "CLKFBOUT_MULT_F"         -> CLKFBOUT_MULT_F,
    "CLKFBOUT_PHASE"          -> 0.00,
    "CLKFBOUT_USE_FINE_PS"    -> "FALSE",
    "CLKOUT0_DIVIDE_F"   -> MMCM_CLKOUT0_DIVIDE_F,
    "CLKOUT0_PHASE"   -> 0.000,
    "CLKOUT0_DUTY_CYCLE"   -> 0.500,
    "CLKOUT0_USE_FINE_PS"   -> "FALSE",
    "CLKOUT1_DIVIDE"   -> MMCM_CLKOUT1_DIVIDE_F,
    "CLKOUT2_DIVIDE"   -> MMCM_CLKOUT2_DIVIDE_F,
    "CLKOUT3_DIVIDE"   -> MMCM_CLKOUT3_DIVIDE_F,
    "CLKOUT4_DIVIDE"   -> MMCM_CLKOUT4_DIVIDE_F,
    "CLKOUT5_DIVIDE"   -> MMCM_CLKOUT5_DIVIDE_F,
    "CLKOUT6_DIVIDE"   -> MMCM_CLKOUT6_DIVIDE_F,
    "CLKIN1_PERIOD"    -> MMCM_CLKIN1_PERIOD,
    "REF_JITTER1"   -> 0.010,
    

)){
  val io = IO(new Bundle{
      val CLKIN1    = Input(Clock())
      val CLKIN2    = Input(UInt(1.W))
      val RST       = Input(UInt(1.W))
      val PWRDWN    = Input(UInt(1.W))
      val CDDCREQ   = Input(UInt(1.W))
      val CLKINSEL  = Input(UInt(1.W))
      val DADDR     = Input(UInt(7.W))
      val DEN       = Input(UInt(1.W))
      val DI        = Input(UInt(16.W))
      val DWE       = Input(UInt(1.W))
      val PSCLK     = Input(UInt(1.W))
      val PSEN      = Input(UInt(1.W))
      val DCLK      = Input(UInt(1.W))
      val PSINCDEC  = Input(UInt(1.W))

      val LOCKED  = Output(UInt(1.W))
      val CLKOUT0 = Output(Clock())
      val CLKOUT1 = Output(Clock())
      val CLKOUT2 = Output(Clock())
      val CLKOUT3 = Output(Clock())
      val CLKOUT4 = Output(Clock())
      val CLKOUT5 = Output(Clock())
      val CLKOUT6 = Output(Clock())
  })
}
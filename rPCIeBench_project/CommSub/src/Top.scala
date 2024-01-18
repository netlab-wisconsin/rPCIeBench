package CommSub

import chisel3._
import chisel3.util._
import common._
import common.storage._
import common.axi._
import common.ToZero


class Top extends RawModule{
	val qdma_pin		= IO(new QDMAPin())
	val sys_100M_0_p	= IO(Input(Clock()))
  	val sys_100M_0_n	= IO(Input(Clock()))

	val mmcm = Module(new MMCME4_ADV_Wrapper(
		CLKFBOUT_MULT_F 		= 12,
		MMCM_DIVCLK_DIVIDE		= 1,
		MMCM_CLKOUT0_DIVIDE_F	= 12,
		MMCM_CLKOUT1_DIVIDE_F	= 4,
		MMCM_CLKOUT2_DIVIDE_F	= 12,
		
		MMCM_CLKIN1_PERIOD 		= 10
	))
		
	mmcm.io.CLKIN1	:= IBUFDS(sys_100M_0_p, sys_100M_0_n)
	mmcm.io.RST		:= 0.U

	// val dbg_clk 	= BUFG(mmcm.io.CLKOUT2)
	// dontTouch(dbg_clk)

	val hbm_driver_clk	= mmcm.io.CLKOUT0 //100M
	val user_clk 		= mmcm.io.CLKOUT1 //300M

	val user_rstn 		= mmcm.io.LOCKED

	def risingedge(x: Bool) = x && !RegNext(x)


//------------------ HBM definition (hbm_clk)

    val hbmDriver = withClockAndReset(hbm_driver_clk, false.B) {Module(new HBM_DRIVER(WITH_RAMA=false))}  //(WITH_RAMA=false)
	hbmDriver.getTCL()
	val hbm_clk		= hbmDriver.io.hbm_clk
	val hbm_rstn	= withClockAndReset(hbm_clk,false.B) {RegNext(hbmDriver.io.hbm_rstn.asBool)}

	for (i <- 0 until 32) {
		hbmDriver.io.axi_hbm(i).hbm_init()	// Read hbm_init function if you're not familiar with AXI.
	}

//------------------ QDMA definition (user_clk)

	//your VIVADO version and path to your project's IP location
	val qdma = Module(new QDMA("202101"))//edit me
	qdma.getTCL()

	ToZero(qdma.io.reg_status)
	qdma.io.pin <> qdma_pin

	qdma.io.user_clk	:= user_clk
	qdma.io.user_arstn	:= user_rstn

	val pcie_clk		= Wire(Clock())
	val pcie_rstn		= Wire(UInt(1.W))
	pcie_clk 			:= qdma.io.pcie_clk
	pcie_rstn			:= qdma.io.pcie_arstn

	qdma.io.h2c_data.ready	:= 0.U
	qdma.io.c2h_data.valid	:= 0.U
	qdma.io.c2h_data.bits	:= 0.U.asTypeOf(new C2H_DATA)

	qdma.io.h2c_cmd.valid	:= 0.U
	qdma.io.h2c_cmd.bits	:= 0.U.asTypeOf(new H2C_CMD)
	qdma.io.c2h_cmd.valid	:= 0.U
	qdma.io.c2h_cmd.bits	:= 0.U.asTypeOf(new C2H_CMD)


//------------------ Control and Status Registers (user_clk)

	val control_reg = qdma.io.reg_control
	val status_reg = qdma.io.reg_status

//------------------ h2d (user_clk)

	//h2d_cmd_queue (user_clk)
	val h2d_cmd_queue 							= withClockAndReset(user_clk,!user_rstn){Module(new h2dcmdqueue())}
	h2d_cmd_queue.io.debug_clk 					:= user_clk
	
	h2d_cmd_queue.io.cmd_in.bits.h2c_start_addr	:= Cat(control_reg(50), control_reg(51))
	h2d_cmd_queue.io.cmd_in.bits.h2m_start_addr	:= Cat(control_reg(52)(1,0), control_reg(53))
	h2d_cmd_queue.io.cmd_in.bits.h2m_length		:= control_reg(54)
	h2d_cmd_queue.io.cmd_in.bits.pkt_size		:= control_reg(55)
	h2d_cmd_queue.io.qin						:= control_reg(56)
	h2d_cmd_queue.io.cmd_in.bits.h2c_cpt_addr	:= Cat(control_reg(57), control_reg(58))
	h2d_cmd_queue.io.cmd_in.valid				:= withClockAndReset(user_clk,!user_rstn){risingedge(RegNext(RegNext(RegNext(control_reg(59)===1.U))))}

	//h2c (user_clk)
	val h2c =  withClockAndReset(user_clk,!user_rstn){Module(new H2C())}
	h2c.io.debug_clk := user_clk 
	h2c.io.start_addr							:= h2d_cmd_queue.io.cmd_out.bits.h2c_start_addr
	h2c.io.length								:= h2d_cmd_queue.io.h2c_length
	h2c.io.start								:= h2d_cmd_queue.io.cmd_out.fire()
	h2c.io.h2c_cmd								<> qdma.io.h2c_cmd
	h2c.io.h2c_data								<> qdma.io.h2c_data
	h2c.io.count_time							<> status_reg(51)
	h2c.io.send_cmd_count 						<> status_reg(52)

//------------------ d2h (user_clk)

	//d2h_cmd_queue (user_clk)
	val d2h_cmd_queue 							= withClockAndReset(user_clk,!user_rstn){Module(new d2hcmdqueue())}
	d2h_cmd_queue.io.debug_clk 					:= user_clk
	
	d2h_cmd_queue.io.cmd_in.bits.c2h_start_addr	:= Cat(control_reg(70), control_reg(71))
	d2h_cmd_queue.io.cmd_in.bits.m2h_start_addr	:= Cat(control_reg(72)(1,0), control_reg(73))
	d2h_cmd_queue.io.cmd_in.bits.m2h_length		:= control_reg(74)
	d2h_cmd_queue.io.cmd_in.bits.pkt_size		:= control_reg(75)
	d2h_cmd_queue.io.qin						:= control_reg(76)
	d2h_cmd_queue.io.cmd_in.bits.c2h_cpt_addr	:= Cat(control_reg(77), control_reg(78))
	d2h_cmd_queue.io.cmd_in.valid				:= withClockAndReset(user_clk,!user_rstn){risingedge(RegNext(RegNext(RegNext(control_reg(79)===1.U))))}

	//c2h (user_clk)
	val c2h = withClockAndReset(user_clk,!user_rstn){Module(new C2H())}
	c2h.io.debug_clk := user_clk

	c2h.io.start_addr							:= d2h_cmd_queue.io.cmd_out.bits.c2h_start_addr
	c2h.io.length								:= d2h_cmd_queue.io.c2h_length
	c2h.io.start								:= d2h_cmd_queue.io.cmd_out.fire()
	c2h.io.pfch_tag								:= control_reg(80)

	c2h.io.count_time							<> status_reg(71)
	c2h.io.send_cmd_count 						<> status_reg(72)


//------------------ c2h completion module (user_clk)

	val c2h_cpt = withClockAndReset(user_clk,!user_rstn){Module(new C2H_Complete())}
	c2h_cpt.io.h2c_start						:= withClockAndReset(user_clk,!user_rstn){risingedge(h2d_cmd_queue.io.last)}
	c2h_cpt.io.c2h_start						:= withClockAndReset(user_clk,!user_rstn){risingedge(d2h_cmd_queue.io.last)}
	c2h_cpt.io.polling							<> control_reg(20)

	d2h_cmd_queue.io.h2m_complete_start			:= withClockAndReset(user_clk,!user_rstn){risingedge(h2d_cmd_queue.io.h2m_complete)}
	c2h_cpt.io.h2c_complete						:= d2h_cmd_queue.io.h2m_complete
	c2h_cpt.io.h2c_cpt_addr 					:= h2d_cmd_queue.io.cmd_out.bits.h2c_cpt_addr
	c2h_cpt.io.h2c_cpt_complete					<> h2d_cmd_queue.io.h2m_cpt_complete
	c2h_cpt.io.h2c_cpt_complete					<> d2h_cmd_queue.io.h2m_cpt_complete

	c2h_cpt.io.c2h_complete						:= d2h_cmd_queue.io.m2h_complete
	c2h_cpt.io.c2h_cpt_addr 					:= d2h_cmd_queue.io.cmd_out.bits.c2h_cpt_addr
	c2h_cpt.io.c2h_cpt_complete					<> d2h_cmd_queue.io.m2h_cpt_complete
	c2h_cpt.io.pfch_tag							:= control_reg(80)

	c2h_cpt.io.debug_clk						:= user_clk

	qdma.io.c2h_cmd.valid 						:= Mux(c2h_cpt.io.c2h_cmd.valid, c2h_cpt.io.c2h_cmd.valid, c2h.io.c2h_cmd.valid)
	qdma.io.c2h_cmd.bits  						:= Mux(c2h_cpt.io.c2h_cmd.valid, c2h_cpt.io.c2h_cmd.bits, c2h.io.c2h_cmd.bits)
	c2h.io.c2h_cmd.ready						:= qdma.io.c2h_cmd.ready & !c2h_cpt.io.c2h_cmd.valid
	c2h_cpt.io.c2h_cmd.ready 					:= qdma.io.c2h_cmd.ready
	c2h_cpt.io.c2h_data.ready 					:= qdma.io.c2h_data.ready
 
  

//------------------ H2M & M2H definition and connection (hbm_clk)

	val h2m = withClockAndReset(hbm_clk,!hbm_rstn){Module(new H2M())}
	h2m.io.debug_clk := hbm_clk
	val m2h = withClockAndReset(hbm_clk,!hbm_rstn){Module(new M2H())}
	m2h.io.debug_clk := hbm_clk

	//h2m
	val h2m_start_addr							= cdc_array(34)(user_clk,hbm_clk)
	val h2m_length								= cdc_array(32)(user_clk,hbm_clk)
	val h2m_last								= cdc_array(1)(user_clk,hbm_clk)
	val h2m_start_pulse							= cdc_pulse(user_clk,hbm_clk)
	val h2m_complete							= cdc_array(1)(hbm_clk,user_clk)
	h2m_complete.io.src_in						:= h2m.io.complete
	val h2m_clear								= cdc_array(1)(hbm_clk,user_clk)
	h2m_clear.io.src_in							:= h2m.io.clear

	val h2m_cmd_buffer							= withClockAndReset(user_clk,!user_rstn){XQueue(UInt(67.W),16)}
	h2m_cmd_buffer.io.in.bits					:= Cat(h2d_cmd_queue.io.h2m_last,h2d_cmd_queue.io.cmd_out.bits.h2m_start_addr,h2d_cmd_queue.io.cmd_out.bits.h2m_length)
	h2m_cmd_buffer.io.in.valid					:= h2d_cmd_queue.io.cmd_out.fire()

	val h2m_cmd_buffer_ready					= withClockAndReset(user_clk,!user_rstn){Module(new h2mcmdbufferready())}
	h2m_cmd_buffer.io.out.ready					:= h2m_cmd_buffer_ready.io.ready
	h2m_cmd_buffer_ready.io.valid				:= h2m_cmd_buffer.io.out.valid
	h2m_cmd_buffer_ready.io.complete			:= h2m_complete.io.dest_out

	val h2m_start_addr_reg						= withClockAndReset(user_clk,!user_rstn){RegInit(UInt(34.W), 0.U)}
	val h2m_length_reg							= withClockAndReset(user_clk,!user_rstn){RegInit(UInt(34.W), 0.U)}
	val h2m_last_reg							= withClockAndReset(user_clk,!user_rstn){RegInit(UInt(1.W), 0.U)}
	when(h2m_cmd_buffer.io.out.fire()){
		h2m_last_reg		:= h2m_cmd_buffer.io.out.bits(66)
		h2m_start_addr_reg	:= h2m_cmd_buffer.io.out.bits(65,32)
		h2m_length_reg		:= h2m_cmd_buffer.io.out.bits(31,0)
	}
	h2m_start_addr.io.src_in					:= h2m_start_addr_reg
	h2m_length.io.src_in						:= h2m_length_reg
	h2m_last.io.src_in							:= h2m_last_reg

	h2m_start_addr.io.dest_out					<> h2m.io.start_addr
	h2m_length.io.dest_out						<> h2m.io.length
	h2m_last.io.dest_out						<> h2m.io.last

	h2m_start_pulse.io.src_pulse				:= h2m_cmd_buffer.io.out.fire()
	h2m.io.start								:= withClockAndReset(hbm_clk,!hbm_rstn){RegNext(h2m_start_pulse.io.dest_pulse)}

	val last_h2m_fire							= withClockAndReset(user_clk,!user_rstn){RegNext(h2d_cmd_queue.io.cmd_out.fire())}
	h2d_cmd_queue.io.cmd_out.ready				:= h2c.io.complete & h2m_cmd_buffer.io.in.ready & (!h2d_cmd_queue.io.last | (h2d_cmd_queue.io.last & h2m_clear.io.dest_out))

	//m2h
	val m2h_start_addr							= cdc_array(64)(user_clk,hbm_clk)
	val m2h_length								= cdc_array(32)(user_clk,hbm_clk)
	val m2h_start_pulse							= cdc_pulse(user_clk,hbm_clk)
	val m2h_complete							= cdc_array(1)(hbm_clk,user_clk)
	m2h_complete.io.src_in						:= m2h.io.complete


	m2h_start_addr.io.src_in					:= d2h_cmd_queue.io.cmd_out.bits.m2h_start_addr
	m2h_length.io.src_in						:= d2h_cmd_queue.io.cmd_out.bits.m2h_length

	m2h_start_addr.io.dest_out					<> m2h.io.start_addr
	m2h_length.io.dest_out						<> m2h.io.length
	m2h.io.start								:= withClockAndReset(hbm_clk,!hbm_rstn){RegNext(m2h_start_pulse.io.dest_pulse)}

	val last_m2h_fire							= withClockAndReset(user_clk,!user_rstn){RegNext(d2h_cmd_queue.io.cmd_out.fire())}
	d2h_cmd_queue.io.cmd_out.ready				:= c2h.io.complete & m2h_complete.io.dest_out & c2h_cpt.io.c2h_cpt_complete
	d2h_cmd_queue.io.c2h_finish					:= c2h.io.complete
	d2h_cmd_queue.io.m2h_finish					:= m2h_complete.io.dest_out
	m2h_start_pulse.io.src_pulse				:= d2h_cmd_queue.io.cmd_out.fire()

	val m2h_last								= cdc_array(1)(user_clk,hbm_clk)
	m2h_last.io.src_in							:= d2h_cmd_queue.io.last
	m2h_last.io.dest_out						<> m2h.io.last
	val m2h_read_count_equal					= cdc_array(1)(hbm_clk,user_clk)
	m2h_read_count_equal.io.src_in				:= m2h.io.read_count_equal
	m2h_read_count_equal.io.dest_out			<> d2h_cmd_queue.io.read_count_equal

	h2d_cmd_queue.io.counter <> status_reg(61)
	d2h_cmd_queue.io.counter <> status_reg(81)

//------------------ c2h_status module (pcie_clk)

	val c2h_status								= withClockAndReset(pcie_clk, !pcie_rstn){Module(new c2h_status())}
	val c2h_status_start						= cdc_array(1)(user_clk,pcie_clk)
	c2h_status_start.io.src_in					:= d2h_cmd_queue.io.cmd_out.fire()
	c2h_status.io.c2h_start						<> c2h_status_start.io.dest_out
	qdma.io.c2h_status_cmp						<> c2h_status.io.c2h_status_cmp
	qdma.io.c2h_status_last						<> c2h_status.io.c2h_status_last
	qdma.io.c2h_status_valid					<> c2h_status.io.c2h_status_valid
	qdma.io.c2h_status_error					<> c2h_status.io.c2h_status_error
	qdma.io.c2h_status_drop						<> c2h_status.io.c2h_status_drop
	val last_count								= cdc_array(32)(pcie_clk,user_clk)
	val cmp_count								= cdc_array(32)(pcie_clk,user_clk)
	val valid_count								= cdc_array(32)(pcie_clk,user_clk)
	val error_count								= cdc_array(32)(pcie_clk,user_clk)
	val drop_count								= cdc_array(32)(pcie_clk,user_clk)
	last_count.io.src_in						:= c2h_status.io.c2h_status_last_count
	last_count.io.dest_out						<> status_reg(75)	
	cmp_count.io.src_in							:= c2h_status.io.c2h_status_cmp_count		
	cmp_count.io.dest_out						<> status_reg(76)
	valid_count.io.src_in						:= c2h_status.io.c2h_status_valid_count	
	valid_count.io.dest_out						<> status_reg(77)
	error_count.io.src_in						:= c2h_status.io.c2h_status_error_count
	error_count.io.dest_out						<> status_reg(78)
	drop_count.io.src_in						:= c2h_status.io.c2h_status_drop_count
	drop_count.io.dest_out						<> status_reg(79)
	qdma.io.tlb_miss_count						<> status_reg(40)

//------------------ FIFO for H2M & M2H definition (user_clk <> hbm_clk)

	val FIFO_reset = false.B
	// val FIFO_reset = (control_reg(500)===1.U)

	val h2m_queue = Module(new xpm_fifo_async(512,256,512))
	val m2h_queue = Module(new xpm_fifo_async(256,512,512))

	// the output of h2c DMA is written into H2M FIFO

	h2m_queue.io.rst							:= 0.U
	h2m_queue.io.wr_clk							:= user_clk
	h2m_queue.io.wr_en							:= qdma.io.h2c_data.valid
	h2m_queue.io.din							:= qdma.io.h2c_data.bits.data
	qdma.io.h2c_data.ready						:= !h2m_queue.io.almost_full & !h2m_queue.io.full

	h2m_queue.io.rd_clk							:= hbm_clk
	h2m_queue.io.rd_en							:= Mux(h2m.io.fifo_rden, hbmDriver.io.axi_hbm(0).w.ready, 0.U)
	h2m_queue.io.dout							<> hbmDriver.io.axi_hbm(0).w.bits.data
	val h2m_valid_tmpreg						= withClockAndReset(hbm_clk,!hbm_rstn){Module(new validreg())}
	h2m_valid_tmpreg.io.ready					:= Mux(h2m.io.fifo_rden, hbmDriver.io.axi_hbm(0).w.ready, 0.U)
	h2m_valid_tmpreg.io.valid					:= h2m_queue.io.data_valid
	hbmDriver.io.axi_hbm(0).w.valid 			<> Mux(h2m.io.fifo_rden, (h2m_queue.io.data_valid + h2m_valid_tmpreg.io.tmpreg), 0.U)

	// the output of M2H FIFO is written into c2h DMA

	m2h_queue.io.rst							:= 0.U
	m2h_queue.io.wr_clk 						:= hbm_clk
	m2h_queue.io.wr_en 							:= hbmDriver.io.axi_hbm(0).r.valid
	m2h_queue.io.din							<> hbmDriver.io.axi_hbm(0).r.bits.data
	hbmDriver.io.axi_hbm(0).r.ready				:= !m2h_queue.io.almost_full & !m2h_queue.io.full

	m2h_queue.io.rd_clk							:= user_clk
	m2h_queue.io.rd_en							:= qdma.io.c2h_data.ready & !c2h_cpt.io.c2h_cmd.valid
	qdma.io.c2h_data.bits.data					:= Mux(c2h_cpt.io.c2h_data.valid, c2h_cpt.io.c2h_data.bits.data, m2h_queue.io.dout)
	val m2h_valid_tmpreg						= withClockAndReset(user_clk,!user_rstn){Module(new validreg())}
	m2h_valid_tmpreg.io.ready					:= qdma.io.c2h_data.ready & !c2h_cpt.io.c2h_cmd.valid
	m2h_valid_tmpreg.io.valid					:= m2h_queue.io.data_valid
	qdma.io.c2h_data.valid 						:= Mux(c2h_cpt.io.c2h_data.valid, c2h_cpt.io.c2h_data.valid, m2h_queue.io.data_valid + m2h_valid_tmpreg.io.tmpreg)

	d2h_cmd_queue.io.empty						:= m2h_queue.io.empty
	val m2h_queue_empty							= cdc_array(1)(user_clk,hbm_clk)
	m2h_queue_empty.io.src_in					:= m2h_queue.io.empty
	m2h.io.m2h_queue_empty						<> m2h_queue_empty.io.dest_out
	val m2h_valid_tmpreg_tom2h					= cdc_array(1)(user_clk,hbm_clk)
	m2h_valid_tmpreg_tom2h.io.src_in			:= m2h_valid_tmpreg.io.tmpreg
	m2h.io.m2h_valid_tmpreg						<> m2h_valid_tmpreg_tom2h.io.dest_out
	d2h_cmd_queue.io.m2h_valid_tmpreg			:= m2h_valid_tmpreg.io.tmpreg

//------------------ Connection of h2m&m2h_queue, h2m&m2h and HBM_AXI(256 bit)

	// *** write data channel, the output of h2m FIFO is the input of HBM-AXI
	hbmDriver.io.axi_hbm(0).w.bits.strb 		:= Fill(32,1.U)
	h2m.io.wlast								<> hbmDriver.io.axi_hbm(0).w.bits.last
	h2m.io.wfire								:= hbmDriver.io.axi_hbm(0).w.fire()

	// *** write address channel, provided by h2m(H2M) module
	h2m.io.awaddr								<> hbmDriver.io.axi_hbm(0).aw.bits.addr
	h2m.io.awvalid								<> hbmDriver.io.axi_hbm(0).aw.valid
	h2m.io.awready								<> hbmDriver.io.axi_hbm(0).aw.ready
	// hbmDriver.io.axi_hbm(0).aw.bits.id     	<> "b111111".U
	h2m.io.awlen								<> hbmDriver.io.axi_hbm(0).aw.bits.len
	hbmDriver.io.axi_hbm(0).aw.bits.size		:= "b101".U
	hbmDriver.io.axi_hbm(0).aw.bits.burst		:= "b01".U

	// *** write response channel 
	// hbmDriver.io.axi_hbm(0).b.bits.resp		<> 
	// hbmDriver.io.axi_hbm(0).b.valid			<> 
	// hbmDriver.io.axi_hbm(0).b.bits.id		<> "b111111".U
	hbmDriver.io.axi_hbm(0).b.ready				:= 1.U

	// *** read address channel, provided by m2h(M2H) module
	m2h.io.araddr								<> hbmDriver.io.axi_hbm(0).ar.bits.addr
	m2h.io.arvalid								<> hbmDriver.io.axi_hbm(0).ar.valid
	m2h.io.arready								<> hbmDriver.io.axi_hbm(0).ar.ready
	m2h.io.rfire								:= hbmDriver.io.axi_hbm(0).r.fire()
	// hbmDriver.io.axi_hbm(0).ar.bits.id     	<> "b111111".U
	m2h.io.arlen								<> hbmDriver.io.axi_hbm(0).ar.bits.len
	hbmDriver.io.axi_hbm(0).ar.bits.size		:= "b101".U
	hbmDriver.io.axi_hbm(0).ar.bits.burst		:= "b01".U

	// *** read data channel, the output of HBM read is the input of m2h FIFO
	// hbmDriver.io.axi_hbm(0).r.bits.id      	<> "b111111".U
	
	
//------------------ PCIe AXIB to HBM (pcie_clk to hbm_clk)
	val axicc = Module(new AXIClockConverterBlackBox())
	val axib = qdma.io.axib

	axicc.io.s_axi_aclk				:= pcie_clk
	axicc.io.s_axi_aresetn			:= pcie_rstn

	axib.aw.bits.addr				<> axicc.io.s_axi_awaddr
	axib.aw.bits.len				<> axicc.io.s_axi_awlen
	axib.aw.bits.size				<> axicc.io.s_axi_awsize
	axib.aw.bits.burst				<> axicc.io.s_axi_awburst
	axib.aw.valid					<> axicc.io.s_axi_awvalid
	axib.aw.ready					<> axicc.io.s_axi_awready
	axib.w.bits.data				<> axicc.io.s_axi_wdata
	axib.w.bits.strb				<> axicc.io.s_axi_wstrb
	axib.w.bits.last				<> axicc.io.s_axi_wlast
	axib.w.valid					<> axicc.io.s_axi_wvalid
	axib.w.ready					<> axicc.io.s_axi_wready
	axib.ar.bits.addr				<> axicc.io.s_axi_araddr
	axib.ar.bits.len				<> axicc.io.s_axi_arlen
	axib.ar.bits.size				<> axicc.io.s_axi_arsize
	axib.ar.bits.burst				<> axicc.io.s_axi_arburst
	axib.ar.valid					<> axicc.io.s_axi_arvalid
	axib.ar.ready					<> axicc.io.s_axi_arready
	axib.r.bits.data				<> axicc.io.s_axi_rdata
	axib.r.bits.last				<> axicc.io.s_axi_rlast
	axib.r.valid					<> axicc.io.s_axi_rvalid
	axib.r.ready					<> axicc.io.s_axi_rready
	axib.b.valid					<> axicc.io.s_axi_bvalid
	axib.b.ready					<> axicc.io.s_axi_bready

	axib.r.bits.user				:= 0.U
	axib.r.bits.id					:= 0.U
	axib.r.bits.resp				:= 0.U
	axib.b.bits.user				:= 0.U
	axib.b.bits.id					:= 0.U
	axib.b.bits.resp				:= 0.U
	axicc.io.s_axi_awprot			:= 0.U
	axicc.io.s_axi_awlock			:= 0.U
	axicc.io.s_axi_awcache			:= 0.U
	axicc.io.s_axi_awregion 		:= 0.U
	axicc.io.s_axi_awqos			:= 0.U
	axicc.io.s_axi_arprot			:= 0.U
	axicc.io.s_axi_arlock			:= 0.U
	axicc.io.s_axi_arcache			:= 0.U
	axicc.io.s_axi_arregion			:= 0.U
    axicc.io.s_axi_arqos			:= 0.U
	axicc.io.s_axi_bready			:= 1.U

	val axidwc = Module(new AXIDataWidthConverterBlackBox())

	axicc.io.m_axi_aclk				:= hbm_clk
	axicc.io.m_axi_aresetn			:= hbm_rstn
	axidwc.io.s_axi_aclk			:= hbm_clk
	axidwc.io.s_axi_aresetn			:= hbm_rstn

    axicc.io.m_axi_awaddr    		<> axidwc.io.s_axi_awaddr
    axicc.io.m_axi_awlen     		<> axidwc.io.s_axi_awlen
    axicc.io.m_axi_awsize    		<> axidwc.io.s_axi_awsize
    axicc.io.m_axi_awburst   		<> axidwc.io.s_axi_awburst
    axicc.io.m_axi_awvalid   		<> axidwc.io.s_axi_awvalid
    axicc.io.m_axi_awready   		<> axidwc.io.s_axi_awready
    axicc.io.m_axi_wdata     		<> axidwc.io.s_axi_wdata
    axicc.io.m_axi_wstrb     		<> axidwc.io.s_axi_wstrb
    axicc.io.m_axi_wlast     		<> axidwc.io.s_axi_wlast
    axicc.io.m_axi_wvalid    		<> axidwc.io.s_axi_wvalid
    axicc.io.m_axi_wready    		<> axidwc.io.s_axi_wready
    axicc.io.m_axi_araddr    		<> axidwc.io.s_axi_araddr
    axicc.io.m_axi_arlen     		<> axidwc.io.s_axi_arlen
    axicc.io.m_axi_arsize    		<> axidwc.io.s_axi_arsize
    axicc.io.m_axi_arburst   		<> axidwc.io.s_axi_arburst
    axicc.io.m_axi_arvalid   		<> axidwc.io.s_axi_arvalid
    axicc.io.m_axi_arready   		<> axidwc.io.s_axi_arready
    axicc.io.m_axi_rdata     		<> axidwc.io.s_axi_rdata
    axicc.io.m_axi_rlast     		<> axidwc.io.s_axi_rlast
    axicc.io.m_axi_rvalid    		<> axidwc.io.s_axi_rvalid
    axicc.io.m_axi_rready    		<> axidwc.io.s_axi_rready
	axicc.io.m_axi_bvalid			<> axidwc.io.s_axi_bvalid
	axicc.io.m_axi_bready			<> axidwc.io.s_axi_bready

	axidwc.io.s_axi_awprot			:= 0.U
	axidwc.io.s_axi_awlock			:= 0.U
	axidwc.io.s_axi_awcache			:= 0.U
	axidwc.io.s_axi_awregion 		:= 0.U
	axidwc.io.s_axi_awqos			:= 0.U
	axidwc.io.s_axi_arprot			:= 0.U
	axidwc.io.s_axi_arlock			:= 0.U
	axidwc.io.s_axi_arcache			:= 0.U
	axidwc.io.s_axi_arregion		:= 0.U
    axidwc.io.s_axi_arqos			:= 0.U
	axidwc.io.s_axi_bready			:= 1.U

    axidwc.io.m_axi_awaddr    		<> hbmDriver.io.axi_hbm(1).aw.bits.addr
    axidwc.io.m_axi_awlen     		<> hbmDriver.io.axi_hbm(1).aw.bits.len
    axidwc.io.m_axi_awsize    		<> hbmDriver.io.axi_hbm(1).aw.bits.size
    axidwc.io.m_axi_awburst   		<> hbmDriver.io.axi_hbm(1).aw.bits.burst
    axidwc.io.m_axi_awvalid   		<> hbmDriver.io.axi_hbm(1).aw.valid
    axidwc.io.m_axi_awready   		<> hbmDriver.io.axi_hbm(1).aw.ready
    axidwc.io.m_axi_wdata     		<> hbmDriver.io.axi_hbm(1).w.bits.data
    axidwc.io.m_axi_wstrb     		<> hbmDriver.io.axi_hbm(1).w.bits.strb
    axidwc.io.m_axi_wlast     		<> hbmDriver.io.axi_hbm(1).w.bits.last
    axidwc.io.m_axi_wvalid    		<> hbmDriver.io.axi_hbm(1).w.valid
    axidwc.io.m_axi_wready    		<> hbmDriver.io.axi_hbm(1).w.ready
    axidwc.io.m_axi_araddr    		<> hbmDriver.io.axi_hbm(1).ar.bits.addr
    axidwc.io.m_axi_arlen     		<> hbmDriver.io.axi_hbm(1).ar.bits.len
    axidwc.io.m_axi_arsize    		<> hbmDriver.io.axi_hbm(1).ar.bits.size
    axidwc.io.m_axi_arburst   		<> hbmDriver.io.axi_hbm(1).ar.bits.burst
    axidwc.io.m_axi_arvalid   		<> hbmDriver.io.axi_hbm(1).ar.valid
    axidwc.io.m_axi_arready   		<> hbmDriver.io.axi_hbm(1).ar.ready
    axidwc.io.m_axi_rdata     		<> hbmDriver.io.axi_hbm(1).r.bits.data
    axidwc.io.m_axi_rlast     		<> hbmDriver.io.axi_hbm(1).r.bits.last
    axidwc.io.m_axi_rvalid    		<> hbmDriver.io.axi_hbm(1).r.valid
    axidwc.io.m_axi_rready    		<> hbmDriver.io.axi_hbm(1).r.ready
	axidwc.io.m_axi_bvalid			<> hbmDriver.io.axi_hbm(1).b.valid
	axidwc.io.m_axi_bready			<> hbmDriver.io.axi_hbm(1).b.ready

	
	val p2p_counter 				= withClockAndReset(hbm_clk,!hbm_rstn){Module(new p2p_counter())}
	p2p_counter.io.debug_clk 		:= hbm_clk
	p2p_counter.io.wready			:= hbmDriver.io.axi_hbm(1).w.ready
	p2p_counter.io.wvalid			:= hbmDriver.io.axi_hbm(1).w.valid
	p2p_counter.io.wdatasample		:= hbmDriver.io.axi_hbm(1).w.bits.data(31,0)

	val p2p_cpt_addr				= cdc_array(64)(user_clk,hbm_clk)
	val p2p_length					= cdc_array(32)(user_clk,hbm_clk)
	val p2p_start					= cdc_array(1)(user_clk,hbm_clk)
	val p2p_complete				= cdc_pulse(hbm_clk,user_clk)
	val p2p_cpt_complete			= cdc_pulse(user_clk,hbm_clk)

	p2p_start.io.src_in 			:= withClockAndReset(user_clk,!user_rstn){(RegNext(RegNext(RegNext(control_reg(91)===1.U))))}
	p2p_counter.io.start			:= withClockAndReset(hbm_clk,!hbm_rstn){RegNext(p2p_start.io.dest_out)}
	p2p_cpt_addr.io.src_in			:= Cat(control_reg(92), control_reg(93))
	p2p_counter.io.cpt_addr			:= p2p_cpt_addr.io.dest_out
	p2p_length.io.src_in			:= control_reg(94)
	p2p_counter.io.length			:= p2p_length.io.dest_out
	p2p_complete.io.src_pulse		:= p2p_counter.io.p2p_complete
	c2h_cpt.io.p2p_cpt_addr			:= Cat(control_reg(92), control_reg(93))
	c2h_cpt.io.p2p_complete			:= p2p_complete.io.dest_pulse
	p2p_cpt_complete.io.src_pulse	:= c2h_cpt.io.p2p_cpt_complete
	p2p_cpt_complete.io.dest_pulse	<> p2p_counter.io.p2p_cpt_complete



//------------------ ILAs
// class ila_hbm(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_hbm = Module(new ila_hbm(Seq(	

// 	hbmDriver.io.axi_hbm(1).aw.ready,
// 	hbmDriver.io.axi_hbm(1).aw.valid,
// 	hbmDriver.io.axi_hbm(1).w.ready,
// 	hbmDriver.io.axi_hbm(1).w.valid,
// 	hbmDriver.io.axi_hbm(1).ar.ready,
// 	hbmDriver.io.axi_hbm(1).ar.valid,
// 	hbmDriver.io.axi_hbm(1).r.ready,
// 	hbmDriver.io.axi_hbm(1).r.valid,
	
// )))
// inst_ila_hbm.connect(hbmDriver.io.hbm_clk)

// class ila_user(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_user = Module(new ila_user(Seq(	

// 	wrapper.io.m_axi_gmem_AWVALID,
// 	wrapper.io.m_axi_gmem_AWREADY,
// 	wrapper.io.m_axi_gmem_WVALID,
// 	wrapper.io.m_axi_gmem_WREADY,
// 	wrapper.io.m_axi_gmem_ARVALID,
// 	wrapper.io.m_axi_gmem_ARREADY,
// 	wrapper.io.m_axi_gmem_RVALID,
// 	wrapper.io.m_axi_gmem_RREADY,

// )))
// inst_ila_user.connect(user_clk)

// class ila_axib(seq:Seq[Data]) extends BaseILA(seq)
// val inst_ila_axib = Module(new ila_axib(Seq(	

// 	axib.aw.ready,
// 	axib.aw.valid,
// 	axib.ar.ready,
// 	axib.ar.valid,
// 	axib.w.ready,
// 	axib.w.valid,
// 	axib.r.ready,
// 	axib.r.valid,

// )))
// inst_ila_axib.connect(pcie_clk)

}
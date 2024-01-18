package CommSub

import chisel3._
import chisel3.util._
import common.storage._
import common._

class WR_TLB extends Bundle{
	val vaddr_high	= UInt(32.W)
	val vaddr_low	= UInt(32.W)
	val paddr_high	= UInt(32.W)
	val paddr_low	= UInt(32.W)
	val is_base		= UInt(1.W)
	//is_base :: paddr :: vaddr
}

class TLB extends Module{
	val io	= IO(new Bundle{
		val wr_tlb			= Flipped(Decoupled(new WR_TLB))
		val h2c_in			= Flipped(Decoupled(new H2C_CMD))
		val c2h_in			= Flipped(Decoupled(new C2H_CMD))

		val h2c_out			= Decoupled(new H2C_CMD)
		val c2h_out			= Decoupled(new C2H_CMD)

		val tlb_miss_count	= Output(UInt(32.W))
	})

	val entry_num			= 16*1024
	val PAGE_SHIFT			= 21
	val Q_DEPTH				= 10

	val base_page 			= RegInit(UInt((64-PAGE_SHIFT).W),0.U)
	val tlb_miss_count 		= RegInit(UInt(32.W),0.U)

	val wrtlb_index			= RegInit(UInt((log2Up(entry_num+1)).W),0.U)//must add 1 to avoid it overflows when pages==capacity
	val h2c_page 			= io.h2c_in.bits.addr>>PAGE_SHIFT
	val h2c_index			= h2c_page - base_page
	val h2c_outrange		= (h2c_page < base_page) | (h2c_page >= base_page+wrtlb_index)
	val c2h_page 			= io.c2h_in.bits.addr>>PAGE_SHIFT
	val c2h_index			= c2h_page - base_page
	val c2h_outrange		= (c2h_page < base_page) | (c2h_page >= base_page+wrtlb_index)

	val tlb_table			=  XRam(UInt(64.W),entry_num)

	
	io.wr_tlb.ready			:= true.B//always high for write ram
	tlb_table.io.wr_en_a	:= io.wr_tlb.fire()
	tlb_table.io.data_in_a	:= Cat(io.wr_tlb.bits.paddr_high, io.wr_tlb.bits.paddr_low)//paddr 127:64

	when(io.wr_tlb.fire()){
		tlb_table.io.addr_a		:= wrtlb_index
		wrtlb_index				:= wrtlb_index + 1.U
		when(io.wr_tlb.bits.is_base===1.U){
			base_page			:= Cat(io.wr_tlb.bits.vaddr_high, io.wr_tlb.bits.vaddr_low) >> PAGE_SHIFT
			tlb_table.io.addr_a	:= 0.U
			wrtlb_index			:= 1.U
			tlb_miss_count		:= 0.U
		}
	}.otherwise{
		tlb_table.io.addr_a	:= h2c_index
	}
	tlb_table.io.addr_b		:= c2h_index

	val h2c_bits_delay		= Wire(new H2C_CMD)
	val c2h_bits_delay		= Wire(new C2H_CMD)
	h2c_bits_delay			:= RegNext(RegNext(io.h2c_in.bits))
	c2h_bits_delay			:= RegNext(RegNext(io.c2h_in.bits))
	h2c_bits_delay.addr 	:= tlb_table.io.data_out_a + RegNext(RegNext(io.h2c_in.bits.addr(PAGE_SHIFT-1,0)))
	c2h_bits_delay.addr 	:= tlb_table.io.data_out_b + RegNext(RegNext(io.c2h_in.bits.addr(PAGE_SHIFT-1,0)))
	val q_h2c				= Module(new Queue(new H2C_CMD,Q_DEPTH))
	val q_c2h				= Module(new Queue(new C2H_CMD,Q_DEPTH))

	io.h2c_in.ready			:= q_h2c.io.count < (Q_DEPTH-2).U
	io.c2h_in.ready			:= q_c2h.io.count < (Q_DEPTH-2).U

	when(RegNext(RegNext(io.h2c_in.valid)) && RegNext(RegNext(io.h2c_in.ready)) && !RegNext(RegNext(h2c_outrange))){
		q_h2c.io.enq.valid 	:= true.B
	}.otherwise{
		q_h2c.io.enq.valid 	:= false.B
	}
	
	when(RegNext(RegNext(io.c2h_in.valid)) && RegNext(RegNext(io.c2h_in.ready)) && !RegNext(RegNext(c2h_outrange))){
		q_c2h.io.enq.valid 	:= true.B
	}.otherwise{
		q_c2h.io.enq.valid 	:= false.B
	}

	val h2c_miss	= h2c_outrange & io.h2c_in.valid & io.h2c_in.ready
	val c2h_miss	= c2h_outrange & io.c2h_in.valid & io.c2h_in.ready
	when(h2c_miss | c2h_miss){
		tlb_miss_count	:= tlb_miss_count+1.U
		when(h2c_miss & c2h_miss){
			tlb_miss_count	:= tlb_miss_count+2.U
		}
	}

	q_h2c.io.enq.bits	:= h2c_bits_delay
	io.h2c_out			<> q_h2c.io.deq

	q_c2h.io.enq.bits	:= c2h_bits_delay
	io.c2h_out			<> q_c2h.io.deq

	io.tlb_miss_count	:= tlb_miss_count

}
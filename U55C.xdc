create_clock -name sys_clk -period 10 [get_ports qdma_pin_sys_clk_p]

set_false_path -from [get_ports qdma_pin_sys_rst_n]
set_property PULLUP true [get_ports qdma_pin_sys_rst_n]
set_property IOSTANDARD LVCMOS18 [get_ports qdma_pin_sys_rst_n]
set_property PACKAGE_PIN BF41 [get_ports qdma_pin_sys_rst_n]
set_property CONFIG_VOLTAGE 1.8 [current_design]

set_property LOC [get_package_pins -of_objects [get_bels [get_sites -filter {NAME =~ *COMMON*} -of_objects [get_iobanks -of_objects [get_sites GTYE4_CHANNEL_X1Y7]]]/REFCLK0P]] [get_ports qdma_pin_sys_clk_p]
set_property LOC [get_package_pins -of_objects [get_bels [get_sites -filter {NAME =~ *COMMON*} -of_objects [get_iobanks -of_objects [get_sites GTYE4_CHANNEL_X1Y7]]]/REFCLK0N]] [get_ports qdma_pin_sys_clk_n]

#set_property PACKAGE_PIN D32 [get_ports led]
#set_property IOSTANDARD  LVCMOS18 [get_ports led]

create_clock -name sys_100M_clock_0 -period 10 -add [get_ports sys_100M_0_p]

set_property PACKAGE_PIN BK10 [get_ports sys_100M_0_p]
set_property PACKAGE_PIN BL10 [get_ports sys_100M_0_n]
set_property IOSTANDARD  DIFF_SSTL12 [get_ports sys_100M_0_p]
set_property IOSTANDARD  DIFF_SSTL12 [get_ports sys_100M_0_n]

set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmAxi/mmcm4_adv/CLKOUT0}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmGlbl/mmcm4_adv/CLKOUT0}]]
set_false_path -to [get_pins -hier hbm_rstn_reg/D]

set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmGlbl/mmcm4_adv/CLKOUT0}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]]
set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmGlbl/mmcm4_adv/CLKOUT0}]]
set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmAxi/mmcm4_adv/CLKOUT0}]]
set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */mmcmAxi/mmcm4_adv/CLKOUT0}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]]

set_false_path -from [get_cells -regexp {qdma/axil2reg/reg_control_[0-9]*_reg\[.*]}]
set_false_path -to [get_cells -regexp {qdma/axil2reg/reg_status_[0-9]*_reg\[.*]}]
#reg_control_0_reg[0]
#set_false_path -from [get_cells qdma/axil2reg/reg_control_[*]]
#set_false_path -to [get_cells qdma/axil_reg_0/reg_status_[*]]

###
set_false_path -to [get_pins -hier *sync_reg[0]/D]
###
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
#connect_debug_port dbg_hub/clk [get_nets dbg_clk_pad_O]

set_false_path -from [get_pins -hier apb_complete_0_r_reg/C] -to [get_pins -hier apb_complete_0_reg/D]
set_false_path -from [get_pins -hier apb_complete_1_r_reg/C] -to [get_pins -hier apb_complete_1_reg/D]

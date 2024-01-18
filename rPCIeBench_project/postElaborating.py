import sys
import os
import subprocess
import getopt
import json
import re

class Moniter():
	def __init__(self, name):
		self.name = name
		self.buses = []
		self.instName = None
		self.widths = []
		if name.startswith("ila"):
			self.type = "ila"
		elif name.startswith("vio"):
			self.type = "vio"
		else:
			print("Error ipType")
			sys.exit(0)

class Bus():
	def __init__(self, name):
		self.name = name
		self.signals = []

class Signal():
	def __init__(self, name, width):
		self.name = name
		self.width = width
		
configFileName = "config.json"
configFile = open(configFileName)
config = json.load(configFile)
configFile.close()

projectName = str(sys.argv[1])
moduleName = str(sys.argv[2])
showTCL = False
postRun = False

if not projectName in config:
	print("Project not in config file!")
	sys.exit(0)

moniterDepth = config[projectName]["moniterDepth"] if "moniterDepth" in config[projectName] else 1024
moniterDelay = config[projectName]["moniterDelay"] if "moniterDelay" in config[projectName] else 0

argv = sys.argv[3:]

opts, args = getopt.getopt(argv, "tp")  # 短选项模式
for opt, arg in opts:
	if opt in ['-t']:
		showTCL = True
	elif opt in ['-p']:
		postRun = True

moniters = []

destIPRepoPath = ""
destSrcPath = ""

destIPRepoPath = config[projectName]["destIPRepoPath"]
destSrcPath = config[projectName]["destSrcPath"]

def get_width(str):
	l = str.split(' ')
	widthStr = list(filter(None,l))[1]
	if widthStr.startswith("["):
		w = widthStr.split(':')[0].replace('[','')
		return int(w)+1
	else:
		return 1

def append_wrapper_to_sv(wrapper):
	fileName = "Verilog/" + moduleName + ".sv"
	f = open(fileName,'a')
	f.write(wrapper)
	f.close()

# you must first add below code to your testbench before any print, and make sure fd == h80000003
# fd = $fopen("~/output", "w");
# $display("fd = %x\n",fd);
def replace_print():
	fileName = "Verilog/" + moduleName + ".sv"
	f = open(fileName,'r')
	lines = f.readlines()
	f.close()

	#fileName = destSrcPath+"/%s.sv"%moduleName
	fileName = "/home/hwt22/rPCIeBench_project/Verilog/Top.sv"
	print(fileName)
	f = open(fileName,'w+')
	for line in lines:
		l = line.replace("h80000002", "h80000003") #h80000001
		f.write(l)
	f.close()
	print("Done moving file")

def initial_moniters_from_txt():
	fileName = "Verilog/" + moduleName + ".txt"
	f = None
	try:
		f = open(fileName,'r')
	except IOError:
		return
	lines = f.readlines()
	signal_buses = [] 
	for l in lines:
		line = l.replace("\n","")
		
		if line.endswith(":"):
			moniterName = line[:-1]
			m = Moniter(moniterName)
			for bus in signal_buses:
				m.buses.append(Bus(bus))
			moniters.append(m)
			for bus in signal_buses:
				if signal_buses.count(bus) != 1:
					print(f"Bus/Signal name conflict! Duplicated buses/signals named {bus} in {moniterName} detected!")
					sys.exit(0)
			signal_buses = []
		else:
			signal_buses.append(line)
	f.close()

def parse_verilog():
	fileName = "Verilog/" + moduleName + ".sv"
	f = open(fileName,'r')
	lines = f.readlines()
	for line in lines:
		# Skip all lines not starting with "ila_" or "vio_"
		if not line.strip().startswith("ila_") and not line.strip().startswith("vio_"):
			continue
		for moniter in moniters:
			# Find lines such as below: 
			# ila_control_reg mod1 ( // @[QDMATop.scala 74:26]
			matchObj = re.match(rf"\s+{moniter.name} (\S+) \( // @\[.+\]", line)
			if matchObj != None:
				moniter.instName = matchObj.group(1)
	instNames = [moniter.instName for moniter in moniters]
	for instName in instNames:
		if instNames.count(instName) != 1:
			print(f"Instant name conflict! Duplicated instants named {instName} detected!")
			sys.exit(0)
	f.close()

def parse_width():
	# Pick and parse all signals within an moniter.
	fileName = "Verilog/" + moduleName + ".sv"
	f = open(fileName,'r')
	lines = f.readlines()
	for line in lines:
		# Skip all lines not starting with "wire".
		if not line.strip().startswith("wire"):
			continue
		for moniter in moniters:
			instName = moniter.instName
			buses = moniter.buses
			# Parse the wire name and see if match target pattern.
			matchObj = re.match(rf"\s+wire (|\[([0-9]+):0\]) {instName}_data_([0-9]+)(\S*);", line)
			if matchObj != None:
				# Signal found.
				width = matchObj.group(2)
				busId = matchObj.group(3)
				signalName = matchObj.group(4)
				if (width != None and not width.isdigit()):
					break
				if (not busId.isdigit() or int(busId) >= len(buses)):
					break
				busId = int(busId)
				if (width == None):
					width = 1
				else:
					width = int(width) + 1
				moniter.buses[busId].signals.append(Signal(signalName, width))

def generate_wrapper(moniter):
	ip_name = moniter.name
	wrapper = f"module {ip_name}(\n"
	wrapper += "input clk,\n"
	signal_names = []
	signal_widths = []
	target_names = []
	# Sort signals in each bus to make easier to read.
	for bus in moniter.buses:
		sorted(bus.signals, key=lambda x: x.name)
	for busIdx, bus in enumerate(moniter.buses):
		for signal in bus.signals:
			signal_name = f"data_{busIdx}{signal.name}"
			if signal_name in signal_names:
				print(f"Pin name conflict! Duplicated pins named {signal_name} in {ip_name} detected!")
				sys.exit(0)
			signal_names.append(signal_name)
			signal_widths.append(signal.width)
			target_name = f"{bus.name}{signal.name}"
			if target_name in target_names:
				print(f"Signal name conflict! Duplicated signals named {target_name} in {ip_name} detected!")
				sys.exit(0)
			target_names.append(target_name)
	io = "input" if moniter.type=="ila"  else "output"
	probe_cnt = len(signal_names)
	for i in range(probe_cnt):
		name = signal_names[i]
		width = signal_widths[i]
		if i != probe_cnt - 1:
			wrapper += "%s [%d:0] %s,\n"%(io,width-1,name)
		else:
			wrapper += "%s [%d:0] %s);\n"%(io,width-1,name)
	converts = ""
	if moniter.type=="ila":
		for i in range(probe_cnt):
			converts += f"wire [{signal_widths[i]-1}:0] {target_names[i]} = {signal_names[i]};\n"
	else:
		for i in range(probe_cnt):
			converts += f"(* keep = \"true\" *)wire [{signal_widths[i]-1}:0] {target_names[i]};\n"
			converts += f"assign {target_names[i]} = {signal_names[i]};\n"
	instance = f"{ip_name}_inner inst_{ip_name}(\n.clk(clk),\n"

	for i in range(probe_cnt):
		name = target_names[i]
		width = signal_widths[i]
		if moniter.type=="ila":
			if i != probe_cnt-1:
				instance += f".probe{i}({name}), //[{width-1}:0]\n"
			else:
				instance += f".probe{i}({name})); //[{width-1}:0]\n"
		else:
			if i != probe_cnt-1:
				instance += f".probe_out{i}({name}), //[{width-1}:0]\n"
			else:
				instance += f".probe_out{i}({name})); //[{width-1}:0]\n"
	moniter.widths = signal_widths
	return wrapper + "\n" + converts + "\n" + instance + "endmodule\n"

def generate_tcl(moniter):
	widths = moniter.widths
	tcl = ""
	if moniter.type == "ila":
		ip_name = moniter.name+"_inner"
		tcl1 = "create_ip -name ila -vendor xilinx.com -library ip -version 6.2 -module_name "+ip_name
		tcl2 = "set_property -dict [list "
		tcl2 += "CONFIG.C_INPUT_PIPE_STAGES {%d} "%(moniterDelay)
		for i in range(len(widths)):
			tcl2 += "CONFIG.C_PROBE%d_WIDTH {%d} "%(i,widths[i])
		tcl2+="CONFIG.C_DATA_DEPTH {%d} CONFIG.C_NUM_OF_PROBES {%d} ] [get_ips %s]"%(moniterDepth,len(widths),ip_name)
		tcl3 = "generate_target {instantiation_template} [get_files %s/%s/%s.xci]"%(destIPRepoPath,ip_name,ip_name)
		tcl4 = "update_compile_order -fileset sources_1\n\n"
		tcl = tcl1+"\n"+tcl2+"\n"+tcl3+"\n"+tcl4
	else:
		ip_name = moniter.name+"_inner"
		tcl1 = "create_ip -name vio -vendor xilinx.com -library ip -version 3.0 -module_name "+ip_name
		tcl2 = "set_property -dict [list "
		tcl2 += "CONFIG.C_PROBE_OUT0_INIT_VAL {0x0} "
		for i in range(len(widths)):
			tcl2 += "CONFIG.C_PROBE_OUT%d_WIDTH {%d} "%(i,widths[i])
		tcl2+="CONFIG.C_NUM_PROBE_OUT {%d} CONFIG.C_EN_PROBE_IN_ACTIVITY {0} CONFIG.C_NUM_PROBE_IN {0}] [get_ips %s]"%(len(widths),ip_name)
		tcl3 = "generate_target {instantiation_template} [get_files %s/%s/%s.xci]"%(destIPRepoPath,ip_name,ip_name)
		tcl4 = "update_compile_order -fileset sources_1\n\n"
		tcl = tcl1+"\n"+tcl2+"\n"+tcl3+"\n"+tcl4
	return tcl
def post_run():
	initial_moniters_from_txt()
	parse_verilog()
	parse_width()

	wrapperStr = ""
	for moniter in moniters:
		wrapperStr+= generate_wrapper(moniter)
	append_wrapper_to_sv(wrapperStr)

	for moniter in moniters:
		tcl = generate_tcl(moniter)
		if showTCL:
			print(tcl)
	
	replace_print()


try:
	os.remove("Verilog/"+moduleName+".txt")
	print("txt file deleted")
except:
	print("No txt file to be deleted")

print("Running mill " + projectName+" " + moduleName)
p = subprocess.Popen(["mill", projectName, moduleName])
p.wait()
ret = p.returncode

if ret != 0:
	print("Mill compiling error!")
else:
	if postRun == True:
		post_run()

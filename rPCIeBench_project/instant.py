import sys

# expected_module = "CUCKOO"
expected_module = sys.argv[1]

##############
start_str = """
`timescale 1ns / 1ns
module tb_%s(

    );
"""%expected_module

file_name = expected_module+".sv"
io_names = {}
bits_count = {}
io_instants = {}
global_width = {}

f = open("Verilog/"+file_name)

flag = 0
lines = []

sim = []
cur_module_name=""

g_in_modules = []
g_in_sig_names = []
g_in_sig_widths = []
g_out_modules = []

def is_start(line):
	if line.startswith("module"):
		return True
	else:
		return False

def is_end(line):
	if line.strip().endswith(");"):
		return True
	else:
		return False

def get_instance_start(line):
	global cur_module_name
	name = line.split()[1].strip('(')
	cur_module_name = name
	return name + " " + name + "_inst(\n"

def parse_width(s):
	s = s.replace('[','').replace(']','')
	s = int(s.split(':')[0]) + 1
	return s

def pre_scan(lines):
	names = []
	ios = {}
	for line in lines:
		res = res = line.split()
		io = res[0]
		name = ""
		for item in res[1:]:
			item = item.strip(",")
			if item=="reg" or item == "wire":
				print("Error") # impossible for generated verilog
				continue
			elif item.startswith("["):
				pass
			else:
				name = item
		names.append(name)
		ios[name] = io
	raw_io_names = {}
	for name in names:
		if name.endswith("_valid"):
			raw_io_names[name[:-6]] = 0
	for name in names:
		if name.endswith("_ready"):
			id = name[:-6]
			if id in raw_io_names.keys():
				raw_io_names[id] = 1
	ids = []
	inst_type = {}
	for k,v in raw_io_names.items():
		if v == 1:
			ids.append(k)
	for id in ids:
		if ios[id+"_valid"] == "input":
			inst_type[id] = "IN"
		elif ios[id+"_valid"] == "output":
			inst_type[id] = "OUT"
		else:
			print("Error")

	for id in ids:
		bits_name = id + "_bits"
		bits_count[bits_name] = 0
		io_instants[bits_name] = ""
		io_names[bits_name] = inst_type[id]
	print(inst_type)
def parse_line(line):
	res = line.split()
	io = res[0]
	var_type = ""
	postfix = ""
	if io=="input":
		var_type="reg"
		postfix="=0;"
	else:
		var_type="wire"
		postfix=";"
	width = ""
	width_int = 1
	name = ""
	for item in res[1:]:
		item = item.strip(",")
		if item=="reg" or item == "wire":
			print("Error") # impossible for generated verilog
			continue
		elif item.startswith("["):
			width = item
			width_int = parse_width(item)
		else:
			name = item
	inst_line = ".%s(%s),//%s %s\n"%(name,name,io,width)
	inst_line = "."+ name.ljust(30) + "(" + name.ljust(30)+"),//"+io+" "+width+"\n"
	sim_line = var_type.ljust(10) + width.ljust(10) + name.ljust(30)+postfix+"\n"
	sim.append(sim_line)

	for k,v in bits_count.items():
		if name.startswith(k):
			bits_count[k] = v + width_int
			io_instants[k] += name+","
			global_width[name] = width_int

def process(lines):
	#start line
	get_instance_start(lines[0])
	print("module " + cur_module_name + " found")
	lines = lines[1:]

	if cur_module_name == expected_module:
		pre_scan(lines)
		for line in lines:
			parse_line(line)

		print(start_str)
		for line in sim:
			print(line,end="")
		print()
		
		for k,v in bits_count.items():
			bits_name = k
			io_type = io_names[k]
			if not k.endswith("_bits"):
				print("Error")
			bundle_name = k[:-5]
			mod_name = io_type.lower()+"_"+ bundle_name
			
			print(io_type+"#(%d)%s"%(v,mod_name)+"(")
			print("\tclock,")
			print("\treset,")
			print("\t{" + io_instants[k][:-1] + "},") #-1 is to eliminate ,
			print("\t"+bundle_name+"_valid,")
			print("\t"+bundle_name+"_ready")
			print(");")

			print("// ", end="")
			names = [name[name.index("bits")+5:] for name in io_instants[k][:-1].split(",")]
			widths = [global_width[name] for name in io_instants[k][:-1].split(",")]
			width_str = [str(w)+"'h0" for w in widths]
			print(*names, sep=', ')
			print("// ", end="")
			print(*width_str, sep=', ')
			print()

			if io_type == "OUT":
				g_out_modules.append(mod_name)
			elif io_type == "IN":
				g_in_modules.append(mod_name)
				g_in_sig_names.append(names)
				g_in_sig_widths.append(width_str)

	print()


while True:
	line = f.readline()
	if line=="":
		break
	if line=="\n":
		continue
	if(is_start(line)):
		flag = 1
	if flag == 1 and is_end(line):
		flag = 0
		process(lines)
		sim=[]
		lines=[]
	if flag:
		lines.append(line)

# print module instant
print("%s %s_inst("%(expected_module, expected_module))
print("\t.*")
print(");\n")

print("/*")
for i in range(len(g_in_modules)):
	print(*g_in_sig_names[i],sep=',')
	print(g_in_modules[i]+".write({",end='')
	print(*g_in_sig_widths[i],sep=',',end='')
	print("});\n")
print("*/")
# print initial begin
initial_str0 = """
initial begin
	reset <= 1;
	clock = 1;
	#1000;
	reset <= 0;
	#100;"""

initial_str1 = """
end

always #5 clock=~clock;
endmodule
"""
print(initial_str0)
for name in g_out_modules:
	print("\t"+name+".start();")
print("\t#50;",end='')
print(initial_str1)

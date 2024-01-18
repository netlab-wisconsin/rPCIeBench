id=$1

echo 1024 > /sys/bus/pci/devices/0000\:$id\:00.0/qdma/qmax
dma-ctl qdma${id}000 q add idx 0 mode st dir bi
dma-ctl qdma${id}000 q start idx 0 dir bi desc_bypass_en pfetch_bypass_en
dma-ctl qdma${id}000 q add idx 1 mode st dir bi
dma-ctl qdma${id}000 q start idx 1 dir bi desc_bypass_en pfetch_bypass_en
dma-ctl qdma${id}000 q add idx 2 mode st dir bi
dma-ctl qdma${id}000 q start idx 2 dir bi desc_bypass_en pfetch_bypass_en
dma-ctl qdma${id}000 q add idx 3 mode st dir bi
dma-ctl qdma${id}000 q start idx 3 dir bi desc_bypass_en pfetch_bypass_en
